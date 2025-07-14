package net.herobrine.quirkbattle.game.quirks.hero;

import net.herobrine.core.HerobrinePVPCore;
import net.herobrine.gamecore.*;
import net.herobrine.gamecore.Class;
import net.herobrine.quirkbattle.QuirkBattlesPlugin;
import net.herobrine.quirkbattle.event.FrostbiteEvent;
import net.herobrine.quirkbattle.event.OverheatEvent;
import net.herobrine.quirkbattle.event.QuirkErasureEvent;
import net.herobrine.quirkbattle.game.CustomDeathCause;
import net.herobrine.quirkbattle.game.quirks.abilities.Abilities;
import net.herobrine.quirkbattle.game.quirks.abilities.Ability;
import net.herobrine.quirkbattle.game.quirks.abilities.AbilitySets;
import net.herobrine.quirkbattle.game.quirks.abilities.Stealable;
import net.herobrine.quirkbattle.game.quirks.villain.AllForOne;
import net.herobrine.quirkbattle.game.stats.PlayerStats;
import net.herobrine.quirkbattle.util.Quirk;
import net.herobrine.quirkbattle.util.Switchable;
import org.bukkit.*;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerToggleSneakEvent;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class Engine extends Class implements Quirk, Switchable, Stealable {
    private final List<Ability> abilities;
    private PlayerStats stats;
    private final Arena arena;
    private Player player;
    private final UUID originalId;

    private boolean isBeingErased = false;
    private int boosts = 0;
    private final float baseSpeed = .2F;
    private boolean isStunned = false;


    public Engine(UUID uuid) {
        super(uuid, ClassTypes.ENGINE);
        arena = Manager.getArena(Bukkit.getPlayer(uuid));
        player = Bukkit.getPlayer(uuid);
        this.originalId = uuid;
        this.abilities = new ArrayList<>();
    }

    @Override
    public void onStart(Player player) {
        stats = new PlayerStats(uuid, 200, 200, 40, 0, 0, 0, true, 50, 250);
        PotionEffect effect = PotionEffectType.SPEED.createEffect(10000000, 0);
        arena.getQuirkBattleGame().getPlayerStatsMap().put(uuid, stats);
        player.getInventory().clear();
        ItemBuilder basicAttack = new ItemBuilder(Material.STICK);
        basicAttack.setDisplayName(ChatColor.YELLOW + "Use your boosts to speed up!");

        player.getInventory().setItem(0, basicAttack.build());
        abilities.add(arena.getQuirkBattleGame().getAbilityManager().registerAbility(Abilities.ENGINE_BOOST, this, 2));
        abilities.add(arena.getQuirkBattleGame().getAbilityManager().registerAbility(Abilities.RECIPRO_BURST, this, 3));
        abilities.add(arena.getQuirkBattleGame().getAbilityManager().registerAbility(Abilities.RAPID_KICK, this, 4));
        player.getInventory().setHeldItemSlot(0);
        player.addPotionEffect(effect);
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
                Switchable quirk =  (Switchable) arena.getClasses().get(uuid);
                int tempChange = quirk.getCurrentSet().getTempPerSecond();
                if (getActiveBoosts() > 0) tempChange = getActiveBoosts() * 5;
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


    @Override
    public List<Ability> getAbilities() {
        return abilities;
    }

    @Override
    public boolean isBeingErased() {
        return isBeingErased;
    }

    public boolean isStunned() {return isStunned;}

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

    }

    public int getActiveBoosts() {
        return boosts;
    }

    public void changeTemp(int temp) {
        int stm = stats.getTemp() + temp;
        if (stm < 0) stm = 0;
        stats.setTemp(stm);
    }


    public void addBoost() {
    boosts = boosts + 1;
    player.setWalkSpeed(baseSpeed + baseSpeed * boosts);
    }

    public void removeBoost() {
    boosts = boosts - 1;
    if (boosts < 0) boosts = 0;
    if (boosts != 0) player.getInventory().getItem(getAbilities().get(0).getSlot()).setAmount(getActiveBoosts());
    else player.getInventory().getItem(getAbilities().get(0).getSlot()).setAmount(1);
    player.setWalkSpeed(baseSpeed + baseSpeed * boosts);
    }

    public void removeAllBoosts() {
    boosts = 0;
    player.setWalkSpeed(baseSpeed);
    player.sendMessage(ChatColor.RED + "Your boosts have been cancelled!");
    player.playSound(player.getLocation(), Sound.BLAZE_DEATH, .4f, .8f);
    player.getInventory().getItem(getAbilities().get(0).getSlot()).setAmount(1);
    }

    public void setBoostLevel(int boost) {
        boosts = boost;
        if (boosts < 0) boosts = 0;
        if (boosts != 0) player.getInventory().getItem(getAbilities().get(0).getSlot()).setAmount(getActiveBoosts());
        else player.getInventory().getItem(getAbilities().get(0).getSlot()).setAmount(1);
        player.setWalkSpeed(baseSpeed + baseSpeed * boosts);
    }

    @EventHandler
    public void onErase(QuirkErasureEvent event) {
        if (event.getQuirk() != this) return;
        if (event.isErasing()) {
            isBeingErased = true;
            removeAllBoosts();
        }
        else isBeingErased = false;
    }

    @EventHandler
    public void onSneak(PlayerToggleSneakEvent event) {
        if (event.getPlayer().getUniqueId() != player.getUniqueId()) return;
        if (event.isSneaking() && boosts > 0 && event.isSneaking()) removeAllBoosts();
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
        removeAllBoosts();

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

    @Override
    public AbilitySets getCurrentSet() {
        return AbilitySets.ENGINE;
    }

    @Override
    public AbilitySets[] getAvailableSets() {
        return new AbilitySets[0];
    }

    @Override
    public void switchAbilitySet(AbilitySets set) {

    }



    @Override
    public void steal(Player stealer) {
        isStunned = false;
        stats.setTemp(stats.getBaseTemp());
        this.uuid = stealer.getUniqueId();
        this.player = stealer;
        this.stats = arena.getQuirkBattleGame().getStats(stealer);

        AllForOne afo = (AllForOne) arena.getClasses().get(stealer.getUniqueId());
        afo.giveAbilitySet(AbilitySets.ENGINE, this);
    }

    @Override
    public void restore() {
        removeAllBoosts();

        this.uuid = getOriginalId();
        this.player = Bukkit.getPlayer(getOriginalId());
        this.stats = arena.getQuirkBattleGame().getPlayerStatsMap().get(getOriginalId());

        QuirkErasureEvent event = new QuirkErasureEvent(player, false);
        Bukkit.getServer().getPluginManager().callEvent(event);

    }
}
