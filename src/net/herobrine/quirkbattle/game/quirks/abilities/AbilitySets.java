package net.herobrine.quirkbattle.game.quirks.abilities;

public enum AbilitySets {
    ICE(new Abilities[] {Abilities.ICE_ABILITY_TEST, Abilities.ICE_WALL}, true, -1),
    FIRE(new Abilities[] {Abilities.FIRE_ABILITY_TEST}, true, 1),
    ONE_FOR_ALL(new Abilities[] {Abilities.DETROIT_SMASH, Abilities.SHOOT_STYLE, Abilities.AIR_PROPULSION}, false, 0),
    EXPLOSION(new Abilities[] {Abilities.EXPLOSION_DASH, Abilities.EXPLOSION_PUNCH, Abilities.HOWITZER_IMPACT}, false, 0),
    HARDENING(new Abilities[] {Abilities.SHARP_CLAW, Abilities.STONE_CHARGE, Abilities.UNBREAKABLE}, false, 0),
    ALL_FOR_ONE(new Abilities[] {}, false, 0),
    ZEROGRAVITY(new Abilities[] {},false, 0),
    ERASURE(new Abilities[] {},false, 0),
    PERMEATION(new Abilities[] {},false, 0),
    OFA_SWITCH_TEST(new Abilities[] {Abilities.AIR_PROPULSION, Abilities.DETROIT_SMASH, Abilities.SHOOT_STYLE}, false, 0);


    private Abilities[] abilities;
    public boolean useTemperature;
    public int tempPerSecond;

    private AbilitySets(Abilities[] abilities, boolean useTemperature, int tempPerSecond) {
        this.abilities = abilities;
        this.useTemperature = useTemperature;
        this.tempPerSecond = tempPerSecond;
    }

    public Abilities[] getAbilities() {return abilities;}

    public boolean useTemperature() {return useTemperature;}

    public int getTempPerSecond() {return tempPerSecond;}

}
