package net.herobrine.quirkbattle.game.quirks.abilities.villain.warpgate;

import net.herobrine.gamecore.GameState;
import net.herobrine.quirkbattle.QuirkBattlesPlugin;
import net.herobrine.quirkbattle.game.quirks.abilities.Abilities;
import net.herobrine.quirkbattle.game.quirks.abilities.Ability;
import net.herobrine.quirkbattle.game.quirks.abilities.SpecialCase;
import net.herobrine.quirkbattle.util.Quirk;
import net.herobrine.quirkbattle.util.WarpGateManager;
import net.herobrine.quirkbattle.util.npc.CloneManager;
import net.herobrine.quirkbattle.util.npc.CloneTypes;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.util.UUID;

public class WarpGateAbility extends Ability implements SpecialCase {

    private final WarpGateManager manager;
    private final CloneManager cloneManager;

    private Location portalA, portalB;
    private Location frozenLocation;
    private boolean placing = false;

    private final int portalDuration = 200; // 200 ticks = 10 seconds

    public WarpGateAbility(Abilities ability, Quirk quirk, int id, int slot) {
        super(ability, quirk, id, slot);
        manager = new WarpGateManager(this);
        cloneManager = arena.getQuirkBattleGame().getCloneManager();
        arena.getQuirkBattleGame().registerWarpGateManager(manager, uuid);
    }

    @Override
    public void doAbility(Player player) {
        placing = true;
        frozenLocation = player.getLocation().clone();

        // Make player "freecam"
        player.setAllowFlight(true);
        player.setFlying(true);

        for (UUID targetId : arena.getPlayers()) {
            Player target = Bukkit.getPlayer(targetId);
            if (target == null) continue;
            if (target.getUniqueId().equals(player.getUniqueId())) continue; // Don't hide from self
            if (arena.getSpectators().contains(targetId)) continue;
            target.hidePlayer(player); // Hide the caster from other players
        }

        // Spawn static NPC clone
        cloneManager.spawnClones(player, 1, 0, CloneTypes.KUROGIRI_DOUBLE);

        // Helix VFX around the clone
        new BukkitRunnable() {
            double helixAngle = 0;

            @Override
            public void run() {
                if (!placing || !isActive()) {
                    placing = false;
                    cancel();
                    return;
                }

                // Purple mist at clone location
                spawnRGBParticles(frozenLocation.clone().add(0, 1, 0),
                        30, 0, 60, true); // purple mist

                // Helix effect around the clone
                for (int i = 0; i < 2; i++) {
                    double angle = helixAngle + (i * Math.PI); // Two helixes 180 degrees apart
                    double y = 0;

                    // Create helix from feet to head
                    for (double height = 0; height <= 2; height += 0.2) {
                        double x = Math.cos(angle + height * 2) * 0.5;
                        double z = Math.sin(angle + height * 2) * 0.5;

                        Location helixLoc = frozenLocation.clone().add(x, height, z);

                        // Purple energy particles
                        spawnRGBParticles(helixLoc, 90, 0, 180, true);
                        // Dark core
                        if (Math.random() < 0.3) {
                            spawnRGBParticles(helixLoc, 20, 0, 40, true);
                        }
                    }
                }

                // Energy burst at random heights
                if (Math.random() < 0.2) {
                    double burstHeight = Math.random() * 2;
                    Location burstLoc = frozenLocation.clone().add(0, burstHeight, 0);

                    for (int i = 0; i < 3; i++) {
                        double angle = Math.random() * Math.PI * 2;
                        double radius = Math.random() * 0.8;
                        double x = Math.cos(angle) * radius;
                        double z = Math.sin(angle) * radius;

                        spawnRGBParticles(burstLoc.clone().add(x, 0, z), 120, 0, 200, true);
                    }
                }

                helixAngle += Math.PI / 10; // Rotate the helix
            }
        }.runTaskTimer(QuirkBattlesPlugin.getInstance(), 0L, 2L);

        // Portal preview visualization (unchanged)
        new BukkitRunnable() {
            @Override
            public void run() {
                if (!placing || !isActive() || hasActivePortals()) {
                    cancel();
                    return;
                }

                // The eye location 3 blocks ahead
                Location eye = player.getEyeLocation().add(player.getEyeLocation().getDirection().normalize().multiply(3));
                Vector dir = eye.getDirection().normalize();
                Vector up = new Vector(0, 1, 0);

                if (Math.abs(dir.dot(up)) > 0.99) {
                    up = new Vector(1, 0, 0);
                }

                Vector right = dir.clone().crossProduct(up).normalize();
                Vector portalUp = right.clone().crossProduct(dir).normalize();

                double radius = 1.5;

                // Circle in portal plane
                for (double t = 0; t < Math.PI * 2; t += Math.PI / 8) {
                    double x = Math.cos(t) * radius;
                    double y = Math.sin(t) * radius;

                    Vector offset = right.clone().multiply(x).add(portalUp.clone().multiply(y));
                    Location particleLoc = eye.clone().add(offset);

                    spawnRGBParticlesForSelf(particleLoc, 90, 0, 180); // purple outline
                }
            }
        }.runTaskTimer(QuirkBattlesPlugin.getInstance(), 0L, 4L);

        player.sendMessage(ChatColor.DARK_PURPLE + "Warp Gate active: Left-click to place Portal A, Right-click for Portal B.");
    }

    @EventHandler
    public void onMove(PlayerMoveEvent e) {
        if (!placing || e.getPlayer().getUniqueId() != uuid) return;

        if (e.getTo().distance(frozenLocation) > ability.getRadius()) {
            e.setTo(new Location(e.getFrom().getWorld(), e.getFrom().getX(), e.getTo().getY(), e.getFrom().getZ(), e.getTo().getYaw(), e.getTo().getPitch()));
            e.getPlayer().sendMessage(ChatColor.RED + "You can't place a portal this far away!");
        }
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent e) {
        Player player = e.getPlayer();
        if (!placing) return;
        if (!player.getUniqueId().equals(uuid)) return;

        switch (e.getAction()) {
            case LEFT_CLICK_AIR:
            case LEFT_CLICK_BLOCK:
                if (portalA == null) {
                    portalA = player.getEyeLocation().add(player.getEyeLocation().getDirection().normalize().multiply(3)).clone();
                    spawnPortalEffect(portalA);
                    player.sendMessage(ChatColor.DARK_PURPLE + "Portal A set.");
                    player.playSound(player.getLocation(), Sound.DOOR_OPEN, 1f, 1.9f);
                    e.setCancelled(true);
                }
                break;

            case RIGHT_CLICK_AIR:
            case RIGHT_CLICK_BLOCK:
                if (portalA != null && portalB == null) {

                    if (player.getEyeLocation().add(player.getEyeLocation().getDirection().normalize().multiply(3)).distance(portalA) < 3) {
                        player.sendMessage(ChatColor.RED + "You can't place your portals this close together!");
                        player.playSound(player.getLocation(), Sound.ENDERMAN_TELEPORT, 1f, .8f);
                        e.setCancelled(true);
                        return;
                    }
                    portalB = player.getEyeLocation().add(player.getEyeLocation().getDirection().normalize().multiply(3)).clone();
                    spawnPortalEffect(portalB);
                    player.sendMessage(ChatColor.DARK_PURPLE + "Portal B set.");
                    player.playSound(player.getLocation(), Sound.DOOR_OPEN, 1f, 1.9f);
                    e.setCancelled(true);

                    finishPlacement(player);
                }
                else if (portalA == null) {
                    player.sendMessage(ChatColor.RED + "Place Portal A first!");
                    player.playSound(player.getLocation(), Sound.ENDERMAN_TELEPORT, 1f, .8f);
                }
                break;
        }
    }

    private void spawnPortalEffect(Location loc) {
        Vector dir = loc.getDirection().normalize();
        Vector up = new Vector(0, 1, 0);

        // If the portal is facing straight up/down, choose a safe up vector
        if (Math.abs(dir.dot(up)) > 0.99) {
            up = new Vector(1, 0, 0);
        }

        // Build the portal plane (right + up in relation to the portal's direction)
        Vector right = dir.clone().crossProduct(up).normalize();
        Vector portalUp = right.clone().crossProduct(dir).normalize();

        new BukkitRunnable() {
            double angle = 0;
            int ticks = 0;

            @Override
            public void run() {
                if (ticks > portalDuration && !hasActivePortals()) {
                    cancel();
                    return;
                }

                double baseRadius = 2.0;

                // === 1. Dark Center Core ===
                for (int i = 0; i < 8; i++) {
                    Location core = loc.clone().add(
                            (Math.random() - 0.5) * 0.3,
                            (Math.random() - 0.5) * 0.3,
                            (Math.random() - 0.5) * 0.3
                    );
                    spawnRGBParticles(core, 20, 0, 20, true); // dark purple/black
                }

                // === 2. Swirling Vortex in Portal Plane ===
                for (double t = 0; t < 2 * Math.PI; t += Math.PI / 15) {
                    double radius = baseRadius - (ticks % 40) * 0.05;
                    if (radius < 0.3) radius = baseRadius;

                    double x = Math.cos(t + angle) * radius;
                    double y = Math.sin(t + angle) * radius;

                    // Rotate circle into the portal's plane
                    Vector offset = right.clone().multiply(x).add(portalUp.clone().multiply(y));
                    Location particleLoc = loc.clone().add(offset);
                    spawnRGBParticles(particleLoc, 50, 0, 100, true);
                }

                // === 3. Misty Tendrils Around ===
                if (Math.random() < 0.2) {
                    for (int i = 0; i < 2; i++) {
                        double dx = (Math.random() - 0.5) * 2.5;
                        double dy = (Math.random() * 0.8);
                        double dz = (Math.random() - 0.5) * 2.5;

                        Vector tendrilOffset = right.clone().multiply(dx)
                                .add(portalUp.clone().multiply(dy))
                                .add(dir.clone().multiply(dz));

                        Location tendril = loc.clone().add(tendrilOffset);
                        spawnRGBParticles(tendril, 70, 0, 120, true);
                    }
                }

                angle += Math.PI / 40;
                ticks += 2;
            }
        }.runTaskTimer(QuirkBattlesPlugin.getInstance(), 0L, 2L);
    }

    private void cancelAbility(Player player) {
        placing = false;

        // Return player to frozen location
        player.teleport(frozenLocation);

        // FIXED: Show player to others again
        for (UUID targetId : arena.getPlayers()) {
            Player target = Bukkit.getPlayer(targetId);
            if (target == null) continue;
            if (target.getUniqueId().equals(player.getUniqueId())) continue;
            target.showPlayer(player); // Show the caster to other players
        }

        player.setAllowFlight(false);
        player.setFlying(false);
        player.setGameMode(GameMode.ADVENTURE);

        cloneManager.despawnAll(uuid);

        player.sendMessage(ChatColor.RED + "Your ability was cancelled!");
    }

    private void finishPlacement(Player player) {
        placing = false;

        // Return player to frozen location
        player.teleport(frozenLocation);
        player.sendMessage(ChatColor.LIGHT_PURPLE  + "Warp Gate ready!");
        player.getWorld().playSound(player.getLocation(), Sound.PORTAL_TRAVEL, 1f, 1.2f);

        // FIXED: Show player to others again
        for (UUID targetId : arena.getPlayers()) {
            Player target = Bukkit.getPlayer(targetId);
            if (target == null) continue;
            if (target.getUniqueId().equals(player.getUniqueId())) continue;
            target.showPlayer(player); // Show the caster to other players
        }

        player.setAllowFlight(false);
        player.setFlying(false);
        player.setGameMode(GameMode.ADVENTURE);

        cloneManager.despawnAll(uuid);

        // Keep portals active for portalDuration ticks
        new BukkitRunnable() {
            int ticks = 0;
            @Override
            public void run() {
                if (ticks > portalDuration || portalA == null || portalB == null || arena.getState() != GameState.LIVE) {
                    cancel();
                    portalA = null;
                    portalB = null;

                    setCooldown(System.currentTimeMillis());
                    doAbilityCooldown();
                    return;
                }

                // Check all entities in arena (players + mobs)
                for (Entity entity : arena.getSpawn().getWorld().getEntities()) {
                    if (!(entity instanceof LivingEntity)) continue; // only living things
                    if (entity instanceof ArmorStand) continue;
                    if (entity.isDead()) continue;

                    // Prevent double-teleport glitch by small cooldown
                    if (entity.hasMetadata("warpCooldown")) continue;

                    if (entity.getLocation().distance(portalA) < 2.0) {
                        teleportThroughGate(entity, portalB);
                    } else if (entity.getLocation().distance(portalB) < 2.0) {
                        teleportThroughGate(entity, portalA);
                    }
                }

                if (hasActivePortals()) ticks++;
            }
        }.runTaskTimer(QuirkBattlesPlugin.getInstance(), 0L, 2L);
    }

    private void teleportThroughGate(Entity entity, Location target) {
        Vector velocity = entity.getVelocity().clone();

        // Use the portal's facing direction
        Vector targetDir = target.getDirection();

        // Exit slightly in front of the portal, facing its yaw/pitch
        Location tpLoc = target.clone().add(targetDir.clone().normalize().multiply(1.2));
        tpLoc.setDirection(targetDir);

        entity.teleport(tpLoc);
        entity.setVelocity(velocity);

        spawnRGBParticles(tpLoc, 30, 0, 60, true);
        entity.getWorld().playSound(tpLoc, Sound.ENDERMAN_TELEPORT, 1f, 1f);

        entity.setMetadata("warpCooldown", new FixedMetadataValue(
                QuirkBattlesPlugin.getInstance(), System.currentTimeMillis()));

        Bukkit.getScheduler().runTaskLater(QuirkBattlesPlugin.getInstance(), () -> {
            entity.removeMetadata("warpCooldown", QuirkBattlesPlugin.getInstance());
        }, 20L);
    }

    @Override
    public void remove() {
        arena.getQuirkBattleGame().unregisterWarpGateManager(uuid);
        super.remove();
    }

    public boolean hasActivePortals() {
        return portalA != null && portalB != null;
    }

    public Location getPortalA() {return portalA;}
    public Location getPortalB() {return portalB;}

    @Override
    public boolean doesCasePass(Player player) {
        if (placing) return false;
        if (hasActivePortals()) return false;

        return true;
    }

    @Override
    public void doNoPass(Player player) {
        if (placing) player.sendMessage(ChatColor.RED + "Place your portals down, silly!");
        if (hasActivePortals()) player.sendMessage(ChatColor.RED + "Your Warp Gate is already active!");

        player.playSound(player.getLocation(), Sound.VILLAGER_NO, 1f, 1f);
    }
}