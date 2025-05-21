package net.herobrine.quirkbattle.game.quirks.abilities.hero.engine;

import net.herobrine.core.HerobrinePVPCore;
import net.herobrine.gamecore.Arena;
import net.herobrine.gamecore.Class;
import net.herobrine.gamecore.GameState;
import net.herobrine.gamecore.Manager;
import net.herobrine.quirkbattle.QuirkBattlesPlugin;
import net.herobrine.quirkbattle.game.CustomDeathCause;
import net.herobrine.quirkbattle.game.quirks.abilities.Abilities;
import net.herobrine.quirkbattle.game.quirks.abilities.Ability;
import net.herobrine.quirkbattle.game.quirks.hero.Engine;
import net.herobrine.quirkbattle.util.Quirk;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Sound;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.UUID;

public class RapidKickAbility extends Ability {
    private final Engine engine;
    public RapidKickAbility(Abilities ability, Quirk quirk, int id, int slot) {
        super(ability, quirk, id, slot);
        this.engine = (Engine) quirk;


    }

    @Override
    public void doAbility(Player player) {

    Vector vec = player.getLocation().getDirection();
    Vector rotatedRight = vec.clone().crossProduct(new Vector(0, 1, 0)).normalize().multiply(5);
    Vector rotatedLeft = vec.clone().crossProduct(new Vector(0,1,0)).normalize().multiply(-6.5);

    double upwardBoost = 0.5;
    rotatedRight.setY(upwardBoost);
    rotatedLeft.setY(upwardBoost);

    player.setVelocity(rotatedRight);
    doCollision();
    long delay = 20;
    if (engine.getActiveBoosts() != 0) {
        delay = Math.round((float)20 / engine.getActiveBoosts());
    }

    player.playSound(player.getLocation(), Sound.GHAST_FIREBALL, 1.2f, 1f);
    new BukkitRunnable() {
        @Override
        public void run() {
            if (engine.isBeingErased() && engine.getOriginalId() == player.getUniqueId()) return;
            if (engine.isStunned()) return;
            player.playSound(player.getLocation(), Sound.GHAST_FIREBALL, 1.2f, 1f);
            player.setVelocity(rotatedLeft);
            doCollision();
        }
    }.runTaskLater(QuirkBattlesPlugin.getInstance(), delay);
    }


    public void doCollision() {
        new BukkitRunnable() {
            Player player = Bukkit.getPlayer(uuid);
            int cooldown = 15;
            final ArrayList<UUID> hasHit = new ArrayList<>();

            @Override
            public void run() {
                if (!Manager.isPlaying(player) || !Manager.getArena(player).getState().equals(GameState.LIVE)) {
                    cancel();
                    hasHit.clear();
                }
                if (!isActive()) {
                    cancel();
                    hasHit.clear();
                    return;
                }
                if (cooldown == 0) {
                    cancel();
                    hasHit.clear();
                } else {
                    for (Entity en : player.getNearbyEntities(1.5, 1, 1.5)) {
                        if (en.getType().equals(EntityType.PLAYER)) {
                            Player pl1 = (Player) en;
                            Arena arena = Manager.getArena(player);

                            if (!arena.getType().isTeamsMode()) {
                                if (pl1 != player && !hasHit.contains(player.getUniqueId()) && arena.getQuirkBattleGame().getAlivePlayers().contains(pl1.getUniqueId())) {
                                    hasHit.add(player.getUniqueId());
                                    doDamageTo(player, pl1, ability.getDamage(), CustomDeathCause.RAPID_KICK);
                                    pl1.sendMessage(HerobrinePVPCore.translateString("&6" + player.getName() + "&a just hit you with their &lRapid Kick &r&aattack!"));
                                    player.sendMessage(HerobrinePVPCore.translateString("&aYou just hit &6" + pl1.getName() + "&a with your &lRapid Kick &r&aattack!"));
                                }
                            } else if (pl1 != player && arena.getTeam(pl1) != arena.getTeam(player) && !hasHit.contains(player.getUniqueId()) && arena.getQuirkBattleGame().getAlivePlayers().contains(pl1.getUniqueId())) {
                                hasHit.add(player.getUniqueId());
                                doDamageTo(player, pl1, ability.getDamage(), CustomDeathCause.RAPID_KICK);
                                pl1.sendMessage(HerobrinePVPCore.translateString(arena.getTeam(player).getColor() + player.getName() + "&a just hit you with their &lRapid Kick &r&aattack!"));
                                player.sendMessage(HerobrinePVPCore.translateString("&aYou just hit " + arena.getTeam(pl1).getColor() + pl1.getName() + "&a with your &lRapid Kick &r&aattack!"));
                            }
                        }

                    }
                }

                cooldown--;
            }
        }.runTaskTimerAsynchronously(QuirkBattlesPlugin.getInstance(), 0, 1);
    }
}
