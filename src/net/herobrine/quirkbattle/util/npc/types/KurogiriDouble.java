package net.herobrine.quirkbattle.util.npc.types;

import net.herobrine.gamecore.Arena;
import net.herobrine.quirkbattle.util.npc.Clone;
import net.herobrine.quirkbattle.util.npc.CloneTypes;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.entity.Player;

public class KurogiriDouble extends Clone {

    public KurogiriDouble(Player owner, Location spawnLoc, Arena arena) {
        super(owner, spawnLoc, CloneTypes.KUROGIRI_DOUBLE, arena);
    }

    @Override
    public void onSpawn() {
        owner.sendMessage(ChatColor.GREEN + "Spawned your stunt double!");
    }
}
