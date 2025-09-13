package net.herobrine.quirkbattle.util.npc;

import com.mojang.authlib.GameProfile;
import com.mojang.authlib.properties.Property;
import net.herobrine.gamecore.Arena;
import net.herobrine.quirkbattle.QuirkBattlesPlugin;
import net.minecraft.server.v1_8_R3.EntityPlayer;
import net.minecraft.server.v1_8_R3.MinecraftServer;
import net.minecraft.server.v1_8_R3.Packet;
import net.minecraft.server.v1_8_R3.PacketPlayOutAnimation;
import net.minecraft.server.v1_8_R3.PacketPlayOutEntity;
import net.minecraft.server.v1_8_R3.PacketPlayOutEntityDestroy;
import net.minecraft.server.v1_8_R3.PacketPlayOutEntityHeadRotation;
import net.minecraft.server.v1_8_R3.PacketPlayOutEntityTeleport;
import net.minecraft.server.v1_8_R3.PacketPlayOutNamedEntitySpawn;
import net.minecraft.server.v1_8_R3.PacketPlayOutPlayerInfo;
import net.minecraft.server.v1_8_R3.PlayerInteractManager;
import net.minecraft.server.v1_8_R3.WorldServer;
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

import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

public abstract class Clone {
    private final Plugin plugin;
    protected final Player owner;
    protected final CloneTypes type;
    protected final Arena arena;
    private final int lifetimeTicks;
    protected final double speedPerTick, chaseRadius, attackRange, damagePerHit;
    private final int attackCooldownTicks;

    private final MinecraftServer nmsServer;
    private final WorldServer nmsWorld;
    private final EntityPlayer npc;

    private Location loc;
    private Location wanderTarget;
    private int ticksAlive = 0, attackCD = 0;
    private BukkitRunnable task;

    public Clone(Player owner, Location spawnLoc, CloneTypes type, Arena arena) {
        this.plugin = QuirkBattlesPlugin.getInstance();
        this.owner = owner;
        this.type = type;
        this.arena = arena;
        this.lifetimeTicks = type.getLifetimeTicks();
        this.speedPerTick = type.getSpeedPerTick();
        this.chaseRadius = type.getChaseRadius();
        this.attackRange = type.getAttackRange();
        this.damagePerHit = type.getDamage();
        this.attackCooldownTicks = type.getAttackCooldownTicks();

        this.nmsServer = ((CraftServer) Bukkit.getServer()).getServer();
        this.nmsWorld = ((CraftWorld) spawnLoc.getWorld()).getHandle();

        GameProfile profile = makeProfileWithOwnersSkin(owner);
        this.npc = new EntityPlayer(nmsServer, nmsWorld, profile, new PlayerInteractManager(nmsWorld));
        this.loc = spawnLoc.clone();
        this.npc.setLocation(loc.getX(), loc.getY(), loc.getZ(), loc.getYaw(), loc.getPitch());
    }

    public GameProfile makeProfileWithOwnersSkin(Player owner) {
        GameProfile ownerProfile = ((CraftPlayer) owner).getProfile();
        GameProfile profile = new GameProfile(UUID.randomUUID(), owner.getName());
        if (ownerProfile.getProperties().containsKey("textures")) {
            for (Property prop : ownerProfile.getProperties().get("textures")) {
                profile.getProperties().put("textures",
                        new Property("textures", prop.getValue(), prop.getSignature()));
            }
        }
        return profile;
    }

   public abstract void onSpawn();


    public void spawn() {
        // add + spawn
        broadcast(new PacketPlayOutPlayerInfo(PacketPlayOutPlayerInfo.EnumPlayerInfoAction.ADD_PLAYER, npc));
        broadcast(new PacketPlayOutNamedEntitySpawn(npc));
        lookAt(loc.getYaw(), loc.getPitch());

        // remove from tab a moment later
        Bukkit.getScheduler().runTaskLater(plugin, () ->
                broadcast(new PacketPlayOutPlayerInfo(PacketPlayOutPlayerInfo.EnumPlayerInfoAction.REMOVE_PLAYER, npc)), 20L);

        onSpawn();

        // start AI

        task = new BukkitRunnable() {
            @Override public void run() {
                // Despawn NPC after lifetime if it is not a permanent one, and tick the AI if it has AI.
                if (ticksAlive++ >= lifetimeTicks && lifetimeTicks != -1) { despawn(); cancel(); return; }
                if (type.hasAI()) tickAI();
            }
        };
        task.runTaskTimer(plugin, 1L, 1L);
    }


    private void tickAI() {
        if (attackCD > 0) attackCD--;

        Player target = null;
        if (type.isAggressive()) target = findNearestEnemy(owner, loc, chaseRadius);
        if (target != null) {
            moveTowards(target.getLocation());
            tryAttack(target);
        } else {
            wander();
        }
    }

    private Player findNearestEnemy(Player owner, Location from, double radius) {
        Player nearest = null;
        double min = Double.MAX_VALUE;
        World w = from.getWorld();
        for (Player p : w.getPlayers()) {
            if (p.equals(owner) || !p.isOnline() || p.isDead()) continue;
            double d = p.getLocation().distance(from);
            if (d <= radius * radius && d < min) { min = d; nearest = p; }
        }
        return nearest;
    }

    private void moveTowards(Location target) {
        Vector dir = target.toVector().subtract(loc.toVector());
        double dist = dir.length(); if (dist < 1e-6) return;
        dir.normalize().multiply(speedPerTick);

        float yaw = (float) Math.toDegrees(Math.atan2(-dir.getX(), dir.getZ()));
        lookAt(yaw, 0f);

        loc.add(dir);
        teleport(loc);
    }

    private void wander() {
        if (wanderTarget == null
                || loc.distance(wanderTarget) < 0.25
                || ThreadLocalRandom.current().nextDouble() < 0.02) {
            double angle = ThreadLocalRandom.current().nextDouble() * Math.PI * 2;
            double r = 3 + ThreadLocalRandom.current().nextDouble() * 4; // 3–7 blocks
            wanderTarget = loc.clone().add(Math.cos(angle) * r, 0, Math.sin(angle) * r);
            wanderTarget.setY(loc.getY());
        }
        moveTowards(wanderTarget);
    }

    private void tryAttack(Player target) {
        if (attackCD > 0) return;
        if (!target.getWorld().equals(loc.getWorld())) return;
        if (target.getLocation().distance(loc) > attackRange * attackRange) return;

        broadcast(new PacketPlayOutAnimation(npc, 0)); // arm swing
        target.damage(damagePerHit, owner);
        attackCD = attackCooldownTicks;
    }

    private void teleport(Location newLoc) {
        npc.setLocation(newLoc.getX(), newLoc.getY(), newLoc.getZ(), newLoc.getYaw(), newLoc.getPitch());
        broadcast(new PacketPlayOutEntityTeleport(npc));
    }

    private void lookAt(float yaw, float pitch) {
        npc.yaw = yaw; npc.pitch = pitch;
        byte yb = (byte)((yaw % 360) * 256 / 360f);
        byte pb = (byte)((pitch % 360) * 256 / 360f);
        broadcast(new PacketPlayOutEntity.PacketPlayOutEntityLook(npc.getId(), yb, pb, true));
        broadcast(new PacketPlayOutEntityHeadRotation(npc, yb));
    }

    public void despawn() {
        if (task != null) task.cancel();
        broadcast(new PacketPlayOutEntityDestroy(npc.getId()));
        broadcast(new PacketPlayOutPlayerInfo(PacketPlayOutPlayerInfo.EnumPlayerInfoAction.REMOVE_PLAYER, npc));
    }

    private void broadcast(Packet<?> packet) {
        for (Player p : Bukkit.getOnlinePlayers()) {
            ((CraftPlayer) p).getHandle().playerConnection.sendPacket(packet);
        }
    }

}
