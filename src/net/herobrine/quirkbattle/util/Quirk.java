package net.herobrine.quirkbattle.util;

import net.herobrine.quirkbattle.game.quirks.abilities.Ability;
import net.herobrine.quirkbattle.game.quirks.abilities.AbilitySets;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.UUID;

public interface Quirk {
    List<Ability> getAbilities();

    boolean isBeingErased();
    boolean shouldUseAbilityAttack();
    UUID getUniqueId();
    UUID getOriginalId();

    void useAbilityAttack(Player target);

    void registerAbilities(AbilitySets set);


}
