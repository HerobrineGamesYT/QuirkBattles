package net.herobrine.quirkbattle.game.quirks.abilities.hero.icyhot.ice;

import net.herobrine.gamecore.Class;
import net.herobrine.gamecore.GameState;
import net.herobrine.quirkbattle.QuirkBattlesPlugin;
import net.herobrine.quirkbattle.game.CustomDeathCause;
import net.herobrine.quirkbattle.game.quirks.abilities.Abilities;
import net.herobrine.quirkbattle.game.quirks.abilities.Ability;
import net.herobrine.quirkbattle.util.Quirk;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.EulerAngle;
import org.bukkit.util.Vector;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class IceShardAbility extends Ability {

    private final Player player = Bukkit.getPlayer(quirk.getUniqueId());
    private final Map<UUID, Boolean> hasHit = new HashMap<>();

    public IceShardAbility(Abilities ability, Quirk quirk, int id, int slot) {
        super(ability, quirk, id, slot);
    }


    @Override
    public void doAbility(Player player) {
        hasHit.clear();
        Location eyeLocation = player.getEyeLocation();
        Vector directionVector = eyeLocation.getDirection();
        Location frontLocation = eyeLocation.add(directionVector);

        doVFX(frontLocation);

    }

    public void doVFX(Location location) {
        new BukkitRunnable() {
            int projectiles = 0;
            @Override
            public void run() {
                if (projectiles > 2) {
                    cancel();
                    return;
                }
                location.getWorld().playSound(location, Sound.GLASS, 1f, 1f);
                ArmorStand stand = (ArmorStand) location.getWorld().spawnEntity(location, EntityType.ARMOR_STAND);
                stand.setCustomNameVisible(false);
                stand.setCustomName(uuid.toString());
                stand.setBasePlate(false);
                stand.setGravity(true);
                stand.setSmall(true);
                stand.setMarker(true);
                stand.setVisible(false);
                stand.setHeadPose(new EulerAngle(Math.random(), Math.random(), Math.random()));
                stand.setHelmet(new ItemStack(Material.PACKED_ICE));

                startProjectileMovement(stand);
                projectiles++;
            }
        }.runTaskTimer(QuirkBattlesPlugin.getInstance(), 0L, 5L);
        
    }

    public void startProjectileMovement(ArmorStand stand) {
        new BukkitRunnable() {
            int ticks = 0;

        @Override
        public void run() {
            if (ticks > 25 || arena.getState() != GameState.LIVE || !arena.getQuirkBattleGame().getAlivePlayers().contains(player.getUniqueId())) {
                cancel();
                removeStand(stand);
                return;
            }
            Vector vec = stand.getLocation().getDirection();
            vec.normalize();
            vec.multiply(1.2);


            stand.setVelocity(vec);
            stand.setHeadPose(new EulerAngle(Math.random(), Math.random(), Math.random()));
            doCollision(stand);

            ticks++;
        }
        }.runTaskTimer(QuirkBattlesPlugin.getInstance(), 0L, 1L);
    }

    public void removeStand(ArmorStand stand) {
        stand.getLocation().getWorld().playSound(stand.getLocation(), Sound.GLASS, 1f, .7f);
        spawnRGBParticles(stand.getLocation(), 10, 128, 128, true);
        stand.remove();
    }

    public void doCollision(ArmorStand stand) {
        for (Entity entity : stand.getNearbyEntities(1f,1f,1f)) {
            if (!(entity instanceof Player)) continue;
            Player target = (Player) entity;
            if (arena.getState() != GameState.LIVE) return;
            if (arena.getSpectators().contains(target.getUniqueId())) continue;
            if (!arena.getQuirkBattleGame().getAlivePlayers().contains(target.getUniqueId())) continue;
            if (arena.getType().isTeamsMode()) {
                if (arena.getTeam(player).equals(arena.getTeam(target))) continue;
            }
            if (target == player) continue;
            if (hasHit.containsKey(target.getUniqueId())) continue;

            hasHit.put(target.getUniqueId(), true);
            target.addPotionEffect(new PotionEffect(PotionEffectType.SLOW, 40, 3), true);
            target.sendMessage(ChatColor.GREEN + "You have been stunned by " + player.getName() + "'s" + ability.getDisplay() + "!");
            player.sendMessage(ChatColor.GREEN + "You have stunned " + target.getName() + " with your " + ability.getDisplay() + "!");
            EntityDamageEvent dmg = new EntityDamageEvent(target, EntityDamageEvent.DamageCause.CUSTOM, ability.getDamage());
            arena.getQuirkBattleGame().getCustomDeathCause().put(target.getUniqueId(), CustomDeathCause.ICE_SHARD);
            arena.getQuirkBattleGame().getLastAbilityAttacker().put(target.getUniqueId(), player.getUniqueId());
            target.damage(0);
            target.setLastDamageCause(dmg);
            Bukkit.getPluginManager().callEvent(dmg);
        }
    }
}
