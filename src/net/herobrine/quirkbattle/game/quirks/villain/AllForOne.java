package net.herobrine.quirkbattle.game.quirks.villain;

import net.herobrine.core.HerobrinePVPCore;
import net.herobrine.gamecore.*;
import net.herobrine.gamecore.Class;
import net.herobrine.quirkbattle.QuirkBattlesPlugin;
import net.herobrine.quirkbattle.event.QuirkErasureEvent;
import net.herobrine.quirkbattle.event.QuirkStealEvent;
import net.herobrine.quirkbattle.game.quirks.abilities.Abilities;
import net.herobrine.quirkbattle.game.quirks.abilities.Ability;
import net.herobrine.quirkbattle.game.quirks.abilities.AbilitySets;
import net.herobrine.quirkbattle.game.quirks.abilities.Stealable;
import net.herobrine.quirkbattle.game.quirks.abilities.hero.ofa.SwitchAbilitySetTest;
import net.herobrine.quirkbattle.game.stats.PlayerStats;
import net.herobrine.quirkbattle.util.Quirk;
import net.herobrine.quirkbattle.util.Switchable;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.player.PlayerToggleFlightEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

public class AllForOne extends Class implements Quirk, Switchable {
    private PlayerStats stats;
    private final Arena arena;
    private final Player player;
    private boolean isSwitcherActive = false;
    private final List<Ability> transferList = new ArrayList<>();
    private final List<Ability> secondaryAbilities = new ArrayList<>();
    private AbilitySets currentSet;
    private AbilitySets[] availableSets;
    private boolean isBeingErased = false;
    private boolean shouldSteal = false;
    private boolean flyingCooldownTimer = false;
    private final List<Ability> abilities;
    private Stealable stolenQuirk;

    public AllForOne(UUID uuid) {
        super(uuid, ClassTypes.ALL_FOR_ONE);
        this.player = Bukkit.getPlayer(uuid);
        this.arena = Manager.getArena(player);
        this.abilities = new ArrayList<>();
        this.currentSet = AbilitySets.ALL_FOR_ONE;
        this.availableSets = new AbilitySets[] {AbilitySets.ALL_FOR_ONE};
    }

    @Override
    public void onStart(Player player) {
        stats = new PlayerStats(player.getUniqueId(), 600, 600, 100, 100, 350, 50, false, 500, 1000);

        arena.getQuirkBattleGame().getPlayerStatsMap().put(uuid, stats);
        player.getInventory().clear();
        ItemBuilder basicAttack = new ItemBuilder(Material.BLAZE_ROD);
        basicAttack.setDisplayName(ChatColor.RED + "Steal an ability to switch between your sets!");

        player.getInventory().setItem(0, basicAttack.build());
        registerAbilities(AbilitySets.ALL_FOR_ONE);

        player.getInventory().setHeldItemSlot(0);
        player.setAllowFlight(true);
    }

    @Override
    public List<Ability> getAbilities() {
        return abilities;
    }

    @Override
    public boolean isBeingErased() {
        return isBeingErased;
    }

    @Override
    public boolean shouldUseAbilityAttack() {
        return shouldSteal;
    }

    @Override
    public UUID getUniqueId() {
        return uuid;
    }

    @Override
    public UUID getOriginalId() {
        return getUUID();
    }

    @Override
    public void useAbilityAttack(Player target) {
    Quirk quirk =  (Quirk) arena.getClasses().get(target.getUniqueId());
    if (!(quirk instanceof Stealable)) {
        player.sendMessage(ChatColor.RED + "You cannot steal this Quirk!");
        player.playSound(player.getLocation(), Sound.VILLAGER_NO, 1f, 1f);
        return;
    }
    shouldSteal = false;
    QuirkStealEvent event = new QuirkStealEvent(target, player, true);
    Bukkit.getServer().getPluginManager().callEvent(event);
    player.sendMessage(ChatColor.GREEN + "You have stolen " + target.getName() + "'s quirk: " + arena.getClass(target.getUniqueId()).getDisplay());
    player.playSound(player.getLocation(), Sound.PISTON_EXTEND, .8f, .8f);
    new BukkitRunnable() {
        int seconds = 15;
        @Override
        public void run() {
            if (seconds == 0 && arena.getState() == GameState.LIVE) {
                removeQuirk();
                player.sendMessage(HerobrinePVPCore.translateString("&c&lWOAH!&r &7The quirk you stole is fighting back- looks like it went back to its owner."));
                player.playSound(player.getLocation(), Sound.WITHER_SHOOT, .6f, .8f);
            }
            seconds--;
        }
    }.runTaskTimer(QuirkBattlesPlugin.getInstance(), 0L, 20L);

    }

    @Override
    public void registerAbilities(AbilitySets set) {
        this.currentSet = set;
        int i = 2;
        for (Abilities ability : set.getAbilities()) {
             if(set == AbilitySets.ALL_FOR_ONE) abilities.add(arena.getQuirkBattleGame().getAbilityManager().registerAbility(ability, this, i));
             else abilities.add(arena.getQuirkBattleGame().getAbilityManager().registerAbility(ability, (Quirk) stolenQuirk, i));
            i++;
        }

        if (!isSwitcherActive) {
            arena.getQuirkBattleGame().getAbilityManager().registerAbility(Abilities.OFA_ABILITY_SWITCH_TEST, this, 5);
            isSwitcherActive = true;

        }


    }

    public void removeQuirk() {
        availableSets = new AbilitySets[] {AbilitySets.ALL_FOR_ONE};
        if(currentSet != AbilitySets.ALL_FOR_ONE) switchAbilitySet(AbilitySets.ALL_FOR_ONE);
        stolenQuirk.restore();
        getAbilities().get(0).setCooldown(System.currentTimeMillis());
        getAbilities().get(0).doAbilityCooldown();

        for (Ability ability : secondaryAbilities) {
            arena.getQuirkBattleGame().getAbilityManager().unregisterAbility(ability);
        }
        new BukkitRunnable() {
            @Override
            public void run() {
                secondaryAbilities.clear();
                transferList.clear();
                stolenQuirk = null;
            }
        }.runTaskLater(QuirkBattlesPlugin.getInstance(), 2L);
    }


    public void giveAbilitySet(AbilitySets set, Quirk quirk) {
        availableSets = new AbilitySets[] {AbilitySets.ALL_FOR_ONE, set};
        this.stolenQuirk = (Stealable) quirk;
        switchAbilitySet(set);
    }
    public void setSteal(boolean steal) {
        this.shouldSteal = steal;
    }

    @Override
    public AbilitySets getCurrentSet() {
        return currentSet;
    }

    @Override
    public AbilitySets[] getAvailableSets() {
        return availableSets;
    }

    public Quirk getStolenQuirk() {return  (Quirk)stolenQuirk;}

    @Override
    public void switchAbilitySet(AbilitySets set) {
        stats.setUseTemperature(set.useTemperature);

        if (secondaryAbilities.isEmpty()) {
            for (Ability ability : abilities) {
                secondaryAbilities.add(ability);
                ability.setActive(false);
            }
            abilities.clear();
            registerAbilities(set);
        } else {
            for (Ability ability : abilities) {
                ability.setActive(false);
                transferList.add(ability);
                player.getInventory().setItem(ability.getSlot(), new ItemStack(Material.AIR));
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


    @EventHandler
    public void onErase(QuirkErasureEvent e) {
        if (e.getQuirk() != this) return;

        if (e.isErasing()) {
        isBeingErased = true;
        shouldSteal = false;
        SwitchAbilitySetTest switcher = (SwitchAbilitySetTest) arena.getQuirkBattleGame().getAbilityManager().getAbilityFromQuirk(this, Abilities.OFA_ABILITY_SWITCH_TEST);
        switcher.erase();
        }
        else {
            isBeingErased = false;
            SwitchAbilitySetTest switcher = (SwitchAbilitySetTest) arena.getQuirkBattleGame().getAbilityManager().getAbilityFromQuirk(this, Abilities.OFA_ABILITY_SWITCH_TEST);
            switcher.setActive(true);
        }
    }

    @EventHandler
    public void onFlightToggle(PlayerToggleFlightEvent e) {
        if (e.getPlayer() != player) return;
        if (e.isFlying() && !flyingCooldownTimer) {
            flyingCooldownTimer = true;
            player.setFlying(true);
            player.setFlySpeed(.03f);
            new BukkitRunnable() {
                int seconds = 20;
                @Override
                public void run() {
                if (arena.getState() != GameState.LIVE) {
                    cancel();
                    return;
                }
                player.setLevel(seconds);

                if (seconds == 20){
                    player.setAllowFlight(false);
                    player.setFlying(false);
                    player.sendMessage(ChatColor.RED + "Your float ability is now on cooldown!");
                    player.playSound(player.getLocation(), Sound.ENDERMAN_TELEPORT, .7f, 1.3f);
                    flyingCooldownTimer = false;
                }
                if (seconds == 0) {
                    cancel();
                    player.setAllowFlight(true);
                    player.sendMessage(ChatColor.GREEN + "Your float ability is recharged!");
                    player.playSound(player.getLocation(), Sound.ORB_PICKUP, 1f, 1f);
                    return;
                }

                seconds--;
                }
            }.runTaskTimer(QuirkBattlesPlugin.getInstance(), 300L, 20L);
        }
    }
}
