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
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class Hardening extends Class implements Quirk {
    public Hardening(UUID uuid) {
        super(uuid, ClassTypes.HARDENING);
        this.arena = Manager.getArena(Bukkit.getPlayer(uuid));
        this.abilities = new ArrayList<>();
    }
    Arena arena;
    PlayerStats stats;
    Player player;
    boolean isSharpClaw;

    boolean isUnbreakable;

    int hitCount;
    List<Ability> abilities;

    @Override
    public void onStart(Player player) {
    stats = new PlayerStats(uuid, 220, 220, 75, 0, 100, 0);
    arena.getQuirkBattleGame().getPlayerStatsMap().put(uuid, stats);
    this.player = player;
    this.hitCount = 0;
    player.getInventory().clear();
    ItemBuilder defaultItem = new ItemBuilder(Material.STICK);
    defaultItem.setDisplayName(ChatColor.RED + "Soak up damage to gain stamina!");
    player.getInventory().setHeldItemSlot(0);
    player.getInventory().setItem(0, defaultItem.build());
    abilities.add(arena.getQuirkBattleGame().getAbilityManager().registerAbility(Abilities.SHARP_CLAW, this, 1));
    abilities.add(arena.getQuirkBattleGame().getAbilityManager().registerAbility(Abilities.STONE_CHARGE, this, 2));
    abilities.add(arena.getQuirkBattleGame().getAbilityManager().registerAbility(Abilities.UNBREAKABLE, this, 3));
    // let's not do this for now for balancing purposes. doStaminaGainPerSecond();
    }

    public void doStaminaGainPerSecond() {
        new BukkitRunnable() {
            @Override
            public void run() {
                if (arena.getState() != GameState.LIVE) {
                    cancel();
                    return;
                }

                if (!arena.getQuirkBattleGame().getAlivePlayers().contains(uuid)) {
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
    private Hardening getInstance() {return this;}

    public boolean isClawSharp() {return isSharpClaw;}
    public boolean isUnbreakable() {return isUnbreakable;}
    public void setUnbreakable(boolean isUnbreakable) {this.isUnbreakable = isUnbreakable;}
    public void setSharpClaw(boolean isSharpClaw) {this.isSharpClaw = isSharpClaw;}

    @Override
    public List<Ability> getAbilities() {
        return abilities;
    }

    @Override
    public boolean shouldUseAbilityAttack() {
        return isClawSharp();
    }

    @Override
    public void useAbilityAttack(Player target) {
     if (hitCount < 3) {
        int damage = (int) (arena.getClass(player).getBaseDamage() + getAbilities().get(0).getAbility().getDamage());
        EntityDamageEvent event = new EntityDamageEvent(target, EntityDamageEvent.DamageCause.CUSTOM, damage);
        arena.getQuirkBattleGame().getCustomDeathCause().put(target.getUniqueId(), CustomDeathCause.SHARP_CLAW);
        arena.getQuirkBattleGame().getLastAbilityAttacker().put(target.getUniqueId(), player.getUniqueId());
        Bukkit.getPluginManager().callEvent(event);
        target.setLastDamageCause(event);
        player.playSound(player.getLocation(), Sound.CAT_HIT, .7f, .8f);
        target.playSound(target.getLocation(), Sound.CAT_HIT, .7f, 8f);

        target.sendMessage(ChatColor.GOLD + player.getName() + ChatColor.GREEN + " just hit you with their " + HerobrinePVPCore.translateString("&c&lSharpened Claw &r&aattack!"));
        player.sendMessage(ChatColor.GREEN + "You just hit " + ChatColor.GOLD + target.getName() +
                ChatColor.GREEN + " with your " + HerobrinePVPCore.translateString("&c&lSharpened Claw &r&aattack!"));
        if(!isUnbreakable) hitCount = hitCount + 1;
        if (hitCount >= 3) {
            getAbilities().get(0).setCooldown(System.currentTimeMillis());
            getAbilities().get(0).doAbilityCooldown();
            this.isSharpClaw = false;
            this.hitCount = 0;
        }
    }
    }

    @Override
    public void registerAbilities(AbilitySets set) {

    }

    public void giveStaminaBoostForDamage(double damage) {
        if (arena.getQuirkBattleGame().getCustomDeathCause().get(player.getUniqueId()).equals(CustomDeathCause.OUTSIDE_MAP)) return;
        int stamina = Math.round((float)damage / 5) * 2;
        giveStaminaBoost(stamina);
    }
}
