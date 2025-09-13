package net.herobrine.quirkbattle.game.quirks.abilities.villain.warpgate;

import net.herobrine.quirkbattle.game.quirks.abilities.Abilities;
import net.herobrine.quirkbattle.game.quirks.abilities.Ability;
import net.herobrine.quirkbattle.game.quirks.abilities.SpecialCase;
import net.herobrine.quirkbattle.util.Quirk;
import net.minecraft.server.v1_8_R3.EnumParticle;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

public class WarpStepAbility extends Ability implements SpecialCase {
    public WarpStepAbility(Abilities ability, Quirk quirk, int id, int slot) {
        super(ability, quirk, id, slot);
    }

    @Override
    public void doAbility(Player player) {
        boolean solidFound = false;
        Location loc = player.getLocation();
        Vector dir;
        // will check for the next 8 blocks forward if the player will face a solid block and if it does it will set the teleport location
        // to the one before it
        for (int i = 1; i<9; i++) {
            loc = player.getLocation();
            dir = loc.getDirection();
            dir.multiply(i);
            loc.add(dir);
            loc.add(0, .5, 0);

            if (loc.getBlock().getType().isSolid()) {
                loc = player.getLocation();
                dir = loc.getDirection();
                dir.multiply(i - 1);
                loc.add(dir);
                solidFound = true;
                doNoPass(player);
                break;
            }
        }

        if (!solidFound) {
            loc = player.getLocation();
            dir = loc.getDirection();
            dir.multiply(8); // 8 blocks away
            loc.add(dir);
        }

        if (!loc.getBlock().getRelative(BlockFace.UP).getType().equals(Material.AIR)) loc.add(0, 0.5, 0);
        if (loc.subtract(0, 0.6, 0).getBlock().getType().isSolid()) loc.add(0, 1.2, 0);
        spawnParticle(player.getLocation(), EnumParticle.PORTAL, true);
        player.playSound(player.getLocation(), Sound.ENDERMAN_TELEPORT, 1f, 1f);
        player.teleport(loc);
        spawnParticle(player.getLocation(), EnumParticle.PORTAL, true);
        player.sendMessage(ChatColor.GREEN + "Teleported!");
    }

    @Override
    public boolean doesCasePass(Player player) {
        Location loc;
        Vector dir;
        for (int i = 1; i<3; i++) {
            loc = player.getLocation();
            dir = loc.getDirection();
            dir.multiply(i);
            loc.add(dir);
            loc.add(0,.5,0);

            if (loc.getBlock().getType().isSolid()) {
                return false;
            }
        }
        boolean solidFound = false;
        Location loc2 = player.getLocation();
        Vector dir2;

        for (int i = 1; i<9; i++) {
            loc2 = player.getLocation();
            dir2 = loc2.getDirection();
            dir2.multiply(i);
            loc2.add(dir2);
            loc2.add(0, .5, 0);

            if (loc2.getBlock().getType().isSolid()) {
                loc2 = player.getLocation();
                dir2 = loc2.getDirection();
                dir2.multiply(i - 1);
                loc2.add(dir2);
                solidFound = true;
                doNoPass(player);
                break;
            }
        }

        if (!solidFound) {
            loc2 = player.getLocation();
            dir2 = loc2.getDirection();
            dir2.multiply(8); // 8 blocks
            loc2.add(dir2);
        }

        if (!loc2.getBlock().getRelative(BlockFace.UP).getType().equals(Material.AIR)) loc2.add(0, 0.5, 0);
        if (loc2.subtract(0, 0.6, 0).getBlock().getType().isSolid()) loc2.add(0, 1.2, 0);
        String str = "" + loc2.getX();
        if (str.equalsIgnoreCase("NaN")) return false;
        if (loc2.getYaw() == 0.0) return false;


        return true;
    }

    @Override
    public void doNoPass(Player player) {
        player.sendMessage(ChatColor.RED + "There are blocks in the way!");
    }
}
