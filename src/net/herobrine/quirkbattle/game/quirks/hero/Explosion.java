package net.herobrine.quirkbattle.game.quirks.hero;

import net.herobrine.core.HerobrinePVPCore;
import net.herobrine.gamecore.*;
import net.herobrine.gamecore.Class;
import net.herobrine.quirkbattle.QuirkBattlesPlugin;
import net.herobrine.quirkbattle.game.CustomDeathCause;
import net.herobrine.quirkbattle.game.quirks.abilities.Abilities;
import net.herobrine.quirkbattle.game.quirks.abilities.Ability;
import net.herobrine.quirkbattle.game.quirks.abilities.AbilitySets;
import net.herobrine.quirkbattle.game.stats.PlayerStats;
import net.herobrine.quirkbattle.util.Quirk;
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
import java.util.List;
import java.util.UUID;

public class Explosion extends Class implements Quirk {
    public Explosion(UUID uuid) {
        super(uuid, ClassTypes.EXPLOSION);
        arena = Manager.getArena(Bukkit.getPlayer(uuid));
        player = Bukkit.getPlayer(uuid);
        this.abilities = new ArrayList<>();
    }
    List<Ability> abilities;
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

        player.getInventory().setItem(0, basicAttack.build());

        abilities.add(arena.getQuirkBattleGame().getAbilityManager().registerAbility(Abilities.EXPLOSION_DASH, this, 2));
        abilities.add(arena.getQuirkBattleGame().getAbilityManager().registerAbility(Abilities.EXPLOSION_PUNCH, this, 3));
        abilities.add(arena.getQuirkBattleGame().getAbilityManager().registerAbility(Abilities.HOWITZER_IMPACT, this, 4));

        player.getInventory().setHeldItemSlot(0);
        doStaminaGainPerSecond();
    }


    public void resetDefaultItem() {
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


    public void explodeForPunch(Location hitLocation) {
        arena.getQuirkBattleGame().getAbilityManager().getAbilityFromQuirk(this, Abilities.EXPLOSION_PUNCH).setCooldown(System.currentTimeMillis());
        arena.getQuirkBattleGame().getAbilityManager().getAbilityFromQuirk(this, Abilities.EXPLOSION_PUNCH).doAbilityCooldown();
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
                                if (pl1 != player && !hasHit.contains(pl1.getUniqueId()) && arena.getQuirkBattleGame().getAlivePlayers().contains(pl1.getUniqueId())) {
                                    hasHit.add(pl1.getUniqueId());
                                    doDamageTo(player, pl1, getClassType().getBaseDamage() + 5, CustomDeathCause.EXPLOSION_PUNCH);
                                    pl1.sendMessage(HerobrinePVPCore.translateString("&6" + player.getName() + "&a just hit you with their &6&lExplosion Punch &r&aattack!"));
                                    player.sendMessage(HerobrinePVPCore.translateString("&aYou just hit &6" + pl1.getName() + "&a with your &6&lExplosion Punch &r&aattack!"));
                                }
                            }
                            else if (pl1 != player && arena.getTeam(pl1) != arena.getTeam(player)
                                    && !hasHit.contains(pl1.getUniqueId()) && arena.getQuirkBattleGame().getAlivePlayers().contains(pl1.getUniqueId())) {
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

    public boolean isExplosivePunch() {
        return explosivePunch;
    }
    public void setExplosivePunch(boolean explosivePunch) {
        this.explosivePunch = explosivePunch;
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

    @Override
    public List<Ability> getAbilities() {
        return abilities;
    }

    @Override
    public boolean shouldUseAbilityAttack() {
        return isExplosivePunch();
    }

    @Override
    public void useAbilityAttack(Player target) {
        explodeForPunch(target.getLocation());
    }

    @Override
    public void registerAbilities(AbilitySets set) {

    }
}
