package net.herobrine.quirkbattle.game.quirks.abilities;

import org.bukkit.entity.Player;

public interface Stealable {

     void steal(Player stealer);
     void restore();
}
