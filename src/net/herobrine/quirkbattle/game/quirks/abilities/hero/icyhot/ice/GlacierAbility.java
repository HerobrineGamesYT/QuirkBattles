package net.herobrine.quirkbattle.game.quirks.abilities.hero.icyhot.ice;

import net.herobrine.core.HerobrinePVPCore;
import net.herobrine.gamecore.Class;
import net.herobrine.gamecore.GameState;
import net.herobrine.quirkbattle.QuirkBattlesPlugin;
import net.herobrine.quirkbattle.game.CustomDeathCause;
import net.herobrine.quirkbattle.game.quirks.abilities.Abilities;
import net.herobrine.quirkbattle.game.quirks.abilities.Ability;
import net.herobrine.quirkbattle.game.quirks.abilities.SpecialCase;
import net.herobrine.quirkbattle.game.quirks.hero.IcyHot;
import net.herobrine.quirkbattle.util.Quirk;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class GlacierAbility extends Ability implements SpecialCase {

    private final IcyHot icy;
    private final Player player = Bukkit.getPlayer(uuid);

    private final Map<UUID, Long> hasHit = new HashMap<>();

    public GlacierAbility(Abilities ability, Quirk quirk, int id, int slot) {
        super(ability, quirk, id, slot);
        this.icy = (IcyHot) quirk;
    }

    @Override
    public void doAbility(Player player) {
        hasHit.clear();
        icy.setGlacierOn(true);
        doVFX((float) ability.getRadius());
        doCollison();
        arena.playSound(Sound.WITHER_SPAWN);
        if (!arena.getType().isTeamsMode()) arena.sendMessage(HerobrinePVPCore.getRankColor(player) + player.getName() + ChatColor.GREEN + " is now using their " + HerobrinePVPCore.translateString("&b&lGLACIER&r&a abiltiy!"));
        else arena.sendMessage(arena.getTeam(player).getColor() + player.getName() + ChatColor.GREEN + " is now using their " + HerobrinePVPCore.translateString("&b&lGLACIER&r&a ability!"));

    }


    public void doCollison() {
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

                if (!icy.isGlacierOn()) {
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

                    hasHit.put(target.getUniqueId(), System.currentTimeMillis());
                    target.addPotionEffect(new PotionEffect(PotionEffectType.SLOW, 60, 1), true);
                    target.sendMessage(ChatColor.GREEN + "You have been stunned by " + player.getName() + "'s" + ability.getDisplay() + "!");
                    player.sendMessage(ChatColor.GREEN + "You have stunned " + target.getName() + " with your " + ability.getDisplay() + ChatColor.GREEN + " effect!");
                    EntityDamageEvent dmg = new EntityDamageEvent(target, EntityDamageEvent.DamageCause.CUSTOM, ability.getDamage());
                    arena.getQuirkBattleGame().getCustomDeathCause().put(target.getUniqueId(), CustomDeathCause.GLACIER);
                    arena.getQuirkBattleGame().getLastAbilityAttacker().put(target.getUniqueId(), player.getUniqueId());
                    target.damage(0);
                    target.setLastDamageCause(dmg);
                    Bukkit.getPluginManager().callEvent(dmg);
                }
            }
        }.runTaskTimer(QuirkBattlesPlugin.getInstance(), 0L, 1L);
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

                if (!icy.isGlacierOn()) {
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
                    spawnRGBParticles(new Location(loc.getWorld(), loc.getX() + x, loc.getY() + addToY, loc.getZ() + z), 10, 128, 128, false);
                }


                addToY++;
            }
        }.runTaskTimer(QuirkBattlesPlugin.getInstance(), 0L, 10L);
    }

    public void stopGlacierNoCooldown() {
        icy.setGlacierOn(false);
        if (arena.getType().isTeamsMode()) arena.sendMessage(HerobrinePVPCore.getFileManager().getRank(player).getColor() + player.getName() + ChatColor.RED + " is no longer using their " + HerobrinePVPCore.translateString("&b&lGLACIER&r&c ability."));
        else arena.sendMessage(arena.getTeam(player).getColor() + player.getName() + ChatColor.RED + " is no longer using their " + HerobrinePVPCore.translateString("&b&lGLACIER&r&c ability."));
    }

    public void stopGlacier() {
        icy.setGlacierOn(false);
        setCooldown(System.currentTimeMillis());
        doAbilityCooldown();
        if (!arena.getType().isTeamsMode()) arena.sendMessage(HerobrinePVPCore.getFileManager().getRank(player).getColor() + player.getName() + ChatColor.RED + " is no longer using their " + HerobrinePVPCore.translateString("&b&lGLACIER&r&c ability."));
        else arena.sendMessage(arena.getTeam(player).getColor() + player.getName() + ChatColor.RED + " is no longer using their " + HerobrinePVPCore.translateString("&b&lGLACIER&r&c ability."));
    }


    @Override
    public boolean doesCasePass(Player player) {
        return !icy.isGlacierOn();
    }

    @Override
    public void doNoPass(Player player) {
        player.sendMessage(ChatColor.RED + "You are already using the Glacier Ability!");
        player.playSound(player.getLocation(), Sound.VILLAGER_NO, 1f, 1f);
    }
}
