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

public class FrostbiteEvent extends Event {
    private static final HandlerList HANDLERS = new HandlerList();

    private final Player player;

    public FrostbiteEvent(Player player) {
    this.player = player;
    player.sendMessage(HerobrinePVPCore.translateString("&b&lBRR! &fYou're too cold!"));
    player.playSound(player.getLocation(), Sound.GLASS, 1f, 1f);
    GameCoreMain.getInstance().sendActionBar(player, "&b&lYOU ARE FROZEN!");
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