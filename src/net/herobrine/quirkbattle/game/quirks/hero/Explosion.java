package net.herobrine.quirkbattle.game.quirks.hero;

import net.herobrine.core.HerobrinePVPCore;
import net.herobrine.gamecore.*;
import net.herobrine.gamecore.Class;
import net.herobrine.quirkbattle.QuirkBattlesPlugin;
import net.herobrine.quirkbattle.game.CustomDeathCause;
import net.herobrine.quirkbattle.game.stats.PlayerStats;
import org.bukkit.*;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.UUID;

public class Explosion extends Class {
    public Explosion(UUID uuid) {
        super(uuid, ClassTypes.EXPLOSION);
        arena = Manager.getArena(Bukkit.getPlayer(uuid));
        player = Bukkit.getPlayer(uuid);
    }

    PlayerStats stats;
    Arena arena;
    Player player;

    boolean explosivePunch = false;
    long lastExplosionDash = 0;
    long lastExplosionPunch = 0;
    long howitzerImpact = 0;

    int explosionPunchCooldown;
    int howitzerImpactCooldown;



    @Override
    public void onStart(Player player) {
        stats = new PlayerStats(uuid, 200, 200, 40, 0, 100, 10);
        arena.getQuirkBattleGame().getPlayerStatsMap().put(uuid, stats);
        player.getInventory().clear();
        ItemBuilder basicAttack = new ItemBuilder(Material.STICK);
        basicAttack.setDisplayName(ChatColor.GOLD + "Attack to gain stamina!");

        ItemBuilder explosionDash = new ItemBuilder(Material.BOW);
        explosionDash.setDisplayName(ChatColor.GOLD + "Explosion Dash");

        ItemBuilder explosionPunch = new ItemBuilder(Material.BLAZE_POWDER);

        explosionPunch.setDisplayName(ChatColor.GOLD + "Explosion Punch");

        ItemBuilder howitzerImpact = new ItemBuilder(Material.FIREBALL);
        howitzerImpact.setDisplayName(ChatColor.GOLD + "Howitzer Impact");

        player.getInventory().setItem(0, basicAttack.build());
        player.getInventory().setItem(2, explosionDash.build());
        player.getInventory().setItem(3, explosionPunch.build());
        player.getInventory().setItem(4, howitzerImpact.build());
        player.getInventory().setHeldItemSlot(0);
        doStaminaGainPerSecond();
    }


    public void takeCost(int cost) {
        stats.setMana(stats.getMana() - cost);
        ItemBuilder basicAttack = new ItemBuilder(Material.STICK);
        basicAttack.setDisplayName(ChatColor.GOLD + "Attack to gain stamina!");
        player.getInventory().setItem(0, basicAttack.build());
    }


    public void doStaminaGainPerSecond() {
        new BukkitRunnable() {
            @Override
            public void run() {
                if (arena.getState() != GameState.LIVE) {
                    cancel();
                    return;
                }
                if (arena.getClasses().get(uuid) != getInstance()) {
                    cancel();
                    return;
                }
                giveStaminaBoost(1);
            }
        }.runTaskTimer(QuirkBattlesPlugin.getInstance(), 0L, 20L);
    }


    private Explosion getInstance() {return this;}

    public void doExplosionPunch() {
        if (explosivePunch) {
            player.sendMessage(ChatColor.RED + "You've already powered up!");
            player.playSound(player.getLocation(), Sound.VILLAGER_NO, 1f, 1f);
            return;
        }
        if (stats.getMana() < 25) {
            player.sendMessage(ChatColor.RED + "You don't have enough stamina to use this!");
            player.playSound(player.getLocation(), Sound.VILLAGER_NO, 1f,1f);
            return;
        }

        explosivePunch = true;
        player.sendMessage(ChatColor.GREEN + "You've just powered up your next punch with explosive power!");
        player.playSound(player.getLocation(), Sound.CREEPER_HISS, 1f, 1f);
        takeCost(25);
    }

    public void explodeForPunch(Location hitLocation) {
        lastExplosionPunch = System.currentTimeMillis();
        explosionPunchCooldown = 6;
        doExplosionPunchCooldown(3);
        explosivePunch = false;
        giveStaminaBoost(5);
        player.getLocation().getWorld().createExplosion(hitLocation.getX(),
                hitLocation.getY(), hitLocation.getZ(), 2f, false, false);
        doExplosionPunchCollision(hitLocation);
    }

    public void doExplosionPunchCollision(Location explosionLocation) {
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
                    cancel();
                    for (Entity en : explosionLocation.getWorld().getNearbyEntities(explosionLocation, 2, 1.5, 2)) {
                        if (en.getType().equals(EntityType.PLAYER)) {
                            Player pl1 = (Player) en;
                            Arena arena = Manager.getArena(player);

                            if (arena.getType().equals(GameType.ONE_V_ONE)) {
                                if (pl1 != player && !hasHit.contains(pl1.getUniqueId())) {
                                    hasHit.add(pl1.getUniqueId());
                                    doDamageTo(player, pl1, getClassType().getBaseDamage() + 5, CustomDeathCause.EXPLOSION_PUNCH);
                                    pl1.sendMessage(HerobrinePVPCore.translateString("&6" + player.getName() + "&a just hit you with their &6&lExplosion Punch &r&aattack!"));
                                    player.sendMessage(HerobrinePVPCore.translateString("&aYou just hit &6" + pl1.getName() + "&a with your &6&lExplosion Punch &r&aattack!"));
                                }
                            }
                            else if (pl1 != player && arena.getTeam(pl1) != arena.getTeam(player)
                                    && !hasHit.contains(pl1.getUniqueId())) {
                                hasHit.add(pl1.getUniqueId());
                                doDamageTo(player, pl1, getClassType().getBaseDamage() + 5, CustomDeathCause.EXPLOSION_PUNCH);
                                pl1.sendMessage(HerobrinePVPCore.translateString(arena.getTeam(player).getColor() + player.getName() + "&a just hit you with their &&6lExplosion Punch &r&aattack!"));
                                player.sendMessage(HerobrinePVPCore.translateString("&aYou just hit " + arena.getTeam(pl1).getColor() +  pl1.getName() + "&a with your &6&lExplosion Punch &r&aattack!"));
                            }
                        }
                    }
                }
            }
        }.runTaskTimerAsynchronously(QuirkBattlesPlugin.getInstance(), 0, 1);
    }
    public void doExplosionDash() {

        if (stats.getMana() < 10) {
            player.sendMessage(ChatColor.RED + "You don't have enough stamina to use this!");
            player.playSound(player.getLocation(), Sound.VILLAGER_NO, 1f,1f);
            return;
        }

        lastExplosionDash = System.currentTimeMillis();
        doExplosionDashCooldown(2);
        Vector dir = player.getLocation().getDirection();

        dir.normalize();
        dir.multiply(3);
        player.setVelocity(dir);
        player.getLocation().getWorld().createExplosion(player.getLocation().getX(),
                player.getLocation().getY(), player.getLocation().getZ(), 2f, false, false);
        doExplosionDashCollison(player.getLocation());
        player.playSound(player.getLocation(), Sound.GHAST_FIREBALL, 1f, 1.2f);
        takeCost(10);
        giveStaminaBoost(3);
    }

    public void doHowitzerImpact() {
    if (stats.getMana() < 40) {
        player.sendMessage(ChatColor.RED + "You don't have enough stamina to use this!");
        player.playSound(player.getLocation(), Sound.VILLAGER_NO, 1f,1f);
        return;
    }
    howitzerImpact = System.currentTimeMillis();
    howitzerImpactCooldown = 25;
    doHowitzerImpactCooldown(4);
    takeCost(40);
    Vector vector = new Vector(0,4,0);

    player.setVelocity(vector);
        player.getLocation().getWorld().createExplosion(player.getLocation().getX(),
                player.getLocation().getY(), player.getLocation().getZ(), 2f, false, false);
        doExplosionCollisonForHowitzer(player.getLocation(), 2f, 10);
    new BukkitRunnable() {
        int maxExplosions = 3;
        int explosionCount = 0;
        int ticks = 0;
        @Override
        public void run() {
            if (arena.getState() != GameState.LIVE) {
                cancel();
                return;
            }
            if (arena.getClasses().get(player.getUniqueId()) != getInstance()) {
                cancel();
                return;
            }

            if (player.isOnGround()) {
                cancel();
                player.getLocation().getWorld().createExplosion(player.getLocation().getX(),
                        player.getLocation().getY(), player.getLocation().getZ(), 5f, false, false);
                doExplosionCollisonForHowitzer(player.getLocation(), 5f, 30);
                return;
            }
            if (ticks % 10 == 0 && explosionCount < maxExplosions) {
                player.getLocation().getWorld().createExplosion(player.getLocation().getX(),
                        player.getLocation().getY(), player.getLocation().getZ(), 2f, false, false);
                doExplosionCollisonForHowitzer(player.getLocation(), 2f, 10);
                explosionCount = explosionCount + 1;
            }
            ticks++;
        }
    }.runTaskTimer(QuirkBattlesPlugin.getInstance(), 4L, 1L);

    }

    public boolean isExplosivePunch() {
        return explosivePunch;
    }
    public boolean setExplosivePunch(boolean explosivePunch) {
        return explosivePunch;
    }

    public void giveStaminaBoost(int stamina) {
        int stm = stats.getMana() + stamina;
        if (stm > stats.getIntelligence()) stm = stats.getIntelligence();
        stats.setMana(stm);
        if (stm == stats.getIntelligence()) {
                ItemBuilder defaultHeldItem = new ItemBuilder(Material.STICK);
                defaultHeldItem.setDisplayName(ChatColor.GREEN + "Power is full!");
                defaultHeldItem.addItemFlag(ItemFlag.HIDE_ENCHANTS);
                defaultHeldItem.addEnchant(Enchantment.DURABILITY, 1);
                player.getInventory().setItem(0, defaultHeldItem.build());

        }

    }

    public void doExplosionCollisonForHowitzer(Location explosionLocation, float radius, int damage) {
        new BukkitRunnable() {
            int cooldown = 10;
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
                    cancel();
                    for (Entity en : explosionLocation.getWorld().getNearbyEntities(explosionLocation, radius, 1.5, radius)) {
                        if (en.getType().equals(EntityType.PLAYER)) {
                            Player pl1 = (Player) en;
                            Arena arena = Manager.getArena(player);

                            if (arena.getType().equals(GameType.ONE_V_ONE)) {
                                if (pl1 != player && !hasHit.contains(pl1.getUniqueId())) {
                                    hasHit.add(pl1.getUniqueId());
                                    doDamageTo(player, pl1, damage, CustomDeathCause.HOWITZER_IMPACT);
                                    pl1.sendMessage(HerobrinePVPCore.translateString("&6" + player.getName() + "&a just hit you with an explosion their &6&lHowitzer Impact &r&aattack!"));
                                    player.sendMessage(HerobrinePVPCore.translateString("&aYou just hit &6" + pl1.getName() + "&a with an explosion from your &6&lHowitzer Impact &r&aattack!"));
                                    giveStaminaBoost(5);
                                }
                            }
                            else if (pl1 != player && arena.getTeam(pl1) != arena.getTeam(player)
                                    && !hasHit.contains(pl1.getUniqueId())) {
                                hasHit.add(pl1.getUniqueId());
                                doDamageTo(player, pl1, damage, CustomDeathCause.HOWITZER_IMPACT);
                                pl1.sendMessage(HerobrinePVPCore.translateString(arena.getTeam(player).getColor() + player.getName() + "&a just hit you with an explosion from their &&6lHowitzer Impact &r&aattack!"));
                                player.sendMessage(HerobrinePVPCore.translateString("&aYou just hit " + arena.getTeam(pl1).getColor() +  pl1.getName() + "&a with an explosion from your &6&lHowitzer Impact &r&aattack!"));
                                giveStaminaBoost(5);
                            }
                        }
                    }
                }
            }
        }.runTaskTimerAsynchronously(QuirkBattlesPlugin.getInstance(), 0, 1);
    }

    public void doExplosionDashCollison(Location explosionLocation) {
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
                    cancel();
                    for (Entity en : explosionLocation.getWorld().getNearbyEntities(explosionLocation, 2, 1.5, 2)) {
                        if (en.getType().equals(EntityType.PLAYER)) {
                            Player pl1 = (Player) en;
                            Arena arena = Manager.getArena(player);

                            if (arena.getType().equals(GameType.ONE_V_ONE)) {
                                if (pl1 != player && !hasHit.contains(pl1.getUniqueId())) {
                                    hasHit.add(pl1.getUniqueId());
                                    doDamageTo(player, pl1, 5, CustomDeathCause.EXPLOSION_DASH);
                                    pl1.sendMessage(HerobrinePVPCore.translateString("&6" + player.getName() + "&a just hit you with their &6&lExplosion Dash &r&aattack!"));
                                    player.sendMessage(HerobrinePVPCore.translateString("&aYou just hit &6" + pl1.getName() + "&a with your &6&lExplosion Dash &r&aattack!"));
                                }
                            }
                            else if (pl1 != player && arena.getTeam(pl1) != arena.getTeam(player)
                                    && !hasHit.contains(pl1.getUniqueId())) {
                                hasHit.add(pl1.getUniqueId());
                                doDamageTo(player, pl1, 5, CustomDeathCause.EXPLOSION_DASH);
                                pl1.sendMessage(HerobrinePVPCore.translateString(arena.getTeam(player).getColor() + player.getName() + "&a just hit you with their &&6lExplosion Dash &r&aattack!"));
                                player.sendMessage(HerobrinePVPCore.translateString("&aYou just hit " + arena.getTeam(pl1).getColor() +  pl1.getName() + "&a with your &6&lExplosion Dash &r&aattack!"));
                            }
                        }
                    }
                }
            }
        }.runTaskTimerAsynchronously(QuirkBattlesPlugin.getInstance(), 0, 1);
    }

    public void doExplosionPunchCooldown(int slot) {
        ItemBuilder onCooldown = new ItemBuilder(Material.SULPHUR);
        onCooldown.setDisplayName(ChatColor.GRAY + "Explosion Punch (On Cooldown)");
        onCooldown.setAmount(explosionPunchCooldown);
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
                if (explosionPunchCooldown == 0) {
                    ItemBuilder explosionPunch = new ItemBuilder(Material.BLAZE_POWDER);
                    explosionPunch.setDisplayName(ChatColor.GOLD + "Explosion Punch");
                    player.getInventory().setItem(slot, explosionPunch.build());
                    cancel();
                    return;
                }
                onCooldown.setAmount(explosionPunchCooldown);
                player.getInventory().setItem(slot, onCooldown.build());
                explosionPunchCooldown--;
            }
        }.runTaskTimer(QuirkBattlesPlugin.getInstance(), 0L, 20L);
    }


    public void doExplosionDashCooldown(int slot) {
        ItemBuilder onCooldown = new ItemBuilder(Material.SULPHUR);
        onCooldown.setDisplayName(ChatColor.GRAY + "Explosion Dash (On Cooldown)");
        player.getInventory().setItem(slot, onCooldown.build());
        new BukkitRunnable() {
            @Override
            public void run() {
                ItemBuilder explosionDash = new ItemBuilder(Material.BOW);
                explosionDash.setDisplayName(ChatColor.GOLD + "Explosion Dash");
                player.getInventory().setItem(slot, explosionDash.build());
            }
        }.runTaskLater(QuirkBattlesPlugin.getInstance(), 30L);
    }

    public void doHowitzerImpactCooldown(int slot) {
        ItemBuilder onCooldown = new ItemBuilder(Material.SULPHUR);
        onCooldown.setDisplayName(ChatColor.GRAY + "Howitzer Impact (On Cooldown)");
        onCooldown.setAmount(howitzerImpactCooldown);
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
                if (howitzerImpactCooldown == 0) {
                    ItemBuilder howitzerImpact = new ItemBuilder(Material.FIREBALL);
                    howitzerImpact.setDisplayName(ChatColor.GOLD + "Howitzer Impact");
                    player.getInventory().setItem(slot, howitzerImpact.build());
                    cancel();
                    return;
                }
                onCooldown.setAmount(howitzerImpactCooldown);
                player.getInventory().setItem(slot, onCooldown.build());
                howitzerImpactCooldown--;
            }
        }.runTaskTimer(QuirkBattlesPlugin.getInstance(), 0L, 20L);
    }
    public void doDamageTo(Player damager, Player target, double damage, CustomDeathCause cause) {
        Arena arena = Manager.getArena(damager);
        arena.getQuirkBattleGame().getLastAbilityAttacker().put(target.getUniqueId(), damager.getUniqueId());
        arena.getQuirkBattleGame().getCustomDeathCause().put(target.getUniqueId(), cause);


        target.damage(0);
        @SuppressWarnings("deprecation")
        EntityDamageEvent event = new EntityDamageEvent(target, EntityDamageEvent.DamageCause.CUSTOM, damage);
        target.setLastDamageCause(event);
        Bukkit.getPluginManager().callEvent(event);

    }

    @EventHandler
    public void onHeld(PlayerItemHeldEvent e) {
        if (e.getPlayer().getUniqueId() != this.uuid) return;
        if (arena.getState() != GameState.LIVE) return;
        Player player = e.getPlayer();
        e.setCancelled(true);
        switch (e.getNewSlot()) {
            case 2:
                if (System.currentTimeMillis() - lastExplosionDash >= 1500) doExplosionDash();
                else {
                    player.sendMessage(ChatColor.RED + "This ability is on cooldown!");
                    player.playSound(player.getLocation(), Sound.VILLAGER_NO, 1f,1f);
                }
                break;
            case 3:
                if (System.currentTimeMillis() - lastExplosionPunch >= 6000) doExplosionPunch();
                else {
                    player.sendMessage(ChatColor.RED + "This ability is on cooldown!");
                    player.playSound(player.getLocation(), Sound.VILLAGER_NO, 1f,1f);
                }
                break;
            case 4:
                if (System.currentTimeMillis() - howitzerImpact >= 20000) doHowitzerImpact();
                else {
                    player.sendMessage(ChatColor.RED + "This ability is on cooldown!");
                    player.playSound(player.getLocation(), Sound.VILLAGER_NO, 1f,1f);
                }
                break;
            default:
                break;
        }
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent e)  {

    }
}
