package net.herobrine.quirkbattle.game.quirks.hero;

import net.herobrine.core.HerobrinePVPCore;
import net.herobrine.gamecore.*;
import net.herobrine.gamecore.Class;
import net.herobrine.quirkbattle.QuirkBattlesPlugin;
import net.herobrine.quirkbattle.event.FrostbiteEvent;
import net.herobrine.quirkbattle.event.OverheatEvent;
import net.herobrine.quirkbattle.game.CustomDeathCause;
import net.herobrine.quirkbattle.game.quirks.abilities.Abilities;
import net.herobrine.quirkbattle.game.quirks.abilities.Ability;
import net.herobrine.quirkbattle.game.quirks.abilities.AbilitySets;
import net.herobrine.quirkbattle.game.stats.PlayerStats;
import net.herobrine.quirkbattle.util.Quirk;
import net.herobrine.quirkbattle.util.Switchable;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class IcyHot extends Class implements Quirk, Switchable {
    public IcyHot(UUID uuid) {
        super(uuid, ClassTypes.ICYHOT);
        arena = Manager.getArena(Bukkit.getPlayer(uuid));
        this.abilities = new ArrayList<>();
        this.availableSets = new AbilitySets[] {AbilitySets.ICE, AbilitySets.FIRE};
    }

    List<Ability> abilities;
    PlayerStats stats;
    Arena arena;

    boolean isSwitcherActive = false;
    AbilitySets currentSet;
    AbilitySets[] availableSets;

    boolean isStunned = false;

    @Override
    public void onStart(Player player) {
        stats = new PlayerStats(player.getUniqueId(),200, 200, 40, 0, 0, 0, true, 50, 100);
        arena.getQuirkBattleGame().getPlayerStatsMap().put(player.getUniqueId(), stats);
        player.getInventory().clear();
        ItemBuilder defaultHeldItem = new ItemBuilder(Material.STICK);
        defaultHeldItem.setDisplayName(ChatColor.YELLOW + "Keep your temperature balanced while fighting!");
        defaultHeldItem.addItemFlag(ItemFlag.HIDE_ENCHANTS);

        player.getInventory().setHeldItemSlot(0);
        player.getInventory().setItem(0, defaultHeldItem.build());
        registerAbilities(AbilitySets.ICE);

        tempPerSecondTick();
    }

    @Override
    public List<Ability> getAbilities() {
        return abilities;
    }

    @Override
    public boolean shouldUseAbilityAttack() {
        return false;
    }

    @Override
    public void useAbilityAttack(Player target) {

    }

    @Override
    public void registerAbilities(AbilitySets set) {
        this.currentSet = set;
        int i = 2;
        for (Abilities ability : set.getAbilities()) {
            abilities.add(arena.getQuirkBattleGame().getAbilityManager().registerAbility(ability, this, i));
            i++;
        }

        if (!isSwitcherActive) {
            arena.getQuirkBattleGame().getAbilityManager().registerAbility(Abilities.OFA_ABILITY_SWITCH_TEST, this, i);
           isSwitcherActive = true;

        }

    }

    @Override
    public AbilitySets getCurrentSet() {
        return currentSet;
    }

    @Override
    public AbilitySets[] getAvailableSets() {
        return availableSets;
    }

    @Override
    public void switchAbilitySet(AbilitySets set) {
        if (isStunned) {
            Bukkit.getPlayer(uuid).sendMessage(ChatColor.RED + "You can't switch sets while stunned!");
            return;
        }
        if (secondaryAbilities.isEmpty()) {
            for (Ability ability : abilities) {
                secondaryAbilities.add(ability);
                ability.setActive(false);
            }
            abilities.clear();
            registerAbilities(set);
        }
        else {
            for (Ability ability : abilities) {
                ability.setActive(false);
                transferList.add(ability);
            }
            abilities.clear();
            for (Ability ability : secondaryAbilities) {
                ability.setActive(true);
                abilities.add(ability);
            }
            secondaryAbilities.clear();
            secondaryAbilities.addAll(transferList);
            transferList.clear();

            for (Ability ability : secondaryAbilities) {
                ability.setActive(false);
            }

            this.currentSet = set;
        }
    }

    public void tempPerSecondTick() {
        new BukkitRunnable() {
            @Override
            public void run() {
                if (arena.getState() != GameState.LIVE) {
                    cancel();
                    secondaryAbilities.clear();
                    transferList.clear();
                    abilities.clear();
                    return;
                }
                int tempChange = currentSet.getTempPerSecond();
                if (isStunned) return;
                if (stats.useTemperature() && stats.getTemp() + tempChange < 0) {
                    stats.setTemp(tempChange + stats.getTemp());
                    FrostbiteEvent frost = new FrostbiteEvent(Bukkit.getPlayer(uuid));
                    Bukkit.getPluginManager().callEvent(frost);
                }
                if (stats.useTemperature() && tempChange + stats.getTemp() > stats.getMaxTemp()) {
                    stats.setTemp(tempChange + stats.getTemp());
                    OverheatEvent heat = new OverheatEvent(Bukkit.getPlayer(uuid));
                    Bukkit.getPluginManager().callEvent(heat);
                }

               stats.setTemp(tempChange + stats.getTemp());
            }
        }.runTaskTimer(QuirkBattlesPlugin.getInstance(), 0L, 20L);
    }

    @EventHandler
    public void onFrost(FrostbiteEvent event) {
    if (event.getQuirk() != this) return;
    isStunned = true;
    int damage = 3;
    int warmPerTick = 2;
        for (Ability ability : abilities) {
            ability.setActive(false);
        }
    new BukkitRunnable() {
        @Override
        public void run() {
            if (arena.getState() != GameState.LIVE) {
                isStunned = false;
                cancel();
                return;
            }
            if (stats.getTemp() == stats.getBaseTemp()) {
                cancel();
                for (Ability ability : abilities) {
                    ability.setActive(true);
                }
                isStunned = false;
                event.getPlayer().sendMessage(HerobrinePVPCore.translateString("&e&lPHEW! &fYou've warmed up now. Be careful!"));
                return;
            }
            if (stats.getTemp() + 1 == stats.getBaseTemp()) stats.setTemp(stats.getBaseTemp());
            else stats.setTemp(stats.getTemp() + warmPerTick);
            EntityDamageEvent dmg = new EntityDamageEvent(event.getPlayer(), EntityDamageEvent.DamageCause.CUSTOM, damage);
            event.getArena().getQuirkBattleGame().getCustomDeathCause().put(event.getPlayer().getUniqueId(), CustomDeathCause.FROSTBITE);
            event.getArena().getQuirkBattleGame().getLastAbilityAttacker().put(event.getPlayer().getUniqueId(), event.getPlayer().getUniqueId());
            Bukkit.getPluginManager().callEvent(dmg);
        }
    }.runTaskTimer(QuirkBattlesPlugin.getInstance(), 0L, 2L);
    }

    @EventHandler
    public void onOverHeat(OverheatEvent event) {
        if (event.getQuirk() != this) return;
        isStunned = true;
        int damage = 3;
        int coolPerTick = -2;
        for (Ability ability : abilities) {
            ability.setActive(false);
        }
        new BukkitRunnable() {
            @Override
            public void run() {
                if (arena.getState() != GameState.LIVE) {
                    isStunned = false;
                    cancel();
                    return;
                }
                if (stats.getTemp() == stats.getBaseTemp()) {
                    cancel();
                    isStunned = false;
                    event.getPlayer().sendMessage(HerobrinePVPCore.translateString("&e&lPHEW! &fYou've cooled off now. Be careful!"));
                    for (Ability ability : abilities) {
                        ability.setActive(true);
                    }
                    return;
                }
                if (stats.getTemp() - 1 == stats.getBaseTemp()) stats.setTemp(stats.getBaseTemp());
                else stats.setTemp(stats.getTemp() + coolPerTick);
                EntityDamageEvent dmg = new EntityDamageEvent(event.getPlayer(), EntityDamageEvent.DamageCause.CUSTOM, damage);
                event.getArena().getQuirkBattleGame().getCustomDeathCause().put(event.getPlayer().getUniqueId(), CustomDeathCause.OVERHEAT);
                event.getArena().getQuirkBattleGame().getLastAbilityAttacker().put(event.getPlayer().getUniqueId(), event.getPlayer().getUniqueId());
                Bukkit.getPluginManager().callEvent(dmg);
            }
        }.runTaskTimer(QuirkBattlesPlugin.getInstance(), 0L, 2L);
    }

    @EventHandler
    public void onMove(PlayerMoveEvent e) {
        if (!isStunned || e.getPlayer().getUniqueId() != uuid) return;
        if (arena.getState() != GameState.LIVE) {
            isStunned = false;
            return;
        }
        if (e.getTo().getX() != e.getFrom().getX() || e.getTo().getZ() != e.getFrom().getZ()) {
            e.setTo(new Location(e.getFrom().getWorld(), e.getFrom().getX(), e.getTo().getY(), e.getFrom().getZ(), e.getTo().getYaw(), e.getTo().getPitch()));
        }
    }
}
