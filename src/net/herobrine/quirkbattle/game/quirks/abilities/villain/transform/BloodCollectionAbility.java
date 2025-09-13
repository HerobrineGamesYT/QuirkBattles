package net.herobrine.quirkbattle.game.quirks.abilities.villain.transform;

import net.herobrine.gamecore.GameCoreMain;
import net.herobrine.quirkbattle.game.quirks.abilities.Abilities;
import net.herobrine.quirkbattle.game.quirks.abilities.Ability;
import net.herobrine.quirkbattle.game.quirks.villain.Transform;
import net.herobrine.quirkbattle.util.Quirk;
import org.bukkit.ChatColor;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;

public class BloodCollectionAbility extends Ability {

    public BloodCollectionAbility(Abilities ability, Quirk quirk, int id, int slot) {
        super(ability, quirk, id, slot);
    }

    @Override
    public void doAbility(Player player) {
        Transform transform = (Transform) quirk;
        transform.setBloodCollection(true);
        
        // Set hitCount to 0 to allow for 3 boosted hits
        transform.setHitCount(0);
        
        player.sendMessage(ChatColor.RED + "Blood Collection activated! Your next 3 attacks will deal increased damage and collect blood.");
        player.playSound(player.getLocation(), Sound.GHAST_SCREAM, 0.5f, 2.0f);
        
        // Visual feedback
        GameCoreMain.getInstance().sendActionBar(player, ChatColor.RED + "Blood Collection Mode Activated - Next 3 hits boosted!");
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
        
        // Can't collect blood if already in blood collection mode
        if (transform.shouldUseAbilityAttack()) {
            return false;
        }
        
        // Can't collect blood if already at max
        if (transform.getBloodAmount() >= 100) {
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
        
        if (transform.shouldUseAbilityAttack()) {
            player.sendMessage(ChatColor.RED + "You are already in blood collection mode!");
        } else if (transform.getBloodAmount() >= 100) {
            player.sendMessage(ChatColor.RED + "Your blood vials are already full!");
        }
        
        player.playSound(player.getLocation(), Sound.VILLAGER_NO, 1.0f, 1.0f);
    }
}