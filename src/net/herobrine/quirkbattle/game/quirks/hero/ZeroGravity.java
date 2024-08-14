package net.herobrine.quirkbattle.game.quirks.hero;

import net.herobrine.gamecore.Class;
import net.herobrine.gamecore.ClassTypes;
import org.bukkit.entity.Player;

import java.util.UUID;

public class ZeroGravity extends Class {
    public ZeroGravity(UUID uuid) {
        super(uuid, ClassTypes.ZEROGRAVITY);
    }

    @Override
    public void onStart(Player player) {

    }
}
