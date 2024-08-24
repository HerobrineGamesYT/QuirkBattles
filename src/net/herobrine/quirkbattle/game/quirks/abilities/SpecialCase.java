package net.herobrine.quirkbattle.game.quirks.abilities;

import org.bukkit.entity.Player;

public interface SpecialCase {

    public boolean doesCasePass(Player player);

    public void doNoPass(Player player);

}
