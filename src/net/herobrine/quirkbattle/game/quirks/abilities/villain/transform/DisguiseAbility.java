package net.herobrine.quirkbattle.game.quirks.abilities.villain.transform;

import net.herobrine.gamecore.GameCoreMain;
import net.herobrine.gamecore.GameState;
import net.herobrine.quirkbattle.QuirkBattlesPlugin;
import net.herobrine.quirkbattle.game.quirks.abilities.Abilities;
import net.herobrine.quirkbattle.game.quirks.abilities.Ability;
import net.herobrine.quirkbattle.game.quirks.villain.Transform;
import net.herobrine.quirkbattle.util.DisguisePlayer;
import net.herobrine.quirkbattle.util.Quirk;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerToggleSneakEvent;
import org.bukkit.scheduler.BukkitRunnable;

public class DisguiseAbility extends Ability {
    private boolean isDisguised = false;
    private static final int DISGUISE_COST = 20;
    private String originalName;
    private Player disguisedAs;
    
    public DisguiseAbility(Abilities ability, Quirk quirk, int id, int slot) {
        super(ability, quirk, id, slot);
    }

    @Override
    public void doAbility(Player player) {
        Transform transform = (Transform) quirk;
        Player targetPlayer = transform.getTargetPlayer();
        
        if (targetPlayer == null) {
            player.sendMessage(ChatColor.RED + "No target selected for disguise!");
            return;
        }
        
        // Check if player has enough blood
        if (transform.getBloodAmount() < DISGUISE_COST) {
            player.sendMessage(ChatColor.RED + "You need more blood to disguise! (" + transform.getBloodAmount() + "/" + DISGUISE_COST + ")");
            player.playSound(player.getLocation(), Sound.VILLAGER_NO, 1.0f, 1.0f);
            return;
        }
        
        // Consume initial blood cost
        transform.setBlood(transform.getBloodAmount() - DISGUISE_COST);
        
        // Store original name for later restoration
        originalName = player.getName();
        disguisedAs = targetPlayer;
        
        try {
            // Apply skin disguise using DisguisePlayer utility
            arena.getQuirkBattleGame().getDisguiseManager().giveDisguise(player, targetPlayer.getName());

            // Apply disguise effect
            isDisguised = true;
            
            // Visual and sound effects for disguise
            player.playSound(player.getLocation(), Sound.ZOMBIE_UNFECT, 0.5f, 1.5f);
            
            // Send messages
            player.sendMessage(ChatColor.GRAY + "You disguised yourself as " + targetPlayer.getName() + "!");
            GameCoreMain.getInstance().sendActionBar(player, 
                ChatColor.GRAY + "Disguised as " + targetPlayer.getName() + 
                ChatColor.RED + " - Blood: " + transform.getBloodAmount());
            
            // Set a timer to drain blood over time
            new BukkitRunnable() {
                // Blood drain rate (per second)
                private static final int BLOOD_DRAIN_RATE = 4;
                
                @Override
                public void run() {
                    // Check if disguise should end
                    if (arena.getState() != GameState.LIVE) {
                        removeDisguise(player);
                        cancel();
                        return;
                    }
                    if (!isDisguised) {
                        cancel();
                        return;
                    }
                    
                    // Get current blood amount
                    int currentBlood = transform.getBloodAmount();
                    
                    // Check if out of blood
                    if (currentBlood <= 0) {
                        player.sendMessage(ChatColor.RED + "You've run out of blood! Your disguise has ended.");
                        removeDisguise(player);
                        cancel();
                        return;
                    }
                    
                    // Drain blood
                    transform.setBlood(Math.max(currentBlood - BLOOD_DRAIN_RATE, 0));

                    
                    // Warn player when blood is low
                    if (transform.getBloodAmount() <= 10 && transform.getBloodAmount() > 0) {
                        player.sendMessage(ChatColor.YELLOW + "Your blood is running low! Disguise will end soon!");
                    }
                }
            }.runTaskTimer(QuirkBattlesPlugin.getInstance(), 0L, 20L);
            
        } catch (Exception e) {
            player.sendMessage(ChatColor.RED + "Failed to apply disguise: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    private void removeDisguise(Player player) {
        if (!isDisguised || !player.isOnline()) return;
        
        try {
            // Reset player's skin to their original skin
            arena.getQuirkBattleGame().getDisguiseManager().giveDisguise(player, originalName);

            // Reset player's name to their original name
           // HerobrinePVPCore.changeName(originalName, player);

            isDisguised = false;
            player.sendMessage(ChatColor.RED + "Your disguise has ended!");
            player.playSound(player.getLocation(), Sound.ZOMBIE_UNFECT, 1.0f, 1.0f);

            if (arena.getState() == GameState.LIVE) {
                setCooldown(System.currentTimeMillis());
                doAbilityCooldown();
            }
        } catch (Exception e) {
            player.sendMessage(ChatColor.RED + "Failed to remove disguise: " + e.getMessage());
            e.printStackTrace();
        }
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
    
    /**
     * Special case check for the ability
     * @param player The player using the ability
     * @return Whether the special case passes
     */
    public boolean doesCasePass(Player player) {
        Transform transform = (Transform) quirk;
        
        // Can't disguise if already disguised
        if (isDisguised) {
            return false;
        }
        
        // Can't disguise if already transformed
        if (transform.isTransformed()) {
            return false;
        }
        
        // Can't disguise if no target has been selected
        if (transform.getTargetPlayer() == null) {
            return false;
        }
        
        // Check if player has enough blood
        if (transform.getBloodAmount() < DISGUISE_COST) {
            return false;
        }

        return true;
    }
    
    /**
     * What to do if the special case doesn't pass
     * @param player The player using the ability
     */
    public void doNoPass(Player player) {
        Transform transform = (Transform) quirk;
        
        if (isDisguised) {
            player.sendMessage(ChatColor.RED + "You are already disguised!");
        } else if (transform.isTransformed()) {
            player.sendMessage(ChatColor.RED + "You can't disguise while transformed!");
        } else if (transform.getTargetPlayer() == null) {
            player.sendMessage(ChatColor.RED + "You need to collect blood from a target first!");
        } else if (transform.getBloodAmount() < DISGUISE_COST) {
            player.sendMessage(ChatColor.RED + "Not enough blood!");
            player.playSound(player.getLocation(), Sound.VILLAGER_NO, 1f, 2f);
            GameCoreMain.getInstance().sendActionBar(player, "&c&lNOT ENOUGH BLOOD");
        }
        
        player.playSound(player.getLocation(), Sound.VILLAGER_NO, 1.0f, 1.0f);
    }

    /**
     * Event handler for player sneaking
     * Allows player to cancel disguise by sneaking
     */

    @EventHandler
    public void onPlayerSneak(PlayerToggleSneakEvent event) {
        Player player = event.getPlayer();
        // Check if this is our player, they are disguised, and they are sneaking (not un-sneaking)
        if (player.getUniqueId().equals(uuid) && isDisguised && event.isSneaking()) {
            player.sendMessage(ChatColor.YELLOW + "You canceled your disguise by sneaking!");
            removeDisguise(player);
        }
    }
    
    /**
     * Event handler for player death
     * Removes the disguise if the player dies
     */
    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
        Player player = event.getEntity();
        
        // Check if this is our player and they are disguised
        if (player.getUniqueId().equals(uuid) && isDisguised) {
            removeDisguise(player);
        }
    }
    
    /**
     * Override the remove method to ensure disguise is removed when the ability is removed
     * This handles game end and other cases where the ability might be removed
     */
    @Override
    public void remove() {
        // If the player is disguised, remove the disguise
        if (isDisguised) {
            Player player = Bukkit.getPlayer(uuid);
            if (player != null && player.isOnline()) {
                removeDisguise(player);
            }
        }
        
        // Call the parent remove method
        super.remove();
    }
}