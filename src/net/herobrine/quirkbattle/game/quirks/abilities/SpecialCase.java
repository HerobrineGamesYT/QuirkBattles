package net.herobrine.quirkbattle.game.quirks.abilities;

import org.bukkit.entity.Player;

public interface SpecialCase {

    boolean doesCasePass(Player player);
    void doNoPass(Player player);

}
