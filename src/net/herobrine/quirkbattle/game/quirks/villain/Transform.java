package net.herobrine.quirkbattle.game.quirks.villain;

import net.herobrine.core.HerobrinePVPCore;
import net.herobrine.gamecore.Arena;
import net.herobrine.gamecore.Class;
import net.herobrine.gamecore.ClassTypes;
import net.herobrine.gamecore.Manager;
import net.herobrine.quirkbattle.QuirkBattlesPlugin;
import net.herobrine.quirkbattle.game.CustomDeathCause;
import net.herobrine.quirkbattle.game.quirks.abilities.Ability;
import net.herobrine.quirkbattle.game.quirks.abilities.AbilitySets;
import net.herobrine.quirkbattle.game.stats.PlayerStats;
import net.herobrine.quirkbattle.util.Quirk;
import net.herobrine.quirkbattle.util.Switchable;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class Transform extends Class implements Quirk {
    private final List<Ability> abilities;
    private PlayerStats stats;
    private final Arena arena;
    private Player player;
    private final UUID originalId;
    private UUID targetId;
    private Player targetPlayer;
    private boolean isTransformed = false;
    private boolean isBeingErased = false;
    private boolean shouldCollectBlood = false;
    private int hitCount = 0;
    public Transform(UUID uuid) {
        super(uuid, ClassTypes.TRANSFORM);
        this.player = Bukkit.getPlayer(uuid);
        this.arena = Manager.getArena(player);
        this.abilities = new ArrayList<>();
        this.originalId = uuid;
    }

    @Override
    public void onStart(Player player) {
        stats = new PlayerStats(player.getUniqueId(), 200, 200, 40, 100, 100, 40);
        arena.getQuirkBattleGame().getPlayerStatsMap().put(uuid, stats);

        // Initialize blood to 0 and enable blood display
        stats.setBlood(0);
        stats.setUseBlood(true);
        
        player.getInventory().clear();
        
        // Basic attack item
        net.herobrine.core.ItemBuilder basicAttack = new net.herobrine.core.ItemBuilder(Material.STICK);
        basicAttack.setDisplayName(ChatColor.RED + "Blood Collection");
        basicAttack.setLore(ChatColor.GRAY + "Use your abilities on your enemies to collect their blood.");
        
        player.getInventory().setItem(0, basicAttack.build());
        registerAbilities(AbilitySets.TRANSFORM);
        
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
        return shouldCollectBlood;
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
        // Collect blood from target
        int bloodGain = 10;
        int currentBlood = stats.getBlood();
        stats.setBlood(Math.min(stats.getMaxBlood(), currentBlood + bloodGain));
        
        // Apply damage boost if hitCount < 3
        if (hitCount < 3) {
            // Calculate boosted damage (base damage + 4, similar to SharpenedKnife)
            int damage = (arena.getClass(player).getBaseDamage() + 4);
            
            // Apply damage to target
            EntityDamageEvent event = new EntityDamageEvent(target, EntityDamageEvent.DamageCause.CUSTOM, damage);
            arena.getQuirkBattleGame().getCustomDeathCause().put(target.getUniqueId(), CustomDeathCause.SHARPENED_KNIFE);
            arena.getQuirkBattleGame().getLastAbilityAttacker().put(target.getUniqueId(), player.getUniqueId());
            Bukkit.getPluginManager().callEvent(event);
            target.setLastDamageCause(event);
            
            // Blood particle effects
            target.getWorld().playEffect(target.getLocation().add(0, 1, 0), org.bukkit.Effect.STEP_SOUND, Material.REDSTONE_BLOCK);
            
            // Increment hit count
            hitCount++;
            
            // Notify players
            target.sendMessage(ChatColor.RED + player.getName() + ChatColor.DARK_RED + " drew some blood from you with their stab attack!");
            player.sendMessage(ChatColor.RED + "You collected blood from " + target.getName() + "! (" + hitCount + "/3)");
            
            // Reset if we've reached 3 hits
            if (hitCount >= 3) {
                hitCount = 0;
                shouldCollectBlood = false;
                player.sendMessage(ChatColor.RED + "Your blood stab ability has ended.");
                getAbilities().get(0).setCooldown(System.currentTimeMillis());
                getAbilities().get(0).doAbilityCooldown();
            }
        }
        player.playSound(player.getLocation(), Sound.DRINK, 1.0f, 1.0f);
        
        // Store target for potential transformation
        targetId = target.getUniqueId();
        targetPlayer = target;
    }

    @Override
    public void registerAbilities(AbilitySets set) {
        int i = 2;
        for (net.herobrine.quirkbattle.game.quirks.abilities.Abilities ability : set.getAbilities()) {
            abilities.add(arena.getQuirkBattleGame().getAbilityManager().registerAbility(ability, this, i));
            i++;
        }
    }

    
    public void setBloodCollection(boolean collect) {
        this.shouldCollectBlood = collect;
    }
    
    public int getBloodAmount() {
        return stats.getBlood();
    }
    
    public void setBlood(int amount) {
        stats.setBlood(amount);
    }

    public boolean isTransformed() {
        return isTransformed;
    }
    
    public Player getTargetPlayer() {
        return targetPlayer;
    }
    
    public void setTargetPlayer(Player targetPlayer) {
        this.targetPlayer = targetPlayer;
        this.targetId = targetPlayer.getUniqueId();
    }
    
    public void setHitCount(int count) {
        this.hitCount = count;
    }
    
    public int getHitCount() {
        return hitCount;
    }

}
