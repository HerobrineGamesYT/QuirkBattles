package net.herobrine.quirkbattle.game.quirks.abilities.villain.afo;

import net.herobrine.gamecore.GameState;
import net.herobrine.quirkbattle.QuirkBattlesPlugin;
import net.herobrine.quirkbattle.game.CustomDeathCause;
import net.herobrine.quirkbattle.game.quirks.abilities.Abilities;
import net.herobrine.quirkbattle.game.quirks.abilities.Ability;
import net.herobrine.quirkbattle.game.quirks.abilities.SpecialCase;
import net.herobrine.quirkbattle.game.quirks.villain.AllForOne;
import net.herobrine.quirkbattle.util.Quirk;
import org.bukkit.*;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.player.PlayerToggleSneakEvent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class TendrilAbility extends Ability implements SpecialCase {
    private Player attachedPlayer;
    private final Player player;
    private final Map<UUID, Boolean> hasHit = new HashMap<>();
    private boolean hasTendrilAttached = false;
    private final AllForOne afo;

    public TendrilAbility(Abilities ability, Quirk quirk, int id, int slot) {
        super(ability, quirk, id, slot);
        this.afo = (AllForOne) quirk;
        this.player = Bukkit.getPlayer(afo.getUniqueId());
    }

    @Override
    public void doAbility(Player player) {
        doVFX();
        player.getLocation().getWorld().playSound(player.getLocation(), Sound.WITHER_SHOOT, 1f, 1.2f);
    }

    public void doVFX() {
        new BukkitRunnable() {
            double t = 0;
            Location loc = player.getLocation();
            final Vector direction = loc.getDirection().normalize();

            public void run() {
                t = t + 1;
                double x = direction.getX() * t;
                double y = direction.getY() * t + 1.5;
                double z = direction.getZ() * t;
                loc.add(x, y, z);
                if (!isActive()) {
                    hasHit.clear();
                    this.cancel();
                    return;
                }
                if (loc.getBlock().getType() != Material.AIR) {
                    this.cancel();
                    if (!hasHit.isEmpty()) {
                        hasHit.clear();
                        return;
                    }
                    Vector direction = loc.toVector().subtract(player.getLocation().toVector()).normalize();
                    direction.multiply(2);
                    player.setVelocity(direction);
                    player.sendMessage(ChatColor.GREEN + "You've pulled yourself!");
                    player.playSound(player.getLocation(), Sound.DOOR_OPEN, 1f, 1.9f);

                } else {
                    doCollision(loc);

                    spawnRGBParticles(loc, 64, 5, 16, true);
                    spawnRGBParticles(loc, 15, 11, 12, true);
                    spawnRGBParticles(loc, 184, 17, 58, true);
                }

                loc.subtract(x, y, z);

                if (t > 20) {
                    hasHit.clear();
                    this.cancel();
                }
            }
        }.runTaskTimer(QuirkBattlesPlugin.getInstance(), 0,1L);
    }

    public void doCollision(Location loc) {
        for (Entity ent : loc.getWorld().getNearbyEntities(loc, .7,1,.7)) {
            if (!(ent instanceof Player)) continue;
            Player target = (Player) ent;
            if (arena.getSpectators().contains(target.getUniqueId())) continue;
            if (!arena.getQuirkBattleGame().getAlivePlayers().contains(target.getUniqueId())) continue;
            if (hasHit.containsKey(target.getUniqueId())) continue;
            if (target == player) continue;
            if (arena.getType().isTeamsMode()) {
                if (arena.getTeam(player).equals(arena.getTeam(target))) {
                    //TODO attach tendril to player and start continuous VFX logic.
                    if (attachedPlayer != null) {
                        player.sendMessage(ChatColor.RED + "You already have your Tendril attached to a teammate!");
                        player.sendMessage(ChatColor.RED + "Sneak to use Forcible Quirk Activation and remove the link!");
                        continue;
                    }

                    hasHit.put(target.getUniqueId(), true);
                    hasTendrilAttached = true;
                    attachedPlayer = target;
                    player.playSound(player.getLocation(), Sound.DOOR_CLOSE, 1f, 1.9f);
                    player.sendMessage(ChatColor.GREEN + "You have attached your tendril to " + arena.getTeam(target).getColor() + target.getName() + ChatColor.GREEN + "!");
                    player.sendMessage(ChatColor.GREEN + "Sneak to use Forcible Quirk Activation and remove the link!");
                    target.sendMessage(ChatColor.RED + player.getName() + ChatColor.GREEN + " has attached their Tendril to you!");
                    doContinuousVFX();
                    continue;
                }
            }

            hasHit.put(target.getUniqueId(), true);
            Vector direction = target.getLocation().toVector()
                    .subtract(player.getLocation().toVector()).normalize();
            direction.setX(direction.getX() * -1);
            direction.setZ(direction.getZ() * -1);
            direction.multiply(2.5);
            target.setVelocity(direction);
            PotionEffect banditSpeed = PotionEffectType.SLOW.createEffect(120,
                    5);
            target.addPotionEffect(banditSpeed);
            doDamageTo(player, target, 30.0, CustomDeathCause.TENDRIL);
            player.sendMessage(ChatColor.GREEN + "You captured " + target.getName() + " with your tendril!");
            player.playSound(player.getLocation(), Sound.DOOR_CLOSE, 1f, 1.9f);
            target.sendMessage(ChatColor.RED + "You have been captured by " + player.getName() + "!");
        }
    }

    @Override
    public boolean doesCasePass(Player player) {
        return true;
    }

    // Using the special cases in a new way is possible here - allowing you to add additional conditions to an ability (granting additional functions)
    // with no additional stamina cost for the player.
    @Override
    public void doNoPass(Player player) {
        // may just make it detach the tendril instead.
    player.sendMessage(ChatColor.RED + "Your Tendril is already attached to " + attachedPlayer.getName() + "!");
    }

    public void doContinuousVFX() {
        new BukkitRunnable() {
            @Override
            public void run() {
                if (attachedPlayer == null) {
                    cancel();
                    return;
                }
                if (arena.getState() != GameState.LIVE) {
                    cancel();
                    return;
                }

                Vector playerLocVector = player.getLocation().toVector();
                Vector attachedVector = attachedPlayer.getLocation().toVector();

                // Calculate vector between with "direction = b - a"
                Vector betweenPandTVector = attachedVector.clone().subtract(playerLocVector);
                Vector directionVector = betweenPandTVector.clone().normalize();
                Location loc = player.getLocation().clone();

                for (int i = 0; i < betweenPandTVector.length(); i++) {
                    Vector particlePoint = playerLocVector.clone().add(directionVector.clone().multiply(i));

                    loc.setX(particlePoint.getX());
                    loc.setY(particlePoint.getY() + 1);
                    loc.setZ(particlePoint.getZ());

                    spawnRGBParticles(loc, 64, 5, 16, true);
                    spawnRGBParticles(loc, 15, 11, 12, true);
                    spawnRGBParticles(loc, 184, 17, 58, true);
                }
            }


        }.runTaskTimerAsynchronously(QuirkBattlesPlugin.getInstance(), 0L, 5L);

    }

    @EventHandler
    public void onPlayerSneak(PlayerToggleSneakEvent event) {
        if (event.getPlayer().getUniqueId() != quirk.getUniqueId()) return;
        if (!hasTendrilAttached) return;

        player.sendMessage(ChatColor.GREEN + "You have activated forcible quirk activation on " + attachedPlayer.getName() + "!");
        attachedPlayer.sendMessage(ChatColor.RED + player.getName() + ChatColor.GREEN + " used forcible quirk activation on you!");
        player.playSound(player.getLocation(), Sound.FIZZ, 1f, 1f);
        hasTendrilAttached = false;
        attachedPlayer = null;
        //TODO: Implement forcible quirk activation via Forcible interface that will run a method with a different special effect for each villain quirk.

    }
}
