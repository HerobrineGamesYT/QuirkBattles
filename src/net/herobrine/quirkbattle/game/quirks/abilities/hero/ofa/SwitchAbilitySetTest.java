package net.herobrine.quirkbattle.game.quirks.abilities.hero.ofa;

import net.herobrine.quirkbattle.game.quirks.abilities.Abilities;
import net.herobrine.quirkbattle.game.quirks.abilities.Ability;
import net.herobrine.quirkbattle.game.quirks.abilities.AbilitySets;
import net.herobrine.quirkbattle.game.quirks.abilities.SpecialCase;
import net.herobrine.quirkbattle.util.Quirk;
import net.herobrine.quirkbattle.util.Switchable;;
import org.bukkit.ChatColor;
import org.bukkit.Sound;
import org.bukkit.entity.Player;

public class SwitchAbilitySetTest extends Ability implements SpecialCase {
    private final Switchable switchable;

    public SwitchAbilitySetTest(Abilities ability, Quirk quirk, int id, int slot) {
        super(ability, quirk, id, slot);
        this.switchable = (Switchable) quirk;
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

    @Override
    public boolean doesCasePass(Player player) {
        return switchable.getAvailableSets().length > 1;
    }

    @Override
    public void doNoPass(Player player) {
    player.sendMessage(ChatColor.RED + "You don't have another quirk to switch to!");
    player.playSound(player.getLocation(), Sound.VILLAGER_NO, 1f, 1f);
    }
}
