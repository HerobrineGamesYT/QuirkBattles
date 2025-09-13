package net.herobrine.quirkbattle.game.quirks.villain;

import net.herobrine.core.HerobrinePVPCore;
import net.herobrine.gamecore.*;
import net.herobrine.gamecore.Class;
import net.herobrine.quirkbattle.QuirkBattlesPlugin;
import net.herobrine.quirkbattle.event.OverheatEvent;
import net.herobrine.quirkbattle.game.CustomDeathCause;
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
import org.bukkit.scheduler.BukkitRunnable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

public class Blueflame extends Class implements Quirk {
    private final List<Ability> abilities;
    private PlayerStats stats;
    private final Arena arena;
    private Player player;
    private final UUID originalId;

    private boolean isStunned = false;

    private boolean explosivePunch = false;

    private boolean isBeingErased = false;

    public Blueflame(UUID uuid) {
        super(uuid, ClassTypes.BLUEFLAME);
        arena = Manager.getArena(Bukkit.getPlayer(uuid));
        player = Bukkit.getPlayer(uuid);
        this.originalId = uuid;
        this.abilities = new ArrayList<>();
    }


    @Override
    public void onStart(Player player) {
        stats = new PlayerStats(player.getUniqueId(), 200, 200, 40, 30, 300, 40, true, 70,300);
        arena.getQuirkBattleGame().getPlayerStatsMap().put(uuid, stats);

        player.getInventory().clear();

        // Basic attack item
        net.herobrine.core.ItemBuilder basicAttack = new net.herobrine.core.ItemBuilder(Material.STICK);
        basicAttack.setDisplayName(ChatColor.BLUE + "Melee attacks reduce your temperature!");
        basicAttack.setLore(ChatColor.GRAY + "Your abilities hit hard but use a lot of temperature- don't overheat!");

        player.getInventory().setItem(0, basicAttack.build());
        registerAbilities(AbilitySets.BLUEFLAME);

        player.getInventory().setHeldItemSlot(0);

        tempPerSecondTick();
    }
    public void tempPerSecondTick() {
        new BukkitRunnable() {
            @Override
            public void run() {
                if (arena.getState() != GameState.LIVE || !arena.getQuirkBattleGame().getAlivePlayers().contains(player.getUniqueId())) {
                    cancel();
                    abilities.clear();
                    return;
                }
                int tempChange = AbilitySets.BLUEFLAME.getTempPerSecond();
                if (isStunned) return;
                if (isBeingErased && originalId == uuid) return;
                if (stats.useTemperature() && stats.getTemp() + tempChange < 0) {
                    return;
                }
                if (stats.useTemperature() && stats.getTemp() + tempChange > stats.getMaxTemp()) {
                    OverheatEvent event = new OverheatEvent(player);
                    Bukkit.getPluginManager().callEvent(event);
                }
                stats.setTemp(tempChange + stats.getTemp());
            }
        }.runTaskTimer(QuirkBattlesPlugin.getInstance(), 0L, 20L);

    }

    public void changeTemp(int temp) {
        int stm = stats.getTemp() + temp;
        if (stm < 0) stm = 0;
        stats.setTemp(stm);
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
        return false;
    }

    @Override
    public UUID getUniqueId() {
        return uuid;
    }

    @Override
    public UUID getOriginalId() {
        return originalId;
    }

    @Override
    public void useAbilityAttack(Player target) {

    }

    @Override
    public void registerAbilities(AbilitySets set) {
        int i = 2;
        for (net.herobrine.quirkbattle.game.quirks.abilities.Abilities ability : set.getAbilities()) {
            abilities.add(arena.getQuirkBattleGame().getAbilityManager().registerAbility(ability, this, i));
            i++;
        }
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

    @EventHandler
    public void onOverHeat(OverheatEvent event) {
        if (event.getQuirk() != this && originalId == uuid) return;
        if (event.getPlayer().getUniqueId() != uuid) return;
        isStunned = true;

        int damage = 1;
        int coolPerTick = -6;
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
                if (uuid != originalId) {
                    cancel();
                    return;
                }

                if (stats.getTemp() <= stats.getBaseTemp()) {
                    cancel();
                    isStunned = false;
                    event.getPlayer().sendMessage(HerobrinePVPCore.translateString("&e&lPHEW! &fYou've cooled off now. Be careful!"));
                    for (Ability ability : abilities) {
                        ability.setActive(true);
                    }
                    return;
                }
                Quirk quirk = (Quirk) arena.getClasses().get(uuid);
                quirk.getAbilities().get(0).spawnRGBParticles(event.getPlayer().getEyeLocation().add(0, 1.5, 0),  179, 67, 27, false);
                if (stats.getTemp() - 1 == stats.getBaseTemp()) stats.setTemp(stats.getBaseTemp());
                else stats.setTemp(stats.getTemp() + coolPerTick);
                EntityDamageEvent dmg = new EntityDamageEvent(event.getPlayer(), EntityDamageEvent.DamageCause.CUSTOM, damage);
                event.getArena().getQuirkBattleGame().getCustomDeathCause().put(event.getPlayer().getUniqueId(), CustomDeathCause.OVERHEAT);
                event.getArena().getQuirkBattleGame().getLastAbilityAttacker().put(event.getPlayer().getUniqueId(), event.getPlayer().getUniqueId());
                Bukkit.getPluginManager().callEvent(dmg);
            }
        }.runTaskTimer(QuirkBattlesPlugin.getInstance(), 0L, 2L);
    }

}
