package net.herobrine.quirkbattle.game.quirks.abilities.villain.doubles;

import net.herobrine.quirkbattle.game.quirks.abilities.Abilities;
import net.herobrine.quirkbattle.game.quirks.abilities.Ability;
import net.herobrine.quirkbattle.game.quirks.abilities.SpecialCase;
import net.herobrine.quirkbattle.util.npc.CloneManager;
import net.herobrine.quirkbattle.util.Quirk;
import net.herobrine.quirkbattle.util.npc.CloneTypes;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

public class CloneArmyAbility extends Ability implements SpecialCase {

    private CloneManager cloneManager;

    public CloneArmyAbility(Abilities ability, Quirk quirk, int id, int slot) {
        super(ability, quirk, id, slot);
        this.cloneManager = arena.getQuirkBattleGame().getCloneManager();

    }

    @Override
    public void doAbility(Player player) {

        int numClones = 3;
        int lifetime = 200;
        double spread = 1.5;
        double damage       = Math.max(1.0, ability.getDamage());
        double chaseRadius  = Math.max(4.0, ability.getRadius());

        cloneManager.spawnClones(player, numClones, spread, CloneTypes.TWICE_CLONE);

       // for (int i = 0 ; i < numClones; i++) {

      //      double angle = (2 * Math.PI / numClones) * i;
      //      double x = player.getLocation().getX() + Math.cos(angle) * spread;
      //      double z = player.getLocation().getZ() + Math.sin(angle) * spread;

         //   cloneManager.spawnClone(player, player.getLocation().clone().add(x - player.getLocation().getX(), 0,
           //         z - player.getLocation().getZ()), lifetime);

         //   player.sendMessage(ChatColor.GREEN + "Spawned a clone!");
       // }

    }

    @Override
    public boolean doesCasePass(Player player) {
        return true;
    }

    @Override
    public void doNoPass(Player player) {

    }
}
