package net.herobrine.quirkbattle.game.quirks.hero;

import net.herobrine.gamecore.Class;
import net.herobrine.gamecore.ClassTypes;
import net.herobrine.quirkbattle.game.quirks.abilities.Ability;
import net.herobrine.quirkbattle.game.quirks.abilities.AbilitySets;
import net.herobrine.quirkbattle.util.Quirk;
import net.herobrine.quirkbattle.util.Switchable;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.UUID;

public class IcyHot extends Class implements Quirk, Switchable {
    public IcyHot(UUID uuid) {
        super(uuid, ClassTypes.ICYHOT);
    }

    @Override
    public void onStart(Player player) {

    }

    @Override
    public List<Ability> getAbilities() {
        return null;
    }

    @Override
    public boolean shouldUseAbilityAttack() {
        return false;
    }

    @Override
    public void useAbilityAttack(Player target) {

    }

    @Override
    public void registerAbilities(AbilitySets set) {

    }

    @Override
    public AbilitySets getCurrentSet() {
        return null;
    }

    @Override
    public AbilitySets[] getAvailableSets() {
        return new AbilitySets[0];
    }

    @Override
    public void switchAbilitySet(AbilitySets set) {

    }
}
