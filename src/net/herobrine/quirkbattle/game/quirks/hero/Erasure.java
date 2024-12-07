package net.herobrine.quirkbattle.game.quirks.hero;

import net.herobrine.core.HerobrinePVPCore;
import net.herobrine.gamecore.Arena;
import net.herobrine.gamecore.Class;
import net.herobrine.gamecore.ClassTypes;
import net.herobrine.gamecore.GameState;
import net.herobrine.gamecore.GameType;
import net.herobrine.gamecore.ItemBuilder;
import net.herobrine.gamecore.Manager;
import net.herobrine.quirkbattle.QuirkBattlesPlugin;
import net.herobrine.quirkbattle.event.QuirkErasureEvent;
import net.herobrine.quirkbattle.game.CustomDeathCause;
import net.herobrine.quirkbattle.game.quirks.abilities.Abilities;
import net.herobrine.quirkbattle.game.quirks.abilities.Ability;
import net.herobrine.quirkbattle.game.quirks.abilities.AbilitySets;
import net.herobrine.quirkbattle.game.stats.PlayerStats;
import net.herobrine.quirkbattle.util.Quirk;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerToggleSneakEvent;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;

public class Erasure extends Class implements Quirk {

    private final List<Ability> abilities;
    private PlayerStats stats;
    private final Arena arena;
    private final Player player;


    // Stamina you gain every second for erasing a player quirk, per player.
    // For example, erasing one player's quirk will get you x stamina per second.
    // Erasing 2 player quirks at once will get you double that amount every second.
    // Once you hit max stamina, erasure will deactivate and then go on cooldown.
    private int erasureStaminaGain = 8;

    private boolean isErasing = false;

    private boolean isBeingErased = false;

    private long erasureCooldown = 0;

    private final int cooldownTime = 30;

    private int cooldownSeconds = 0;

    private long ticks = 0;

    private int hitCount = 0;

    private boolean sharpenedKnife = false;

    private final List<UUID> erasingPlayers = new CopyOnWriteArrayList<>();

    public Erasure(UUID uuid) {
        super(uuid, ClassTypes.ERASURE);
        arena = Manager.getArena(Bukkit.getPlayer(uuid));
        player = Bukkit.getPlayer(uuid);
        this.abilities = new ArrayList<>();
        // We make the Stamina gain higher in 1v1s to make the quirk more balanced!
        if (arena.getType() == GameType.ONE_V_ONE) this.erasureStaminaGain = 16;
    }

    @Override
    public void onStart(Player player) {
        stats = new PlayerStats(uuid, 200, 200, 40, 0, 250, 1);
        PotionEffect effect = PotionEffectType.SPEED.createEffect(10000000, 0);
        arena.getQuirkBattleGame().getPlayerStatsMap().put(uuid, stats);
        player.getInventory().clear();
        ItemBuilder basicAttack = new ItemBuilder(Material.STICK);
        basicAttack.setDisplayName(ChatColor.RED + "Use sneak to activate erasure!");

        player.getInventory().setItem(0, basicAttack.build());
        abilities.add(arena.getQuirkBattleGame().getAbilityManager().registerAbility(Abilities.CAPTURE, this, 2));
        abilities.add(arena.getQuirkBattleGame().getAbilityManager().registerAbility(Abilities.SHARPENED_KNIFE, this, 3));
        abilities.add(arena.getQuirkBattleGame().getAbilityManager().registerAbility(Abilities.EYEDROPS, this, 4));

        player.getInventory().setHeldItemSlot(0);
        player.addPotionEffect(effect);

        startErasing();
    }

    public void setSharpenedKnife(boolean sharp) {this.sharpenedKnife = sharp;}

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
        return sharpenedKnife;
    }

    @Override
    public void useAbilityAttack(Player target) {
        if (hitCount < 3) {
            int damage = (int) (arena.getClass(player).getBaseDamage() + getAbilities().get(1).getAbility().getDamage());
            EntityDamageEvent event = new EntityDamageEvent(target, EntityDamageEvent.DamageCause.CUSTOM, damage);
            arena.getQuirkBattleGame().getCustomDeathCause().put(target.getUniqueId(), CustomDeathCause.SHARPENED_KNIFE);
            arena.getQuirkBattleGame().getLastAbilityAttacker().put(target.getUniqueId(), player.getUniqueId());
            Bukkit.getPluginManager().callEvent(event);
            target.setLastDamageCause(event);
            player.playSound(player.getLocation(), Sound.BLAZE_HIT, .7f, .8f);
            target.playSound(target.getLocation(), Sound.BLAZE_HIT, .7f, 8f);

            target.sendMessage(ChatColor.GOLD + player.getName() + ChatColor.GREEN + " just hit you with their " + HerobrinePVPCore.translateString("&c&lSharpened Knife &r&aattack!"));
            player.sendMessage(ChatColor.GREEN + "You just hit " + ChatColor.GOLD + target.getName() +
                    ChatColor.GREEN + " with your " + HerobrinePVPCore.translateString("&c&lSharpened Knife &r&aattack!"));
            hitCount = hitCount + 1;
            if (hitCount >= 3) {
                getAbilities().get(1).setCooldown(System.currentTimeMillis());
                getAbilities().get(1).doAbilityCooldown();
                this.sharpenedKnife = false;
                this.hitCount = 0;
            }
        }
    }

    @Override
    public void registerAbilities(AbilitySets set) {

    }


    public int getCooldownSeconds() {return cooldownSeconds;}
    public void setCooldownSeconds(int seconds) {
        this.cooldownSeconds = seconds;
        player.setLevel(seconds);
        player.setExp((float)seconds / cooldownTime);
    }

    public long getErasureCooldown() {return erasureCooldown;}

    public void setErasureCooldown(long cooldown) {this.erasureCooldown = cooldown;}

    public void giveStaminaBoost(int stamina) {
        int stm = stats.getMana() + stamina;
        if (stm > stats.getIntelligence()) stm = stats.getIntelligence();
        stats.setMana(stm);
        if (stm == stats.getIntelligence()) {
            stopErasing();
        }

    }

    private void stopErasing() {
        erasureCooldown = System.currentTimeMillis();
        isErasing = false;
        cooldownSeconds = cooldownTime;

       if(arena.getQuirkBattleGame().getAlivePlayers().contains(uuid)) doErasureCooldown();
       for (UUID uuid : erasingPlayers) {
            QuirkErasureEvent event = new QuirkErasureEvent(Bukkit.getPlayer(uuid), false);
            Bukkit.getPluginManager().callEvent(event);
        }
        erasingPlayers.clear();
        player.sendMessage(ChatColor.RED + "Erasure has been deactivated!");
        player.playSound(player.getLocation(), Sound.ANVIL_LAND, 0.9f, 0.7f);
    }

    private void doErasureCooldown() {
        new BukkitRunnable() {
            @Override
            public void run() {
                if (!arena.getState().equals(GameState.LIVE)) {
                    cancel();
                    return;
                }
                if (!arena.getQuirkBattleGame().getAlivePlayers().contains(player.getUniqueId())) {
                    cancel();
                    return;
                }
                if (cooldownSeconds <= 0) {
                    cooldownSeconds = 0;
                    player.sendMessage(ChatColor.GREEN + "You can use Erasure again!");
                    player.playSound(player.getLocation(), Sound.ORB_PICKUP, 1f, 1f);
                    player.setExp(0);
                    player.setLevel(0);
                    cancel();
                    return;
                }
                player.setLevel(cooldownSeconds);
                player.setExp((float) cooldownSeconds / cooldownTime);
                cooldownSeconds--;
            }
        }.runTaskTimer(QuirkBattlesPlugin.getInstance(), 0L, 20L);
    }

    private void startErasing() {

        new BukkitRunnable() {
            @Override
            public void run() {
                if (!arena.getState().equals(GameState.LIVE)) {
                    cancel();
                    return;
                }
                if (!arena.getQuirkBattleGame().getAlivePlayers().contains(player.getUniqueId())) {
                    stopErasing();
                    cancel();
                    return;
                }
                if (!isErasing) return;

                if (ticks % 20 == 0) giveStaminaBoost(erasureStaminaGain * erasingPlayers.size());
                if (ticks % 2 == 0) playEffect();

                // Why an extra one? Because after the stamina boost is applied you could potentially no longer be erasing if you hit max stamina.
                if (!isErasing) return;
                for (Entity ent : player.getNearbyEntities(15f, 15f, 15f)) {
                    if (!(ent instanceof Player)) continue;
                    Player player = (Player) ent;
                    if (!Manager.isPlaying(player)) continue;
                    if (arena.getSpectators().contains(player.getUniqueId())) continue;
                    if (arena.getType().isTeamsMode()) {
                        if (arena.getTeam(player).equals(arena.getTeam(Bukkit.getPlayer(uuid)))) continue;

                    }
                    else if (player.equals(Bukkit.getPlayer(uuid))) continue;

                    Quirk quirk = (Quirk) arena.getClasses().get(player.getUniqueId());

                    if (quirk.isBeingErased()) continue;

                    if (getLookingAt(Bukkit.getPlayer(uuid), player) && !erasingPlayers.contains(player.getUniqueId())) {
                        erasingPlayers.add(player.getUniqueId());
                        Bukkit.getPlayer(uuid).sendMessage(ChatColor.GREEN + "You are now erasing " + player.getName() + "'s quirk!");
                        Bukkit.getPlayer(uuid).playSound(Bukkit.getPlayer(uuid).getLocation(),Sound.ORB_PICKUP, 1f, 1f);
                        QuirkErasureEvent event = new QuirkErasureEvent(player, true);
                        Bukkit.getPluginManager().callEvent(event);
                    }


                }
                for (UUID uuid1 : erasingPlayers) {
                    Player quirkUser = Bukkit.getPlayer(uuid);
                    Player erased = Bukkit.getPlayer(uuid1);
                    if (!getLookingAt(quirkUser, erased)) {
                        QuirkErasureEvent event = new QuirkErasureEvent(erased, false);
                        Bukkit.getPluginManager().callEvent(event);
                        erasingPlayers.remove(uuid1);
                        quirkUser.sendMessage(ChatColor.RED + "You are no longer erasing " + erased.getName());
                    }
                }
                ticks++;
            }
        }.runTaskTimer(QuirkBattlesPlugin.getInstance(), 0L, 1L);
    }

    private void playEffect() {
        getAbilities().get(0).spawnRGBParticles(player.getEyeLocation(), 54, 0, 7, false);
        for (UUID uuid : erasingPlayers) {
            Player erased = Bukkit.getPlayer(uuid);
            Quirk enemyQuirk = (Quirk) arena.getClasses().get(uuid);
            enemyQuirk.getAbilities().get(0).spawnRGBParticles(erased.getEyeLocation().add(0, 1.5, 0),  255, 0, 51, false);
        }
    }

    // Use this to check if you are looking at a player in your radius!
    private boolean getLookingAt(Player player, Player player1) {
        Location eye = player.getEyeLocation();
        Vector toEntity = player1.getEyeLocation().toVector().subtract(eye.toVector());
        double dot = toEntity.normalize().dot(eye.getDirection());

        // .99D is most accurate. However, we have this set lower so that you don't have to have your crosshair directly on the player to erase them, they just need to be
        // somewhat close to the center of your FOV.
        return dot > 0.75D;
    }

    @EventHandler
    public void onSneak(PlayerToggleSneakEvent e) {
        if (arena.getState() != GameState.LIVE) return;
        if (!arena.getQuirkBattleGame().getAlivePlayers().contains(player.getUniqueId())) return;
        if (e.getPlayer().getUniqueId() != getUUID()) return;
      //  if (e.isSneaking() && isErasing) {
        //    stopErasing();
        //    return;
       // }

        if (e.isSneaking() && !isErasing && System.currentTimeMillis() - erasureCooldown >= cooldownTime * 1000L) {
            isErasing = true;
            player.sendMessage(ChatColor.GREEN + "You have activated " + ChatColor.RED + "Erasure!");
            player.sendMessage(ChatColor.GREEN + "Look at a player to temporarily " + ChatColor.RED + "erase " + ChatColor.GREEN + "their quirk!");
        }
        else if (e.isSneaking() && !isErasing) {
            player.sendMessage(ChatColor.RED + "Erasure is on cooldown!");
            player.playSound(player.getLocation(), Sound.VILLAGER_NO, 1f, 1f);
        }
    }

    @EventHandler
    public void onErase(QuirkErasureEvent e) {
        if (e.getQuirk() != this) return;
        if (e.isErasing()) {
            isBeingErased = true;
            setSharpenedKnife(false);
            this.hitCount = 0;
        }
        else isBeingErased = false;
    }

}
