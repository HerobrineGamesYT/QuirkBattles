package net.herobrine.quirkbattle.game.quirks.abilities.hero.hardening;

import net.herobrine.gamecore.Class;
import net.herobrine.quirkbattle.game.quirks.abilities.Abilities;
import net.herobrine.quirkbattle.game.quirks.abilities.Ability;
import net.herobrine.quirkbattle.game.quirks.abilities.SpecialCase;
import net.herobrine.quirkbattle.game.quirks.hero.Hardening;
import org.bukkit.ChatColor;
import org.bukkit.Sound;
import org.bukkit.entity.Player;

public class SharpClawAbility extends Ability implements SpecialCase {
    public SharpClawAbility(Abilities ability, Class quirk, int id, int slot) {
        super(ability, quirk, id, slot);
        this.hardening = (Hardening) quirk;
    }
    Hardening hardening;
    @Override
    public void doAbility(Player player) {
    hardening.setSharpClaw(true);
    }

    @Override
    public boolean doesCasePass(Player player) {return !hardening.isClawSharp();}

    @Override
    public void doNoPass(Player player) {
    player.sendMessage(ChatColor.RED + "Your attacks have already been sharpened!");
    player.playSound(player.getLocation(), Sound.VILLAGER_NO, 1f,1f);
    }
}
