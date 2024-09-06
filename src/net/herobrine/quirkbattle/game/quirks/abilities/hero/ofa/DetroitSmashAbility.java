package net.herobrine.quirkbattle.game.quirks.abilities.hero.ofa;

import net.herobrine.core.HerobrinePVPCore;
import net.herobrine.gamecore.*;
import net.herobrine.gamecore.Class;
import net.herobrine.quirkbattle.QuirkBattlesPlugin;
import net.herobrine.quirkbattle.game.CustomDeathCause;
import net.herobrine.quirkbattle.game.quirks.abilities.Abilities;
import net.herobrine.quirkbattle.game.quirks.abilities.Ability;
import net.herobrine.quirkbattle.game.quirks.hero.OneForAll;
import net.minecraft.server.v1_8_R3.EnumParticle;
import net.minecraft.server.v1_8_R3.PacketPlayOutWorldParticles;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.UUID;

public class DetroitSmashAbility extends Ability {
    public DetroitSmashAbility(Abilities ability, Class quirk, int id, int slot) {
        super(ability, quirk, id, slot);
    }

    @Override
    public void doAbility(Player player) {
        Vector vecUp = new Vector(0, 2, 0);
        Vector vecDown = player.getLocation().getDirection().setY(-2);

        vecDown.normalize();
        vecDown.multiply(1.5);

        player.setVelocity(vecUp);
        player.playSound(player.getLocation(), Sound.GHAST_FIREBALL, 1f, 1.2f);
        new BukkitRunnable() {
            @Override
            public void run() {
                player.playSound(player.getLocation(), Sound.GHAST_FIREBALL, 1f, .7f);
                player.setVelocity(vecDown);
                doDetroitSmashCollisionChecks(player, stats.getMana());
                OneForAll quirk = (OneForAll) getQuirk();
                quirk.resetPower();

            }
        }.runTaskLater(QuirkBattlesPlugin.getInstance(), 10L);
    }

    public void circleEffect(Location loc, float radius) {
        for (double t = 0; t < 1000; t += 0.5) {
            float x = radius * (float) Math.sin(t);
            float z = radius * (float) Math.cos(t);

            PacketPlayOutWorldParticles packet = new PacketPlayOutWorldParticles(EnumParticle.BLOCK_CRACK, true,
                    (float) loc.getX() + x, (float) loc.getY(), (float) loc.getZ() + z, 0, 0, 0, 0, 1, 3);
            Manager.getArena(Bukkit.getPlayer(uuid)).sendPacket(packet);
        }

    }

    public void doDetroitSmashCollisionChecks(Player player, int power) {
        new BukkitRunnable() {
            ArrayList<UUID> hasHit = new ArrayList<>();
            @Override
            public void run() {
                if (!Manager.isPlaying(player) || !Manager.getArena(player).getState().equals(GameState.LIVE)) {
                    cancel();
                    hasHit.clear();
                }
                if (player.isOnGround()) {
                    cancel();
                    player.getLocation().getWorld().playSound(player.getLocation(), Sound.ZOMBIE_WOODBREAK, 1f, .75f);
                    circleEffect(player.getLocation(), (float) ability.getRadius());
                    for (Entity en : player.getNearbyEntities(ability.getRadius(), 1, ability.getRadius())) {
                        if (en.getType().equals(EntityType.PLAYER)) {
                            Player pl1 = (Player) en;
                            if (!arena.getType().isTeamsMode()) {
                                if (pl1 != player && !hasHit.contains(pl1.getUniqueId()) && arena.getQuirkBattleGame().getAlivePlayers().contains(pl1.getUniqueId())) {
                                    hasHit.add(pl1.getUniqueId());
                                    doDamageTo(player, pl1, ability.getDamage(), power, CustomDeathCause.DETRIOT_SMASH);
                                    pl1.sendMessage(HerobrinePVPCore.translateString("&6" + player.getName() + "&a just hit you with their &lDetroit Smash &r&aattack at &6" + power + "% &aPower!"));
                                    player.sendMessage(HerobrinePVPCore.translateString("&aYou just hit &6" + pl1.getName() + "&a with your &lDetroit Smash &r&aattack!"));
                                }
                            }
                            else if (pl1 != player && arena.getTeam(pl1) != arena.getTeam(player)
                                    && !hasHit.contains(pl1.getUniqueId()) && arena.getQuirkBattleGame().getAlivePlayers().contains(pl1.getUniqueId())) {
                                hasHit.add(pl1.getUniqueId());
                                doDamageTo(player, pl1, ability.getDamage(), power, CustomDeathCause.DETRIOT_SMASH);
                                pl1.sendMessage(HerobrinePVPCore.translateString(arena.getTeam(player).getColor() + player.getName() + "&a just hit you with their &lDetroit Smash &r&aattack at &6" + power + "% &aPower!"));
                                player.sendMessage(HerobrinePVPCore.translateString("&aYou just hit " + arena.getTeam(pl1).getColor() +  pl1.getName() + "&a with your &lDetroit Smash &r&aattack!"));
                            }
                        }
                    }
                }
            }
        }.runTaskTimerAsynchronously(QuirkBattlesPlugin.getInstance(), 0, 1);
    }
}
