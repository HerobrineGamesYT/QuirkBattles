package net.herobrine.quirkbattle.game.quirks.hero;

import net.herobrine.gamecore.Arena;
import net.herobrine.gamecore.Class;
import net.herobrine.gamecore.ClassTypes;
import net.herobrine.gamecore.Manager;
import net.herobrine.quirkbattle.event.QuirkErasureEvent;
import net.herobrine.quirkbattle.game.quirks.abilities.Ability;
import net.herobrine.quirkbattle.game.quirks.abilities.AbilitySets;
import net.herobrine.quirkbattle.game.stats.PlayerStats;
import net.herobrine.quirkbattle.util.Quirk;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class Engine extends Class implements Quirk {
    private final List<Ability> abilities;
    private PlayerStats stats;
    private final Arena arena;
    private final Player player;

    private boolean isBeingErased = false;

    public Engine(UUID uuid) {
        super(uuid, ClassTypes.ENGINE);
        arena = Manager.getArena(Bukkit.getPlayer(uuid));
        player = Bukkit.getPlayer(uuid);
        this.abilities = new ArrayList<>();
    }

    @Override
    public void onStart(Player player) {

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
        return false;
    }

    @Override
    public void useAbilityAttack(Player target) {

    }

    @Override
    public void registerAbilities(AbilitySets set) {

    }

    @EventHandler
    public void onErase(QuirkErasureEvent e) {
        if (e.getQuirk() != this) return;
        if (e.isErasing()) isBeingErased = true;
        else isBeingErased = false;
    }
}
