package net.herobrine.quirkbattle.game.quirks.abilities;

import org.bukkit.entity.Player;

public interface Stealable {

     // Steal method: Reset whatever Quirk-Specific stats are required to prepare for the stealing player
     // Set active UUID, player, and stats to match that of the stealing player.
     // Grant ability set to the AFO player.
     void steal(Player stealer);
     // Reset any abilities to prepare for the original player upon restore.
     // Set active UUID, player, and stats to match that of the original player.
     // Grant original player their abilities back by using a restoring QuirkErasureEvent. (erasure event that has value set to false)
     void restore();
}
