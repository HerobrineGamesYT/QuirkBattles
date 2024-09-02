package net.herobrine.quirkbattle.game.quirks.abilities.hero.ofa;

import net.herobrine.core.HerobrinePVPCore;
import net.herobrine.gamecore.Class;
import net.herobrine.quirkbattle.game.quirks.abilities.Abilities;
import net.herobrine.quirkbattle.game.quirks.abilities.Ability;
import net.herobrine.quirkbattle.game.quirks.abilities.AbilitySets;
import net.herobrine.quirkbattle.game.quirks.hero.OneForAll;
import net.herobrine.quirkbattle.util.Switchable;
import org.bukkit.ChatColor;
import org.bukkit.Sound;
import org.bukkit.entity.Player;

public class SwitchAbilitySetTest extends Ability {
    public SwitchAbilitySetTest(Abilities ability, Class quirk, int id, int slot) {
        super(ability, quirk, id, slot);
    }
    Switchable switchable = (Switchable) quirk;

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
