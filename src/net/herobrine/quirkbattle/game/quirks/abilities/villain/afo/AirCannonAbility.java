package net.herobrine.quirkbattle.game.quirks.abilities.villain.afo;

import net.herobrine.quirkbattle.QuirkBattlesPlugin;
import net.herobrine.quirkbattle.game.CustomDeathCause;
import net.herobrine.quirkbattle.game.quirks.abilities.Abilities;
import net.herobrine.quirkbattle.game.quirks.abilities.Ability;
import net.herobrine.quirkbattle.util.projectile.PortalProjectileService;
import net.herobrine.quirkbattle.util.Quirk;
import net.minecraft.server.v1_8_R3.EnumParticle;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

public class AirCannonAbility extends Ability {
    private PortalProjectileService portalService;

    public AirCannonAbility(Abilities ability, Quirk quirk, int id, int slot) {
        super(ability, quirk, id, slot);
    }

    @Override
    public void doAbility(Player player) {
        // Initialize portal service with current managers
        portalService = new PortalProjectileService(arena.getQuirkBattleGame().getWarpGateManagers());

        player.playSound(player.getLocation(), Sound.GHAST_FIREBALL, 1f, .8f);
        doVFX(player);
    }

    public void doVFX(Player player) {
        Location start = player.getEyeLocation();
        Vector direction = start.getDirection().normalize();

        double maxRange = 35.0;
        double maxRadius = 1.7;
        int maxTicks = 20; // Duration of animation

        // Create cone attack using the fixed service
        PortalProjectileService.ConeParticleAttack coneAttack = portalService.createConeAttack(
                start,
                direction,
                maxRange,
                maxRadius
        );

        new BukkitRunnable() {
            int tick = 0;

            @Override
            public void run() {
                if (tick >= maxTicks) {
                    cancel();
                    return;
                }

                // Progress from 0 to 1 over the animation
                double progress = (double) (tick + 1) / maxTicks; // Start with some progress so particles are visible

                // Generate particles using the service and get back the locations
                List<Location> particleLocations = coneAttack.generateParticles(progress, 20, (location) -> {
                    // Spawn visual particles at each location
                    spawnRGBParticles(location, 237, 240, 245, true);
                    spawnRGBParticles(location, 57, 64, 59, true);
                });

                // Do collision checks on some of the generated locations
                for (int i = 0; i < particleLocations.size(); i++) {
                    if (i % 3 == 0) { // Check every 3rd particle for performance
                        doCollision(particleLocations.get(i));
                    }
                }

                tick++;
            }
        }.runTaskTimer(QuirkBattlesPlugin.getInstance(), 0L, 1L);
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

            Vector knockback = player.getLocation().toVector()
                    .subtract(caster.getLocation().toVector())
                    .normalize()
                    .multiply(1.7);
            player.setVelocity(knockback);

            doDamageTo(caster, player, ability.getDamage(), CustomDeathCause.AIR_CANNON);
        }
    }
}