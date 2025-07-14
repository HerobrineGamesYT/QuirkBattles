package net.herobrine.quirkbattle.game.quirks.abilities.villain.afo;

import net.herobrine.gamecore.Arena;
import net.herobrine.gamecore.Manager;
import net.herobrine.quirkbattle.QuirkBattlesPlugin;
import net.herobrine.quirkbattle.game.CustomDeathCause;
import net.herobrine.quirkbattle.game.quirks.abilities.Abilities;
import net.herobrine.quirkbattle.game.quirks.abilities.Ability;
import net.herobrine.quirkbattle.util.Quirk;
import net.minecraft.server.v1_8_R3.EnumParticle;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.util.concurrent.ThreadLocalRandom;

public class AirCannonAbility extends Ability {


    public AirCannonAbility(Abilities ability, Quirk quirk, int id, int slot) {
        super(ability, quirk, id, slot);
    }

    private final ThreadLocalRandom random = ThreadLocalRandom.current();

    @Override
    public void doAbility(Player player) {
        player.playSound(player.getLocation(), Sound.GHAST_FIREBALL, 1f, .8f);
        doVFX(player);
    }

    public void doVFX(Player player) {
        Location start = player.getEyeLocation();
        Vector direction = start.getDirection().normalize();

        double maxRange = 35.0;
        double maxRadius = 1.7;
        int maxTicks = 20; // Duration of animation

        new BukkitRunnable() {
            int tick = 0;

            @Override
            public void run() {
                if (tick >= maxTicks) {
                    cancel();
                    return;
                }

                // Progress from 0 to 1 over the animation
                double progress = (double) tick / maxTicks;

                // Current cone reaches this far
                double currentMaxDistance = maxRange * progress;

                // Spawn particles for this frame
                for (int i = 0; i < 20; i++) { // 20 particles per tick
                    // Random distance from 0 to current max distance
                    double distance = Math.random() * currentMaxDistance;

                    // Radius increases with distance
                    double radius = (distance / maxRange) * maxRadius;

                    Vector particleOffset = generateConePoint(direction, distance, radius);
                    Location loc = start.clone().add(particleOffset);
                    if (i % 3 == 0) doCollision(loc);
                    spawnRGBParticles(loc, 237, 240, 245, true);
                    spawnRGBParticles(loc, 57, 64, 59, true);
                }


                tick++;
            }
        }.runTaskTimer(QuirkBattlesPlugin.getInstance(), 0L, 1L);
    }



     //Generates a random point within a cone

    public Vector generateConePoint(Vector direction, double distance, double radius) {
        // Create two perpendicular vectors to the direction
        Vector perpendicular1 = getPerpendicular(direction);
        Vector perpendicular2 = direction.clone().crossProduct(perpendicular1).normalize();

        // Generate random point in a circle (uniform distribution)
        double angle = random.nextDouble() * 2 * Math.PI;
        double r = Math.sqrt(random.nextDouble()) * radius;

        // Convert polar coordinates to cartesian in the perpendicular plane
        Vector circlePoint = perpendicular1.clone().multiply(Math.cos(angle) * r)
                .add(perpendicular2.clone().multiply(Math.sin(angle) * r));

        // Add the distance component along the cone axis
        return direction.clone().multiply(distance).add(circlePoint);
    }


    //Gets a perpendicular vector to the given vector

    private Vector getPerpendicular(Vector vector) {
        Vector result = new Vector(0, 1, 0);

        // If the vector is parallel to Y-axis, use X-axis instead
        if (Math.abs(vector.getY()) > 0.9) {
            result = new Vector(1, 0, 0);
        }

        // Create perpendicular vector using cross product
        return vector.clone().crossProduct(result).normalize();
    }

    public void doCollision(Location loc) {
        for (Entity ent : loc.getWorld().getNearbyEntities(loc, ability.getRadius(), 1, ability.getRadius())) {
            if (!(ent instanceof Player)) continue;
            Player player = (Player) ent;
            Player caster = Bukkit.getPlayer(uuid);
            if (player.getUniqueId() == caster.getUniqueId()) continue;
            if (arena.getType().isTeamsMode()) {
                if (arena.getTeam(player).equals(arena.getTeam(caster))) continue;
            }
                     Location impact = player.getLocation();
                     spawnParticle(impact, EnumParticle.EXPLOSION_LARGE, true);
                     impact.getWorld().playSound(impact, Sound.EXPLODE, 2f, 0.7f);
                     Vector knockback = player.getLocation().toVector().subtract(caster.getLocation().toVector()).normalize().multiply(1.7);
                     player.setVelocity(knockback);
                     doDamageTo(caster, player, ability.getDamage(), CustomDeathCause.AIR_CANNON);

        }
    }
}

