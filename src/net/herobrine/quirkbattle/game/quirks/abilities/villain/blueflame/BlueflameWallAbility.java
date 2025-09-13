package net.herobrine.quirkbattle.game.quirks.abilities.villain.blueflame;

import net.herobrine.core.SkullMaker;
import net.herobrine.gamecore.GameState;
import net.herobrine.quirkbattle.QuirkBattlesPlugin;
import net.herobrine.quirkbattle.game.CustomDeathCause;
import net.herobrine.quirkbattle.game.quirks.abilities.Abilities;
import net.herobrine.quirkbattle.game.quirks.abilities.Ability;
import net.herobrine.quirkbattle.util.projectile.PortalProjectileService;
import net.herobrine.quirkbattle.util.Quirk;
import net.minecraft.server.v1_8_R3.EnumParticle;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.EulerAngle;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Predicate;

public class BlueflameWallAbility extends Ability {
    private final Player player = Bukkit.getPlayer(quirk.getUniqueId());
    private final List<ArmorStand> standLocations = new ArrayList<>();
    private final Map<UUID, Long> hasHit = new HashMap<>();
    private PortalProjectileService portalService;

    public BlueflameWallAbility(Abilities ability, Quirk quirk, int id, int slot) {
        super(ability, quirk, id, slot);
    }

    @Override
    public void doAbility(Player player) {
        // Initialize portal service with current managers
        portalService = new PortalProjectileService(arena.getQuirkBattleGame().getWarpGateManagers());

        Location eyeLocation = player.getEyeLocation();
        Vector directionVector = eyeLocation.getDirection();
        Location frontLocation = eyeLocation.add(directionVector);
        doVFX(frontLocation);
    }

    public void doVFX(Location centerLocation) {
        standLocations.clear();
        hasHit.clear();
        spawnStand(centerLocation);
        centerLocation.getWorld().playSound(centerLocation, Sound.FIRE_IGNITE, 1f, 1f);

        // Create sequential projectile using the service
        final double stepSize = 0.5;
        Vector initialDir = centerLocation.getDirection().setY(0).normalize();

        PortalProjectileService.SequentialProjectile projectile = portalService.createSequentialProjectile(
                centerLocation,
                initialDir,
                stepSize,
                (location, direction, wentThroughPortal) -> {
                    // Handle portal transition effects
                    if (wentThroughPortal) {
                        // White flash at portal entry/exit
                        spawnRGBParticles(location, 255, 255, 255, true);
                    }
                }
        );

        new BukkitRunnable() {
            int i = 0;

            @Override
            public void run() {
                if (i > 25) {
                    this.cancel();
                    collisionRunnable();
                    revertFire(standLocations);
                    return;
                }

                // Get current position from projectile
                Location currentLoc = projectile.getCurrentLocation();

                // Visual effects at current location
                spawnRGBParticles(currentLoc, 135, 206, 250, true);
                spawnRGBParticles(currentLoc, 25, 25, 112, true);
                spawnRGBParticles(currentLoc, 138, 43, 226, true);

                // Step the projectile forward
                projectile.step();

                // Spawn stands at intervals
                if (i % 2 == 0) {
                    spawnStand(projectile.getCurrentLocation());
                } else {
                    doCollision();
                }

                i++;
            }
        }.runTaskTimer(QuirkBattlesPlugin.getInstance(), 0, 1L);
    }

    public void addStandToList(ArmorStand stand) {
        standLocations.add(stand);
    }

    public void revertFire(List<ArmorStand> stands) {
        new BukkitRunnable() {
            @Override
            public void run() {
                for (ArmorStand stand : stands) {
                    stand.getWorld().playSound(stand.getLocation(), Sound.FIZZ, .7f, 1f);
                    spawnParticle(stand.getLocation(), EnumParticle.SMOKE_LARGE, true);
                    stand.remove();
                }
            }
        }.runTaskLater(QuirkBattlesPlugin.getInstance(), 40L);
    }

    public void collisionRunnable() {
        new BukkitRunnable() {
            int ticks = 0;

            @Override
            public void run() {
                if (ticks >= 40) {
                    cancel();
                    spawnRGBParticlesForStands(36, 5, 0, true);
                    return;
                }

                if (ticks <= 10) spawnRGBParticlesForStands(54, 204, 227, true);
                if (ticks <= 20) spawnRGBParticlesForStands(18, 100, 148, true);
                if (ticks <= 30) spawnRGBParticlesForStands(0, 65, 82, true);
                if (ticks % 2 == 0) doCollision();

                ticks++;
            }
        }.runTaskTimer(QuirkBattlesPlugin.getInstance(), 0L, 1L);
    }

    public void spawnRGBParticlesForStands(int red, int green, int blue, boolean sendToSelf) {
        for (ArmorStand stand : standLocations) {
            spawnRGBParticles(stand.getLocation().clone().add(0, 1, 0), red, green, blue, sendToSelf);
        }
    }

    public void spawnStand(Location loc) {
        ArmorStand fire = loc.getWorld().spawn(loc.clone().add(new Vector(0, 1, 0)), ArmorStand.class);
        loc.getWorld().playSound(loc, Sound.GHAST_FIREBALL, .8f, .8f);
        addStandToList(fire);

        SkullMaker skull = new SkullMaker("BlueFire", Collections.singletonList(" "),
                "https://textures.minecraft.net/texture/561aaa05887898fcba3f479a073fc916bb6ac0689391379d832545b5f545742f");
        fire.setCustomNameVisible(false);
        fire.setCustomName(uuid.toString());
        fire.setBasePlate(false);
        fire.setGravity(true);
        fire.setSmall(true);
        fire.setMarker(true);
        fire.setVisible(false);
        fire.setHeadPose(new EulerAngle(Math.random(), Math.random(), Math.random()));
        fire.setHelmet(skull.getSkull());
    }

    public void doTeamBuff(Player target) {
        Predicate<PotionEffect> hasSlowness = potionEffect -> potionEffect.getType().equals(PotionEffectType.SLOW);
        if (target.getActivePotionEffects().stream().anyMatch(hasSlowness)) {
            target.removePotionEffect(PotionEffectType.SLOW);
            if (target != player) {
                target.sendMessage(ChatColor.GREEN + "You have been unfrozen by " + player.getName() + "'s " + ability.getDisplay() + "!");
            } else {
                target.sendMessage(ChatColor.GREEN + "You unfroze yourself!");
            }
        }
    }

    public void doCollision() {
        for (ArmorStand stand : standLocations) {
            for (Entity entity : stand.getNearbyEntities(1f, 1f, 1f)) {
                if (!(entity instanceof Player)) continue;
                Player target = (Player) entity;

                if (arena.getState() != GameState.LIVE) return;
                if (arena.getSpectators().contains(target.getUniqueId())) continue;
                if (!arena.getQuirkBattleGame().getAlivePlayers().contains(target.getUniqueId())) continue;

                if (arena.getType().isTeamsMode()) {
                    if (arena.getTeam(player).equals(arena.getTeam(target))) {
                        doTeamBuff(target);
                        continue;
                    }
                }

                if (target == player) {
                    doTeamBuff(target);
                    continue;
                }

                if (hasHit.containsKey(target.getUniqueId())) {
                    if (System.currentTimeMillis() - hasHit.get(target.getUniqueId()) < 1000) continue;
                }

                hasHit.put(target.getUniqueId(), System.currentTimeMillis());
                target.sendMessage(ChatColor.BLUE + "You have been burned by " + player.getName() + "'s" + ability.getDisplay() + "!");
                player.sendMessage(ChatColor.BLUE + "You have hit " + target.getName() + " with your " + ability.getDisplay() + "!");
                doDOTFor(player, target, 1L);
            }
        }
    }

    public void doDOTFor(Player caster, Player victim, long delay) {
        new BukkitRunnable() {
            @Override
            public void run() {
                victim.getWorld().playEffect(victim.getLocation().add(0, 1, 0), org.bukkit.Effect.STEP_SOUND, Material.LAPIS_BLOCK);
                doDamageTo(caster, victim, 25, CustomDeathCause.BLUEFLAME_WALL);
            }
        }.runTaskLater(QuirkBattlesPlugin.getInstance(), delay);
    }
}