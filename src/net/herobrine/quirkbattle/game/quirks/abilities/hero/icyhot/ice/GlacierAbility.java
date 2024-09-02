package net.herobrine.quirkbattle.game.quirks.abilities.hero.icyhot.ice;

import net.herobrine.gamecore.Class;
import net.herobrine.quirkbattle.game.quirks.abilities.Abilities;
import net.herobrine.quirkbattle.game.quirks.abilities.Ability;
import net.herobrine.quirkbattle.game.quirks.hero.IcyHot;
import org.bukkit.entity.Player;

public class GlacierAbility extends Ability {
    public GlacierAbility(Abilities ability, Class quirk, int id, int slot) {
        super(ability, quirk, id, slot);
    }
    IcyHot icy = (IcyHot) quirk;

    @Override
    public void doAbility(Player player) {

    }

    public void doVFX() {

    }


}
