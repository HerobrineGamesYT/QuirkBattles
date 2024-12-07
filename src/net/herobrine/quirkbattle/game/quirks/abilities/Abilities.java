package net.herobrine.quirkbattle.game.quirks.abilities;

import org.bukkit.ChatColor;
import org.bukkit.Material;

public enum Abilities {

    DETROIT_SMASH(ChatColor.GOLD + "Detroit Smash", new String[]{"Launch yourself up into the air", "and then smash into the ground,", "dealing damage to nearby players."},
            Material.DIAMOND_AXE, 0, 0, 0, 0, 0, 15000, 2.5, 15, true, false, false),
    SHOOT_STYLE(ChatColor.GREEN + "Shoot Style", new String[]{"Launch yourself forward, allowing you to dash into", "another player with a hard kick,", "dealing massive damage to them!"},
            Material.BOW, 0, 0, 0, 0, 0, 25000, 1, 20, true, false, false),
    AIR_PROPULSION(ChatColor.BLUE + "Air Propulsion", new String[]{"Propel yourself around the arena", "with bursts of air! Power up to travel farther."},
            Material.GLOWSTONE_DUST, 0, 0, 0, 0, 0, 1200, 0, 0, true, false, false),
    EXPLOSION_DASH(ChatColor.GOLD + "Explosion Dash", new String[]{"Propel yourself forward with an explosion-", "dealing damage to whoever is around."},
            Material.BOW, 0, 10, 10, 2, 0, 1500, 2, 5, false, false, false),
    EXPLOSION_PUNCH(ChatColor.GOLD + "Explosion Punch", new String[]{"Charge your next punch with explosive power-", "allowing you to gain increased area damage", "in exchange for stamina."},
            Material.BLAZE_POWDER, 0, 25, 25, 5, 0, 6000, 2, 5, false, true, true),

    // Howitzer Impact small explosion stats are in the Howitzer ability class. To keep things consistent some special one-time use ability-specific stats are kept to the class, while their main base stats are stored here.
    HOWITZER_IMPACT(ChatColor.GOLD + "Howitzer Impact", new String[]{"Launch yourself into the air and", "create 3 small explosions and 1 big explosion", "upon landing, dealing massive damage to anyone near."},
            Material.FIREBALL, 0, 40, 40, 5, 0, 20000, 5, 30, false, false, false),
    STONE_CHARGE(ChatColor.RED + "Stone Charge", new String[]{"Temporarily increase your speed and defense,", "allowing you to charge into a player without a care!"},
            Material.FIREWORK_CHARGE, 0, 30, 30, 0, 75, 15000, 1.5, 20, false, true, true),
    SHARP_CLAW(ChatColor.RED + "Sharp Claw", new String[]{"Power up your next 3 hits", "with increased damage by sharpening your fists!"}, Material.BLAZE_POWDER, 0,
            20, 20, 0, 0, 10000, 0, 3, false, true, true),
    UNBREAKABLE(ChatColor.RED + "Unbreakable", new String[]{"Push yourself to your absolute limits!", "Temporarily gain a massive defense", "boost and sharpen all your attacks!"},
            Material.GLOWSTONE_DUST, 0, 60, 60, 0, 100, 40000, 0, 0, false, true, true),
    OFA_ABILITY_SWITCH_TEST(ChatColor.RED + "Ability Switcher", new String[]{"Switch to your other ability set!", "This is currently a test ability."},
            Material.CLAY_BALL, 0, 0, 0, 0, 0, 1000, 0, 0, false, false, false),
    ICE_ABILITY_TEST(ChatColor.BLUE + "Ice Ability Test", new String[]{"This is an ability meant to help", "test the temperature system for ice."},
            Material.PACKED_ICE, 0, -25, 25, 0, 0, 2000, 0, 0, false, false, false),
    FIRE_ABILITY_TEST(ChatColor.RED + "Fire Ability Test", new String[]{"This is an ability meant to help", "test the temperature system for fire."},
            Material.FIREWORK_CHARGE, 0, 25, 25, 0, 0, 2000, 0, 0, false, false, false),
    ICE_WALL(ChatColor.AQUA + "Ice Wall", new String[]{"Spawn a massive Ice Wall in", "front of you, stunning and damaging", "all enemies caught within it!"},
            Material.PACKED_ICE, 0, -30, 0, 0, 0, 10000, 0, 10, false, false, false),
    CAPTURE(ChatColor.RED + "Capture Tape", new String[] {"Launch out a magical aura that will", "pull and stun any player it hits towards you!"}, Material.FISHING_ROD,
            0, 10, 10, 0,0,3000, 0, 0,false, false, false),
    SHARPENED_KNIFE(ChatColor.RED + "Sharpened Knife", new String[] {"Sharpen your knife to greatly increase", "your attack damage for the next 3 hits!"}, Material.IRON_SWORD,
            0, 10, 10,0,0,4000,0,4,false,true,true),
    EYEDROPS(ChatColor.RED + "Eye Drops", new String[] {"Take some eye drops and shorten the", "cooldown timer on your erasure ability!"}, Material.GLASS_BOTTLE,
            0,50,50,0,0,4000,0,0,false,true,false),
    FIRE_WALL(ChatColor.RED + "Fire Wall", new String[] {"Spawn a wall of fire in", "front of you, dealing massive damage",
            "and also giving a burn effect", "to your enemies! Also can be used", "to unfreeze yourself or your allies!"}, Material.FLINT, 0, 30, 0, 0,0,9000,
            0,15,false,false,false),
    ICE_SHARD(ChatColor.AQUA + "Ice Shards", new String[] {"Shoot 3 Ice projectiles and deal burst", "damage to any enemies that are hit!"}, Material.SNOW, 0, -20, 0,
            0,0,5000,0,10,false,false,false),
    FLASHFIRE_FIST(ChatColor.RED + "Flashfire Fist", new String[] {"Power up your first with your flames", "to add burning damage to", "your next melee attack!"},
            Material.BLAZE_POWDER, 0, 20, 0,0,0,7000,0,6,false,true,true),
    GLACIER(ChatColor.AQUA + "Glacier", new String[] {"Create a massive Glacier Zone around you,"," slowing down all enemies in its radius", "and dealing a small amount of DPS to them! Will be cancelled ","" +
            "when you switch your ability set to fire,", "or get frostbite- whichever happens first."}, Material.MONSTER_EGG, 0, 0,-10, 0,0,40000,
            3,5,false,true,true),
    FLAME_OVERDRIVE(ChatColor.RED + "Overdrive", new String[] {"Power up your flames and go into maximum overdrive!", "Move faster and apply a burning effect to all enemies that are nearby!"},
            Material.LAVA_BUCKET, 0, 0, 90, 0,0,40000,3,6,false,true,true);


    private final String display;

    private final Material material;
    private final int durability;
    private final String[] description;
    private final int cost;
    private final int minStamina;
    private final int staminaBoost;

    private final int defenseBoost;
    private final long cooldown;
    private final double radius;
    private final double damage;
    private final boolean useManaForPower;
    private final boolean hasSpecialCase;

    private final boolean waitForCooldown;


     Abilities(String display, String[] description, Material material, int durability, int cost, int minStamina, int staminaBoost, int defenseBoost, long cooldown, double radius, double damage, boolean useManaForPower, boolean hasSpecialCase
            , boolean waitForCooldown) {
        this.display = display;
        this.description = description;
        this.material = material;
        this.durability = durability;
        this.cost = cost;
        this.minStamina = minStamina;
        this.staminaBoost = staminaBoost;
        this.defenseBoost = defenseBoost;
        this.cooldown = cooldown;
        this.radius = radius;
        this.damage = damage;
        this.useManaForPower = useManaForPower;
        this.hasSpecialCase = hasSpecialCase;
        this.waitForCooldown = waitForCooldown;
    }

    public String getDisplay() {
        return display;
    }

    public String[] getDescription() {
        return description;
    }

    public Material getMaterial() {
        return material;
    }

    public int getDurability() {
        return durability;
    }

    public int getStaminaBoost() {
        return staminaBoost;
    }

    public int getDefenseBoost() {
        return defenseBoost;
    }

    public double getRadius() {
        return radius;
    }

    public double getDamage() {
        return damage;
    }

    public int getCost() {
        return cost;
    }

    public int getMinStamina() {return minStamina;}

    public long getCooldown() {
        return cooldown;
    }

    public boolean useManaForPower() {
        return useManaForPower;
    }

    public boolean hasSpecialCase() {
        return hasSpecialCase;
    }

    public boolean waitForCooldown() {
        return waitForCooldown;
    }


}
