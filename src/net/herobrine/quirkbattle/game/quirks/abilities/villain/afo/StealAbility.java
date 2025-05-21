package net.herobrine.quirkbattle.game.quirks.abilities.villain.afo;

import net.herobrine.core.HerobrinePVPCore;
import net.herobrine.gamecore.Class;
import net.herobrine.quirkbattle.game.quirks.abilities.Abilities;
import net.herobrine.quirkbattle.game.quirks.abilities.Ability;
import net.herobrine.quirkbattle.game.quirks.abilities.SpecialCase;
import net.herobrine.quirkbattle.game.quirks.villain.AllForOne;
import net.herobrine.quirkbattle.util.Quirk;
import org.bukkit.ChatColor;
import org.bukkit.Sound;
import org.bukkit.entity.Player;

public class StealAbility extends Ability implements SpecialCase {
    private final AllForOne afo;

    public StealAbility(Abilities ability, Quirk quirk, int id, int slot) {
        super(ability, quirk, id, slot);
        this.afo = (AllForOne) quirk;
     }

    @Override
    public void doAbility(Player player) {
        afo.setSteal(true);
        player.playSound(player.getLocation(), Sound.CREEPER_HISS, 1f, 1f);
        player.sendMessage(HerobrinePVPCore.translateString("&c&lHEH...&r &7The next player you hit will have their Quirk stolen!"));
    }

    @Override
    public boolean doesCasePass(Player player) {
        return afo.getAvailableSets().length == 1 && !afo.shouldUseAbilityAttack();
    }

    @Override
    public void doNoPass(Player player) {
     if(afo.shouldUseAbilityAttack()) player.sendMessage(ChatColor.RED + "You already have your Steal ability active! Hit a player to take their Quirk!");
     else player.sendMessage(ChatColor.RED + "You can only steal 1 quirk at a time!");

     player.playSound(player.getLocation(), Sound.VILLAGER_NO, 1f, 1f);
    }
}
