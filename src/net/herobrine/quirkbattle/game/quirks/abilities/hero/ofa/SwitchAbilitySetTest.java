package net.herobrine.quirkbattle.game.quirks.abilities.hero.ofa;

import net.herobrine.gamecore.Class;
import net.herobrine.quirkbattle.game.quirks.abilities.Abilities;
import net.herobrine.quirkbattle.game.quirks.abilities.Ability;
import net.herobrine.quirkbattle.game.quirks.abilities.AbilitySets;
import net.herobrine.quirkbattle.util.Switchable;;
import org.bukkit.Sound;
import org.bukkit.entity.Player;

public class SwitchAbilitySetTest extends Ability {
    private final Switchable switchable = (Switchable) quirk;

    public SwitchAbilitySetTest(Abilities ability, Class quirk, int id, int slot) {
        super(ability, quirk, id, slot);
    }

    @Override
    public void doAbility(Player player) {
        AbilitySets currentSet = switchable.getCurrentSet();
        AbilitySets newSet = null;
        for (AbilitySets set : switchable.getAvailableSets()) {
            if (set != currentSet) {
                newSet = set;
                break;
            }
        }
        switchable.switchAbilitySet(newSet);
        player.playSound(player.getLocation(), Sound.WOOD_CLICK, 1f, 1f);
    }
}
