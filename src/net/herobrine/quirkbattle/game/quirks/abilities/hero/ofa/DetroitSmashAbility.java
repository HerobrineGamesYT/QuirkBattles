package net.herobrine.quirkbattle.game.quirks.abilities.hero.ofa;

import net.herobrine.core.HerobrinePVPCore;
import net.herobrine.gamecore.GameState;
import net.herobrine.gamecore.Manager;
import net.herobrine.quirkbattle.QuirkBattlesPlugin;
import net.herobrine.quirkbattle.game.CustomDeathCause;
import net.herobrine.quirkbattle.game.quirks.abilities.Abilities;
import net.herobrine.quirkbattle.game.quirks.abilities.Ability;
import net.herobrine.quirkbattle.game.quirks.hero.OneForAll;
import net.herobrine.quirkbattle.util.Quirk;
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
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class DetroitSmashAbility extends Ability {

    private final OneForAll ofa;

    public DetroitSmashAbility(Abilities ability, Quirk quirk, int id, int slot) {
        super(ability, quirk, id, slot);
        this.ofa = (OneForAll) quirk;
    }

    @Override
    public void doAbility(Player player) {
        // Check if we should do quintuple smash
        boolean isQuintuple = shouldDoQuintuple();

        if (isQuintuple) {
            // Consume Fa-Jin energy for quintuple

            ofa.getFaJinSystem().consumeEnergy(100);
            arena.sendMessage(ChatColor.GOLD + player.getName() + " is doing a " + ChatColor.LIGHT_PURPLE + "" + ChatColor.BOLD + "DETROIT SMASH: QUINTUPLE!");

            doQuintupleSmash(player);
        } else {
            // Normal Detroit Smash
            doNormalSmash(player);
        }
    }

    private boolean shouldDoQuintuple() {
        // Check if Enhanced OFA with Gearshift active and 100+ Fa-Jin
        return ofa.isAwakened() &&
                ofa.isGearshiftActive() &&
                ofa.getFaJinSystem() != null &&
                ofa.getFaJinSystem().hasEnergy(100);
    }

    private void doNormalSmash(Player player) {
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
                doNormalSmashCollisionChecks(player, stats.getMana());
                ofa.resetPower();
            }
        }.runTaskLater(QuirkBattlesPlugin.getInstance(), 10L);
    }

    private void doQuintupleSmash(Player player) {
        Vector vecUp = new Vector(0, 2.5, 0); // Higher jump
        Vector vecDown = player.getLocation().getDirection().setY(-3);

        vecDown.normalize();
        vecDown.multiply(2);

        player.setVelocity(vecUp);
        player.playSound(player.getLocation(), Sound.GHAST_FIREBALL, 1.5f, 1.2f);
        player.playSound(player.getLocation(), Sound.ENDERDRAGON_WINGS, 1.0f, 1.5f);

        // Visual effect for enhanced power
        for (int i = 0; i < 10; i++) {
            Location particleLoc = player.getLocation().add(
                    (Math.random() - 0.5) * 2,
                    Math.random() * 2,
                    (Math.random() - 0.5) * 2
            );
            spawnRGBParticles(particleLoc, 255, 215, 0, true); // Gold particles
        }

        new BukkitRunnable() {
            @Override
            public void run() {
                player.playSound(player.getLocation(), Sound.GHAST_FIREBALL, 1.5f, .5f);
                player.playSound(player.getLocation(), Sound.ENDERDRAGON_GROWL, 0.5f, 1.5f);
                player.setVelocity(vecDown);
                doQuintupleSmashCollisionChecks(player, stats.getMana());
                ofa.resetPower();
            }
        }.runTaskLater(QuirkBattlesPlugin.getInstance(), 15L);
    }

    private void doNormalSmashCollisionChecks(Player player, int power) {
        new BukkitRunnable() {
            ArrayList<UUID> hasHit = new ArrayList<>();

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

                if (player.isOnGround()) {
                    cancel();
                    Location loc = player.getLocation();
                    loc.getWorld().playSound(loc, Sound.ZOMBIE_WOODBREAK, 1f, .75f);
                    circleEffect(loc, (float) ability.getRadius());

                    for (Entity en : player.getNearbyEntities(ability.getRadius(), 1, ability.getRadius())) {
                        if (en.getType().equals(EntityType.PLAYER)) {
                            Player pl1 = (Player) en;
                            if (!arena.getType().isTeamsMode()) {
                                if (pl1 != player && !hasHit.contains(pl1.getUniqueId()) && arena.getQuirkBattleGame().getAlivePlayers().contains(pl1.getUniqueId())) {
                                    hasHit.add(pl1.getUniqueId());
                                    doDamageTo(player, pl1, ability.getDamage(), power, CustomDeathCause.DETRIOT_SMASH);
                                    pl1.sendMessage(HerobrinePVPCore.translateString("&6" + player.getName() + "&a just hit you with their &lDetroit Smash &r&aattack at &6" + power + "% &aPower!"));
                                    player.sendMessage(HerobrinePVPCore.translateString("&aYou just hit &6" + pl1.getName() + "&a with your &lDetroit Smash &r&aattack!"));
                                    if (ofa.getFaJinSystem() != null) {
                                        if (!ofa.isGearshiftActive()) ofa.getFaJinSystem().addEnergy(5);
                                    }
                                }
                            } else if (pl1 != player && arena.getTeam(pl1) != arena.getTeam(player) && !hasHit.contains(pl1.getUniqueId()) && arena.getQuirkBattleGame().getAlivePlayers().contains(pl1.getUniqueId())) {
                                hasHit.add(pl1.getUniqueId());
                                doDamageTo(player, pl1, ability.getDamage(), power, CustomDeathCause.DETRIOT_SMASH);
                                pl1.sendMessage(HerobrinePVPCore.translateString(arena.getTeam(player).getColor() + player.getName() + "&a just hit you with their &lDetroit Smash &r&aattack at &6" + power + "% &aPower!"));
                                player.sendMessage(HerobrinePVPCore.translateString("&aYou just hit " + arena.getTeam(pl1).getColor() + pl1.getName() + "&a with your &lDetroit Smash &r&aattack!"));
                                if (ofa.getFaJinSystem() != null) {
                                    if (!ofa.isGearshiftActive()) ofa.getFaJinSystem().addEnergy(5);
                                }
                            }
                        }
                    }
                }
            }
        }.runTaskTimerAsynchronously(QuirkBattlesPlugin.getInstance(), 0, 1);
    }

    private void doQuintupleSmashCollisionChecks(Player player, int power) {
        new BukkitRunnable() {
            boolean hasLanded = false;

            @Override
            public void run() {
                if (!Manager.isPlaying(player) || !Manager.getArena(player).getState().equals(GameState.LIVE)) {
                    cancel();
                    return;
                }

                if (!isActive()) {
                    cancel();
                    return;
                }

                if (player.isOnGround() && !hasLanded) {
                    hasLanded = true;
                    performQuintupleSmashes(player, power);
                    cancel();
                }
            }
        }.runTaskTimer(QuirkBattlesPlugin.getInstance(), 0, 1);
    }

    private void performQuintupleSmashes(Player player, int power) {
        Location smashLocation = player.getLocation();

        new BukkitRunnable() {
            int smashNumber = 0;
            final Set<UUID> totalHit = new HashSet<>();

            @Override
            public void run() {
                if (smashNumber >= 5 || !isActive()) {
                    cancel();
                    return;
                }

                smashNumber++;
                totalHit.clear();

                // Each smash gets progressively stronger
                double radius = ability.getRadius() + (smashNumber * 0.5);
                int damage = (int) (ability.getDamage() + (smashNumber * 5));

                // Visual and audio effects
                smashLocation.getWorld().playSound(smashLocation, Sound.ZOMBIE_WOODBREAK, 1.5f, 0.5f + (smashNumber * 0.1f));
                smashLocation.getWorld().playSound(smashLocation, Sound.EXPLODE, 1.0f, 0.8f);

                // Create expanding circle effect
                circleEffect(smashLocation, (float) radius);

                // Particle burst - golden for enhanced
                for (int i = 0; i < 20 + (smashNumber * 5); i++) {
                    double angle = Math.random() * Math.PI * 2;
                    double r = Math.random() * radius;
                    double x = Math.cos(angle) * r;
                    double z = Math.sin(angle) * r;
                    Location particleLoc = smashLocation.clone().add(x, 0.5, z);
                    spawnRGBParticles(particleLoc, 255, 215, 0, true); // Gold
                    if (i % 3 == 0) {
                        spawnRGBParticles(particleLoc, 255, 255, 255, true); // White sparkles
                    }
                }

                // Damage calculation
                for (Entity en : smashLocation.getWorld().getNearbyEntities(smashLocation, radius, 2, radius)) {
                    if (!(en instanceof Player)) continue;
                    Player target = (Player) en;

                    if (target == player) continue;
                    if (!arena.getQuirkBattleGame().getAlivePlayers().contains(target.getUniqueId())) continue;

                    // Check teams
                    if (arena.getType().isTeamsMode()) {
                        if (arena.getTeam(player).equals(arena.getTeam(target))) continue;
                    }

                    // Only hit each player once per smash in the sequence
                    if (!totalHit.contains(target.getUniqueId())) {
                        totalHit.add(target.getUniqueId());
                        // Knockback increases with each smash
                        Vector knockback = target.getLocation().toVector()
                                .subtract(smashLocation.toVector()).normalize();
                        knockback.multiply(0.5 + (smashNumber * 0.3));
                        knockback.setY(0.3 + (smashNumber * 0.1));
                        target.setVelocity(knockback);

                        doDamageTo(player, target, damage, power, CustomDeathCause.DETRIOT_SMASH);

                        String smashName = ChatColor.GOLD + "" + ChatColor.BOLD + "DETROIT SMASH QUINTUPLE (x" + smashNumber + ")";
                        target.sendMessage(HerobrinePVPCore.translateString(
                                "&6" + player.getName() + "&a hit you with " + smashName + " &r&a(Hit #" + smashNumber + ")!"));

                        if (smashNumber == 1) {
                            player.sendMessage(HerobrinePVPCore.translateString(
                                    "&aYou hit &6" + target.getName() + "&a with " + smashName + "!"));
                        }
                    }
                }

                // Add Fa-Jin energy for each smash that hits
                if (ofa.getFaJinSystem() != null && !totalHit.isEmpty()) {
                    ofa.getFaJinSystem().addEnergy(10 * totalHit.size());
                }
            }
        }.runTaskTimer(QuirkBattlesPlugin.getInstance(), 0L, 8L); // 8 ticks between each smash
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
}