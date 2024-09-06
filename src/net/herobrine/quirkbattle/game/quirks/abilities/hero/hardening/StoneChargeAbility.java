package net.herobrine.quirkbattle.game.quirks.abilities.hero.hardening;

import net.herobrine.core.HerobrinePVPCore;
import net.herobrine.gamecore.*;
import net.herobrine.gamecore.Class;
import net.herobrine.quirkbattle.QuirkBattlesPlugin;
import net.herobrine.quirkbattle.game.CustomDeathCause;
import net.herobrine.quirkbattle.game.quirks.abilities.Abilities;
import net.herobrine.quirkbattle.game.quirks.abilities.Ability;
import net.herobrine.quirkbattle.game.quirks.abilities.SpecialCase;
import net.herobrine.quirkbattle.game.quirks.hero.Hardening;
import net.minecraft.server.v1_8_R3.EnumParticle;
import net.minecraft.server.v1_8_R3.PacketPlayOutWorldParticles;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.ArrayList;
import java.util.UUID;

public class StoneChargeAbility extends Ability implements SpecialCase {
    public StoneChargeAbility(Abilities ability, Class quirk, int id, int slot) {
        super(ability, quirk, id, slot);
        this.hardening = (Hardening) quirk;
        this.player = Bukkit.getPlayer(uuid);
    }

    Hardening hardening;
    Player player;

    boolean isCharging;

    @Override
    public void doAbility(Player player) {
    arena.getQuirkBattleGame().getStats(player).setDefense(arena.getQuirkBattleGame().getStats(player).getDefense() + ability.getDefenseBoost());
    player.setWalkSpeed(.4F);
    player.playSound(player.getLocation(), Sound.ZOMBIE_WOOD, 1f, 1f);
    player.sendMessage(ability.getDisplay() + ChatColor.GREEN + " has been activated!");
    isCharging = true;
    doStoneChargeCollisionChecks(player);
    new BukkitRunnable() {
        @Override
        public void run() {
        if (isCharging) {
            stopStoneCharge();
            player.sendMessage(ChatColor.RED + "You couldn't hit anybody with your " + ability.getDisplay() + "! Charge a little harder next time.");
            player.playSound(player.getLocation(), Sound.BAT_IDLE, 1f, 1f);
        }
        }
    }.runTaskLater(QuirkBattlesPlugin.getInstance(), 100L);

    }

    public void stopStoneCharge() {
        setCooldown(System.currentTimeMillis());
        doAbilityCooldown();
        isCharging = false;
        player.setWalkSpeed(.2F);
        arena.getQuirkBattleGame().getStats(player).setDefense(arena.getQuirkBattleGame().getStats(player).getDefense() - ability.getDefenseBoost());
    }


    public void doStoneChargeCollisionChecks(Player player) {
        new BukkitRunnable() {
            ArrayList<UUID> hasHit = new ArrayList<>();

            @Override
            public void run() {
                if (!Manager.isPlaying(player) || !Manager.getArena(player).getState().equals(GameState.LIVE)) {
                    cancel();
                    hasHit.clear();
                    return;
                }

                if (!hasHit.isEmpty()) {
                    cancel();
                    hasHit.clear();
                    stopStoneCharge();
                }
                else {
                    for (Entity en : player.getNearbyEntities(1, 0.5, 1)) {
                        if (en.getType().equals(EntityType.PLAYER)) {
                            Player pl1 = (Player) en;
                            Arena arena = Manager.getArena(player);

                            if (!arena.getType().isTeamsMode()) {
                                if (pl1 != player && !hasHit.contains(player.getUniqueId()) && arena.getQuirkBattleGame().getAlivePlayers().contains(pl1.getUniqueId())) {
                                    hasHit.add(player.getUniqueId());
                                    doDamageTo(player, pl1, ability.getDamage(), CustomDeathCause.STONE_CHARGE);
                                    playHitFX(pl1.getLocation());
                                    pl1.sendMessage(HerobrinePVPCore.translateString("&6" + player.getName() + "&a just hit you with their &l" + ability.getDisplay() + " &r&aattack!"));
                                    player.sendMessage(HerobrinePVPCore.translateString("&aYou just hit &6" + pl1.getName() + "&a with your &l" + ability.getDisplay() + " &r&aattack!"));
                                }
                            } else if (pl1 != player && arena.getTeam(pl1) != arena.getTeam(player)
                                    && !hasHit.contains(player.getUniqueId()) && arena.getQuirkBattleGame().getAlivePlayers().contains(pl1.getUniqueId())) {
                                hasHit.add(player.getUniqueId());
                                doDamageTo(player, pl1, ability.getDamage(), CustomDeathCause.STONE_CHARGE);
                                playHitFX(pl1.getLocation());
                                pl1.sendMessage(HerobrinePVPCore.translateString(arena.getTeam(player).getColor() + player.getName() + "&a just hit you with their &l" + ability.getDisplay() + " &r&aattack!"));
                                player.sendMessage(HerobrinePVPCore.translateString("&aYou just hit " + arena.getTeam(pl1).getColor() + pl1.getName() + "&a with your &l" + ability.getDisplay() + " &r&aattack!"));
                            }
                        }

                    }
                }

            }
        }.runTaskTimer(QuirkBattlesPlugin.getInstance(), 0, 1);
    }

    public void playHitFX(Location l) {

        double r = 2.0;
        for (double phi = 0; phi <= Math.PI; phi += Math.PI / 15) {
            double y = r * Math.cos(phi) + 1.5;
            for (double theta = 0; theta <= 2 * Math.PI; theta += Math.PI / 30) {
                double x = r * Math.cos(theta) * Math.sin(phi);
                double z = r * Math.sin(theta) * Math.sin(phi);
                l.add(x, y, z);
                PacketPlayOutWorldParticles packet = new PacketPlayOutWorldParticles(EnumParticle.BLOCK_CRACK, true, (float) l.getX(), (float) l.getY(), (float) l.getZ(), 0, 0, 0, 0, 1, 1);
                arena.sendPacket(packet);
                l.subtract(x, y, z);
            }
        }
        l.getWorld().playSound(l, Sound.ZOMBIE_WOODBREAK, 1f, 1f);
    }

    @Override
    public boolean doesCasePass(Player player) {
        return !isCharging;
    }

    @Override
    public void doNoPass(Player player) {
    player.sendMessage(ChatColor.RED + "You are already charging!");
    player.playSound(player.getLocation(), Sound.VILLAGER_NO, 1f,1f);
    }
}
