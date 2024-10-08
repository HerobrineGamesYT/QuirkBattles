package net.herobrine.quirkbattle.game.stats;


import net.herobrine.gamecore.Manager;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.UUID;

public class PlayerStats {

    private UUID uuid;
    private int health;
    private int maxHealth;
    private int defense;
    private int mana;
    private int intelligence;
    private int strength;
    private int lastRegenMana;


    private boolean useTemperature;

    private int baseTemp;
    private int temp;
    private int maxTemp;

    public PlayerStats(UUID uuid, int health, int maxHealth, int defense, int mana, int intelligence, int strength) {
        this.uuid = uuid;
        this.health = health;
        this.maxHealth = maxHealth;
        this.defense = defense;
        this.mana = mana;
        this.intelligence = intelligence;
        this.strength = strength;
        this.lastRegenMana = mana;
        this.useTemperature = false;
    }

    public PlayerStats(UUID uuid, int health, int maxHealth, int defense, int mana, int intelligence, int strength, boolean useTemperature, int temp, int maxTemp) {
        this.uuid = uuid;
        this.health = health;
        this.maxHealth = maxHealth;
        this.defense = defense;
        this.mana = mana;
        this.intelligence = intelligence;
        this.strength = strength;
        this.lastRegenMana = mana;
        this.useTemperature = useTemperature;
        this.temp = temp;
        this.baseTemp = temp;
        this.maxTemp = maxTemp;
    }

    public int getHealth() {return health;}

    public int getMaxHealth() {return maxHealth;}

    public int getDefense() {return defense;}

    public int getMana() {return mana;}

    public int getIntelligence() {return intelligence;}

    public int getStrength() {return strength;}

    public int getTemp() {return temp;}

    public int getBaseTemp() {return baseTemp;}

    public void setTemp(int temp) {
        this.temp = temp;
        Manager.getArena(Bukkit.getPlayer(uuid)).getQuirkBattleGame().updatePlayerStats(Bukkit.getPlayer(uuid));
    }

    public boolean useTemperature() {return useTemperature;}

    public int getMaxTemp() {return maxTemp;}

    public void setHealth(int health) {
        this.health = health;
        if (Manager.isPlaying(Bukkit.getPlayer(uuid))) Manager.getArena(Bukkit.getPlayer(uuid)).getQuirkBattleGame().updatePlayerStats(Bukkit.getPlayer(uuid));
    }

    public void setMaxHealth(int health) {
        this.maxHealth = health;
        Manager.getArena(Bukkit.getPlayer(uuid)).getQuirkBattleGame().updatePlayerStats(Bukkit.getPlayer(uuid));
    }

    public void setDefense(int defense) {
        Manager.getArena(Bukkit.getPlayer(uuid)).getQuirkBattleGame().updatePlayerStats(Bukkit.getPlayer(uuid));
        this.defense = defense;
    }

    public void setMana(int mana) {
        lastRegenMana = mana;
        this.mana = mana;
        Manager.getArena(Bukkit.getPlayer(uuid)).getQuirkBattleGame().updatePlayerStats(Bukkit.getPlayer(uuid));
    }

    public void setManaSpecial(int mana) {
        this.mana = mana;
        Manager.getArena(Bukkit.getPlayer(uuid)).getQuirkBattleGame().updatePlayerStats(Bukkit.getPlayer(uuid));
    }
    public void setIntelligence(int intelligence) {
        this.intelligence = intelligence;
        Manager.getArena(Bukkit.getPlayer(uuid)).getQuirkBattleGame().updatePlayerStats(Bukkit.getPlayer(uuid));
    }

    public void setStrength(int strength) {
        this.strength = strength;
        Manager.getArena(Bukkit.getPlayer(uuid)).getQuirkBattleGame().updatePlayerStats(Bukkit.getPlayer(uuid));
    }

}
