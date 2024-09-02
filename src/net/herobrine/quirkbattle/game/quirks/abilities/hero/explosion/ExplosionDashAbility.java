package net.herobrine.quirkbattle.game.quirks.abilities.hero.explosion;

import net.herobrine.core.HerobrinePVPCore;
import net.herobrine.gamecore.*;
import net.herobrine.gamecore.Class;
import net.herobrine.quirkbattle.QuirkBattlesPlugin;
import net.herobrine.quirkbattle.game.CustomDeathCause;
import net.herobrine.quirkbattle.game.quirks.abilities.Abilities;
import net.herobrine.quirkbattle.game.quirks.abilities.Ability;
import net.herobrine.quirkbattle.game.quirks.hero.Explosion;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.UUID;

public class ExplosionDashAbility extends Ability {
    public ExplosionDashAbility(Abilities ability, Class quirk, int id, int slot) {
        super(ability, quirk, id, slot);
        exp = (Explosion) quirk;
    }
    Explosion exp;

    @Override
    public void doAbility(Player player) {
        exp.resetDefaultItem();
        Vector dir = player.getLocation().getDirection();
        dir.normalize();
        dir.multiply(3);
        player.setVelocity(dir);
        player.getLocation().getWorld().createExplosion(player.getLocation().getX(),
                player.getLocation().getY(), player.getLocation().getZ(), 2f, false, false);
        doExplosionDashCollison(player.getLocation(), player);
        player.playSound(player.getLocation(), Sound.GHAST_FIREBALL, 1f, 1.2f);
        exp.giveStaminaBoost(ability.getStaminaBoost());
    }

    public void doExplosionDashCollison(Location explosionLocation, Player player) {
        new BukkitRunnable() {
            int cooldown = 15;
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
                    for (Entity en : explosionLocation.getWorld().getNearbyEntities(explosionLocation, 2, 1.5, 2)) {
                        if (en.getType().equals(EntityType.PLAYER)) {
                            Player pl1 = (Player) en;
                            Arena arena = Manager.getArena(player);

                            if (arena.getType().equals(GameType.ONE_V_ONE)) {
                                if (pl1 != player && !hasHit.contains(pl1.getUniqueId()) && arena.getQuirkBattleGame().getAlivePlayers().contains(pl1.getUniqueId())) {
                                    hasHit.add(pl1.getUniqueId());
                                    doDamageTo(player, pl1, ability.getDamage(), CustomDeathCause.EXPLOSION_DASH);
                                    pl1.sendMessage(HerobrinePVPCore.translateString("&6" + player.getName() + "&a just hit you with their &6&lExplosion Dash &r&aattack!"));
                                    player.sendMessage(HerobrinePVPCore.translateString("&aYou just hit &6" + pl1.getName() + "&a with your &6&lExplosion Dash &r&aattack!"));
                                }
                            }
                            else if (pl1 != player && arena.getTeam(pl1) != arena.getTeam(player)
                                    && !hasHit.contains(pl1.getUniqueId()) && arena.getQuirkBattleGame().getAlivePlayers().contains(pl1.getUniqueId())) {
                                hasHit.add(pl1.getUniqueId());
                                doDamageTo(player, pl1, ability.getDamage(), CustomDeathCause.EXPLOSION_DASH);
                                pl1.sendMessage(HerobrinePVPCore.translateString(arena.getTeam(player).getColor() + player.getName() + "&a just hit you with their &&6lExplosion Dash &r&aattack!"));
                                player.sendMessage(HerobrinePVPCore.translateString("&aYou just hit " + arena.getTeam(pl1).getColor() +  pl1.getName() + "&a with your &6&lExplosion Dash &r&aattack!"));
                            }
                        }
                    }
                }
            }
        }.runTaskTimerAsynchronously(QuirkBattlesPlugin.getInstance(), 0, 1);
    }
}
