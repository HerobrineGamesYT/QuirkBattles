package net.herobrine.quirkbattle.util;

import net.herobrine.quirkbattle.game.quirks.abilities.Ability;
import net.herobrine.quirkbattle.game.quirks.abilities.AbilitySets;
import org.bukkit.entity.Player;

import java.util.List;

public interface Quirk {
    List<Ability> getAbilities();
    boolean shouldUseAbilityAttack();

    void useAbilityAttack(Player target);

    void registerAbilities(AbilitySets set);


}
