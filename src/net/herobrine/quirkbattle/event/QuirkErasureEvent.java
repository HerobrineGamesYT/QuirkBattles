package net.herobrine.quirkbattle.event;

import net.herobrine.core.HerobrinePVPCore;
import net.herobrine.gamecore.Arena;
import net.herobrine.gamecore.Manager;
import net.herobrine.quirkbattle.game.quirks.abilities.Ability;
import net.herobrine.quirkbattle.game.stats.PlayerStats;
import net.herobrine.quirkbattle.util.Quirk;
import org.bukkit.ChatColor;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;
import org.bukkit.event.player.PlayerEvent;

public class QuirkErasureEvent extends PlayerEvent {

    private static final HandlerList HANDLERS = new HandlerList();

    private final boolean erase;

    //To erase a player Quirk, create this event and call it. Quirks listening to this event will disable/re-enable any extra elements of each individual quirk that exist
    // outside the ability system as well.
    // Quirks that have this will listen for this event and have the proper "switches" implemented.

    public QuirkErasureEvent(Player player, boolean erase) {
        super(player);
        this.erase = erase;

        if (erase) {
            for (Ability ability : getQuirk().getAbilities()) {
                ability.erase();
            }

            player.sendMessage(HerobrinePVPCore.translateString("&cYour quirk has been &c&lERASED!"));
            player.playSound(player.getLocation(), Sound.ANVIL_LAND, 0.7f, 0.7f);
            player.getInventory().setHeldItemSlot(0);
        }
        else {
            for (Ability ability : getQuirk().getAbilities()) {
                ability.setActive(true);
            }
            player.sendMessage(HerobrinePVPCore.translateString("&aYour quirk has been restored!"));
            player.playSound(player.getLocation(), Sound.ORB_PICKUP, 1f, 1f);
        }
    }

    public Arena getArena() {
        return Manager.getArena(player);
    }

    public Quirk getQuirk() {
        return (Quirk) getArena().getClasses().get(player.getUniqueId());
    }

    public PlayerStats getStats() {
        return getArena().getQuirkBattleGame().getStats(player);
    }

    public boolean isErasing() {return erase;}

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }

    @Override
    public HandlerList getHandlers() {
        return HANDLERS;
    }
}
