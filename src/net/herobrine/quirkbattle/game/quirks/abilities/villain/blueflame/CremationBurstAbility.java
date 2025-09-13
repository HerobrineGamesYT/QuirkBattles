package net.herobrine.quirkbattle.game.quirks.abilities.villain.blueflame;

import net.herobrine.quirkbattle.QuirkBattlesPlugin;
import net.herobrine.quirkbattle.game.CustomDeathCause;
import net.herobrine.quirkbattle.game.quirks.abilities.Abilities;
import net.herobrine.quirkbattle.game.quirks.abilities.Ability;
import net.herobrine.quirkbattle.util.Quirk;
import net.herobrine.quirkbattle.util.projectile.PortalProjectileService;
import net.minecraft.server.v1_8_R3.EnumParticle;
import org.bukkit.*;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.sql.Array;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

public class CremationBurstAbility extends Ability {
    private final ThreadLocalRandom random = ThreadLocalRandom.current();
    private final List<UUID> affectedPlayers = new ArrayList<>();
    private PortalProjectileService portalService;

    public CremationBurstAbility(Abilities ability, Quirk quirk, int id, int slot) {
        super(ability, quirk, id, slot);
    }

    @Override
    public void doAbility(Player player) {
        portalService = new PortalProjectileService(arena.getQuirkBattleGame().getWarpGateManagers());
        player.playSound(player.getLocation(), Sound.GHAST_FIREBALL, 1f, .6f);
        doVFX(player);
        affectedPlayers.clear();
    }



    public void doVFX(Player player) {
        Location start = player.getEyeLocation();
        Vector direction = start.getDirection().normalize();

        double maxRange = 20.0;
        double maxRadius = 2;
        int maxTicks = 20; // Duration of animation

        PortalProjectileService.ConeParticleAttack coneAttack = portalService.createConeAttack(start, direction, maxRange, maxRadius);

        new BukkitRunnable() {
            int tick = 0;

            @Override
            public void run() {
                if (tick >= maxTicks) {
                    cancel();
                    return;
                }

                double progress = (double) (tick + 1) / maxTicks;

                // Current cone reaches this far
                double currentMaxDistance = maxRange * progress;

                // Generate particles using the service and get back the locations
                List<Location> particleLocations = coneAttack.generateParticles(progress, 20, (location) -> {
                    // Spawn visual particles at each location
                    spawnRGBParticles(location, 135, 206, 250, true);

                    spawnRGBParticles(location, 25, 25, 112, true);

                    spawnRGBParticles(location, 138, 43, 226, true);
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

            if (affectedPlayers.contains(player.getUniqueId())) continue;

            affectedPlayers.add(player.getUniqueId());
            Location impact = player.getLocation();

            spawnParticle(impact, EnumParticle.BLOCK_CRACK, true);

            impact.getWorld().playSound(impact, Sound.GHAST_FIREBALL, 0.5f, 0.5f);

            player.getWorld().playEffect(player.getLocation().add(0, 1, 0), org.bukkit.Effect.STEP_SOUND, Material.LAPIS_BLOCK);

            player.sendMessage(ChatColor.AQUA + caster.getName() + ChatColor.AQUA + " hit you with their Cremation Burst ability!");
            caster.sendMessage(ChatColor.AQUA + "Your cremation burst hit " + player.getName() + "!");

            doDamageTo(caster, player, ability.getDamage(), CustomDeathCause.CREAMATION_BURST);

            doDOTFor(caster, player, 20L);
            doDOTFor(caster, player, 40L);
        }
    }

    public void doDOTFor(Player caster, Player victim, long delay) {

        new BukkitRunnable() {
            @Override
            public void run() {
                victim.getWorld().playEffect(victim.getLocation().add(0, 1, 0), org.bukkit.Effect.STEP_SOUND, Material.LAPIS_BLOCK);
                doDamageTo(caster, victim, 8, CustomDeathCause.CREAMATION_BURST);
            }
        }.runTaskLater(QuirkBattlesPlugin.getInstance(), delay);

    }

}
