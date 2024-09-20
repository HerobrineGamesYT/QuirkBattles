package net.herobrine.quirkbattle.game.quirks.abilities.hero.icyhot.fire;

import net.herobrine.gamecore.Class;
import net.herobrine.quirkbattle.game.quirks.abilities.Abilities;
import net.herobrine.quirkbattle.game.quirks.abilities.Ability;
import org.bukkit.ChatColor;
import org.bukkit.Sound;
import org.bukkit.entity.Player;

public class FireAbilityTest extends Ability {
    public FireAbilityTest(Abilities ability, Class quirk, int id, int slot) {
        super(ability, quirk, id, slot);
    }

    @Override
    public void doAbility(Player player) {
        player.sendMessage(ChatColor.RED + "Fire Ability Test!");
        player.playSound(player.getLocation(), Sound.FIRE, 1f,1f);
    }
}
