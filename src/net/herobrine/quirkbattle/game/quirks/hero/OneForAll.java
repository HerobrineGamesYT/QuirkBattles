package net.herobrine.quirkbattle.game.quirks.hero;


import net.herobrine.core.HerobrinePVPCore;
import net.herobrine.gamecore.Arena;
import net.herobrine.gamecore.Class;
import net.herobrine.gamecore.ClassTypes;
import net.herobrine.gamecore.GameState;
import net.herobrine.gamecore.GameType;
import net.herobrine.gamecore.ItemBuilder;
import net.herobrine.gamecore.Manager;
import net.herobrine.quirkbattle.event.QuirkErasureEvent;
import net.herobrine.quirkbattle.game.CustomDeathCause;
import net.herobrine.quirkbattle.game.quirks.abilities.Abilities;
import net.herobrine.quirkbattle.game.quirks.abilities.Ability;
import net.herobrine.quirkbattle.game.quirks.abilities.AbilitySets;
import net.herobrine.quirkbattle.game.stats.PlayerStats;
import net.herobrine.quirkbattle.util.Quirk;
import net.herobrine.quirkbattle.util.Switchable;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemFlag;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

public class OneForAll extends Class implements Quirk, Switchable {
    private PlayerStats stats;
    private final Arena arena;
    private long lastStaminaCharge = 0;

    private boolean canPowerUp = true;

    private boolean isBeingErased = false;
    private final List<Ability> abilities;

    private boolean isSwitcherActive = false;
    private AbilitySets currentSet;
    private AbilitySets[] availableSets;

    public OneForAll(UUID uuid) {
        super(uuid, ClassTypes.ONEFORALL);
        arena = Manager.getArena(Bukkit.getPlayer(uuid));
        this.abilities = new ArrayList<>();
        this.availableSets = new AbilitySets[]{AbilitySets.ONE_FOR_ALL, AbilitySets.OFA_SWITCH_TEST};
    }

    @Override
    public void onStart(Player player) {
        stats = new PlayerStats(player.getUniqueId(), 200, 200, 50, 0, 100, 6);
        arena.getQuirkBattleGame().getPlayerStatsMap().put(player.getUniqueId(), stats);
        player.getInventory().clear();
        ItemBuilder defaultHeldItem = new ItemBuilder(Material.STICK);
        defaultHeldItem.setDisplayName(ChatColor.GREEN + "Hold right click to charge power!");
        defaultHeldItem.addItemFlag(ItemFlag.HIDE_ENCHANTS);

        player.getInventory().setHeldItemSlot(0);
        player.getInventory().setItem(0, defaultHeldItem.build());
        registerAbilities(AbilitySets.ONE_FOR_ALL);
    }

    @Override
    public void registerAbilities(AbilitySets set) {
        this.currentSet = set;
        int i = 2;
        for (Abilities ability : set.getAbilities()) {
            abilities.add(arena.getQuirkBattleGame().getAbilityManager().registerAbility(ability, this, i));
            i++;
        }

        if (!isSwitcherActive && arena.getType().equals(GameType.HEROES_VS_VILLAINS)) {
            arena.getQuirkBattleGame().getAbilityManager().registerAbility(Abilities.OFA_ABILITY_SWITCH_TEST, this, i);
            isSwitcherActive = true;
        }
    }

    public void resetPower() {
        Player player = Bukkit.getPlayer(uuid);
        if (Manager.getArena(player).getType() != GameType.HEROES_VS_VILLAINS)
            rollForDamage((double) stats.getMana() / 100);
        stats.setMana(0);
        player.setWalkSpeed(.2F);
        player.setLevel(stats.getMana());
        player.setExp((float) stats.getMana() / (float) stats.getIntelligence());
        ItemBuilder defaultHeldItem = new ItemBuilder(Material.STICK);
        defaultHeldItem.setDisplayName(ChatColor.GREEN + "Hold right click to charge power!");
        defaultHeldItem.addItemFlag(ItemFlag.HIDE_ENCHANTS);
        player.getInventory().setItem(0, defaultHeldItem.build());

    }
    public void resetPowerNoRoll() {
        Player player = Bukkit.getPlayer(uuid);
        stats.setMana(0);
        player.setWalkSpeed(.2F);
        player.setLevel(stats.getMana());
        player.setExp((float) stats.getMana() / (float) stats.getIntelligence());
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
            double damage = 10 + ((double) (10 * stats.getMana()) / 100);
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
        if (shouldChargePower && System.currentTimeMillis() - lastStaminaCharge >= 100 && canPowerUp) {
            lastStaminaCharge = System.currentTimeMillis();
            if (stats.getMana() < 100) {
                stats.setMana(stats.getMana() + 1);
                float speed = .2F + (.2F * stats.getMana() / 10);
                if (speed > 1) speed = 1;
                player.setWalkSpeed(speed);
                player.setLevel(stats.getMana());
                player.setExp((float) stats.getMana() / (float) stats.getIntelligence());
                if (stats.getMana() == 100) {
                    ItemBuilder defaultHeldItem = new ItemBuilder(Material.STICK);
                    defaultHeldItem.setDisplayName(ChatColor.GREEN + "Power is full!");
                    defaultHeldItem.addItemFlag(ItemFlag.HIDE_ENCHANTS);
                    defaultHeldItem.addEnchant(Enchantment.DURABILITY, 1);
                    player.getInventory().setItem(0, defaultHeldItem.build());
                }
            }
            player.playSound(player.getLocation(), Sound.SUCCESSFUL_HIT, 0.5f, stats.getMana() / 50f);
        }
    }

    @Override
    public List<Ability> getAbilities() {
        return abilities;
    }

    @Override
    public boolean isBeingErased() {return isBeingErased;}

    @Override
    public boolean shouldUseAbilityAttack() {
        return false;
    }

    // This method goes unused in OFA because it doesn't have any abilities that require it.
    @Override
    public void useAbilityAttack(Player target) {
    }

    public void switchAbilitySet(AbilitySets set) {
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

    @Override
    public AbilitySets getCurrentSet() {
        return currentSet;
    }

    @Override
    public AbilitySets[] getAvailableSets() {
        return availableSets;
    }

    @EventHandler
    public void onErase(QuirkErasureEvent e) {
        if (e.getQuirk() != this) return;
        if (e.isErasing()) {
            resetPowerNoRoll();
            canPowerUp = false;
            isBeingErased = true;
        }
        if (!e.isErasing()) {
            canPowerUp = true;
            isBeingErased = false;
        }
    }
}
