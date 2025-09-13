package net.herobrine.quirkbattle.game.quirks.abilities.villain.warpgate;

import net.herobrine.quirkbattle.QuirkBattlesPlugin;
import net.herobrine.quirkbattle.game.quirks.abilities.Abilities;
import net.herobrine.quirkbattle.game.quirks.abilities.Ability;
import net.herobrine.quirkbattle.util.Quirk;
import net.minecraft.server.v1_8_R3.EnumParticle;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

public class GatePullAbility extends Ability {

    public GatePullAbility(Abilities ability, Quirk quirk, int id, int slot) {
        super(ability, quirk, id, slot);
    }

    public void doAbility(Player player) {
        Vector dir = player.getLocation().getDirection();
        Location center = player.getLocation();
        dir.multiply(3);
        center.add(dir);
        double pullRadius = getAbility().getRadius();

        startPulling(player, center, pullRadius);

        player.playSound(center, Sound.ENDERMAN_DEATH, 1f, 0.6f);
    }


    public void startPulling(Player player, Location loc, double pullRadius) {
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
                if (ticks > 60 || !isActive()) { cancel(); return; }

                double baseRadius = 1.5;

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

                for (LivingEntity entity : loc.getWorld().getLivingEntities()) {

                    if (entity instanceof Player) {
                        if (arena.getType().isTeamsMode()) {
                            Player target = (Player) entity;
                            if (arena.getTeam(player).equals(arena.getTeam(target))) continue;
                        }
                    }

                    if (entity.equals(player)) continue;
                    if (entity.getLocation().distance(loc) <= pullRadius) {
                        Vector pull = loc.toVector().subtract(entity.getLocation().toVector()).normalize().multiply(0.3);
                        entity.setVelocity(entity.getVelocity().add(pull));
                    }
                }

                angle += Math.PI / 40;
                ticks += 2;
            }
        }.runTaskTimer(QuirkBattlesPlugin.getInstance(), 0L, 2L);
    }
}
