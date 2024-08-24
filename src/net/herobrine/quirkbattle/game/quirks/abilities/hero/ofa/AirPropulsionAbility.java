package net.herobrine.quirkbattle.game.quirks.abilities.hero.ofa;

import net.herobrine.gamecore.Class;
import net.herobrine.quirkbattle.game.quirks.abilities.Abilities;
import net.herobrine.quirkbattle.game.quirks.abilities.Ability;
import net.herobrine.quirkbattle.game.quirks.hero.OneForAll;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

public class AirPropulsionAbility extends Ability {
    public AirPropulsionAbility(Abilities ability, Class quirk, int id, int slot) {
        super(ability, quirk, id, slot);
    }

    @Override
    public void doAbility(Player player) {
        Vector dir = player.getLocation().getDirection();
        dir.normalize();
        dir.multiply(2 + (2*stats.getMana() / 10));
        player.setVelocity(dir);
        player.playSound(player.getLocation(), Sound.GHAST_FIREBALL, 1f, 1.2f);
        OneForAll ofa = (OneForAll) getQuirk();
        ofa.resetPower();
    }
}
