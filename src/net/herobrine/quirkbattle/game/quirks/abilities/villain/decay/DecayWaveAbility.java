package net.herobrine.quirkbattle.game.quirks.abilities.villain.decay;

import net.herobrine.gamecore.GameState;
import net.herobrine.quirkbattle.QuirkBattlesPlugin;
import net.herobrine.quirkbattle.game.CustomDeathCause;
import net.herobrine.quirkbattle.game.quirks.abilities.Abilities;
import net.herobrine.quirkbattle.game.quirks.abilities.Ability;
import net.herobrine.quirkbattle.game.quirks.abilities.SpecialCase;
import net.herobrine.quirkbattle.game.quirks.villain.Decay;
import net.herobrine.quirkbattle.util.Quirk;
import net.herobrine.quirkbattle.util.projectile.PortalProjectileService;
import net.minecraft.server.v1_8_R3.EnumParticle;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

public class DecayWaveAbility extends Ability implements SpecialCase {
    private static final int DECAY_WAVE_COST = 30;
    private static final int SLOWNESS_DURATION = 3 * 20; // 3 seconds in ticks
    private final List<UUID> affectedPlayers = new ArrayList<>();
    private final Decay decay;
    private final Player player;
    private PortalProjectileService portalService;
    
    public DecayWaveAbility(Abilities ability, Quirk quirk, int id, int slot) {
        super(ability, quirk, id, slot);
        this.decay = (Decay) quirk;
        this.player = Bukkit.getPlayer(uuid);
    }
//spawnRGBParticles(particleLoc, 60, 60, 60, true); // Brighter and visible to all
    @Override
    public void doAbility(Player player) {
        portalService = new PortalProjectileService(arena.getQuirkBattleGame().getWarpGateManagers());
        player.playSound(player.getLocation(), Sound.FIZZ, 1.0f, 0.5f);
        player.getWorld().playSound(player.getLocation(), Sound.WITHER_HURT, 0.5f, 1.5f);

        player.sendMessage(ChatColor.DARK_GRAY + "You release a wave of decay in front of you!");

        affectedPlayers.clear();
        createDecayWave();
    }
    
    private void createDecayWave() {
        Location start = player.getEyeLocation();
        Vector direction = start.getDirection().normalize();

        double maxRange = 35.0;
        double maxRadius = 1.7;
        int maxTicks = 20; // Duration of animation


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
                    spawnRGBParticles(location, 60, 60, 60, true);
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

            if (affectedPlayers.contains(player.getUniqueId())) continue;

            affectedPlayers.add(player.getUniqueId());
            Location impact = player.getLocation();

            spawnParticle(impact, EnumParticle.BLOCK_CRACK, true);

            impact.getWorld().playSound(impact, Sound.FIZZ, 0.5f, 0.5f);

            player.getWorld().playEffect(player.getLocation().add(0, 1, 0), org.bukkit.Effect.STEP_SOUND, org.bukkit.Material.SOUL_SAND);

            player.addPotionEffect(new PotionEffect(PotionEffectType.SLOW, SLOWNESS_DURATION, 1), true);
            decay.giveStaminaBoost(5);
            player.sendMessage(ChatColor.DARK_GRAY + caster.getName() + ChatColor.GRAY + " hit you with a wave of decay!");
            caster.sendMessage(ChatColor.GRAY + "Your decay wave hit " + player.getName() + "!");

            doDamageTo(caster, player, ability.getDamage(), CustomDeathCause.DECAY_WAVE);

        }
    }

    @Override
    public boolean doesCasePass(Player player) {
        // Check if player has enough decay charge
        if (decay.getDecayCharge() < DECAY_WAVE_COST) {
            return false;
        }
        
        return true;
    }
    
    @Override
    public void doNoPass(Player player) {
        player.sendMessage(ChatColor.RED + "You don't have enough decay charge! (" + decay.getDecayCharge() + "/" + DECAY_WAVE_COST + ")");
        player.playSound(player.getLocation(), Sound.VILLAGER_NO, 1.0f, 1.0f);
    }
}