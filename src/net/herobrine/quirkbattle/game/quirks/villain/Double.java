package net.herobrine.quirkbattle.game.quirks.villain;

import net.herobrine.gamecore.Arena;
import net.herobrine.gamecore.Class;
import net.herobrine.gamecore.ClassTypes;
import net.herobrine.gamecore.Manager;
import net.herobrine.quirkbattle.game.quirks.abilities.Ability;
import net.herobrine.quirkbattle.game.quirks.abilities.AbilitySets;
import net.herobrine.quirkbattle.game.stats.PlayerStats;
import net.herobrine.quirkbattle.util.Quirk;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

public class Double extends Class implements Quirk {
    private final List<Ability> abilities;
    private PlayerStats stats;
    private final Arena arena;
    private Player player;
    private final UUID originalId;

    private boolean isStunned = false;

    private boolean explosivePunch = false;

    private boolean isBeingErased = false;

    public Double(UUID uuid) {
        super(uuid, ClassTypes.DOUBLE);
        arena = Manager.getArena(Bukkit.getPlayer(uuid));
        player = Bukkit.getPlayer(uuid);
        this.originalId = uuid;
        this.abilities = new ArrayList<>();
    }


    @Override
    public void onStart(Player player) {
        stats = new PlayerStats(player.getUniqueId(), 200, 200, 40, 50, 300, 40);
        arena.getQuirkBattleGame().getPlayerStatsMap().put(uuid, stats);

        player.getInventory().clear();

        // Basic attack item
        net.herobrine.core.ItemBuilder basicAttack = new net.herobrine.core.ItemBuilder(Material.STICK);
        basicAttack.setDisplayName(ChatColor.GRAY + "Time to double up!");
        basicAttack.setLore(ChatColor.GRAY + "Use your clones to confuse enemies!");

        player.getInventory().setItem(0, basicAttack.build());
        registerAbilities(AbilitySets.DOUBLE);

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
        return explosivePunch;
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

    }

    @Override
    public void registerAbilities(AbilitySets set) {
        int i = 2;
        for (net.herobrine.quirkbattle.game.quirks.abilities.Abilities ability : set.getAbilities()) {
            abilities.add(arena.getQuirkBattleGame().getAbilityManager().registerAbility(ability, this, i));
            i++;
        }
    }

}
