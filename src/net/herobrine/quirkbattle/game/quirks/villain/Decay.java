package net.herobrine.quirkbattle.game.quirks.villain;

import net.herobrine.gamecore.*;
import net.herobrine.gamecore.Class;
import net.herobrine.quirkbattle.event.QuirkErasureEvent;
import net.herobrine.quirkbattle.game.CustomDeathCause;
import net.herobrine.quirkbattle.game.quirks.abilities.Ability;
import net.herobrine.quirkbattle.game.quirks.abilities.AbilitySets;
import net.herobrine.quirkbattle.game.stats.PlayerStats;
import net.herobrine.quirkbattle.util.Quirk;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class Decay extends Class implements Quirk {
    private final List<Ability> abilities;
    private PlayerStats stats;
    private final Arena arena;
    private Player player;
    private final UUID originalId;
    private boolean isBeingErased = false;
    private boolean shouldDecay = false;
    private int decayChargeLevel = 0;

    public Decay(UUID uuid) {
        super(uuid, ClassTypes.DECAY);
        this.player = Bukkit.getPlayer(uuid);
        this.arena = Manager.getArena(player);
        this.abilities = new ArrayList<>();
        this.originalId = uuid;
    }

    @Override
    public void onStart(Player player) {
        stats = new PlayerStats(player.getUniqueId(), 200, 200, 40, 30, 300, 40);
        arena.getQuirkBattleGame().getPlayerStatsMap().put(uuid, stats);
        
        player.getInventory().clear();
        
        // Basic attack item
        net.herobrine.core.ItemBuilder basicAttack = new net.herobrine.core.ItemBuilder(Material.STICK);
        basicAttack.setDisplayName(ChatColor.RED + "Decay Touch");
        basicAttack.setLore(ChatColor.GRAY + "Touch your enemies with all five fingers to decay them.");
        
        player.getInventory().setItem(0, basicAttack.build());
        registerAbilities(AbilitySets.DECAY);
        
        player.getInventory().setHeldItemSlot(0);
    }

    @Override
    public List<Ability> getAbilities() {
        return abilities;
    }

    @Override
    public boolean isBeingErased() {
        return isBeingErased;
    }

    @Override
    public boolean shouldUseAbilityAttack() {
        return shouldDecay;
    }

    @Override
    public UUID getUniqueId() {
        return uuid;
    }

    @Override
    public UUID getOriginalId() {
        return originalId;
    }

    @Override
    public void useAbilityAttack(Player target) {
        // Apply decay damage to target
        decayChargeLevel = 0;
        setDecayActive(false);
        int damage = arena.getClass(player).getBaseDamage() + 5;
        
        // Apply damage to target
        abilities.get(0).doDamageTo(player, target, damage, CustomDeathCause.DECAY_TOUCH);

        // Visual and sound effects
        target.getWorld().playEffect(target.getLocation().add(0, 1, 0), org.bukkit.Effect.STEP_SOUND, Material.SOUL_SAND);
        player.playSound(player.getLocation(), Sound.FIZZ, 1.0f, 0.5f);
        target.playSound(target.getLocation(), Sound.FIZZ, 1.0f, 0.5f);
        
        // Notify players
        target.sendMessage(ChatColor.DARK_GRAY + player.getName() + ChatColor.GRAY + " is decaying you with their touch!");
        player.sendMessage(ChatColor.GRAY + "You decayed " + target.getName() + " and gained 15 stamina!");

        giveStaminaBoost(15);
    }

    @Override
    public void registerAbilities(AbilitySets set) {
        int i = 2;
        for (net.herobrine.quirkbattle.game.quirks.abilities.Abilities ability : set.getAbilities()) {
            abilities.add(arena.getQuirkBattleGame().getAbilityManager().registerAbility(ability, this, i));
            i++;
        }
    }

    public void setDecayActive(boolean active) {
        this.shouldDecay = active;
        if (active) {
            player.playSound(player.getLocation(), Sound.FIZZ, 1.0f, 0.5f);
            player.sendMessage(ChatColor.GREEN + "Your decay touch is active! Your next attack has been boosted!");
        }

    }

    public void giveStaminaBoost(int stamina) {
        int stm = stats.getMana() + stamina;
        if (stm > stats.getIntelligence()) stm = stats.getIntelligence();
        stats.setMana(stm);
    }

    public int getDecayCharge() {
        return decayChargeLevel;
    }

    @EventHandler
    public void onAttack(EntityDamageByEntityEvent event) {
        // Check if the damager is the player with disintegration active
        if (!(event.getDamager() instanceof Player)) return;
        Player damager = (Player) event.getDamager();

        if (arena.getState() != GameState.LIVE) return;

        // Check if the entity being damaged is a player
        if (!(event.getEntity() instanceof Player)) return;
        Player target = (Player) event.getEntity();

        // Skip spectators and dead players
        if (arena.getSpectators().contains(target.getUniqueId())) return;
      //  if (!arena.getQuirkBattleGame().getAlivePlayers().contains(target.getUniqueId())) return;

        // Skip teammates in team modes
        if (arena.getType().isTeamsMode() && arena.getTeam(damager).equals(arena.getTeam(target))) return;

        if (!shouldDecay && !isBeingErased) {
            decayChargeLevel = decayChargeLevel + 1;

            if (decayChargeLevel > 3) setDecayActive(true);
        }
    }

    @EventHandler
    public void onErase(QuirkErasureEvent event) {
        if (event.isErasing()) {
            isBeingErased = true;
            if (shouldDecay) setDecayActive(false);
        }
        else isBeingErased = false;
    }
}
