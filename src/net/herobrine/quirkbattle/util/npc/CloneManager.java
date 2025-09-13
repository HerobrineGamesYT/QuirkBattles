package net.herobrine.quirkbattle.util.npc;

import com.mojang.authlib.GameProfile;
import com.mojang.authlib.properties.Property;
import net.herobrine.gamecore.Arena;
import net.herobrine.quirkbattle.QuirkBattlesPlugin;
import net.herobrine.quirkbattle.util.npc.types.KurogiriDouble;
import net.herobrine.quirkbattle.util.npc.types.TwiceClone;
import net.minecraft.server.v1_8_R3.*;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.craftbukkit.v1_8_R3.CraftServer;
import org.bukkit.craftbukkit.v1_8_R3.CraftWorld;
import org.bukkit.craftbukkit.v1_8_R3.entity.CraftPlayer;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

/** Packet-driven fake-player clones for Twice (1.8.8-safe). */
public class CloneManager {

    private final Arena arena;
    private final Map<UUID, List<Clone>> active = new HashMap<>();

    public CloneManager(Arena arena) { this.arena = arena; }

    public void despawnAll(UUID owner) {
        List<Clone> list = active.remove(owner);
        if (list == null) return;
        for (Clone c : list) c.despawn();
    }

    public void spawnClones(Player owner, int count, double spawnSpread, CloneTypes type) {

        List<Clone> list = active.computeIfAbsent(owner.getUniqueId(), k -> new ArrayList<>());
        Location base = owner.getLocation();

        for (int i = 0; i < count; i++) {
            double angle = (2 * Math.PI / Math.max(1, count)) * i;
            Location spawnLoc = base.clone().add(Math.cos(angle) * spawnSpread, 0, Math.sin(angle) * spawnSpread);

            list.add(createClone(owner, spawnLoc, type));
        }
    }

    public void spawnClones(Player owner, Location loc, int count, double spawnSpread, CloneTypes type) {
        List<Clone> list = active.computeIfAbsent(owner.getUniqueId(), k -> new ArrayList<>());

        for (int i = 0; i < count; i++) {
            double angle = (2 * Math.PI / Math.max(1, count)) * i;
            Location spawnLoc = loc.clone().add(Math.cos(angle) * spawnSpread, 0, Math.sin(angle) * spawnSpread);

            list.add(createClone(owner, spawnLoc, type));
        }
    }

    public Clone createClone(Player owner, Location spawnLoc, CloneTypes type) {
        Clone clone;
        switch (type) {
            case TWICE_CLONE:
                clone = new TwiceClone(owner, spawnLoc, arena);
                clone.spawn();
                return clone;
            case KUROGIRI_DOUBLE:
                clone = new KurogiriDouble(owner, spawnLoc, arena);
                clone.spawn();
                return clone;
            default:
                return null;
        }
    }
}
