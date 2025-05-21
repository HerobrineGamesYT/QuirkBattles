package net.herobrine.quirkbattle.game.quirks.abilities.hero.engine;

import net.herobrine.gamecore.Class;
import net.herobrine.quirkbattle.QuirkBattlesPlugin;
import net.herobrine.quirkbattle.event.OverheatEvent;
import net.herobrine.quirkbattle.game.quirks.abilities.Abilities;
import net.herobrine.quirkbattle.game.quirks.abilities.Ability;
import net.herobrine.quirkbattle.game.quirks.abilities.SpecialCase;
import net.herobrine.quirkbattle.game.quirks.hero.Engine;
import net.herobrine.quirkbattle.util.Quirk;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

public class EngineBoostAbility extends Ability implements SpecialCase {
    private final Engine engine;

    public EngineBoostAbility(Abilities ability, Quirk quirk, int id, int slot) {
        super(ability, quirk, id, slot);
        engine = (Engine) quirk;
    }


    @Override
    public void doAbility(Player player) {
        engine.addBoost();
        player.getInventory().getItem(slot).setAmount(engine.getActiveBoosts());
        player.playSound(player.getLocation(), Sound.ORB_PICKUP, .6f, .4f + (engine.getActiveBoosts() * .1f));
        player.sendMessage(ChatColor.GREEN + "Boost applied!");
        new BukkitRunnable() {
            int seconds = 0;
            @Override
            public void run() {
                if (engine.getActiveBoosts() == 0) {
                    cancel();
                    return;
                }

                if (seconds == 4) {
                    if(engine.getActiveBoosts() <= 3) engine.removeBoost();
                    cancel();
                    return;
                }
                seconds++;
            }
        }.runTaskTimer(QuirkBattlesPlugin.getInstance(), 0L, 20L);
    }

    @Override
    public boolean doesCasePass(Player player) {
        return engine.getActiveBoosts() < 3;
    }

    @Override
    public void doNoPass(Player player) {
    player.sendMessage(ChatColor.RED + "You already have the max amount of boosts active!");

    }
}
