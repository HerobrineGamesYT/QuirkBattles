package net.herobrine.quirkbattle.game.quirks.abilities.villain.decay;

import net.herobrine.gamecore.GameCoreMain;
import net.herobrine.gamecore.GameState;
import net.herobrine.quirkbattle.QuirkBattlesPlugin;
import net.herobrine.quirkbattle.game.CustomDeathCause;
import net.herobrine.quirkbattle.game.quirks.abilities.Abilities;
import net.herobrine.quirkbattle.game.quirks.abilities.Ability;
import net.herobrine.quirkbattle.game.quirks.abilities.SpecialCase;
import net.herobrine.quirkbattle.game.quirks.villain.Decay;
import net.herobrine.quirkbattle.game.stats.PlayerStats;
import net.herobrine.quirkbattle.util.Quirk;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class DisintegrationAbility extends Ability implements SpecialCase {
    private static final int DISINTEGRATION_COST = 80;
    private static final double DAMAGE_BOOST = 20.0;
    private static final double DEFENSE_REDUCTION_PERCENT = 0.25; // 25% defense reduction
    private static final int EFFECT_DURATION = 5; // seconds
    
    private final Decay decay;
    private final Player player;
    private boolean isDisintegrationActive = false;
    private final Map<UUID, Long> defenseReductionEndTimes = new HashMap<>();
    
    public DisintegrationAbility(Abilities ability, Quirk quirk, int id, int slot) {
        super(ability, quirk, id, slot);
        this.decay = (Decay) quirk;
        this.player = Bukkit.getPlayer(uuid);
    }

    @Override
    public void doAbility(Player player) {
        // Play sound effects
        player.playSound(player.getLocation(), Sound.WITHER_SPAWN, 1.0f, 0.5f);
        player.getWorld().playSound(player.getLocation(), Sound.GHAST_SCREAM, 0.5f, 0.5f);
        
        // Send messages
        player.sendMessage(ChatColor.DARK_PURPLE + "You channel your decay to its maximum potential!");
        player.sendMessage(ChatColor.GRAY + "Your next attack within 5 seconds will deal massive damage and reduce the target's defense.");
        
        // Visual effect - particles around player's hands
        createHandParticles();
        
        // Activate disintegration effect
        activateDisintegration();
    }
    
    private void createHandParticles() {
        new BukkitRunnable() {
            int ticks = 0;
            final int maxTicks = 5 * 20; // 5 seconds in ticks
            
            @Override
            public void run() {
                if (ticks >= maxTicks || !isDisintegrationActive || !isActive() || arena.getState() != GameState.LIVE ||
                    !arena.getQuirkBattleGame().getAlivePlayers().contains(player.getUniqueId())) {

                    if (isDisintegrationActive) {
                        isDisintegrationActive = false;
                        player.sendMessage(ChatColor.RED + "Your disintegration has been cancelled!");
                    }
                    cancel();
                    return;
                }

                
                // Create particles around player's hands
                for (int i = 0; i < 5; i++) {
                    // Right hand particles (red and black)
                    spawnRGBParticles(player.getLocation().add(
                        Math.cos(ticks * 0.1 + i) * 0.5,
                        1.0 + Math.sin(ticks * 0.1) * 0.1,
                        Math.sin(ticks * 0.1 + i) * 0.5
                    ), 100, 0, 0, true);
                    
                    // Left hand particles (purple and black)
                    spawnRGBParticles(player.getLocation().add(
                        Math.cos(ticks * 0.1 + i + Math.PI) * 0.5,
                        1.0 + Math.sin(ticks * 0.1 + Math.PI) * 0.1,
                        Math.sin(ticks * 0.1 + i + Math.PI) * 0.5
                    ), 100, 0, 100, true);
                }
                
                ticks++;
            }
        }.runTaskTimer(QuirkBattlesPlugin.getInstance(), 0L, 1L);
    }

    private void activateDisintegration() {
        isDisintegrationActive = true;
        
        // Set a timer to deactivate disintegration after 5 seconds
        new BukkitRunnable() {
            @Override
            public void run() {
                if (isDisintegrationActive) {
                    isDisintegrationActive = false;
                    player.sendMessage(ChatColor.RED + "Your Disintegration ability has expired!");
                    player.playSound(player.getLocation(), Sound.FIZZ, 1.0f, 0.5f);
                    setCooldown(System.currentTimeMillis());
                    doAbilityCooldown();
                }
            }
        }.runTaskLater(QuirkBattlesPlugin.getInstance(), 5 * 20); // 5 seconds in ticks
    }
    
    @EventHandler
    public void onPlayerDamage(EntityDamageByEntityEvent event) {
        // Check if the damager is the player with disintegration active
        if (!(event.getDamager() instanceof Player)) return;
        Player damager = (Player) event.getDamager();
        
        if (!damager.getUniqueId().equals(player.getUniqueId())) return;
        if (!isDisintegrationActive) return;
        
        // Check if the entity being damaged is a player
        if (!(event.getEntity() instanceof Player)) return;
        Player target = (Player) event.getEntity();
        
        // Skip spectators and dead players
        if (arena.getSpectators().contains(target.getUniqueId())) return;
        if (!arena.getQuirkBattleGame().getAlivePlayers().contains(target.getUniqueId())) return;
        
        // Skip teammates in team modes
        if (arena.getType().isTeamsMode() && arena.getTeam(player).equals(arena.getTeam(target))) return;
        
        // Apply disintegration effects
        applyDisintegrationEffects(target);
        
        // Deactivate disintegration after use
        player.sendMessage(ChatColor.RED + "Your disentegration was deactivated because it was used.");
        isDisintegrationActive = false;
        setCooldown(System.currentTimeMillis());
        doAbilityCooldown();
    }
    
    private void applyDisintegrationEffects(Player target) {
        // Apply additional damage
        doDamageTo(player, target, ability.getDamage(), CustomDeathCause.DISINTEGRATION);

        
        // Apply defense reduction
        PlayerStats targetStats = arena.getQuirkBattleGame().getStats(target);
        int originalDefense = targetStats.getDefense();
        int reducedDefense = (int) (originalDefense * (1 - DEFENSE_REDUCTION_PERCENT));
        targetStats.setDefense(reducedDefense);
        
        // Store the original defense value and set a timer to restore it
        defenseReductionEndTimes.put(target.getUniqueId(), System.currentTimeMillis() + (EFFECT_DURATION * 1000));
        
        // Schedule defense restoration
        new BukkitRunnable() {
            @Override
            public void run() {
                if (defenseReductionEndTimes.containsKey(target.getUniqueId()) && 
                    System.currentTimeMillis() >= defenseReductionEndTimes.get(target.getUniqueId())) {
                    
                    // Restore defense if player is still online and in the game
                    if (target.isOnline() && arena.getQuirkBattleGame().getAlivePlayers().contains(target.getUniqueId())) {
                        PlayerStats stats = arena.getQuirkBattleGame().getStats(target);
                        stats.setDefense(originalDefense);
                        target.sendMessage(ChatColor.GREEN + "Your defense has been restored!");
                    }
                    
                    defenseReductionEndTimes.remove(target.getUniqueId());
                    cancel();
                }
            }
        }.runTaskTimer(QuirkBattlesPlugin.getInstance(), 20L, 20L); // Check every second
        
        // Send messages
        target.sendMessage(ChatColor.DARK_PURPLE + player.getName() + ChatColor.RED + " hit you with Disintegration! Your defense is reduced by 25% for " + EFFECT_DURATION + " seconds!");
        player.sendMessage(ChatColor.RED + "Your Disintegration hit " + target.getName() + "!");
        
        // Play sound effects
        target.playSound(target.getLocation(), Sound.WITHER_HURT, 1.0f, 0.5f);
        player.playSound(player.getLocation(), Sound.WITHER_HURT, 1.0f, 1.5f);
        
        // Visual effects
        target.getWorld().playEffect(target.getLocation().add(0, 1, 0), org.bukkit.Effect.STEP_SOUND, org.bukkit.Material.REDSTONE_BLOCK);
        
        // Action bar notification for defense reduction
        GameCoreMain.getInstance().sendActionBar(target, ChatColor.RED + "Defense reduced by 25% for " + EFFECT_DURATION + " seconds!");
    }

    @Override
    public boolean doesCasePass(Player player) {
        // Check if disintegration is already active
        return !isDisintegrationActive;
    }
    
    @Override
    public void doNoPass(Player player) {
        player.sendMessage(ChatColor.RED + "Disintegration is already active!");
        player.playSound(player.getLocation(), Sound.VILLAGER_NO, 1.0f, 1.0f);
    }


    @Override
    public void remove() {
        isDisintegrationActive = false;
        
        // Restore defense for all affected players
        for (UUID targetId : defenseReductionEndTimes.keySet()) {
            Player target = Bukkit.getPlayer(targetId);
            if (target != null && target.isOnline()) {
                // We don't know the original defense value here, so we'll just update the player stats
                // which should refresh their display and ensure consistency
                arena.getQuirkBattleGame().updatePlayerStats(target);
            }
        }
        
        defenseReductionEndTimes.clear();
        super.remove();
    }
}