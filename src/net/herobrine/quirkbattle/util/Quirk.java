package net.herobrine.quirkbattle.util;

import net.herobrine.quirkbattle.game.quirks.abilities.Ability;
import org.bukkit.entity.Player;

import java.util.List;

public interface Quirk {
    List<Ability> getAbilities();
    boolean shouldUseAbilityAttack();

    void useAbilityAttack(Player target);


}
