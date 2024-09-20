package net.herobrine.quirkbattle.game.quirks.abilities.hero.hardening;

import net.herobrine.core.HerobrinePVPCore;
import net.herobrine.gamecore.*;
import net.herobrine.gamecore.Class;
import net.herobrine.quirkbattle.QuirkBattlesPlugin;
import net.herobrine.quirkbattle.game.quirks.abilities.Abilities;
import net.herobrine.quirkbattle.game.quirks.abilities.Ability;
import net.herobrine.quirkbattle.game.quirks.abilities.SpecialCase;
import net.herobrine.quirkbattle.game.quirks.hero.Hardening;
import net.herobrine.quirkbattle.util.Quirk;
import net.minecraft.server.v1_8_R3.EnumParticle;
import net.minecraft.server.v1_8_R3.PacketPlayOutWorldParticles;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.craftbukkit.v1_8_R3.entity.CraftPlayer;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.UUID;

public class UnbreakableAbility extends Ability implements SpecialCase {
    public UnbreakableAbility(Abilities ability, Class quirk, int id, int slot) {
        super(ability, quirk, id, slot);
        this.hardening = (Hardening) quirk;
        this.player = Bukkit.getPlayer(uuid);
    }
    Hardening hardening;
    Player player;
    @Override
    public void doAbility(Player player) {
    hardening.setUnbreakable(true);
    hardening.setSharpClaw(true);
    arena.getQuirkBattleGame().getStats(player).setDefense(arena.getQuirkBattleGame().getStats(player).getDefense() + ability.getDefenseBoost());
    arena.playSound(Sound.WITHER_SPAWN);
    if(!arena.getType().isTeamsMode()) arena.sendMessage(HerobrinePVPCore.getFileManager().getRank(player).getColor() + player.getName() + ChatColor.GREEN + " is now " + HerobrinePVPCore.translateString("&c&lUNBREAKABLE&r&a!"));
    else arena.sendMessage(arena.getTeam(player).getColor() + player.getName() + ChatColor.GREEN + " is now " + HerobrinePVPCore.translateString("&c&lUNBREAKABLE&r&a!"));
    startFX(1.5f);
    new BukkitRunnable() {
        @Override
        public void run() {
        if (arena.getState() == GameState.LIVE && arena.getQuirkBattleGame().getAlivePlayers().contains(player.getUniqueId())) stopUnbreakable();
        }
    }.runTaskLater(QuirkBattlesPlugin.getInstance(), 200);
    }

    public void stopUnbreakable() {
    setCooldown(System.currentTimeMillis());
    doAbilityCooldown();
    hardening.setUnbreakable(false);
    hardening.setSharpClaw(false);
    arena.getQuirkBattleGame().getStats(player).setDefense(arena.getQuirkBattleGame().getStats(player).getDefense() - ability.getDefenseBoost());
    player.playSound(player.getLocation(), Sound.FIRE, 1f, 1f);
    if(arena.getType().equals(GameType.ONE_V_ONE)) arena.sendMessage(HerobrinePVPCore.getFileManager().getRank(player).getColor() + player.getName() + ChatColor.RED + " is no longer " + HerobrinePVPCore.translateString("&c&lUNBREAKABLE&r&c."));
    else arena.sendMessage(arena.getTeam(player).getColor() + player.getName() + ChatColor.RED + " is no longer " + HerobrinePVPCore.translateString("&c&lUNBREAKABLE&r&c."));
    }

    public void startFX(float radius) {
            new BukkitRunnable() {
                int addToY = 0;
                Location loc = player.getLocation();
                public void run() {
                    loc = player.getLocation();
                    if (!arena.getState().equals(GameState.LIVE)) {
                        cancel();
                        return;
                    }
                    if (!hardening.isUnbreakable()) {
                        cancel();
                        return;
                    }
                    if (!arena.getQuirkBattleGame().getAlivePlayers().contains(player.getUniqueId())) {
                        cancel();
                        return;
                    }
                    if (addToY > 3) addToY = 0;
                    for (UUID uuid : arena.getPlayers()) {
                        if (uuid == quirk.getUUID()) continue;
                        Player showFor = Bukkit.getPlayer(uuid);
                        for (double t = 0; t < 1000; t += 0.5) {
                            float x = radius * (float) Math.sin(t);
                            float z = radius * (float) Math.cos(t);

                            PacketPlayOutWorldParticles packet = new PacketPlayOutWorldParticles(EnumParticle.BLOCK_CRACK, true,
                                    (float) loc.getX() + x, (float) loc.getY() + addToY, (float) loc.getZ() + z, 0, 0, 0, 0, 1, 1);
                            ((CraftPlayer) showFor).getHandle().playerConnection.sendPacket(packet);
                        }
                    }


                    addToY++;
                }
            }.runTaskTimer(QuirkBattlesPlugin.getInstance(), 0L, 10L);
    }

    @Override
    public boolean doesCasePass(Player player) {
        return !hardening.isUnbreakable();
    }

    @Override
    public void doNoPass(Player player) {
    player.sendMessage(ChatColor.RED + "You are already unbreakable!");
    player.playSound(player.getLocation(), Sound.VILLAGER_NO, 1f, 1f);
    }
}
