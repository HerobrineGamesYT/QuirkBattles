package net.herobrine.quirkbattle.game.quirks.abilities.villain.decay;

import net.herobrine.gamecore.GameState;
import net.herobrine.quirkbattle.QuirkBattlesPlugin;
import net.herobrine.quirkbattle.game.CustomDeathCause;
import net.herobrine.quirkbattle.game.quirks.abilities.Abilities;
import net.herobrine.quirkbattle.game.quirks.abilities.Ability;
import net.herobrine.quirkbattle.game.quirks.abilities.SpecialCase;
import net.herobrine.quirkbattle.game.quirks.villain.Decay;
import net.herobrine.quirkbattle.util.Quirk;
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
import org.bukkit.scheduler.BukkitRunnable;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class DecayAuraAbility extends Ability implements SpecialCase {
    private static final int DECAY_AURA_DURATION = 5; // seconds
    
    private final Decay decay;
    private final Player player;
    private boolean isAuraActive = false;
    private int addToY = 0;
    private final Map<UUID, Long> lastDamageTime = new HashMap<>();
    
    public DecayAuraAbility(Abilities ability, Quirk quirk, int id, int slot) {
        super(ability, quirk, id, slot);
        this.decay = (Decay) quirk;
        this.player = Bukkit.getPlayer(uuid);
    }

    @Override
    public void doAbility(Player player) {
        // Play sound effects
        player.playSound(player.getLocation(), Sound.FIZZ, 1.0f, 0.5f);
        player.getWorld().playSound(player.getLocation(), Sound.WITHER_SPAWN, 0.5f, 1.5f);
        
        // Send messages
        player.sendMessage(ChatColor.DARK_GRAY + "You create an aura of decay around you!");
        
        // Create the decay aura
        createDecayAura();
    }
    
    private void createDecayAura() {
        isAuraActive = true;
        lastDamageTime.clear();
        
        // Create visual effect and damage application for the decay aura
        new BukkitRunnable() {
            int ticks = 0;
            final int maxTicks = DECAY_AURA_DURATION * 20; // Convert seconds to ticks (20 ticks per second)
            
            @Override
            public void run() {
                if (ticks >= maxTicks || !isAuraActive || !isActive() ||  arena.getState() != GameState.LIVE ||
                    !arena.getQuirkBattleGame().getAlivePlayers().contains(player.getUniqueId())) {
                    isAuraActive = false;
                    if (isActive()) {
                        setCooldown(System.currentTimeMillis());
                        doAbilityCooldown();
                    }
                    player.sendMessage(ChatColor.RED + "Your decay aura has worn off!");
                    player.playSound(player.getLocation(), Sound.FIZZ, 0.5f, 0.5f);
                    cancel();
                    return;
                }
                
                // Create visual effect for the aura
                if (ticks % 5 == 0) { // Every 5 ticks (0.25 seconds) to reduce particle load
                    createAuraVisualEffect();
                }

                // Apply damage to nearby players every second
                if (ticks % 20 == 0) { // Every 20 ticks (1 second)
                    applyAuraDamage();
                }
                
                ticks++;
            }
        }.runTaskTimer(QuirkBattlesPlugin.getInstance(), 0L, 1L);
    }
    
    private void createAuraVisualEffect() {
        Location center = player.getLocation();

        if (addToY > 3) addToY = 0;
        for (double t = 0; t < 1000; t += 0.5) {
            double x = ability.getRadius() * Math.sin(t);
            double z = ability.getRadius() * Math.cos(t);

            spawnRGBParticles(new Location(center.getWorld(), center.getX() + x, center.getY() + addToY, center.getZ() + z), 30, 30, 30, false);
            spawnRGBParticles(new Location(center.getWorld(), center.getX() + x, center.getY(), center.getZ() + z), 100, 0, 100, true);
        }
        addToY = addToY + 1;
    }
    
    private void applyAuraDamage() {
        // Get nearby players
        for (Entity entity : player.getNearbyEntities(ability.getRadius(), ability.getRadius(), ability.getRadius())) {
            if (!(entity instanceof Player)) continue;
            Player target = (Player) entity;

            // Skip spectators and dead players
            if (arena.getSpectators().contains(target.getUniqueId())) continue;
            //if (!arena.getQuirkBattleGame().getAlivePlayers().contains(target.getUniqueId())) continue;
            
            // Skip teammates in team modes
            if (arena.getType().isTeamsMode() && arena.getTeam(player).equals(arena.getTeam(target))) continue;
            
            // Skip self
            if (target == player) continue;
            
            // Apply damage
            doDamageTo(player, target, ability.getDamage(), CustomDeathCause.DECAY_AURA);

            // Send messages only the first time a player is damaged
            if (!lastDamageTime.containsKey(target.getUniqueId())) {
                target.sendMessage(ChatColor.DARK_GRAY + player.getName() + ChatColor.GRAY + "'s decay aura is damaging you!");
                player.sendMessage(ChatColor.GRAY + "Your decay aura is damaging " + target.getName() + "!");
            }
            
            // Update last damage time
            lastDamageTime.put(target.getUniqueId(), System.currentTimeMillis());
            decay.giveStaminaBoost(5);
            // Play sound effect at target
            target.playSound(target.getLocation(), Sound.FIZZ, 0.5f, 0.5f);
            
            // Visual effect at target
            target.getWorld().playEffect(target.getLocation().add(0, 1, 0), org.bukkit.Effect.STEP_SOUND, org.bukkit.Material.SOUL_SAND);
        }
    }

    
    @Override
    public boolean doesCasePass(Player player) {
        return !isAuraActive;
    }
    
    @Override
    public void doNoPass(Player player) {
        player.sendMessage(ChatColor.RED + "Decay Aura is already active!");
        player.playSound(player.getLocation(), Sound.VILLAGER_NO, 1.0f, 1.0f);
    }
    
    // Method to forcibly stop the aura (e.g., when player dies)
    public void stopAura() {
        if (isAuraActive) {
            player.sendMessage(ChatColor.RED + "Your decay aura has been stopped!");
            player.playSound(player.getLocation(), Sound.FIZZ, 0.5f, 0.5f);
        }
        isAuraActive = false;
    }
    
    @Override
    public void remove() {
        stopAura();
        super.remove();
    }
}