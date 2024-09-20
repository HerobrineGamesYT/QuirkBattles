package net.herobrine.quirkbattle.game.quirks.abilities.hero.icyhot.ice;

import net.herobrine.gamecore.Class;
import net.herobrine.quirkbattle.QuirkBattlesPlugin;
import net.herobrine.quirkbattle.game.quirks.abilities.Abilities;
import net.herobrine.quirkbattle.game.quirks.abilities.Ability;
import net.minecraft.server.v1_8_R3.EnumParticle;
import net.minecraft.server.v1_8_R3.PacketPlayOutWorldParticles;
import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.util.HashMap;

public class IceWallAbility extends Ability {
    public IceWallAbility(Abilities ability, Class quirk, int id, int slot) {
        super(ability, quirk, id, slot);
    }
    Player player = Bukkit.getPlayer(quirk.getUUID());
    HashMap<Location, Material> blockLocations = new HashMap<>();
    @Override
    public void doAbility(Player player) {
        Location eyeLocation = player.getEyeLocation();
        Vector directionVector = eyeLocation.getDirection();
        Location frontLocation = eyeLocation.add(directionVector);

        // test.
        doVFX(frontLocation);
    }

    public void doVFX(Location centerLocation) {
        blockLocations.clear();
        Location loc = centerLocation.clone();
        centerLocation.getBlock().setType(Material.ICE);
        addLocationToMap(centerLocation);
        centerLocation.getWorld().playSound(centerLocation, Sound.GLASS, 1f,1f);
        Location locb = centerLocation.clone();
        new BukkitRunnable() {


            @Override
            public void run() {
                double r = 2.0;
                for (double phi = 0; phi <= Math.PI; phi += Math.PI / 15) {
                    double y = r * Math.cos(phi) + 1.5;
                    for (double theta = 0; theta <= 2 * Math.PI; theta += Math.PI / 30) {
                        double x = r * Math.cos(theta) * Math.sin(phi);
                        double z = r * Math.sin(theta) * Math.sin(phi);
                        locb.add(x, y, z);
                        locb.getBlock().setType(Material.ICE);
                        addLocationToMap(locb.clone());
                        locb.getWorld().playSound(locb, Sound.GLASS, 1f,1f);
                        locb.subtract(x, y, z);
                    }
                }
                revertIce(blockLocations);
            }
        }.runTask(QuirkBattlesPlugin.getInstance()); // Adjust the timing as desired
    }

    public void addLocationToMap(Location loc) {
        blockLocations.put(loc, Material.AIR);
    }

    public void revertIce(HashMap<Location, Material> blockLocations) {
        new BukkitRunnable() {
            @Override
            public void run() {
                for (Location loc : blockLocations.keySet()) {
                    if (blockLocations.get(loc) == null) {
                        loc.getBlock().setType(Material.AIR);
                        loc.getWorld().playSound(loc, Sound.GLASS, 1f, 1f);
                        spawnRGBParticles(loc, 10, 128, 128, true);
                    }
                    else {
                        loc.getBlock().setType(blockLocations.get(loc));
                        loc.getWorld().playSound(loc, Sound.GLASS, 1f, 1f);
                        spawnRGBParticles(loc, 10, 128, 128, true);
                    }

                }
            }
        }.runTaskLater(QuirkBattlesPlugin.getInstance(), 100L);
    }

}
