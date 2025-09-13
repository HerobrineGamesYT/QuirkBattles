package net.herobrine.quirkbattle.util;

import org.bukkit.entity.Player;

/**
 * Interface for Awakened Quirks in Heroes VS Villains mode.
 * Awakened Quirks have enhanced stats, additional abilities, and special mechanics.
 */
public interface Awakened {

    /**
     * Check if this quirk is currently awakened
     */
    boolean isAwakened();

    /**
     * Initialize awakened-specific features
     */
    void initializeAwakened(Player player);

    /**
     * Get enhanced stats for awakened form
     */
    AwakendStats getAwakenedStats();

    /**
     * Handle any per-tick awakened mechanics
     */
    void handleAwakenedTick();

    /**
     * Clean up awakened state when game ends
     */
    void cleanupAwakened();

    /**
     * Inner class to hold awakened stat modifiers
     */
    class AwakendStats {
        public final int health;
        public final int maxHealth;
        public final int defense;
        public final int baseDamage;
        public final int mana;
        public final int intelligence;

        public AwakendStats(int health, int maxHealth, int defense, int baseDamage, int mana, int intelligence) {
            this.health = health;
            this.maxHealth = maxHealth;
            this.defense = defense;
            this.baseDamage = baseDamage;
            this.mana = mana;
            this.intelligence = intelligence;
        }
    }
}