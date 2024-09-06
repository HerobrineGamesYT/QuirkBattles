package net.herobrine.quirkbattle.game.quirks.abilities.hero.explosion;

import net.herobrine.core.HerobrinePVPCore;
import net.herobrine.gamecore.*;
import net.herobrine.gamecore.Class;
import net.herobrine.quirkbattle.QuirkBattlesPlugin;
import net.herobrine.quirkbattle.game.CustomDeathCause;
import net.herobrine.quirkbattle.game.quirks.abilities.Abilities;
import net.herobrine.quirkbattle.game.quirks.abilities.Ability;
import net.herobrine.quirkbattle.game.quirks.hero.Explosion;
import net.minecraft.server.v1_8_R3.EntityPlayer;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.UUID;

public class HowitzerImpactAbility extends Ability {
    public HowitzerImpactAbility(Abilities ability, Class quirk, int id, int slot) {
        super(ability, quirk, id, slot);
        this.exp = (Explosion) quirk;
    }
    Explosion exp;
    @Override
    public void doAbility(Player player) {
        exp.resetDefaultItem();
        Vector vector = new Vector(0,4,0);

        player.setVelocity(vector);
        player.getLocation().getWorld().createExplosion(player.getLocation().getX(),
                player.getLocation().getY(), player.getLocation().getZ(), 2f, false, false);
        doExplosionCollisonForHowitzer(player.getLocation(), 2f, 10, player);
        new BukkitRunnable() {
            int maxExplosions = 3;
            int explosionCount = 0;
            int ticks = 0;
            @Override
            public void run() {
                if (arena.getState() != GameState.LIVE) {
                    cancel();
                    return;
                }
                if (arena.getClasses().get(player.getUniqueId()) != getQuirk()) {
                    cancel();
                    return;
                }

                if (player.isOnGround() && ticks > 10) {
                    cancel();
                    player.getLocation().getWorld().createExplosion(player.getLocation().getX(),
                            player.getLocation().getY(), player.getLocation().getZ(), (float)ability.getRadius(), false, false);
                    doExplosionCollisonForHowitzer(player.getLocation(), (float)ability.getRadius(), (int)ability.getDamage(), player);
                    return;
                }
                if (ticks % 10 == 0 && explosionCount < maxExplosions) {
                    player.getLocation().getWorld().createExplosion(player.getLocation().getX(),
                            player.getLocation().getY(), player.getLocation().getZ(), 2f, false, false);
                    doExplosionCollisonForHowitzer(player.getLocation(), 2f, 10, player);
                    explosionCount = explosionCount + 1;
                }
                ticks++;
            }
        }.runTaskTimer(QuirkBattlesPlugin.getInstance(), 4L, 1L);
    }

    public void doExplosionCollisonForHowitzer(Location explosionLocation, float radius, int damage, Player player) {
        new BukkitRunnable() {
            int cooldown = 10;
            ArrayList<UUID> hasHit = new ArrayList<>();

            @Override
            public void run() {
                if (!Manager.isPlaying(player) || Manager.getArena(player).getState().equals(GameState.RECRUITING)) {
                    cancel();
                    hasHit.clear();
                }
                if (cooldown == 0) {
                    cancel();
                    hasHit.clear();
                }
                else {
                    cancel();
                    for (Entity en : explosionLocation.getWorld().getNearbyEntities(explosionLocation, radius, 1.5, radius)) {
                        if (en.getType().equals(EntityType.PLAYER)) {
                            Player pl1 = (Player) en;
                            Arena arena = Manager.getArena(player);

                            if (!arena.getType().isTeamsMode()) {
                                if (pl1 != player && !hasHit.contains(pl1.getUniqueId()) && arena.getQuirkBattleGame().getAlivePlayers().contains(pl1.getUniqueId())) {
                                    hasHit.add(pl1.getUniqueId());
                                    doDamageTo(player, pl1, damage, CustomDeathCause.HOWITZER_IMPACT);
                                    pl1.sendMessage(HerobrinePVPCore.translateString("&6" + player.getName() + "&a just hit you with an explosion their &6&lHowitzer Impact &r&aattack!"));
                                    player.sendMessage(HerobrinePVPCore.translateString("&aYou just hit &6" + pl1.getName() + "&a with an explosion from your &6&lHowitzer Impact &r&aattack!"));
                                    exp.giveStaminaBoost(ability.getStaminaBoost());
                                }
                            }
                            else if (pl1 != player && arena.getTeam(pl1) != arena.getTeam(player)
                                    && !hasHit.contains(pl1.getUniqueId()) && arena.getQuirkBattleGame().getAlivePlayers().contains(pl1.getUniqueId())) {
                                hasHit.add(pl1.getUniqueId());
                                doDamageTo(player, pl1, damage, CustomDeathCause.HOWITZER_IMPACT);
                                pl1.sendMessage(HerobrinePVPCore.translateString(arena.getTeam(player).getColor() + player.getName() + "&a just hit you with an explosion from their &&6lHowitzer Impact &r&aattack!"));
                                player.sendMessage(HerobrinePVPCore.translateString("&aYou just hit " + arena.getTeam(pl1).getColor() +  pl1.getName() + "&a with an explosion from your &6&lHowitzer Impact &r&aattack!"));
                                exp.giveStaminaBoost(ability.getStaminaBoost());
                            }
                        }
                    }
                }
            }
        }.runTaskTimerAsynchronously(QuirkBattlesPlugin.getInstance(), 0, 1);
    }

}
