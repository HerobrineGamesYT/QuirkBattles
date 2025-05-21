package net.herobrine.quirkbattle.game.quirks.abilities.hero.icyhot.fire;

import net.herobrine.core.HerobrinePVPCore;
import net.herobrine.gamecore.Class;
import net.herobrine.gamecore.GameState;
import net.herobrine.gamecore.GameType;
import net.herobrine.quirkbattle.QuirkBattlesPlugin;
import net.herobrine.quirkbattle.game.CustomDeathCause;
import net.herobrine.quirkbattle.game.quirks.abilities.Abilities;
import net.herobrine.quirkbattle.game.quirks.abilities.Ability;
import net.herobrine.quirkbattle.game.quirks.abilities.SpecialCase;
import net.herobrine.quirkbattle.game.quirks.hero.IcyHot;
import net.herobrine.quirkbattle.util.Quirk;
import net.minecraft.server.v1_8_R3.EnumParticle;
import net.minecraft.server.v1_8_R3.PacketPlayOutWorldParticles;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.craftbukkit.v1_8_R3.entity.CraftPlayer;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class OverdriveAbility extends Ability implements SpecialCase {

    private final Player player = Bukkit.getPlayer(uuid);
    private final IcyHot icy;

    private final Map<UUID, Long> hasHit = new HashMap<>();

    public OverdriveAbility(Abilities ability, Quirk quirk, int id, int slot) {
        super(ability, quirk, id, slot);
        this.icy = (IcyHot) quirk;
    }

    @Override
    public void doAbility(Player player) {
        hasHit.clear();
        icy.setOverdriveOn(true);
        doVFX((float) ability.getRadius());
        doCollision();
        arena.playSound(Sound.WITHER_SPAWN);
        if (!arena.getType().isTeamsMode()) arena.sendMessage(HerobrinePVPCore.getRankColor(player) + player.getName() + ChatColor.GREEN + " is now using their " + HerobrinePVPCore.translateString("&c&lOVERDRIVE&r&a abiltiy!"));
        else arena.sendMessage(arena.getTeam(player).getColor() + player.getName() + ChatColor.GREEN + " is now using their " + HerobrinePVPCore.translateString("&c&OVERDRIVE&r&a ability!"));
    }

    @Override
    public boolean doesCasePass(Player player) {
        return !icy.isOverdriveOn();
    }

    @Override
    public void doNoPass(Player player) {
        player.sendMessage(ChatColor.RED + "You already have overdrive active!");
        player.playSound(player.getLocation(), Sound.VILLAGER_NO, 1f,1f );
    }

    public void doVFX(float radius) {
        new BukkitRunnable() {
            int addToY = 0;
            Location loc = player.getLocation();

            public void run() {
                loc = player.getLocation();
                if (!arena.getState().equals(GameState.LIVE)) {
                    cancel();
                    return;
                }

                if (!icy.isOverdriveOn()) {
                    cancel();
                    return;
                }

                if (!arena.getQuirkBattleGame().getAlivePlayers().contains(player.getUniqueId())) {
                    cancel();
                    return;
                }

                if (addToY > 3) addToY = 0;

                for (double t = 0; t < 1000; t += 0.5) {
                    float x = radius * (float) Math.sin(t);
                    float z = radius * (float) Math.cos(t);
                    spawnRGBParticles(new Location(loc.getWorld(), loc.getX() + x, loc.getY() + addToY, loc.getZ() + z), 179, 67, 27, false);
                }


                addToY++;
            }
        }.runTaskTimer(QuirkBattlesPlugin.getInstance(), 0L, 10L);

    }

    public void doBurningDamage(final Player target) {
        new BukkitRunnable() {
                    int i = 0;
                    @Override
                    public void run() {
                        if (arena.getState().equals(GameState.LIVE) || !arena.getQuirkBattleGame().getAlivePlayers().contains(target.getUniqueId())) {
                            cancel();
                            return;
                        }

                        if (i > 2) {
                            cancel();
                            return;
                        }
                        player.sendMessage(ChatColor.GREEN + "target is " + target.getName());
                        EntityDamageEvent dmg = new EntityDamageEvent(target, EntityDamageEvent.DamageCause.CUSTOM, ability.getDamage());
                        arena.getQuirkBattleGame().getCustomDeathCause().put(target.getUniqueId(), CustomDeathCause.OVERDRIVE);
                        arena.getQuirkBattleGame().getLastAbilityAttacker().put(target.getUniqueId(), player.getUniqueId());
                        target.playSound(target.getLocation(), Sound.FIZZ, 1f, 1f);
                        target.damage(0);
                        target.setLastDamageCause(dmg);
                        Bukkit.getPluginManager().callEvent(dmg);
                spawnRGBParticles(target.getPlayer().getEyeLocation().add(0, 1.5, 0),  179, 67, 27, true);
                i++;
            }
        }.runTaskTimer(QuirkBattlesPlugin.getInstance(), 0L, 10L);
    }

    public void doDamageTick(Player target) {

    }

    public void doCollision() {
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

                if (!icy.isOverdriveOn()) {
                    cancel();
                    return;
                }


                for (Entity entity : player.getNearbyEntities(ability.getRadius(), ability.getRadius(), ability.getRadius())) {
                    if (!(entity instanceof Player)) continue;
                    Player target = (Player) entity;
                    if (arena.getState() != GameState.LIVE) return;
                    if (arena.getSpectators().contains(target.getUniqueId())) continue;
                    if (!arena.getQuirkBattleGame().getAlivePlayers().contains(target.getUniqueId())) continue;
                    if (arena.getType().isTeamsMode()) {
                        if (arena.getTeam(player).equals(arena.getTeam(target))) continue;
                    }
                    if (target == player) continue;
                    if (hasHit.containsKey(target.getUniqueId())) {
                        if (System.currentTimeMillis() - hasHit.get(target.getUniqueId()) < 2000) continue;
                    }

                    EntityDamageEvent dmg = new EntityDamageEvent(target, EntityDamageEvent.DamageCause.CUSTOM, ability.getDamage());
                    arena.getQuirkBattleGame().getCustomDeathCause().put(target.getUniqueId(), CustomDeathCause.OVERDRIVE);
                    arena.getQuirkBattleGame().getLastAbilityAttacker().put(target.getUniqueId(), player.getUniqueId());
                    target.playSound(target.getLocation(), Sound.FIZZ, 1f, 1f);
                    target.damage(0);
                    target.setLastDamageCause(dmg);
                    Bukkit.getPluginManager().callEvent(dmg);
                    spawnRGBParticles(target.getPlayer().getEyeLocation().add(0, 1.5, 0),  179, 67, 27, true);

                    hasHit.put(target.getUniqueId(), System.currentTimeMillis());
                    target.sendMessage(ChatColor.GREEN + "You have been burned by " + player.getName() + "'s" + ability.getDisplay() + "!");
                    player.sendMessage(ChatColor.GREEN + "You have burned " + target.getName() + " with your " + ability.getDisplay() + ChatColor.GREEN + " effect!");
                    //TODO CLEAN UP
                    new BukkitRunnable() {
                        @Override
                        public void run() {
                            EntityDamageEvent dmg = new EntityDamageEvent(target, EntityDamageEvent.DamageCause.CUSTOM, ability.getDamage());
                            arena.getQuirkBattleGame().getCustomDeathCause().put(target.getUniqueId(), CustomDeathCause.OVERDRIVE);
                            arena.getQuirkBattleGame().getLastAbilityAttacker().put(target.getUniqueId(), player.getUniqueId());
                            target.playSound(target.getLocation(), Sound.FIZZ, 1f, 1f);
                            target.damage(0);
                            target.setLastDamageCause(dmg);
                            Bukkit.getPluginManager().callEvent(dmg);
                            spawnRGBParticles(target.getPlayer().getEyeLocation().add(0, 1.5, 0),  179, 67, 27, true);
                        }
                    }.runTaskLater(QuirkBattlesPlugin.getInstance(), 10L);

                    new BukkitRunnable() {
                        @Override
                        public void run() {
                            EntityDamageEvent dmg = new EntityDamageEvent(target, EntityDamageEvent.DamageCause.CUSTOM, ability.getDamage());
                            arena.getQuirkBattleGame().getCustomDeathCause().put(target.getUniqueId(), CustomDeathCause.OVERDRIVE);
                            arena.getQuirkBattleGame().getLastAbilityAttacker().put(target.getUniqueId(), player.getUniqueId());
                            target.playSound(target.getLocation(), Sound.FIZZ, 1f, 1f);
                            target.damage(0);
                            target.setLastDamageCause(dmg);
                            Bukkit.getPluginManager().callEvent(dmg);
                            spawnRGBParticles(target.getPlayer().getEyeLocation().add(0, 1.5, 0),  179, 67, 27, true);
                        }
                    }.runTaskLater(QuirkBattlesPlugin.getInstance(), 20L);
                }
            }
        }.runTaskTimer(QuirkBattlesPlugin.getInstance(), 0L, 1L);
    }
    public void stopOverdriveNoCooldown() {
        icy.setOverdriveOn(false);
        if (!arena.getType().isTeamsMode()) arena.sendMessage(HerobrinePVPCore.getFileManager().getRank(player).getColor() + player.getName() + ChatColor.RED + " is no longer using their " + HerobrinePVPCore.translateString("&c&lOVERDRIVE&r&c ability."));
        else arena.sendMessage(arena.getTeam(player).getColor() + player.getName() + ChatColor.RED + " is no longer using their " + HerobrinePVPCore.translateString("&c&lOVERDRIVE&r&c ability."));
    }

    public void stopOverdrive() {
        icy.setOverdriveOn(false);
        setCooldown(System.currentTimeMillis());
        doAbilityCooldown();
        if (!arena.getType().isTeamsMode()) arena.sendMessage(HerobrinePVPCore.getFileManager().getRank(player).getColor() + player.getName() + ChatColor.RED + " is no longer using their " + HerobrinePVPCore.translateString("&c&lOVERDRIVE&r&c ability."));
        else arena.sendMessage(arena.getTeam(player).getColor() + player.getName() + ChatColor.RED + " is no longer using their " + HerobrinePVPCore.translateString("&c&lOVERDRIVE&r&c ability."));
    }
}
