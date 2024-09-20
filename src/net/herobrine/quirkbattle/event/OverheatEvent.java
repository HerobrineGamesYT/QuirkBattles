package net.herobrine.quirkbattle.event;

import net.herobrine.core.HerobrinePVPCore;
import net.herobrine.gamecore.Arena;
import net.herobrine.gamecore.GameCoreMain;
import net.herobrine.gamecore.Manager;
import net.herobrine.quirkbattle.game.stats.PlayerStats;
import net.herobrine.quirkbattle.util.Quirk;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

public class OverheatEvent extends Event {
    private static final HandlerList HANDLERS = new HandlerList();

    private final Player player;

    public OverheatEvent(Player player) {
        this.player = player;
        player.sendMessage(HerobrinePVPCore.translateString("&c&lOUCH! &fYou are overheating!"));
        player.playSound(player.getLocation(), Sound.FIRE, 1f, 1f);
        GameCoreMain.getInstance().sendActionBar(player, "&c&lYOU ARE OVERHEATING!");
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }

    public Player getPlayer() {return player;}

    public Arena getArena() {return Manager.getArena(player);}

    public Quirk getQuirk() {return (Quirk)getArena().getClasses().get(player.getUniqueId());}

    public PlayerStats getStats() {return getArena().getQuirkBattleGame().getStats(player);}

    @Override
    public HandlerList getHandlers() {
        return HANDLERS;
    }
}
