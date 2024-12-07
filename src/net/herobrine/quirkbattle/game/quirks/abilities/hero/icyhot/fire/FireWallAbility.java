package net.herobrine.quirkbattle.game.quirks.abilities.hero.icyhot.fire;

import net.herobrine.core.SkullMaker;
import net.herobrine.gamecore.Class;
import net.herobrine.gamecore.GameState;
import net.herobrine.quirkbattle.QuirkBattlesPlugin;
import net.herobrine.quirkbattle.game.CustomDeathCause;
import net.herobrine.quirkbattle.game.quirks.abilities.Abilities;
import net.herobrine.quirkbattle.game.quirks.abilities.Ability;
import net.minecraft.server.v1_8_R3.EnumParticle;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.EulerAngle;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Predicate;

public class FireWallAbility extends Ability {
    private final Player player = Bukkit.getPlayer(quirk.getUUID());
    private final List<ArmorStand> standLocations = new ArrayList<>();
    private final Map<UUID, Boolean> hasHit = new HashMap<>();



    public FireWallAbility(Abilities ability, Class quirk, int id, int slot) { super(ability, quirk, id, slot); }

    @Override
    public void doAbility(Player player) {
        Location eyeLocation = player.getEyeLocation();
        Vector directionVector = eyeLocation.getDirection();
        Location frontLocation = eyeLocation.add(directionVector);

        // test.
        doVFX(frontLocation);
    }

    public void doVFX(Location centerLocation) {
        standLocations.clear();
        hasHit.clear();
        Location loc = centerLocation.clone();
        spawnStand(centerLocation);
        centerLocation.getWorld().playSound(centerLocation, Sound.FIRE_IGNITE, 1f, 1f);

        new BukkitRunnable(){
            Location origin = loc;
            Location endpoint = loc.add(loc.getDirection().normalize());
            Vector direction = endpoint.toVector().subtract(origin.toVector());
            Location start = origin.clone();
            int i = 0;

            @Override
            public void run() {

                if(i > 25){
                    this.cancel();
                    collisionRunnable();
                    revertFire(standLocations);
                    return;
                }

                spawnParticle(start, EnumParticle.FLAME, true);
                spawnRGBParticles(start, 179, 67, 27, true);
                spawnRGBParticles(start, 179, 53, 41,true);

                // damage(p,start,playerdata.get(p.getUniqueId()).getQUIRK().getQUIRKCASTMANAGER().getABILITY1_DAMAGE());
                origin = start.clone();
                origin.setY(0);
                endpoint = origin.clone().add(loc.getDirection().normalize());
                endpoint.setY(0);
                direction = endpoint.toVector().subtract(origin.toVector()).normalize();
                direction.setY(0);
                start = start.add(direction.divide(new Vector(2,2,2)));


                // spawn stand and do collision on every other tick!! concurrentmodificationexception prevention, big brain
                if(i%2==0) spawnStand(start);
                else doCollision();

                i++;
            }
        }.runTaskTimer(QuirkBattlesPlugin.getInstance(),0,1L);

    }

    public void doCollision() {
        for (ArmorStand stand : standLocations) {
            for (Entity entity : stand.getNearbyEntities(1f,1f,1f)) {
                if (!(entity instanceof Player)) continue;
                Player target = (Player) entity;
                if (arena.getState() != GameState.LIVE) return;
                if (arena.getSpectators().contains(target.getUniqueId())) continue;
                if (!arena.getQuirkBattleGame().getAlivePlayers().contains(target.getUniqueId())) continue;
                if (arena.getType().isTeamsMode()) {
                    if (arena.getTeam(player).equals(arena.getTeam(target))) {
                        doTeamBuff(target);
                        continue;
                    }
                }
                if (target == player) {
                    doTeamBuff(target);
                    continue;
                }
                if (hasHit.containsKey(target.getUniqueId())) continue;

                hasHit.put(target.getUniqueId(), true);
                target.sendMessage(ChatColor.GREEN + "You have been burned by " + player.getName() + "'s" + ability.getDisplay() + "!");
                player.sendMessage(ChatColor.GREEN + "You have hit " + target.getName() + " with your " + ability.getDisplay() + "!");
                EntityDamageEvent dmg = new EntityDamageEvent(target, EntityDamageEvent.DamageCause.CUSTOM, ability.getDamage());
                arena.getQuirkBattleGame().getCustomDeathCause().put(target.getUniqueId(), CustomDeathCause.FLAME_WALL);
                arena.getQuirkBattleGame().getLastAbilityAttacker().put(target.getUniqueId(), player.getUniqueId());
                target.damage(0);
                target.setLastDamageCause(dmg);
                Bukkit.getPluginManager().callEvent(dmg);

                doBurningDamage(target);
            }
        }
    }

    public void doBurningDamage(Player target) {
        new BukkitRunnable() {
            int i = 0;
            @Override
            public void run() {
                if (arena.getState().equals(GameState.LIVE) || !arena.getQuirkBattleGame().getAlivePlayers().contains(target.getUniqueId())) {
                    cancel();
                    return;
                }

                if (i > 2) {
                    cancel();
                    return;
                }
                EntityDamageEvent dmg = new EntityDamageEvent(target, EntityDamageEvent.DamageCause.CUSTOM, 6);
                arena.getQuirkBattleGame().getCustomDeathCause().put(target.getUniqueId(), CustomDeathCause.FLAME_WALL);
                arena.getQuirkBattleGame().getLastAbilityAttacker().put(target.getUniqueId(), player.getUniqueId());
                target.playSound(target.getLocation(), Sound.FIZZ, 1f, 1f);
                target.damage(0);
                target.setLastDamageCause(dmg);
                Bukkit.getPluginManager().callEvent(dmg);
                spawnRGBParticles(target.getPlayer().getEyeLocation().add(0, 1.5, 0),  179, 67, 27, false);
                i++;
            }
        }.runTaskTimer(QuirkBattlesPlugin.getInstance(), 0L, 10L);
    }


    public void doTeamBuff(Player target) {
        Predicate<PotionEffect> hasSlowness = potionEffect -> potionEffect.getType().equals(PotionEffectType.SLOW);
        if (target.getActivePotionEffects().stream().anyMatch(hasSlowness)) {
            target.removePotionEffect(PotionEffectType.SLOW);
            if(target != player) target.sendMessage(ChatColor.GREEN + "You have been unfrozen by " + player.getName() + "'s " + ability.getDisplay() + "!");
            else target.sendMessage(ChatColor.GREEN + "You unfroze yourself!");
        }
    }


    // The purpose of this runnable is to test collision for the Ice Wall after it has stopped spawning Ice.
    public void collisionRunnable() {

        new BukkitRunnable() {
            int ticks = 0;
            @Override
            public void run() {
                if (ticks >= 40) {
                    cancel();
                    spawnRGBParticlesForStands(36, 5, 0, true);
                    return;

                }
                if (ticks <= 10) spawnRGBParticlesForStands(227, 77, 54, true);
                if (ticks <= 20) spawnRGBParticlesForStands(148, 35, 18, true);
                if(ticks<= 30) spawnRGBParticlesForStands(82, 17, 8, true);
                if(ticks % 2 == 0) doCollision();
                ticks++;
            }
        }.runTaskTimer(QuirkBattlesPlugin.getInstance(), 0L, 1L);
    }

    public void spawnRGBParticlesForStands(int red, int green, int blue, boolean sendToSelf) {
        for (ArmorStand stand : standLocations) {
            spawnRGBParticles(stand.getLocation().clone().add(0,1,0), red, green, blue, sendToSelf);
        }
    }

    public void spawnStand(Location loc) {
        ArmorStand fire = loc.getWorld().spawn(loc.clone().add(new Vector(0,1,0)), ArmorStand.class);
        loc.getWorld().playSound(loc, Sound.FIRE_IGNITE, 1f, 1f);
        addStandToList(fire);
        SkullMaker skull = new SkullMaker("Fire", Collections.singletonList(" "), "http://textures.minecraft.net/texture/c5944af05c888ef3aca2441f4d837dc96abdff34eac0f5192688c8807d2e7701");
        fire.setCustomNameVisible(false);
        fire.setCustomName(uuid.toString());
        fire.setBasePlate(false);
        fire.setGravity(true);
        fire.setSmall(true);
        fire.setMarker(true);
        fire.setVisible(false);
        fire.setHeadPose(new EulerAngle(Math.random(), Math.random(), Math.random()));
        fire.setHelmet(skull.getSkull());
    }

    public void addStandToList(ArmorStand stand) {
        standLocations.add(stand);
    }

    public void revertFire(List<ArmorStand> stands) {

        new BukkitRunnable() {
            @Override
            public void run() {
                for (ArmorStand stand : stands) {
                    stand.getWorld().playSound(stand.getLocation(), Sound.FIZZ, .7f, 1f);
                    spawnParticle(stand.getLocation(), EnumParticle.SMOKE_LARGE,true);
                    stand.remove();
                }
            }
        }.runTaskLater(QuirkBattlesPlugin.getInstance(), 40L);
    }
}
