package net.herobrine.quirkbattle.game.quirks.abilities.hero.icyhot.ice;

import net.herobrine.gamecore.Class;
import net.herobrine.quirkbattle.game.quirks.abilities.Abilities;
import net.herobrine.quirkbattle.game.quirks.abilities.Ability;
import org.bukkit.ChatColor;
import org.bukkit.Sound;
import org.bukkit.entity.Player;

public class IceAbilityTest extends Ability {
    public IceAbilityTest(Abilities ability, Class quirk, int id, int slot) {
        super(ability, quirk, id, slot);
    }

    @Override
    public void doAbility(Player player) {
        player.sendMessage(ChatColor.AQUA + "Ice Ability Test!");
        player.playSound(player.getLocation(), Sound.DIG_SNOW, 1f, 1f);
    }
}
