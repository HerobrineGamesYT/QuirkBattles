package net.herobrine.quirkbattle.game.stats;

import net.herobrine.gamecore.Manager;
import net.herobrine.quirkbattle.util.ofa.FaJinSystem;
import org.bukkit.Bukkit;

import java.util.UUID;

/**
 * Extension of PlayerStats to support Enhanced OFA features
 */
public class EnhancedPlayerStats extends PlayerStats {

    private FaJinSystem faJinSystem;
    private boolean showFaJin = false;
    private String gearDisplay = "";

    public EnhancedPlayerStats(UUID uuid, int health, int maxHealth, int defense, int mana, int intelligence, int strength) {
        super(uuid, health, maxHealth, defense, mana, intelligence, strength);
    }

    public void setFaJinSystem(FaJinSystem system) {
        this.faJinSystem = system;
        this.showFaJin = true;
    }

    public FaJinSystem getFaJinSystem() {
        return faJinSystem;
    }

    public void setGearDisplay(String display) {
        this.gearDisplay = display;
    }

    public String getGearDisplay() {
        return gearDisplay;
    }

    public boolean shouldShowFaJin() {
        return showFaJin && faJinSystem != null;
    }

    /**
     * Get the full display string including Fa-Jin if applicable
     */
    public String getEnhancedDisplay() {
        String base = "&c" + getHealth() + "❤   &a" + getDefense() + "❈ Defense   &3" + getMana() + "/" + getIntelligence() + "⸎ Stamina";

        if (shouldShowFaJin()) {
            base += "   " + faJinSystem.getDisplayString() + " Fa-Jin";
        }

        if (!gearDisplay.isEmpty()) {
            base += "   " + gearDisplay;
        }

        return base;
    }
}