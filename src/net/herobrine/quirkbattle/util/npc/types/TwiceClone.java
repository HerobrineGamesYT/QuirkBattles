package net.herobrine.quirkbattle.util.npc.types;

import net.herobrine.gamecore.Arena;
import net.herobrine.quirkbattle.util.npc.Clone;
import net.herobrine.quirkbattle.util.npc.CloneTypes;
import org.bukkit.Location;
import org.bukkit.entity.Player;

public final class TwiceClone extends Clone {


    public TwiceClone(Player owner, Location spawnLoc, Arena arena) {
        super(owner, spawnLoc, CloneTypes.TWICE_CLONE, arena);
    }

    @Override
    public void onSpawn() {

    }


}
