package net.herobrine.quirkbattle.util.ofa;

import net.herobrine.gamecore.Manager;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Sound;
import org.bukkit.entity.Player;

/**
 * Manages Fa-Jin Energy for Enhanced One For All
 */
public class FaJinSystem {

    private int currentEnergy = 0;
    private final int maxEnergy = 200;
    private final Player player;

    public FaJinSystem(Player player) {
        this.player = player;
    }

    /**
     * Add Fa-Jin energy (from attacks)
     * @param amount Amount to add
     * @return true if energy was added, false if at max
     */
    public void addEnergy(int amount) {
        if (currentEnergy >= maxEnergy) {
            return;
        }

        int oldEnergy = currentEnergy;
        currentEnergy = Math.min(currentEnergy + amount, maxEnergy);

        // Visual/audio feedback for energy gain
        if (currentEnergy > oldEnergy) {
            player.playSound(player.getLocation(), Sound.ORB_PICKUP, 0.5f, 1.5f);

            // Special notification at certain thresholds
            if (currentEnergy >= 100 && oldEnergy < 100) {
                player.sendMessage(ChatColor.GOLD + "" + ChatColor.BOLD + "Fa-Jin Energy at 100! Overdrive ready!");
                player.playSound(player.getLocation(), Sound.LEVEL_UP, 1.0f, 1.2f);
            }
        }

        updateDisplay();
    }

    /**
     * Consume Fa-Jin energy for an ability
     *
     * @param amount Amount to consume
     */
    public void consumeEnergy(int amount) {
        if (currentEnergy < amount) {
            return;
        }

        currentEnergy -= amount;
        updateDisplay();
    }

    /**
     * Check if player has enough energy
     */
    public boolean hasEnergy(int amount) {
        return currentEnergy >= amount;
    }

    /**
     * Get current energy
     */
    public int getCurrentEnergy() {
        return currentEnergy;
    }

    /**
     * Get max energy
     */
    public int getMaxEnergy() {
        return maxEnergy;
    }

    /**
     * Reset energy to 0
     */
    public void reset() {
        currentEnergy = 0;
        updateDisplay();
    }

    /**
     * Update the display of Fa-Jin energy
     */
    private void updateDisplay() {
        Manager.getArena(player).getQuirkBattleGame().updatePlayerStats(player);
    }

    /**
     * Get formatted display string for action bar
     */
    public String getDisplayString() {
        ChatColor color;
        if (currentEnergy >= 100) {
            color = ChatColor.DARK_RED;
        } else if (currentEnergy >= 50) {
            color = ChatColor.RED;
        } else {
            color = ChatColor.GRAY;
        }

        return color + "" + currentEnergy + "/" + maxEnergy + " ⚡";
    }
}