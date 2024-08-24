package net.herobrine.quirkbattle.game.quirks.abilities;

import org.bukkit.ChatColor;
import org.bukkit.Material;

public enum Abilities {

    DETROIT_SMASH(ChatColor.GOLD + "Detroit Smash", new String[] {"Launch yourself up into the air", "and then smash into the ground,", "dealing damage to nearby players."},
            Material.DIAMOND_AXE,0,0, 0, 0, 0, 15000, 2.5, 15, true, false,false),
    SHOOT_STYLE(ChatColor.GREEN + "Shoot Style", new String[] {"Launch yourself forward, allowing you to dash into", "another player with a hard kick,", "dealing massive damage to them!"},
            Material.BOW, 0,0, 0,0, 0,25000, 1, 20, true, false,false),
    AIR_PROPULSION(ChatColor.BLUE + "Air Propulsion", new String[] {"Propel yourself around the arena", "with bursts of air! Power up to travel farther."},
            Material.GLOWSTONE_DUST, 0,0,0, 0,0,1200,0,0,true,false,false),
    EXPLOSION_DASH(ChatColor.GOLD + "Explosion Dash", new String[] {"Propel yourself forward with an explosion-", "dealing damage to whoever is around."},
            Material.BOW, 0,10, 10, 2, 0, 1500, 2, 5, false, false,false),
    EXPLOSION_PUNCH(ChatColor.GOLD + "Explosion Punch", new String[] {"Charge your next punch with explosive power-", "allowing you to gain increased area damage", "in exchange for stamina."},
            Material.BLAZE_POWDER, 0,25,25,5, 0,6000,2,5,false,true,true),

    // Howitzer Impact small explosion stats are in the Howitzer ability class. To keep things consistent some special one-time use ability-specific stats are kept to the class, while their main base stats are stored here.
    HOWITZER_IMPACT(ChatColor.GOLD + "Howitzer Impact", new String[] {"Launch yourself into the air and", "create 3 small explosions and 1 big explosion", "upon landing, dealing massive damage to anyone near."},
            Material.FIREBALL, 0,40, 40, 5, 0, 20000, 5, 30, false, false, false),
    STONE_CHARGE(ChatColor.RED + "Stone Charge", new String[] {"Temporarily increase your speed and defense,", "allowing you to charge into a player without a care!"},
            Material.FIREWORK_CHARGE, 0, 30, 30, 0, 50, 15000, 1.5, 20, false, true, true),
    SHARP_CLAW(ChatColor.RED + "Sharp Claw", new String[] {"Power up your next 3 hits", "with increased damage by sharpening your fists!"}, Material.BLAZE_POWDER, 0,
            20, 20, 0, 0, 10000, 0, 3,false,true,true),
    UNBREAKABLE(ChatColor.RED + "Unbreakable", new String[] {"Push yourself to your absolute limits!", "Temporarily gain a massive defense", "boost and sharpen all your attacks!"},
            Material.GLOWSTONE_DUST, 0, 60, 60, 0, 75, 40000, 0, 0,false,true,true);


    private String display;

    private Material material;
    private int durability;
    private String[] description;
    private int cost;
    private int minStamina;
    private int staminaBoost;

    private int defenseBoost;
    private long cooldown;
    private double radius;
    private double damage;
    private boolean useManaForPower;
    private boolean hasSpecialCase;

    private boolean waitForCooldown;


    private Abilities(String display, String[] description, Material material, int durability, int cost, int minStamina, int staminaBoost, int defenseBoost, long cooldown, double radius, double damage, boolean useManaForPower, boolean hasSpecialCase
    ,boolean waitForCooldown) {
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

    public String getDisplay() {return display;}
    public String[] getDescription() {return description;}
    public Material getMaterial() {return material;}
    public int getDurability() {return durability;}
    public int getStaminaBoost() {return staminaBoost;}
    public int getDefenseBoost() {return defenseBoost;}
    public double getRadius() {return radius;}
    public double getDamage() {return damage;}
    public int getCost() {return cost;}
    public int getMinStamina() {return minStamina;}
    public long getCooldown() {return cooldown;}
    public boolean useManaForPower() {return useManaForPower;}
    public boolean hasSpecialCase() {return hasSpecialCase;}
    public boolean waitForCooldown() {return waitForCooldown;}


}
