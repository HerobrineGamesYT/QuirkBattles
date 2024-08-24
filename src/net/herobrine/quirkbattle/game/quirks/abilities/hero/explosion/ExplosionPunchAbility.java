package net.herobrine.quirkbattle.game.quirks.abilities.hero.explosion;

import net.herobrine.gamecore.Class;
import net.herobrine.quirkbattle.game.quirks.abilities.Abilities;
import net.herobrine.quirkbattle.game.quirks.abilities.Ability;
import net.herobrine.quirkbattle.game.quirks.abilities.SpecialCase;
import net.herobrine.quirkbattle.game.quirks.hero.Explosion;
import org.bukkit.ChatColor;
import org.bukkit.Sound;
import org.bukkit.entity.Player;

public class ExplosionPunchAbility extends Ability implements SpecialCase {
    Explosion exp;

    public ExplosionPunchAbility(Abilities ability, Class quirk, int id, int slot) {
        super(ability, quirk, id, slot);
        this.exp = (Explosion) quirk;
    }

    @Override
    public void doAbility(Player player) {
        exp.resetDefaultItem();
        exp.setExplosivePunch(true);
        player.sendMessage(ChatColor.GREEN + "You've just powered up your next punch with explosive power!");
        player.playSound(player.getLocation(), Sound.CREEPER_HISS, 1f, 1f);
    }

    @Override
    public boolean doesCasePass(Player player) {
        return !exp.isExplosivePunch();
    }

    @Override
    public void doNoPass(Player player) {
    player.sendMessage(ChatColor.RED + "You've already powered up!");
    player.playSound(player.getLocation(), Sound.VILLAGER_NO, 1f, 1f);
    }
}
