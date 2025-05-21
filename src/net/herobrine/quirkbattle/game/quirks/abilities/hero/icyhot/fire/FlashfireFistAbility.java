package net.herobrine.quirkbattle.game.quirks.abilities.hero.icyhot.fire;

import net.herobrine.gamecore.Class;
import net.herobrine.quirkbattle.game.quirks.abilities.Abilities;
import net.herobrine.quirkbattle.game.quirks.abilities.Ability;
import net.herobrine.quirkbattle.game.quirks.abilities.SpecialCase;
import net.herobrine.quirkbattle.game.quirks.hero.IcyHot;
import net.herobrine.quirkbattle.util.Quirk;
import org.bukkit.ChatColor;
import org.bukkit.Sound;
import org.bukkit.entity.Player;

public class FlashfireFistAbility extends Ability implements SpecialCase {

    private final IcyHot icyHot;

    public FlashfireFistAbility(Abilities ability, Quirk quirk, int id, int slot) {
        super(ability, quirk, id, slot);
        this.icyHot = (IcyHot) quirk;
    }

    @Override
    public void doAbility(Player player) {
        icyHot.setFireFist(true);
        player.sendMessage(ChatColor.GREEN + "You just powered up your next Fire attack with Flashfire Fist!");
        player.playSound(player.getLocation(), Sound.CREEPER_HISS, 1f, 1f);
    }

    @Override
    public boolean doesCasePass(Player player) {
        return !icyHot.isFireFistActive();
    }

    @Override
    public void doNoPass(Player player) {
    player.sendMessage(ChatColor.RED + "You already have Flashfire Fist active!");
    }
}
