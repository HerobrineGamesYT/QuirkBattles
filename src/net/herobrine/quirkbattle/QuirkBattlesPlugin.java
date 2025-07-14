package net.herobrine.quirkbattle;

import net.herobrine.core.HerobrinePVPCore;
import net.herobrine.gamecore.GameCoreMain;
import net.herobrine.quirkbattle.commands.DevCommand;
import net.herobrine.quirkbattle.event.QuirkBattlesListener;
import net.herobrine.quirkbattle.menus.ModeSelector;
import net.herobrine.quirkbattle.menus.QuirkSelector;
import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.logging.Level;

public class QuirkBattlesPlugin extends JavaPlugin {
    private static QuirkBattlesPlugin instance;

    @Override
    public void onEnable() {
        instance = this;
        if (getGameCoreAPI() == null || getCustomAPI() == null) {
            getLogger().log(Level.WARNING, "[QUIRK BATTLES] You can't use this plugin without HBPVP Core and GameCore!");
            Bukkit.getServer().getPluginManager().disablePlugin(this);
            return;
        }

        saveDefaultConfig();

        getCommand("qbdev").setExecutor(new DevCommand());

        Bukkit.getPluginManager().registerEvents(new QuirkBattlesListener(), this);
        Bukkit.getPluginManager().registerEvents(new QuirkSelector(), this);
        Bukkit.getPluginManager().registerEvents(new ModeSelector(), this);
    }

    public static QuirkBattlesPlugin getInstance() {
        return instance;
    }

    private GameCoreMain getGameCoreAPI() {
        Plugin plugin = Bukkit.getServer().getPluginManager().getPlugin("GameCore");
        return plugin instanceof GameCoreMain ? (GameCoreMain) plugin : null;
    }

    private HerobrinePVPCore getCustomAPI() {
        Plugin plugin = Bukkit.getServer().getPluginManager().getPlugin("HBPVP-Core");
        return plugin instanceof HerobrinePVPCore ? (HerobrinePVPCore) plugin : null;
    }
}
