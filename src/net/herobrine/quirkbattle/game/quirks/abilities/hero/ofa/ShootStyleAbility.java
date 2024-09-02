package net.herobrine.quirkbattle.game.quirks.abilities.hero.ofa;

import net.herobrine.core.HerobrinePVPCore;
import net.herobrine.gamecore.*;
import net.herobrine.gamecore.Class;
import net.herobrine.quirkbattle.QuirkBattlesPlugin;
import net.herobrine.quirkbattle.game.CustomDeathCause;
import net.herobrine.quirkbattle.game.quirks.abilities.Abilities;
import net.herobrine.quirkbattle.game.quirks.abilities.Ability;
import net.herobrine.quirkbattle.game.quirks.hero.OneForAll;
import org.bukkit.Sound;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.UUID;

public class ShootStyleAbility extends Ability {
    public ShootStyleAbility(Abilities ability, Class quirk, int id, int slot) {
        super(ability, quirk, id, slot);
    }

    @Override
    public void doAbility(Player player) {
        OneForAll ofa = (OneForAll) getQuirk();
        Vector vecUp = new Vector(0, 0.5, 0);
        Vector vecForward = player.getLocation().getDirection().setY(0);

        vecForward.normalize();
        vecForward.multiply(3);

        player.setVelocity(vecUp);
        player.playSound(player.getLocation(), Sound.GHAST_FIREBALL, 1f, 1.2f);
        new BukkitRunnable() {
            @Override
            public void run() {
                player.playSound(player.getLocation(), Sound.GHAST_FIREBALL, 1f, .7f);
                player.setVelocity(vecForward);
                doShootStyleCollisionChecks(player, stats.getMana());
                ofa.resetPower();
            }
        }.runTaskLater(QuirkBattlesPlugin.getInstance(), 10L);
    }

    public void doShootStyleCollisionChecks(Player player, int power) {
        new BukkitRunnable() {
            int cooldown = 15;
            ArrayList<UUID> hasHit = new ArrayList<>();

            @Override
            public void run() {
                if (!Manager.isPlaying(player) || !Manager.getArena(player).getState().equals(GameState.LIVE)) {
                    cancel();
                    hasHit.clear();
                }

                if (cooldown == 0) {
                    cancel();
                    hasHit.clear();
                }
                else {
                    for (Entity en : player.getNearbyEntities(1.5, 1, 1.5)) {
                        if (en.getType().equals(EntityType.PLAYER)) {
                            Player pl1 = (Player) en;
                            Arena arena = Manager.getArena(player);

                            if (arena.getType().equals(GameType.ONE_V_ONE)) {
                                if (pl1 != player && !hasHit.contains(player.getUniqueId()) && arena.getQuirkBattleGame().getAlivePlayers().contains(pl1.getUniqueId())) {
                                    hasHit.add(player.getUniqueId());
                                    doDamageTo(player, pl1, ability.getDamage(), power, CustomDeathCause.SHOOT_STYLE);
                                    pl1.sendMessage(HerobrinePVPCore.translateString("&6" + player.getName() + "&a just hit you with their &lShoot Style &r&aattack at &6" + power + "% &aPower!"));
                                    player.sendMessage(HerobrinePVPCore.translateString("&aYou just hit &6" + pl1.getName() + "&a with your &lShoot Style &r&aattack!"));
                                }
                            }
                            else if (pl1 != player && arena.getTeam(pl1) != arena.getTeam(player)
                                    && !hasHit.contains(player.getUniqueId()) && arena.getQuirkBattleGame().getAlivePlayers().contains(pl1.getUniqueId())) {
                                hasHit.add(player.getUniqueId());
                                doDamageTo(player, pl1, ability.getDamage(), power, CustomDeathCause.SHOOT_STYLE);
                                pl1.sendMessage(HerobrinePVPCore.translateString(arena.getTeam(player).getColor() + player.getName() + "&a just hit you with their &lShoot Style &r&aattack at &6" + power + "% &aPower!"));
                                player.sendMessage(HerobrinePVPCore.translateString("&aYou just hit " + arena.getTeam(pl1).getColor() +  pl1.getName() + "&a with your &lShoot Style &r&aattack!"));
                            }
                        }

                    }
                }

                cooldown--;
            }
        }.runTaskTimerAsynchronously(QuirkBattlesPlugin.getInstance(), 0, 1);
    }
}
