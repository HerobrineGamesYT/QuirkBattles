package net.herobrine.quirkbattle.game.quirks.abilities.villain.afo;

import net.herobrine.gamecore.GameState;
import net.herobrine.quirkbattle.QuirkBattlesPlugin;
import net.herobrine.quirkbattle.game.CustomDeathCause;
import net.herobrine.quirkbattle.game.quirks.abilities.Abilities;
import net.herobrine.quirkbattle.game.quirks.abilities.Ability;
import net.herobrine.quirkbattle.game.quirks.abilities.SpecialCase;
import net.herobrine.quirkbattle.game.quirks.villain.AllForOne;
import net.herobrine.quirkbattle.util.Forcible;
import net.herobrine.quirkbattle.util.projectile.PortalProjectileService;
import net.herobrine.quirkbattle.util.Quirk;
import org.bukkit.*;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.player.PlayerToggleSneakEvent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.util.*;

public class TendrilAbility extends Ability implements SpecialCase {
    private Player attachedPlayer;
    private final Player player;
    private final Map<UUID, Boolean> hasHit = new HashMap<>();
    private boolean hasTendrilAttached = false;
    private final AllForOne afo;
    private PortalProjectileService portalService;

    // Track the path the tendril took
    private List<Location> tendrilPath = new ArrayList<>();
    private boolean tendrilWentThroughPortal = false;

    public TendrilAbility(Abilities ability, Quirk quirk, int id, int slot) {
        super(ability, quirk, id, slot);
        this.afo = (AllForOne) quirk;
        this.player = Bukkit.getPlayer(afo.getUniqueId());
    }

    @Override
    public void doAbility(Player player) {
        // Initialize portal service
        portalService = new PortalProjectileService(arena.getQuirkBattleGame().getWarpGateManagers());

        // Clear previous path
        tendrilPath.clear();
        tendrilWentThroughPortal = false;

        doVFX();
        player.getLocation().getWorld().playSound(player.getLocation(), Sound.WITHER_SHOOT, 1f, 1.2f);
    }

    public void doVFX() {
        Location startLoc = player.getLocation().add(0, 1.5, 0);
        Vector direction = player.getLocation().getDirection().normalize();

        // Create sequential projectile for the tendril
        PortalProjectileService.SequentialProjectile tendrilProjectile = portalService.createSequentialProjectile(
                startLoc,
                direction,
                1.0, // Step size
                (location, dir, wentThroughPortal) -> {
                    // Portal transition effect
                    if (wentThroughPortal) {
                        tendrilWentThroughPortal = true;
                        spawnRGBParticles(location, 255, 255, 255, true); // White flash
                        player.getWorld().playSound(location, Sound.ENDERMAN_TELEPORT, 0.5f, 1.5f);
                    }
                }
        );

        new BukkitRunnable() {
            int steps = 0;
            boolean hitSomething = false;
            Location lastLocation = startLoc.clone();

            public void run() {
                if (steps > 20 || hitSomething) {
                    if (!hitSomething && hasHit.isEmpty()) {
                        // Didn't hit anything, check if we should pull player
                        handleMissedTendril(lastLocation);
                    }
                    hasHit.clear();
                    this.cancel();
                    return;
                }

                if (!isActive()) {
                    hasHit.clear();
                    tendrilPath.clear();
                    this.cancel();
                    return;
                }

                // Step the projectile forward
                tendrilProjectile.step();
                Location currentLoc = tendrilProjectile.getCurrentLocation();
                lastLocation = currentLoc.clone();

                // Track the path
                tendrilPath.add(currentLoc.clone());

                // Check for block collision
                if (currentLoc.getBlock().getType() != Material.AIR) {
                    if (!hasHit.isEmpty()) {
                        hasHit.clear();
                        tendrilPath.clear();
                        this.cancel();
                        return;
                    }

                    // Pull player along the path if it went through a portal
                    if (tendrilWentThroughPortal && !tendrilPath.isEmpty()) {
                        pullPlayerAlongPath();
                    } else {
                        // Direct pull if no portal
                        Vector pullDirection = currentLoc.toVector().subtract(player.getLocation().toVector()).normalize();
                        pullDirection.multiply(2);
                        player.setVelocity(pullDirection);
                    }

                    player.sendMessage(ChatColor.GREEN + "You've pulled yourself!");
                    player.playSound(player.getLocation(), Sound.DOOR_OPEN, 1f, 1.9f);
                    hitSomething = true;
                } else {
                    // Check for entity collision
                    Player hitTarget = doCollision(currentLoc);
                    if (hitTarget != null) {
                        hitSomething = true;

                        // Pull the target back along the path if tendril went through portal
                        if (tendrilWentThroughPortal && !tendrilPath.isEmpty()) {
                            pullTargetAlongPath(hitTarget);
                        }
                        // Normal pull logic is already in doCollision
                    }

                    // Spawn tendril particles
                    spawnRGBParticles(currentLoc, 64, 5, 16, true);
                    spawnRGBParticles(currentLoc, 15, 11, 12, true);
                    spawnRGBParticles(currentLoc, 184, 17, 58, true);
                }

                steps++;
            }
        }.runTaskTimer(QuirkBattlesPlugin.getInstance(), 0, 1L);
    }

    private void pullPlayerAlongPath() {
        // Pull player forward through waypoints along the tendril path
        new BukkitRunnable() {
            int pathIndex = 0;

            @Override
            public void run() {
                if (pathIndex >= tendrilPath.size()) {
                    tendrilPath.clear();
                    cancel();
                    return;
                }

                Location target = tendrilPath.get(pathIndex);
                Vector pullDirection = target.toVector().subtract(player.getLocation().toVector()).normalize();
                pullDirection.multiply(2.5);
                player.setVelocity(pullDirection);

                // Visual effect along the path
                spawnRGBParticles(player.getLocation(), 64, 5, 16, true);

                // Move to next waypoint if close enough to current one
                if (player.getLocation().distance(target) < 3) {
                    pathIndex++;
                }
            }
        }.runTaskTimer(QuirkBattlesPlugin.getInstance(), 0L, 2L);
    }

    private void pullTargetAlongPath(Player target) {
        // Create reverse path for pulling target back
        List<Location> reversePath = new ArrayList<>(tendrilPath);
        Collections.reverse(reversePath);

        new BukkitRunnable() {
            int pathIndex = 0;

            @Override
            public void run() {
                if (pathIndex >= reversePath.size() || target.getLocation().distance(player.getLocation()) < 3) {
                    tendrilPath.clear();
                    cancel();
                    return;
                }

                Location waypoint = reversePath.get(pathIndex);
                Vector pullDirection = waypoint.toVector().subtract(target.getLocation().toVector()).normalize();
                pullDirection.multiply(2.5);
                target.setVelocity(pullDirection);

                // Visual effect showing the pull path
                spawnRGBParticles(target.getLocation(), 184, 17, 58, true);

                // Check if target reached this waypoint
                if (target.getLocation().distance(waypoint) < 2) {
                    pathIndex++;
                }
            }
        }.runTaskTimer(QuirkBattlesPlugin.getInstance(), 0L, 2L);
    }

    private void handleMissedTendril(Location endLocation) {
        // Optional: Add any effects for when tendril misses everything
        spawnRGBParticles(endLocation, 128, 10, 32, true);
        tendrilPath.clear();
    }

    public Player doCollision(Location loc) {
        for (Entity ent : loc.getWorld().getNearbyEntities(loc, .7, 1, .7)) {
            if (!(ent instanceof Player)) continue;
            Player target = (Player) ent;

            if (arena.getSpectators().contains(target.getUniqueId())) continue;
            if (!arena.getQuirkBattleGame().getAlivePlayers().contains(target.getUniqueId())) continue;
            if (hasHit.containsKey(target.getUniqueId())) continue;
            if (target == player) continue;

            if (arena.getType().isTeamsMode()) {
                if (arena.getTeam(player).equals(arena.getTeam(target))) {
                    // Attach tendril to teammate
                    if (attachedPlayer != null) {
                        player.sendMessage(ChatColor.RED + "You already have your Tendril attached to a teammate!");
                        player.sendMessage(ChatColor.RED + "Sneak to use Forcible Quirk Activation and remove the link!");
                        continue;
                    }

                    hasHit.put(target.getUniqueId(), true);
                    hasTendrilAttached = true;
                    attachedPlayer = target;
                    player.playSound(player.getLocation(), Sound.DOOR_CLOSE, 1f, 1.9f);
                    player.sendMessage(ChatColor.GREEN + "You have attached your tendril to " + arena.getTeam(target).getColor() + target.getName() + ChatColor.GREEN + "!");
                    player.sendMessage(ChatColor.GREEN + "Sneak to use Forcible Quirk Activation and remove the link!");
                    target.sendMessage(ChatColor.RED + player.getName() + ChatColor.GREEN + " has attached their Tendril to you!");
                    doContinuousVFX();
                    return null; // Don't pull teammates
                }
            }

            // Hit enemy
            hasHit.put(target.getUniqueId(), true);

            // Only do immediate pull if no portal was involved
            if (!tendrilWentThroughPortal) {
                Vector direction = target.getLocation().toVector()
                        .subtract(player.getLocation().toVector()).normalize();
                direction.setX(direction.getX() * -1);
                direction.setZ(direction.getZ() * -1);
                direction.multiply(2.5);
                target.setVelocity(direction);
            }
            // If portal was involved, pullTargetAlongPath will handle it

            PotionEffect banditSpeed = PotionEffectType.SLOW.createEffect(120, 5);
            target.addPotionEffect(banditSpeed);
            doDamageTo(player, target, 30.0, CustomDeathCause.TENDRIL);

            player.sendMessage(ChatColor.GREEN + "You captured " + target.getName() + " with your tendril!");
            player.playSound(player.getLocation(), Sound.DOOR_CLOSE, 1f, 1.9f);
            target.sendMessage(ChatColor.RED + "You have been captured by " + player.getName() + "!");

            return target; // Return the hit target
        }
        return null;
    }

    @Override
    public boolean doesCasePass(Player player) {
        return true;
    }

    @Override
    public void doNoPass(Player player) {
        player.sendMessage(ChatColor.RED + "Your Tendril is already attached to " + attachedPlayer.getName() + "!");
    }

    public void doContinuousVFX() {
        new BukkitRunnable() {
            @Override
            public void run() {
                if (attachedPlayer == null) {
                    cancel();
                    return;
                }
                if (arena.getState() != GameState.LIVE) {
                    cancel();
                    return;
                }

                Vector playerLocVector = player.getLocation().toVector();
                Vector attachedVector = attachedPlayer.getLocation().toVector();

                Vector betweenPandTVector = attachedVector.clone().subtract(playerLocVector);
                Vector directionVector = betweenPandTVector.clone().normalize();
                Location loc = player.getLocation().clone();

                // Standard direct line visual
                for (int i = 0; i < betweenPandTVector.length(); i++) {
                    Vector particlePoint = playerLocVector.clone().add(directionVector.clone().multiply(i));

                    loc.setX(particlePoint.getX());
                    loc.setY(particlePoint.getY() + 1);
                    loc.setZ(particlePoint.getZ());

                    spawnRGBParticles(loc, 64, 5, 16, true);
                    spawnRGBParticles(loc, 15, 11, 12, true);
                    spawnRGBParticles(loc, 184, 17, 58, true);
                }
            }
        }.runTaskTimerAsynchronously(QuirkBattlesPlugin.getInstance(), 0L, 5L);
    }

    @EventHandler
    public void onPlayerSneak(PlayerToggleSneakEvent event) {
        if (event.getPlayer().getUniqueId() != quirk.getUniqueId()) return;
        if (!hasTendrilAttached) return;
        Forcible forcible = (Forcible) arena.getClasses().get(attachedPlayer.getUniqueId());
        player.sendMessage(ChatColor.GREEN + "You have activated forcible quirk activation on " + attachedPlayer.getName() + "!");
        attachedPlayer.sendMessage(ChatColor.RED + player.getName() + ChatColor.GREEN + " used forcible quirk activation on you!");
        player.playSound(player.getLocation(), Sound.FIZZ, 1f, 1f);
        forcible.doForcibleAbility(afo);
        hasTendrilAttached = false;
        attachedPlayer = null;
        tendrilPath.clear();
    }
}