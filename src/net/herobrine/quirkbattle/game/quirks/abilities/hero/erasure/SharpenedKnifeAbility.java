package net.herobrine.quirkbattle.game.quirks.abilities.hero.erasure;

import net.herobrine.quirkbattle.game.quirks.abilities.Abilities;
import net.herobrine.quirkbattle.game.quirks.abilities.Ability;
import net.herobrine.quirkbattle.game.quirks.abilities.SpecialCase;
import net.herobrine.quirkbattle.game.quirks.hero.Erasure;
import net.herobrine.quirkbattle.util.Quirk;
import org.bukkit.ChatColor;
import org.bukkit.Sound;
import org.bukkit.entity.Player;

public class SharpenedKnifeAbility extends Ability implements SpecialCase {

    private final Erasure erasure;

    public SharpenedKnifeAbility(Abilities ability, Quirk quirk, int id, int slot) {
        super(ability, quirk, id, slot);
        erasure = (Erasure) quirk;
    }

    @Override
    public void doAbility(Player player) {
        erasure.setSharpenedKnife(true);
        player.sendMessage(ChatColor.GREEN + "You sharpened your knife and increased your attack damage for the next 3 hits!");
        player.playSound(player.getLocation(), Sound.HORSE_ARMOR, 1f, 0.8f);
    }

    @Override
    public boolean doesCasePass(Player player) {
        return !erasure.shouldUseAbilityAttack();
    }

    @Override
    public void doNoPass(Player player) {
    player.sendMessage(ChatColor.RED + "Your knife is already sharpened!");
    player.playSound(player.getLocation(), Sound.VILLAGER_NO, 1f, 1f);
    }
}
