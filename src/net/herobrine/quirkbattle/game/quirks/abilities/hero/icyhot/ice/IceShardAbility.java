package net.herobrine.quirkbattle.game.quirks.abilities.hero.icyhot.ice;

import net.herobrine.gamecore.Class;
import net.herobrine.gamecore.GameState;
import net.herobrine.quirkbattle.QuirkBattlesPlugin;
import net.herobrine.quirkbattle.game.CustomDeathCause;
import net.herobrine.quirkbattle.game.quirks.abilities.Abilities;
import net.herobrine.quirkbattle.game.quirks.abilities.Ability;
import net.herobrine.quirkbattle.util.projectile.PortalProjectileService;
import net.herobrine.quirkbattle.util.Quirk;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.EulerAngle;
import org.bukkit.util.Vector;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class IceShardAbility extends Ability {

    private final Player player = Bukkit.getPlayer(quirk.getUniqueId());
    // Track which players have been hit by which projectile
    private final Map<ArmorStand, Set<UUID>> projectileHits = new HashMap<>();
    private PortalProjectileService portalService;

    public IceShardAbility(Abilities ability, Quirk quirk, int id, int slot) {
        super(ability, quirk, id, slot);
    }

    @Override
    public void doAbility(Player player) {
        // Initialize portal service
        portalService = new PortalProjectileService(arena.getQuirkBattleGame().getWarpGateManagers());

        // Clear hit tracking
        projectileHits.clear();

        Location eyeLocation = player.getEyeLocation();
        Vector directionVector = eyeLocation.getDirection();
        Location frontLocation = eyeLocation.add(directionVector);

        doVFX(frontLocation);
    }

    public void doVFX(Location location) {
        new BukkitRunnable() {
            int projectiles = 0;

            @Override
            public void run() {
                if (projectiles > 2) {
                    cancel();
                    return;
                }

                location.getWorld().playSound(location, Sound.GLASS, 1f, 1f);

                // Create armor stand projectile
                ArmorStand stand = (ArmorStand) location.getWorld().spawnEntity(location, EntityType.ARMOR_STAND);
                stand.setCustomNameVisible(false);
                stand.setCustomName(uuid.toString());
                stand.setBasePlate(false);
                stand.setGravity(false); // Changed to false for better control
                stand.setSmall(true);
                stand.setMarker(true);
                stand.setVisible(false);
                stand.setHeadPose(new EulerAngle(Math.random(), Math.random(), Math.random()));
                stand.setHelmet(new ItemStack(Material.PACKED_ICE));

                // Initialize hit tracking for this projectile
                projectileHits.put(stand, new HashSet<>());

                // Add slight spread to projectiles
                Vector spread = location.getDirection().clone();
                double spreadAmount = 0.1;
                spread.add(new Vector(
                        (Math.random() - 0.5) * spreadAmount,
                        (Math.random() - 0.5) * spreadAmount,
                        (Math.random() - 0.5) * spreadAmount
                ));

                startProjectileMovement(stand, spread.normalize());
                projectiles++;
            }
        }.runTaskTimer(QuirkBattlesPlugin.getInstance(), 0L, 5L);
    }

    public void startProjectileMovement(ArmorStand stand, Vector initialDirection) {
        // Create sequential projectile for portal support
        PortalProjectileService.SequentialProjectile projectile = portalService.createSequentialProjectile(
                stand.getLocation(),
                initialDirection,
                1.2, // Step size matching original velocity
                (location, direction, wentThroughPortal) -> {
                    if (wentThroughPortal) {
                        // Portal transition effects
                        spawnRGBParticles(location, 255, 255, 255, true); // White flash
                        location.getWorld().playSound(location, Sound.ENDERMAN_TELEPORT, 0.5f, 1.5f);
                    }
                }
        );

        new BukkitRunnable() {
            int ticks = 0;
            boolean hasGoneThrough = false;

            @Override
            public void run() {
                if (ticks > 25 || arena.getState() != GameState.LIVE ||
                        !arena.getQuirkBattleGame().getAlivePlayers().contains(player.getUniqueId())) {
                    cancel();
                    removeStand(stand);
                    projectileHits.remove(stand); // Clean up tracking
                    return;
                }

                // Step the projectile forward
                projectile.step();
                Location newLoc = projectile.getCurrentLocation();
                Vector currentDir = projectile.getCurrentDirection();

                // Check for block collision
                if (newLoc.getBlock().getType() != Material.AIR) {
                    cancel();
                    removeStand(stand);
                    projectileHits.remove(stand);
                    return;
                }

                // Teleport armor stand to new location
                newLoc.setDirection(currentDir);
                stand.teleport(newLoc);

                // Rotate ice block for visual effect
                stand.setHeadPose(new EulerAngle(Math.random(), Math.random(), Math.random()));

                // Ice particle trail
                spawnRGBParticles(newLoc, 200, 240, 255, true); // Light blue
                spawnRGBParticles(newLoc, 150, 220, 255, true); // Icy blue

                // Check collisions
                doCollision(stand);

                ticks++;
            }
        }.runTaskTimer(QuirkBattlesPlugin.getInstance(), 0L, 1L);
    }

    public void removeStand(ArmorStand stand) {
        stand.getLocation().getWorld().playSound(stand.getLocation(), Sound.GLASS, 1f, .7f);

        // Ice shattering effect
        Location loc = stand.getLocation();
        for (int i = 0; i < 8; i++) {
            Vector offset = new Vector(
                    (Math.random() - 0.5) * 0.5,
                    Math.random() * 0.5,
                    (Math.random() - 0.5) * 0.5
            );
            Location particleLoc = loc.clone().add(offset);
            spawnRGBParticles(particleLoc, 10, 128, 128, true);
            spawnRGBParticles(particleLoc, 200, 240, 255, true);
        }

        stand.remove();
    }

    public void doCollision(ArmorStand stand) {
        // Get the hit tracking set for this specific projectile
        Set<UUID> alreadyHit = projectileHits.get(stand);
        if (alreadyHit == null) return;

        for (Entity entity : stand.getNearbyEntities(1f, 1f, 1f)) {
            if (!(entity instanceof Player)) continue;
            Player target = (Player) entity;

            if (arena.getState() != GameState.LIVE) return;
            if (arena.getSpectators().contains(target.getUniqueId())) continue;
            if (!arena.getQuirkBattleGame().getAlivePlayers().contains(target.getUniqueId())) continue;

            if (arena.getType().isTeamsMode()) {
                if (arena.getTeam(player).equals(arena.getTeam(target))) continue;
            }

            if (target == player) continue;

            // Check if THIS SPECIFIC projectile has already hit this player
            if (alreadyHit.contains(target.getUniqueId())) continue;

            // Mark this player as hit by this projectile
            alreadyHit.add(target.getUniqueId());

            // Apply effects
            target.addPotionEffect(new PotionEffect(PotionEffectType.SLOW, 40, 3), true);
            target.sendMessage(ChatColor.GREEN + "You have been stunned by " + player.getName() + "'s " + ability.getDisplay() + "!");
            player.sendMessage(ChatColor.GREEN + "You have stunned " + target.getName() + " with your " + ability.getDisplay() + "!");

            // Ice impact effects
            Location impact = target.getLocation();
            spawnRGBParticles(impact.clone().add(0, 1, 0), 200, 240, 255, true);
            impact.getWorld().playSound(impact, Sound.GLASS, 1.2f, 1.5f);

            // Damage
            doDamageTo(player, target, ability.getDamage(), CustomDeathCause.ICE_SHARD);
        }
    }
}