package net.herobrine.quirkbattle.event;

import net.herobrine.core.HerobrinePVPCore;
import net.herobrine.gamecore.Arena;
import net.herobrine.gamecore.Manager;
import net.herobrine.quirkbattle.game.quirks.abilities.Ability;
import net.herobrine.quirkbattle.game.quirks.abilities.Stealable;
import net.herobrine.quirkbattle.game.stats.PlayerStats;
import net.herobrine.quirkbattle.util.Quirk;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;
import org.bukkit.event.player.PlayerEvent;

public class QuirkStealEvent extends PlayerEvent {
    private static final HandlerList HANDLERS = new HandlerList();
    private final boolean steal;

    public QuirkStealEvent(Player player, Player stealer, boolean steal) {
        super(player);
        this.steal = steal;
        Stealable stolenQuirk = (Stealable) getQuirk();
        if (steal) {
            QuirkErasureEvent event = new QuirkErasureEvent(player, true);
            Bukkit.getServer().getPluginManager().callEvent(event);
            player.sendMessage(HerobrinePVPCore.translateString("&c&lOH NO!&r &7Looks like your Quirk was stolen by &c" + stealer.getName() + "&7!"));
            stolenQuirk.steal(stealer);
        }
        else {
            stolenQuirk.restore();
            QuirkErasureEvent event = new QuirkErasureEvent(player, false);
            Bukkit.getServer().getPluginManager().callEvent(event);
        }
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
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

    public boolean isSteal() {return steal;}

    @Override
    public HandlerList getHandlers() {
        return HANDLERS;
    }
}
