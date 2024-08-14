package net.herobrine.quirkbattle.game.quirks.hero;


import net.herobrine.core.HerobrinePVPCore;
import net.herobrine.gamecore.*;
import net.herobrine.gamecore.Class;
import net.herobrine.quirkbattle.QuirkBattlesPlugin;
import net.herobrine.quirkbattle.game.CustomDeathCause;
import net.herobrine.quirkbattle.game.stats.PlayerStats;
import net.minecraft.server.v1_8_R3.EnumParticle;
import net.minecraft.server.v1_8_R3.PacketPlayOutWorldParticles;
import org.bukkit.*;
import org.bukkit.craftbukkit.v1_8_R3.entity.CraftPlayer;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.material.MaterialData;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

public class OneForAll extends Class {
    public OneForAll(UUID uuid) {
        super(uuid, ClassTypes.ONEFORALL);
    }

    PlayerStats stats;
    long lastStaminaCharge = 0;

    long lastAirPropulsion = 0;
    long lastDetroitSmash = 0;

    long lastShootStyle = 0;

    int shootStyleCooldownSeconds;
    int detroitSmashCooldownSeconds;
    int airPropulsionCooldownSeconds;


    @Override
    public void onStart(Player player) {
    stats = new PlayerStats(player.getUniqueId(),200, 200, 50, 0, 100, 6);
    Manager.getArena(player).getQuirkBattleGame().getPlayerStatsMap().put(player.getUniqueId(), stats);
    player.getInventory().clear();
    ItemBuilder defaultHeldItem = new ItemBuilder(Material.STICK);
    defaultHeldItem.setDisplayName(ChatColor.GREEN + "Hold right click to charge power!");
    defaultHeldItem.addItemFlag(ItemFlag.HIDE_ENCHANTS);

    ItemBuilder smash = new ItemBuilder(Material.DIAMOND_AXE);
    smash.setDisplayName(ChatColor.GOLD + "Detriot Smash");

    ItemBuilder shootStyle = new ItemBuilder(Material.BOW);
    shootStyle.setDisplayName(ChatColor.GREEN + "Shoot Style");

    ItemBuilder airPropulsion = new ItemBuilder(Material.GLOWSTONE_DUST);
    airPropulsion.setDisplayName(ChatColor.BLUE + "Air Propulsion");

    player.getInventory().setHeldItemSlot(0);
    player.getInventory().setItem(0, defaultHeldItem.build());
    player.getInventory().setItem(2, smash.build());
    player.getInventory().setItem(3, shootStyle.build());
    player.getInventory().setItem(4, airPropulsion.build());
    }


    @EventHandler
    public void onHeldItem(PlayerItemHeldEvent e) {
        if (e.getPlayer().getUniqueId() != this.uuid) return;
        if (Manager.getArena(e.getPlayer()).getState() != GameState.LIVE) return;
        Player player = e.getPlayer();
    switch (e.getNewSlot()) {
        case 2:
            if(System.currentTimeMillis() - lastDetroitSmash >= 15000) doSmashAbility(player);
            else {
                player.playSound(player.getLocation(), Sound.VILLAGER_NO, 1f, 1f);
                player.sendMessage(ChatColor.RED + "That ability is on cooldown!");
            }
            break;
        case 3:
          if (System.currentTimeMillis() - lastShootStyle >= 25000) doShootStyleAbility(player);
          else {
              player.playSound(player.getLocation(), Sound.VILLAGER_NO, 1f, 1f);
              player.sendMessage(ChatColor.RED + "That ability is on cooldown!");
          }
            break;
        case 4:
            if (System.currentTimeMillis() - lastAirPropulsion >= 1200) doAirPropulsion(player);
            else {
                player.playSound(player.getLocation(), Sound.VILLAGER_NO, 1f, 1f);
                player.sendMessage(ChatColor.RED + "That ability is on cooldown!");
            }
            break;
        default: return;
    }
        player.getInventory().setHeldItemSlot(0);
    }


    public void doSmashAbility(Player player) {
        detroitSmashCooldownSeconds = 15;
        doDetroitSmashCooldown(2);
        lastDetroitSmash = System.currentTimeMillis();
        Vector vecUp = new Vector(0, 2, 0);
        Vector vecDown = player.getLocation().getDirection().setY(-2);

        vecDown.normalize();
        vecDown.multiply(1.5);

        player.setVelocity(vecUp);
        player.playSound(player.getLocation(), Sound.GHAST_FIREBALL, 1f, 1.2f);
        new BukkitRunnable() {
            @Override
            public void run() {
                player.playSound(player.getLocation(), Sound.GHAST_FIREBALL, 1f, .7f);
                player.setVelocity(vecDown);
                doDetroitSmashCollisionChecks(player, stats.getMana());
                resetPower();
            }
        }.runTaskLater(QuirkBattlesPlugin.getInstance(), 10L);
    }

    public void doShootStyleAbility(Player player) {
        shootStyleCooldownSeconds = 25;
        doShootStyleCooldown(3);
        lastShootStyle = System.currentTimeMillis();
        Vector vecUp = new Vector(0, 0.5, 0);
        Vector vecForward = player.getLocation().getDirection().setY(0);

    vecForward.normalize();
    vecForward.multiply(3);

    player.setVelocity(vecUp);
    player.playSound(player.getLocation(), Sound.GHAST_FIREBALL, 1f, 1.2f);
    new BukkitRunnable() {
        @Override
        public void run() {
            player.playSound(player.getLocation(), Sound.GHAST_FIREBALL, 1f, .7f);
            player.setVelocity(vecForward);
            doShootStyleCollisionChecks(player, stats.getMana());
            resetPower();
        }
    }.runTaskLater(QuirkBattlesPlugin.getInstance(), 10L);
    }

    public void doShootStyleCollisionChecks(Player player, int power) {
        new BukkitRunnable() {
            int cooldown = 15;
            ArrayList<UUID> hasHit = new ArrayList<>();

            @Override
            public void run() {
                if (!Manager.isPlaying(player) || Manager.getArena(player).getState().equals(GameState.RECRUITING)) {
                    cancel();
                    hasHit.clear();
                }

                if (cooldown == 0) {
                    cancel();
                    hasHit.clear();
                }
                else {
                    for (Entity en : player.getNearbyEntities(1.5, 1, 1.5)) {
                        if (en.getType().equals(EntityType.PLAYER)) {
                            Player pl1 = (Player) en;
                            Arena arena = Manager.getArena(player);

                            if (arena.getType().equals(GameType.ONE_V_ONE)) {
                            if (pl1 != player && !hasHit.contains(player.getUniqueId())) {
                                hasHit.add(player.getUniqueId());
                                doDamageTo(player, pl1, 20, power, CustomDeathCause.SHOOT_STYLE);
                                pl1.sendMessage(HerobrinePVPCore.translateString("&6" + player.getName() + "&a just hit you with their &lShoot Style &r&aattack at &6" + power + "% &aPower!"));
                                player.sendMessage(HerobrinePVPCore.translateString("&aYou just hit &6" + pl1.getName() + "&a with your &lShoot Style &r&aattack!"));
                            }
                            }
                            else if (pl1 != player && arena.getTeam(pl1) != arena.getTeam(player)
                                    && !hasHit.contains(player.getUniqueId())) {
                                hasHit.add(player.getUniqueId());
                                doDamageTo(player, pl1, 20, power, CustomDeathCause.SHOOT_STYLE);
                                pl1.sendMessage(HerobrinePVPCore.translateString(arena.getTeam(player).getColor() + player.getName() + "&a just hit you with their &lShoot Style &r&aattack at &6" + power + "% &aPower!"));
                                player.sendMessage(HerobrinePVPCore.translateString("&aYou just hit " + arena.getTeam(pl1).getColor() +  pl1.getName() + "&a with your &lShoot Style &r&aattack!"));
                            }
                        }

                    }
                }

                cooldown--;
            }
        }.runTaskTimerAsynchronously(QuirkBattlesPlugin.getInstance(), 0, 1);
    }

    public void circleEffect(Location loc, float radius) {
        for (double t = 0; t < 1000; t += 0.5) {
            float x = radius * (float) Math.sin(t);
            float z = radius * (float) Math.cos(t);

            PacketPlayOutWorldParticles packet = new PacketPlayOutWorldParticles(EnumParticle.BLOCK_CRACK, true,
                    (float) loc.getX() + x, (float) loc.getY(), (float) loc.getZ() + z, 0, 0, 0, 0, 1, 3);
            Manager.getArena(Bukkit.getPlayer(uuid)).sendPacket(packet);
        }

    }

    public void doDetroitSmashCollisionChecks(Player player, int power) {
        new BukkitRunnable() {
            int cooldown = 15;
            ArrayList<UUID> hasHit = new ArrayList<>();

            @Override
            public void run() {
                if (!Manager.isPlaying(player) || Manager.getArena(player).getState().equals(GameState.RECRUITING)) {
                    cancel();
                    hasHit.clear();
                }
                if (player.isOnGround()) {
                    cancel();
                    player.getLocation().getWorld().playSound(player.getLocation(), Sound.ZOMBIE_WOODBREAK, 1f, .75f);
                    circleEffect(player.getLocation(), 2.5f);
                    for (Entity en : player.getNearbyEntities(2.5, 1, 2.5)) {
                        if (en.getType().equals(EntityType.PLAYER)) {
                            Player pl1 = (Player) en;
                            Arena arena = Manager.getArena(player);

                            if (arena.getType().equals(GameType.ONE_V_ONE)) {
                                if (pl1 != player && !hasHit.contains(pl1.getUniqueId())) {
                                    hasHit.add(pl1.getUniqueId());
                                    doDamageTo(player, pl1, 15, power, CustomDeathCause.DETRIOT_SMASH);
                                    pl1.sendMessage(HerobrinePVPCore.translateString("&6" + player.getName() + "&a just hit you with their &lDetroit Smash &r&aattack at &6" + power + "% &aPower!"));
                                    player.sendMessage(HerobrinePVPCore.translateString("&aYou just hit &6" + pl1.getName() + "&a with your &lDetroit Smash &r&aattack!"));
                                }
                            }
                            else if (pl1 != player && arena.getTeam(pl1) != arena.getTeam(player)
                                    && !hasHit.contains(pl1.getUniqueId())) {
                                hasHit.add(pl1.getUniqueId());
                                doDamageTo(player, pl1, 15, power, CustomDeathCause.DETRIOT_SMASH);
                                pl1.sendMessage(HerobrinePVPCore.translateString(arena.getTeam(player).getColor() + player.getName() + "&a just hit you with their &lDetroit Smash &r&aattack at &6" + power + "% &aPower!"));
                                player.sendMessage(HerobrinePVPCore.translateString("&aYou just hit " + arena.getTeam(pl1).getColor() +  pl1.getName() + "&a with your &lDetroit Smash &r&aattack!"));
                            }
                        }
                    }
                }
            }
        }.runTaskTimerAsynchronously(QuirkBattlesPlugin.getInstance(), 0, 1);
    }

    public void nearbyCollision(Player player) {

    }

    public void radiusCollision(Player player) {

    }

    public void doShootStyleCooldown(int slot) {
        ItemBuilder onCooldown = new ItemBuilder(Material.SULPHUR);
        onCooldown.setDisplayName(ChatColor.GRAY + "Shoot Style (On Cooldown)");
        onCooldown.setAmount(shootStyleCooldownSeconds);
        Player player = Bukkit.getPlayer(uuid);
        new BukkitRunnable() {

            @Override
            public void run() {
                if (!Manager.isPlaying(player)) {
                    cancel();
                    return;
                }

                if (Manager.getArena(player).getState() != GameState.LIVE) {
                    cancel();
                    return;
                }
                if (shootStyleCooldownSeconds == 0) {
                    ItemBuilder shootStyle = new ItemBuilder(Material.BOW);
                    shootStyle.setDisplayName(ChatColor.GREEN + "Shoot Style");
                    player.getInventory().setItem(slot, shootStyle.build());
                    cancel();
                    return;
                }
                onCooldown.setAmount(shootStyleCooldownSeconds);
                player.getInventory().setItem(slot, onCooldown.build());
                shootStyleCooldownSeconds--;
            }
        }.runTaskTimer(QuirkBattlesPlugin.getInstance(), 0L, 20L);

    }


    public void doDetroitSmashCooldown(int slot) {
        ItemBuilder onCooldown = new ItemBuilder(Material.SULPHUR);
        onCooldown.setDisplayName(ChatColor.GRAY + "Detroit Smash (On Cooldown)");
        onCooldown.setAmount(detroitSmashCooldownSeconds);
        Player player = Bukkit.getPlayer(uuid);
        new BukkitRunnable() {

            @Override
            public void run() {
                if (!Manager.isPlaying(player)) {
                    cancel();
                    return;
                }

                if (Manager.getArena(player).getState() != GameState.LIVE) {
                    cancel();
                    return;
                }
                if (detroitSmashCooldownSeconds == 0) {
                    ItemBuilder smash = new ItemBuilder(Material.DIAMOND_AXE);
                    smash.setDisplayName(ChatColor.GOLD + "Detriot Smash");
                    player.getInventory().setItem(slot, smash.build());
                    cancel();
                    return;
                }
                onCooldown.setAmount(detroitSmashCooldownSeconds);
                player.getInventory().setItem(slot, onCooldown.build());
                detroitSmashCooldownSeconds--;
            }
        }.runTaskTimer(QuirkBattlesPlugin.getInstance(), 0L, 20L);

    }

    //For this cooldown we just set the cooldown item and then schedule a task to set it back 24 ticks - (1.2s) later. For a cooldown under 2s it doesn't make sense to use runTaskTimer,
    // but we can still visually show players the cooldown by doing it this way.
    public void doAirPropulsionCooldown(int slot) {
        Player player = Bukkit.getPlayer(uuid);
        ItemBuilder onCooldown = new ItemBuilder(Material.SULPHUR);
        onCooldown.setDisplayName(ChatColor.GRAY + "Air Propulsion (On Cooldown)");
        player.getInventory().setItem(slot, onCooldown.build());
        new BukkitRunnable() {
            @Override
            public void run() {
                ItemBuilder airPropulsion = new ItemBuilder(Material.GLOWSTONE_DUST);
                airPropulsion.setDisplayName(ChatColor.BLUE + "Air Propulsion");
                player.getInventory().setItem(slot, airPropulsion.build());
            }
        }.runTaskLater(QuirkBattlesPlugin.getInstance(), 24L);
    }


    public void doDamageTo(Player damager, Player target, int damage, int power, CustomDeathCause cause) {
        Arena arena = Manager.getArena(damager);
        arena.getQuirkBattleGame().getLastAbilityAttacker().put(target.getUniqueId(), damager.getUniqueId());
        arena.getQuirkBattleGame().getCustomDeathCause().put(target.getUniqueId(), cause);

        double dmg = damage + (damage * ((double) power /100));
        target.damage(0);
        @SuppressWarnings("deprecation")
        EntityDamageEvent event = new EntityDamageEvent(target, EntityDamageEvent.DamageCause.CUSTOM, dmg);
        target.setLastDamageCause(event);
        Bukkit.getPluginManager().callEvent(event);

    }

    public void doAirPropulsion(Player player) {
        lastAirPropulsion = System.currentTimeMillis();
        doAirPropulsionCooldown(4);
        Vector dir = player.getLocation().getDirection();

        dir.normalize();
        dir.multiply(2 + (2*stats.getMana() / 10));
        player.setVelocity(dir);
        player.playSound(player.getLocation(), Sound.GHAST_FIREBALL, 1f, 1.2f);
        resetPower();
    }


    public void resetPower() {
        Player player = Bukkit.getPlayer(uuid);
        if (Manager.getArena(player).getType() != GameType.HEROES_VS_VILLAINS) rollForDamage((double) stats.getMana() / 100);
        stats.setMana(0);
        player.setWalkSpeed(.2F);
        player.setLevel(stats.getMana());
        player.setExp((float)stats.getMana() / (float)stats.getIntelligence());
        ItemBuilder defaultHeldItem = new ItemBuilder(Material.STICK);
        defaultHeldItem.setDisplayName(ChatColor.GREEN + "Hold right click to charge power!");
        defaultHeldItem.addItemFlag(ItemFlag.HIDE_ENCHANTS);
        player.getInventory().setItem(0, defaultHeldItem.build());

    }


    public void rollForDamage(double power) {
        Player player = Bukkit.getPlayer(uuid);
        ThreadLocalRandom rand = ThreadLocalRandom.current();
        double chance = rand.nextDouble();

        if (chance <= power) {
            player.sendMessage(HerobrinePVPCore.translateString("&6&lWOAH! &r&7Be careful, your power is very unstable!"));
            player.damage(0);
            Manager.getArena(player).getQuirkBattleGame().getCustomDeathCause().put(player.getUniqueId(), CustomDeathCause.ONE_FOR_ALL_SELF);
            // We need to do this so that there won't be any errors in death handling. The killer would be oneself in this instance.
            Manager.getArena(player).getQuirkBattleGame().getLastAbilityAttacker().put(player.getUniqueId(), player.getUniqueId());
            double damage = 10 + ((double) (10 * stats.getMana()) /100);
            @SuppressWarnings("deprecation")
            EntityDamageEvent event = new EntityDamageEvent(player, EntityDamageEvent.DamageCause.CUSTOM, damage);
            Bukkit.getPluginManager().callEvent(event);
            player.setLastDamageCause(event);

        }
    }
    @EventHandler
    public void onRightClick(PlayerInteractEvent e) {
        if (e.getPlayer().getUniqueId() != this.uuid) return;
        if (!Manager.isPlaying(e.getPlayer())) return;
        if (Manager.getArena(e.getPlayer()).getState() != GameState.LIVE) return;
        Player player = e.getPlayer();
        boolean shouldChargePower = e.getAction().equals(Action.RIGHT_CLICK_AIR) || e.getAction().equals(Action.RIGHT_CLICK_BLOCK);
        if (shouldChargePower && System.currentTimeMillis() - lastStaminaCharge >= 100) {
        lastStaminaCharge = System.currentTimeMillis();
        if (stats.getMana() < 100) {
            stats.setMana(stats.getMana() + 1);
            float speed = .2F + (.2F * stats.getMana()/10);
            if (speed > 1) speed = 1;
            player.setWalkSpeed(speed);
            player.setLevel(stats.getMana());
            player.setExp((float)stats.getMana() / (float)stats.getIntelligence());
            if (stats.getMana() == 100) {
                ItemBuilder defaultHeldItem = new ItemBuilder(Material.STICK);
                defaultHeldItem.setDisplayName(ChatColor.GREEN + "Power is full!");
                defaultHeldItem.addItemFlag(ItemFlag.HIDE_ENCHANTS);
                defaultHeldItem.addEnchant(Enchantment.DURABILITY, 1);
                player.getInventory().setItem(0, defaultHeldItem.build());
            }
        }
        player.playSound(player.getLocation(), Sound.SUCCESSFUL_HIT, 0.5f, stats.getMana()/50f);
        }
    }
}
