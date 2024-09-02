package net.herobrine.quirkbattle.util;

import net.herobrine.quirkbattle.game.quirks.abilities.Ability;
import net.herobrine.quirkbattle.game.quirks.abilities.AbilitySets;

import java.util.ArrayList;
import java.util.List;

public interface Switchable {
    List<Ability> transferList = new ArrayList<>();
    List<Ability> secondaryAbilities = new ArrayList<>();

    // set these two up on quirk init.


     AbilitySets getCurrentSet();
     AbilitySets[] getAvailableSets();

     void switchAbilitySet(AbilitySets set);



}
