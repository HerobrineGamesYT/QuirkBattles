package net.herobrine.quirkbattle.game.quirks.abilities;

public enum AbilitySets {
    ICE(new Abilities[] {}), FIRE(new Abilities[] {}), ONE_FOR_ALL(new Abilities[] {Abilities.DETROIT_SMASH, Abilities.SHOOT_STYLE, Abilities.AIR_PROPULSION}),
    EXPLOSION(new Abilities[] {Abilities.EXPLOSION_DASH, Abilities.EXPLOSION_PUNCH, Abilities.HOWITZER_IMPACT}),
    HARDENING(new Abilities[] {Abilities.SHARP_CLAW, Abilities.STONE_CHARGE, Abilities.UNBREAKABLE}),
    ALL_FOR_ONE(new Abilities[] {}), ZEROGRAVITY(new Abilities[] {}), ERASURE(new Abilities[] {}), PERMEATION(new Abilities[] {}),
    OFA_SWITCH_TEST(new Abilities[] {Abilities.AIR_PROPULSION, Abilities.DETROIT_SMASH, Abilities.SHOOT_STYLE});


    private Abilities[] abilities;

    private AbilitySets(Abilities[] abilities) {
        this.abilities = abilities;
    }

    public Abilities[] getAbilities() {return abilities;}
}
