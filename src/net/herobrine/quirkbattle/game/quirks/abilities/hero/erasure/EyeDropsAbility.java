package net.herobrine.quirkbattle.game.quirks.abilities.hero.erasure;

import net.herobrine.gamecore.Class;
import net.herobrine.quirkbattle.game.quirks.abilities.Abilities;
import net.herobrine.quirkbattle.game.quirks.abilities.Ability;
import net.herobrine.quirkbattle.game.quirks.abilities.SpecialCase;
import net.herobrine.quirkbattle.game.quirks.hero.Erasure;
import net.herobrine.quirkbattle.util.Quirk;
import org.bukkit.ChatColor;
import org.bukkit.Sound;
import org.bukkit.entity.Player;

public class EyeDropsAbility extends Ability implements SpecialCase {

    private final Erasure erasure;

    public EyeDropsAbility(Abilities ability, Quirk quirk, int id, int slot) {
        super(ability, quirk, id, slot);
        this.erasure = (Erasure) quirk;
    }

    @Override
    public void doAbility(Player player) {
        int cooldown = erasure.getCooldownSeconds();
        if (cooldown - 5 <= 0) erasure.setCooldownSeconds(0);
        else erasure.setCooldownSeconds(cooldown - 5);
        erasure.setErasureCooldown(erasure.getErasureCooldown() - 5000);

        player.playSound(player.getLocation(), Sound.DRINK, 1f, 1f);
        player.sendMessage(ChatColor.GREEN + "Erasure cooldown reduced!");
    }

    @Override
    public boolean doesCasePass(Player player) {
        return erasure.getCooldownSeconds() != 0;
    }

    @Override
    public void doNoPass(Player player) {
        player.sendMessage(ChatColor.RED + "Erasure isn't on cooldown!");
        player.playSound(player.getLocation(), Sound.VILLAGER_NO, 1f,1f);
    }
}
