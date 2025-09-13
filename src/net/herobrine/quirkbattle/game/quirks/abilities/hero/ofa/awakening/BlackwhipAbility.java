package net.herobrine.quirkbattle.game.quirks.abilities.hero.ofa.awakening;

import net.herobrine.core.HerobrinePVPCore;
import net.herobrine.quirkbattle.QuirkBattlesPlugin;
import net.herobrine.quirkbattle.game.quirks.abilities.Abilities;
import net.herobrine.quirkbattle.game.quirks.abilities.Ability;
import net.herobrine.quirkbattle.game.quirks.hero.OneForAll;
import net.herobrine.quirkbattle.game.stats.EnhancedPlayerStats;
import net.herobrine.quirkbattle.util.Quirk;
import net.herobrine.quirkbattle.util.projectile.PortalProjectileService;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class BlackwhipAbility extends Ability {
    private Player player;
    private final OneForAll ofa;
    private final Map<UUID, Boolean> hasHit = new HashMap<>();
    private PortalProjectileService portalService;

    // Track the path the capture scarf took
    private List<Location> path = new ArrayList<>();
    private boolean wentThroughPortal = false;
    private boolean wasEnhanced = false;
    private EnhancedPlayerStats enhancedStats;

    public BlackwhipAbility(Abilities ability, Quirk quirk, int id, int slot) {
        super(ability, quirk, id, slot);
        this.ofa = (OneForAll) quirk;
        this.player = Bukkit.getPlayer(uuid);
        // This ability will only be registered in Heroes VS Villains mode, so this is a safe cast
        this.enhancedStats = (EnhancedPlayerStats) stats;
    }

    @Override
    public void doAbility(Player player) {
        portalService = new PortalProjectileService(arena.getQuirkBattleGame().getWarpGateManagers());

        // Clear previous path
        path.clear();
        wentThroughPortal = false;
        wasEnhanced = false;
        checkForEnhancement();

        doVFX();
    }

    public void doVFX() {
        Location startLoc = player.getLocation().add(0, 1.5, 0);
        Vector direction = player.getLocation().getDirection().normalize();

        // Create sequential projectile for the capture scarf
        PortalProjectileService.SequentialProjectile captureProjectile = portalService.createSequentialProjectile(
                startLoc,
                direction,
                1.0, // Step size
                (location, dir, wentThroughPortal) -> {
                    // Portal transition effect
                    if (wentThroughPortal) {
                        this.wentThroughPortal = true;
                        spawnRGBParticles(location, 255, 255, 255, true); // White flash
                        player.getWorld().playSound(location, Sound.ENDERMAN_TELEPORT, 0.5f, 1.5f);
                    }
                }
        );

        new BukkitRunnable() {
            int steps = 0;
            boolean hitSomething = false;

            public void run() {
                if (steps > 15 || hitSomething) {
                    hasHit.clear();
                    path.clear();
                    this.cancel();
                    return;
                }

                if (!isActive()) {
                    hasHit.clear();
                    path.clear();
                    this.cancel();
                    return;
                }

                // Step the projectile forward
                captureProjectile.step();
                Location currentLoc = captureProjectile.getCurrentLocation();

                // Track the path
                path.add(currentLoc.clone());

                // Check for block collision
                if (currentLoc.getBlock().getType() != Material.AIR) {
                    hasHit.clear();

                    // Pull player along the path if it went through a portal
                    if (wentThroughPortal && !path.isEmpty()) {
                        pullPlayerAlongPath();
                    } else {
                        // Direct pull if no portal
                        double pullStrength = 2.5;
                        if (wasEnhanced) pullStrength = pullStrength *  2.5;
                        Vector direction = currentLoc.toVector().subtract(player.getLocation().toVector()).normalize();
                        direction.multiply(pullStrength);
                        player.setVelocity(direction);
                    }

                    player.sendMessage(ChatColor.GREEN + "You've pulled yourself!");
                    player.playSound(player.getLocation(), Sound.DOOR_OPEN, 1f, 1.9f);
                    hitSomething = true;
                    this.cancel();
                } else {
                    // Check for entity collision
                    Player hitTarget = doCollision(currentLoc);
                    if (hitTarget != null) {
                        hitSomething = true;

                        // Pull the target back along the path if capture went through portal
                        if (wentThroughPortal && !path.isEmpty()) {
                            pullTargetAlongPath(hitTarget);
                        }


                        // Normal pull logic is already in doCollision
                    }
                    // Different VFX For Chain if Blackchain is enabled
                    spawnBlackwhipParticles(currentLoc, wasEnhanced);
                }

                steps++;
            }
        }.runTaskTimer(QuirkBattlesPlugin.getInstance(), 0, 1L);
    }

    public void spawnBlackwhipParticles(Location loc, boolean enhanced) {
        // Main blackwhip - very dark green/black
        spawnRGBParticles(loc,30,109,82, true);
        spawnRGBParticles(loc,15,101,103, true);
        spawnRGBParticles(loc, 21,34,40, true);

        if (enhanced) {
            // Enhanced with Fa-Jin - add pink/magenta energy (like Deku's Fa-Jin)
            if (Math.random() < 0.4) {
                for (int i = 0; i < 2; i++) {
                    Vector offset = new Vector(
                            (Math.random() - 0.5) * 0.3,
                            (Math.random() - 0.5) * 0.3,
                            (Math.random() - 0.5) * 0.3
                    );
                    Location sparkLoc = loc.clone().add(offset);
                    spawnRGBParticles(sparkLoc, 247, 87, 111, true);
                    spawnRGBParticles(sparkLoc, 0,0,0, true);
                }
            }
        }
    }

    private void pullPlayerAlongPath() {
        // Pull player forward through waypoints along the capture path
        new BukkitRunnable() {
            int pathIndex = 0;

            @Override
            public void run() {
                if (pathIndex >= path.size()) {
                    path.clear();
                    cancel();
                    return;
                }
                double pullStrength = 2.5;
                if (wasEnhanced) pullStrength = pullStrength *  2.5;

                Location target = path.get(pathIndex);
                Vector pullDirection = target.toVector().subtract(player.getLocation().toVector()).normalize();
                pullDirection.multiply(pullStrength);
                player.setVelocity(pullDirection);

                // Visual effect along the path
                spawnRGBParticles(player.getLocation(), 0, 240, 232, true);

                // Move to next waypoint if close enough to current one
                if (player.getLocation().distance(target) < 3) {
                    pathIndex++;
                }
            }
        }.runTaskTimer(QuirkBattlesPlugin.getInstance(), 0L, 2L);
    }

    private void pullTargetAlongPath(Player target) {
        // Create reverse path for pulling target back
        List<Location> reversePath = new ArrayList<>(path);
        Collections.reverse(reversePath);

        new BukkitRunnable() {
            int pathIndex = 0;

            @Override
            public void run() {
                if (pathIndex >= reversePath.size() || target.getLocation().distance(player.getLocation()) < 3) {
                    path.clear();
                    cancel();
                    return;
                }
                double pullStrength = 2.5;
                if (wasEnhanced) pullStrength = pullStrength *  2.5;;
                Location waypoint = reversePath.get(pathIndex);
                Vector pullDirection = waypoint.toVector().subtract(target.getLocation().toVector()).normalize();
                pullDirection.multiply(pullStrength);
                target.setVelocity(pullDirection);

                // Visual effect showing the pull path
                spawnRGBParticles(target.getLocation(), 0, 240, 232, true);

                // Check if target reached this waypoint
                if (target.getLocation().distance(waypoint) < 2) {
                    pathIndex++;
                }
            }
        }.runTaskTimer(QuirkBattlesPlugin.getInstance(), 0L, 2L);
    }

    public void checkForEnhancement() {
        if (enhancedStats.getFaJinSystem().hasEnergy(20) && !wasEnhanced) {
            wasEnhanced = true;
            enhancedStats.getFaJinSystem().consumeEnergy(20);
            player.sendMessage(HerobrinePVPCore.translateString("&c&lBLACKCHAIN! &r&cBlackchain &fwas activated!"));
            player.playSound(player.getLocation(), Sound.HORSE_ARMOR, 1f, 0.8f);
        }
    }

    public Player doCollision(Location loc) {
        for (Entity ent : loc.getWorld().getNearbyEntities(loc, .7, 1, .7)) {
            if (!(ent instanceof Player)) continue;
            Player target = (Player) ent;

            if (arena.getSpectators().contains(target.getUniqueId())) continue;
            if (!arena.getQuirkBattleGame().getAlivePlayers().contains(target.getUniqueId())) continue;

            if (arena.getType().isTeamsMode()) {
                if (arena.getTeam(player).equals(arena.getTeam(target))) continue;
            }

            if (target == player) continue;
            if (hasHit.containsKey(target.getUniqueId())) continue;

            hasHit.put(target.getUniqueId(), true);

            // Only do immediate pull if no portal was involved
            if (!wentThroughPortal) {
                double pullStrength = 2;
                if (wasEnhanced) pullStrength = pullStrength *  2.5;
                Vector direction = target.getLocation().toVector()
                        .subtract(player.getLocation().toVector()).normalize();
                direction.setX(direction.getX() * -1);
                direction.setZ(direction.getZ() * -1);
                direction.multiply(pullStrength);
                target.setVelocity(direction);
            }
            // If portal was involved, pullTargetAlongPath will handle it

            PotionEffect stun;
            if (wasEnhanced) stun = PotionEffectType.SLOW.createEffect(140, 5);
            else stun = PotionEffectType.SLOW.createEffect(120, 4);
            target.addPotionEffect(stun);

            player.playSound(player.getLocation(), Sound.DOOR_CLOSE, 1f, 1.9f);
            player.sendMessage(ChatColor.AQUA + "You captured " + target.getName() + "!");
            target.sendMessage(ChatColor.RED + "You have been captured by " + player.getName() + "!");



            return target; // Return the hit target
        }
        return null;
    }
}
