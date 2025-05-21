package net.herobrine.quirkbattle.game.quirks.abilities.hero.icyhot.ice;

import net.herobrine.gamecore.Class;
import net.herobrine.gamecore.GameState;
import net.herobrine.quirkbattle.QuirkBattlesPlugin;
import net.herobrine.quirkbattle.game.CustomDeathCause;
import net.herobrine.quirkbattle.game.quirks.abilities.Abilities;
import net.herobrine.quirkbattle.game.quirks.abilities.Ability;
import net.herobrine.quirkbattle.util.Quirk;
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class IceWallAbility extends Ability {
    private final Player player = Bukkit.getPlayer(quirk.getUniqueId());
    private final List<ArmorStand> standLocations = new ArrayList<>();
    private final Map<UUID, Boolean> hasHit = new HashMap<>();

    public IceWallAbility(Abilities ability, Quirk quirk, int id, int slot) {
        super(ability, quirk, id, slot);
    }

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
        centerLocation.getWorld().playSound(centerLocation, Sound.GLASS, 1f, 1f);
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
                    revertIce(standLocations);
                    return;
                }

                spawnParticle(start, EnumParticle.SNOW_SHOVEL, true);
                spawnRGBParticles(start, 184,253,255, true);
                spawnRGBParticles(start, 220,254,255,true);
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
                    if (arena.getTeam(player).equals(arena.getTeam(target))) continue;
                }
                if (target == player) continue;
                if (hasHit.containsKey(target.getUniqueId())) continue;

                hasHit.put(target.getUniqueId(), true);
                target.addPotionEffect(new PotionEffect(PotionEffectType.SLOW, 60, 3), true);
                target.sendMessage(ChatColor.GREEN + "You have been stunned by " + player.getName() + "'s" + ability.getDisplay() + "!");
                player.sendMessage(ChatColor.GREEN + "You have stunned " + target.getName() + " with your " + ability.getDisplay() + "!");
                EntityDamageEvent dmg = new EntityDamageEvent(target, EntityDamageEvent.DamageCause.CUSTOM, ability.getDamage());
                arena.getQuirkBattleGame().getCustomDeathCause().put(target.getUniqueId(), CustomDeathCause.ICE_WALL);
                arena.getQuirkBattleGame().getLastAbilityAttacker().put(target.getUniqueId(), player.getUniqueId());
                target.damage(0);
                target.setLastDamageCause(dmg);
                Bukkit.getPluginManager().callEvent(dmg);
            }
        }
    }


    // The purpose of this runnable is to test collision for the Ice Wall after it has stopped spawning Ice.
    public void collisionRunnable() {

        new BukkitRunnable() {
            int ticks = 0;
            @Override
            public void run() {
                if (ticks >= 60) {
                    cancel();
                    return;

                }

                spawnRGBParticlesForStands(184,253,255, true);
                spawnRGBParticlesForStands( 220,254,255,true);

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
        ArmorStand ice = player.getWorld().spawn(loc.clone().add(new Vector(0,1,0)), ArmorStand.class);

        ice.setCustomNameVisible(false);
        ice.setCustomName(uuid.toString());
        ice.setBasePlate(false);
        ice.setGravity(true);
        ice.setSmall(true);
        ice.setHelmet(new ItemStack(Material.ICE, 1));
        ice.setHeadPose(new EulerAngle(Math.random(), Math.random(), Math.random()));
        ice.setVisible(false);

        addStandToList(ice);

        loc.getWorld().playSound(loc, Sound.GLASS, 1f, 1f);

    }

    public void addStandToList(ArmorStand stand) {
        standLocations.add(stand);
    }


    public void revertIce(List<ArmorStand> stands) {
        new BukkitRunnable() {
            @Override
            public void run() {
                for (ArmorStand stand : stands) {
                        stand.getWorld().playSound(stand.getLocation(), Sound.GLASS, 1f, 1f);
                        spawnRGBParticles(stand.getLocation(), 10, 128, 128, true);
                        stand.remove();
                }
            }
        }.runTaskLater(QuirkBattlesPlugin.getInstance(), 60L);
    }

}
