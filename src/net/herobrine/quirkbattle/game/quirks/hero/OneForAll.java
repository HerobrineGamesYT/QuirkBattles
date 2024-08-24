package net.herobrine.quirkbattle.game.quirks.hero;


import net.herobrine.core.HerobrinePVPCore;
import net.herobrine.gamecore.*;
import net.herobrine.gamecore.Class;
import net.herobrine.quirkbattle.QuirkBattlesPlugin;
import net.herobrine.quirkbattle.game.CustomDeathCause;
import net.herobrine.quirkbattle.game.quirks.abilities.Abilities;
import net.herobrine.quirkbattle.game.quirks.abilities.Ability;
import net.herobrine.quirkbattle.game.stats.PlayerStats;
import net.herobrine.quirkbattle.util.Quirk;
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
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

public class OneForAll extends Class implements Quirk {
    public OneForAll(UUID uuid) {
        super(uuid, ClassTypes.ONEFORALL);
        arena = Manager.getArena(Bukkit.getPlayer(uuid));
        this.abilities = new ArrayList<>();
    }

    PlayerStats stats;
    Arena arena;
    long lastStaminaCharge = 0;
    List<Ability> abilities;
    @Override
    public void onStart(Player player) {
    stats = new PlayerStats(player.getUniqueId(),200, 200, 50, 0, 100, 6);
    arena.getQuirkBattleGame().getPlayerStatsMap().put(player.getUniqueId(), stats);
    player.getInventory().clear();
    ItemBuilder defaultHeldItem = new ItemBuilder(Material.STICK);
    defaultHeldItem.setDisplayName(ChatColor.GREEN + "Hold right click to charge power!");
    defaultHeldItem.addItemFlag(ItemFlag.HIDE_ENCHANTS);

    player.getInventory().setHeldItemSlot(0);
    player.getInventory().setItem(0, defaultHeldItem.build());
    abilities.add(arena.getQuirkBattleGame().getAbilityManager().registerAbility(Abilities.DETROIT_SMASH, this, 2));
    abilities.add(arena.getQuirkBattleGame().getAbilityManager().registerAbility(Abilities.SHOOT_STYLE, this, 3));
    abilities.add(arena.getQuirkBattleGame().getAbilityManager().registerAbility(Abilities.AIR_PROPULSION, this, 4));
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

    @Override
    public List<Ability> getAbilities() {
        return abilities;
    }

    @Override
    public boolean shouldUseAbilityAttack() {return false;}

    // This method goes unused in OFA because it doesn't have any abilities that require it.
    @Override
    public void useAbilityAttack(Player target) {}
}
