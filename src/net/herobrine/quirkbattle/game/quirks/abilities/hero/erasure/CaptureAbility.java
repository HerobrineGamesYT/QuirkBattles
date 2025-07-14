package net.herobrine.quirkbattle.game.quirks.abilities.hero.erasure;

import net.herobrine.gamecore.Arena;
import net.herobrine.gamecore.Class;
import net.herobrine.gamecore.GameType;
import net.herobrine.gamecore.Games;
import net.herobrine.gamecore.Manager;
import net.herobrine.quirkbattle.QuirkBattlesPlugin;
import net.herobrine.quirkbattle.game.quirks.abilities.Abilities;
import net.herobrine.quirkbattle.game.quirks.abilities.Ability;
import net.herobrine.quirkbattle.util.Quirk;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Effect;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.entity.Skeleton;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class CaptureAbility extends Ability {

    private Player player;
    private final Map<UUID, Boolean> hasHit = new HashMap<>();
    public CaptureAbility(Abilities ability, Quirk quirk, int id, int slot) {
        super(ability, quirk, id, slot);
        this.player = Bukkit.getPlayer(quirk.getUniqueId());
    }


    @Override
    public void doAbility(Player player) {
        doFX();
    }


    public void doCollision(Location loc) {
    for (Entity ent : loc.getWorld().getNearbyEntities(loc, .7,1,.7)) {
        if (!(ent instanceof Player)) continue;
        Player target = (Player) ent;
        if (arena.getSpectators().contains(target.getUniqueId())) continue;
        if (!arena.getQuirkBattleGame().getAlivePlayers().contains(target.getUniqueId())) continue;
        if (arena.getType().isTeamsMode()) {
            if (arena.getTeam(player).equals(arena.getTeam(target))) continue;
        }
        if (target == player) continue;

        if (hasHit.containsKey(target.getUniqueId())) continue;

        hasHit.put(target.getUniqueId(), true);
        Vector direction = target.getLocation().toVector()
                .subtract(player.getLocation().toVector()).normalize();
        direction.setX(direction.getX() * -1);
        direction.setZ(direction.getZ() * -1);
        direction.multiply(2);
        target.setVelocity(direction);
        PotionEffect banditSpeed = PotionEffectType.SLOW.createEffect(120,
                4);
        target.addPotionEffect(banditSpeed);
        player.playSound(player.getLocation(), Sound.DOOR_CLOSE, 1f, 1.9f);
        player.sendMessage(ChatColor.AQUA + "You captured " + target.getName() + "!");
        target.sendMessage(ChatColor.RED + "You have been captured by " + player.getName() + "!");
    }
    }

    public void doFX() {
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
                    hasHit.clear();
                    this.cancel();
                    Vector direction = loc.toVector().subtract(player.getLocation().toVector()).normalize();
                    direction.multiply(2);
                    player.setVelocity(direction);
                    player.sendMessage(ChatColor.GREEN + "You've pulled yourself!");
                    player.playSound(player.getLocation(), Sound.DOOR_OPEN, 1f, 1.9f);

                } else {
                    doCollision(loc);

                    //Doing it twice to make the effect thicker.
                    spawnRGBParticles(loc, 0, 240, 232, true);
                    spawnRGBParticles(loc, 0, 240, 232, true);
                }

                loc.subtract(x, y, z);

                if (t > 15) {
                    hasHit.clear();
                    this.cancel();
                }
            }
        }.runTaskTimer(QuirkBattlesPlugin.getInstance(), 0,1L);
    }

}

