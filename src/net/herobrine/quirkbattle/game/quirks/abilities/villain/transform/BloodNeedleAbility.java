package net.herobrine.quirkbattle.game.quirks.abilities.villain.transform;

import net.herobrine.quirkbattle.QuirkBattlesPlugin;
import net.herobrine.quirkbattle.game.CustomDeathCause;
import net.herobrine.quirkbattle.game.quirks.abilities.Abilities;
import net.herobrine.quirkbattle.game.quirks.abilities.Ability;
import net.herobrine.quirkbattle.game.quirks.abilities.SpecialCase;
import net.herobrine.quirkbattle.game.quirks.villain.Transform;
import net.herobrine.quirkbattle.util.Quirk;
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
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.List;

public class BloodNeedleAbility extends Ability implements SpecialCase {
    private static final int BLOOD_GAIN = 15;
    private static final double NEEDLE_RANGE = 10.0;
    private static final int BLOOD_NEEDLE_DAMAGE = 5;

    private Player target;
    public BloodNeedleAbility(Abilities ability, Quirk quirk, int id, int slot) {
        super(ability, quirk, id, slot);
    }

    @Override
    public void doAbility(Player player) {
        Transform transform = (Transform) quirk;
        
        // Find target in line of sight (already done with the doNoPass method, but we'll still check again here just to prevent errors)

        if (target == null) {
            player.sendMessage(ChatColor.RED + "No target in range!");
            player.playSound(player.getLocation(), Sound.VILLAGER_NO, 1.0f, 1.0f);
            return;
        }
        
        // Apply damage to target
        doDamageTo(player, target, BLOOD_NEEDLE_DAMAGE, CustomDeathCause.BLOOD_NEEDLE);
        
        // Collect blood from target
        transform.setBlood(Math.min(transform.getBloodAmount() + BLOOD_GAIN, 100));
        
        // Store target for potential transformation
        transform.setTargetPlayer(target);
        
        // Visual and sound effects
        player.playSound(player.getLocation(), Sound.SHOOT_ARROW, 1.0f, 1.5f);
        target.playSound(target.getLocation(), Sound.HURT_FLESH, 1.0f, 1.0f);
        
        // Send messages
        player.sendMessage(ChatColor.RED + "You collected " + ChatColor.DARK_RED + BLOOD_GAIN + ChatColor.RED + " blood from " + target.getName() + "!");
        target.sendMessage(ChatColor.RED + player.getName() + ChatColor.DARK_RED + " collected blood from you with a needle!");
        
        // Create blood sipping effect
        createBloodSippingEffect(player, target);
    }
    
    /**
     * Creates a visual effect of blood traveling from target to player
     * @param player The player using the ability
     * @param target The target player
     */
    private void createBloodSippingEffect(Player player, Player target) {
        final Location startLoc = target.getLocation().add(0, 1.0, 0);
        final Location endLoc = player.getLocation().add(0, 1.0, 0);
        final Vector direction = endLoc.toVector().subtract(startLoc.toVector()).normalize().multiply(0.5);
        
        new BukkitRunnable() {
            Location currentLoc = startLoc.clone();
            int ticks = 0;
            int maxTicks = 20; // 1 second of particles
            
            @Override
            public void run() {
                if (ticks >= maxTicks) {
                    cancel();
                    return;
                }
                
                // Create blood particle at current location
                spawnRGBParticles(currentLoc, 255, 0, 0, true);
                
                // Play sipping sound every few ticks
                if (ticks % 4 == 0) {
                    player.playSound(player.getLocation(), Sound.DRINK, 0.5f, 2.0f);
                }
                
                // Move current location toward player
                currentLoc.add(direction);
                
                ticks++;
            }
        }.runTaskTimer(QuirkBattlesPlugin.getInstance(), 0L, 1L);
    }
    
    /**
     * Finds a player target in line of sight
     * @param player The player using the ability
     * @param range The maximum range to check
     * @return The target player, or null if no target found
     */
    private Player findTargetPlayer(Player player, double range) {
        List<Entity> nearbyEntities = player.getNearbyEntities(range, range, range);
        List<Player> nearbyPlayers = new ArrayList<>();
        
        // Filter for players only
        for (Entity entity : nearbyEntities) {
            if (entity instanceof Player) {
                nearbyPlayers.add((Player) entity);
            }
        }
        
        if (nearbyPlayers.isEmpty()) {
            return null;
        }
        
        // Get player's looking direction
        Vector direction = player.getLocation().getDirection();
        Player closestPlayer = null;
        double closestDot = 0.7; // Minimum dot product (roughly 45 degrees)
        double closestDistance = range;
        
        for (Player target : nearbyPlayers) {
            // Skip players on the same team
            if (arena.getType().isTeamsMode()) {
                if (arena.getTeam(player) == arena.getTeam(target)) continue;
            }

            // Calculate vector from player to target
            Vector toTarget = target.getLocation().toVector().subtract(player.getLocation().toVector()).normalize();
            
            // Calculate dot product (how closely they're looking at the target)
            double dot = direction.dot(toTarget);
            double distance = player.getLocation().distance(target.getLocation());
            
            // Check if this is the closest target they're looking at
            if (dot > closestDot && distance < closestDistance) {
                closestDot = dot;
                closestDistance = distance;
                closestPlayer = target;
            }
        }
        
        return closestPlayer;
    }
    
    /**
     * Applies damage to a target player
     * @param damager The player dealing damage
     * @param target The player receiving damage
     * @param damage The amount of damage to deal
     * @param cause The custom death cause
     */
    @Override
    public void doDamageTo(Player damager, Player target, double damage, CustomDeathCause cause) {
        arena.getQuirkBattleGame().getLastAbilityAttacker().put(target.getUniqueId(), damager.getUniqueId());
        arena.getQuirkBattleGame().getCustomDeathCause().put(target.getUniqueId(), cause);
        
        EntityDamageEvent event = new EntityDamageEvent(target, EntityDamageEvent.DamageCause.CUSTOM, damage);
        target.setLastDamageCause(event);
        org.bukkit.Bukkit.getPluginManager().callEvent(event);
    }
    
    @EventHandler
    @Override
    public void onClick(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        if (player.getUniqueId() != uuid) return;
        if (!isActive()) return;
        
        boolean isRightClick = event.getAction().equals(Action.RIGHT_CLICK_AIR) || event.getAction().equals(Action.RIGHT_CLICK_BLOCK);
        
        if (isRightClick && player.getItemInHand().isSimilar(getItem())) {
            executeAbility(player);
        }
    }

    @Override
    public boolean doesCasePass(Player player) {

        if (stats.getBlood() >= stats.getMaxBlood()) {
            player.sendMessage(ChatColor.RED + "Your blood vial is already full!");
            return false;
        }

        target = findTargetPlayer(player, NEEDLE_RANGE);

        if (target == null) {
            player.sendMessage(ChatColor.RED + "No target in range!");
            player.playSound(player.getLocation(), Sound.VILLAGER_NO, 1.0f, 1.0f);
            return false;
        }

        return true;
    }

    @Override
    public void doNoPass(Player player) {

    }
}