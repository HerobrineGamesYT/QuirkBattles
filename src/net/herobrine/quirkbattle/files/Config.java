package net.herobrine.quirkbattle.files;

import net.herobrine.quirkbattle.QuirkBattlesPlugin;
import org.bukkit.Bukkit;
import org.bukkit.Location;

public class Config {

    private static QuirkBattlesPlugin main;
    public Config(QuirkBattlesPlugin main) {
        Config.main = main;
        main.saveDefaultConfig();
    }

    public static Location getSpawn(int id) {
        return new Location(Bukkit.getWorld(main.getConfig().getString("arenas." + id + ".spawn.world")),
                main.getConfig().getDouble("arenas." + id + ".spawn.x"),
                main.getConfig().getDouble("arenas." + id + ".spawn.y"), main.getConfig().getDouble("arenas." + id + ".spawn.z"),
                main.getConfig().getInt("arenas." + id + ".spawn.pitch"), main.getConfig().getInt("arenas." + id + ".spawn.yaw"));
    }

    public static Location getSpawnTeam1(int id) {
        return new Location(Bukkit.getWorld(main.getConfig().getString("arenas." + id + ".team1spawn.world")),
                main.getConfig().getDouble("arenas." + id + ".team1spawn.x"),
                main.getConfig().getDouble("arenas." + id + ".team1spawn.y"), main.getConfig().getDouble("arenas." + id + ".team1spawn.z"),
                main.getConfig().getInt("arenas." + id + ".team1spawn.pitch"), main.getConfig().getInt("arenas." + id + ".team1spawn.yaw"));
    }

    public static Location getSpawnTeam2(int id) {
        return new Location(Bukkit.getWorld(main.getConfig().getString("arenas." + id + ".team2spawn.world")),
                main.getConfig().getDouble("arenas." + id + ".team2spawn.x"),
                main.getConfig().getDouble("arenas." + id + ".team2spawn.y"), main.getConfig().getDouble("arenas." + id + ".team2spawn.z"),
                main.getConfig().getInt("arenas." + id + ".team2spawn.pitch"), main.getConfig().getInt("arenas." + id + ".team2spawn.yaw"));
    }

    public static Location getFirstPosition(int id) {
        return new Location(Bukkit.getWorld(main.getConfig().getString("arenas." + id + ".region.first-position.world")), main.getConfig().getDouble("arenas." + id + ".region.first-position.x"),
                main.getConfig().getDouble("arenas." + id + ".region.first-position.y"), main.getConfig().getDouble("arenas." + id + ".region.first-position.z"));
    }

    public static Location getSecondPosition(int id) {
        return new Location(Bukkit.getWorld(main.getConfig().getString("arenas." + id + ".region.second-position.world")), main.getConfig().getDouble("arenas." + id + ".region.second-position.x"),
                main.getConfig().getDouble("arenas." + id + ".region.second-position.y"), main.getConfig().getDouble("arenas." + id + ".region.second-position.z"));
    }

    public static Location getBossSpawn(int id) {
        return null;
    }
}
