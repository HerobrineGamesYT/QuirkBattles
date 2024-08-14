package net.herobrine.quirkbattle.game;

import net.herobrine.core.HerobrinePVPCore;
import net.herobrine.core.SongPlayer;
import net.herobrine.core.Songs;
import net.herobrine.gamecore.*;
import net.herobrine.quirkbattle.QuirkBattlesPlugin;
import net.herobrine.quirkbattle.files.Config;
import net.herobrine.quirkbattle.game.stats.PlayerStats;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scoreboard.*;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

public class QuirkBattlesGame {

    private Arena arena;
    private GameType mod;
    private int seconds = 210;
    private long collisionTicks;

    private int endSeconds;

    private HashMap<UUID, PlayerStats> playerStatsMap;

    private HashMap<UUID, CustomDeathCause> customDeathCause = new HashMap<>();

    // Victim UUID -> Attacker UUID
    private HashMap<UUID, UUID> lastAbilityAttacker = new HashMap<>();

    private ArrayList<UUID> alivePlayers = new ArrayList<>();

    private Region region;

    private boolean areRegionsInitialized;

    public QuirkBattlesGame(Arena arena) {
        this.arena = arena;
        this.playerStatsMap = new HashMap<>();
        this.areRegionsInitialized = false;
    }

    public void initRegion() {
        this.region = new Region(Config.getFirstPosition(arena.getID()), Config.getSecondPosition(arena.getID()));
        areRegionsInitialized = true;
    }
    public void start(GameType type) {
        this.mod = type;
        arena.setState(GameState.LIVE);
        playerStatsMap.clear();
        customDeathCause.clear();
        lastAbilityAttacker.clear();
        alivePlayers.clear();
        if (!areRegionsInitialized) initRegion();
        collisionTicks = 0;
        seconds = 210;
        int i = 0;
        for (UUID uuid : arena.getPlayers()) {
            Player player = Bukkit.getPlayer(uuid);
            Scoreboard board = Bukkit.getScoreboardManager().getNewScoreboard();
            Objective obj = board.registerNewObjective("qb", "dummy");
            obj.setDisplayName(ChatColor.GREEN + "Quirk Battles");
            obj.setDisplaySlot(DisplaySlot.SIDEBAR);

            DateFormat df = new SimpleDateFormat("MM/dd/yy");
            Date dateobj = new Date();

            Team dateAndID = board.registerNewTeam("dateandid");
            dateAndID.addEntry(ChatColor.DARK_RED.toString());
            dateAndID.setPrefix(ChatColor.GRAY + df.format(dateobj) + ChatColor.DARK_GRAY + " qb" + arena.getID());
            obj.getScore(ChatColor.DARK_RED.toString()).setScore(8);

            Score blank1 = obj.getScore(" ");
            blank1.setScore(7);

            Team timer = board.registerNewTeam("qbtimer");
            timer.addEntry(ChatColor.LIGHT_PURPLE.toString());
            timer.setPrefix(ChatColor.WHITE + "Time Left: ");
            String time = String.format("%02d:%02d", seconds / 60, seconds % 60);
            timer.setSuffix(ChatColor.GREEN + time);
            obj.getScore(ChatColor.LIGHT_PURPLE.toString()).setScore(6);

            Score blank2 = obj.getScore("  ");
            blank2.setScore(5);

            Score opponent = obj.getScore(HerobrinePVPCore.translateString("&f&lOpponent:"));
            opponent.setScore(4);

            for (UUID uuid1 : arena.getPlayers()) {
                Player player1 = Bukkit.getPlayer(uuid1);
                if (player != player1) {
                    Team opp = board.registerNewTeam("opponent");
                    opp.addEntry(ChatColor.RED.toString());
                    opp.setPrefix(HerobrinePVPCore.getFileManager().getRank(player1).getColor() + player1.getName());
                    opp.setSuffix(" " + ChatColor.GREEN + "100" + ChatColor.RED + "❤");
                    obj.getScore(ChatColor.RED.toString()).setScore(3);
                }
            }
            Score blank3 = obj.getScore("    ");
            blank3.setScore(2);
            Score mode = obj.getScore(ChatColor.WHITE + "Mode: " + mod);
            mode.setScore(1);
            player.setMaxHealth(40.0);
            player.setHealth(40.0);
            player.setScoreboard(board);
            if (i == 0) player.teleport(Config.getSpawnTeam1(arena.getID()));
            else player.teleport(Config.getSpawnTeam2(arena.getID()));
            i++;
            if (arena.getClass(player) == null) {
                arena.setClass(uuid, ClassTypes.ONEFORALL);
                player.sendMessage(HerobrinePVPCore.translateString("&6&lAll Might &r&6has granted you &aOne For All&6! Use it wisely."));
            }
            SongPlayer.playSong(player, Songs.YOU_SAY_RUN);
            alivePlayers.add(uuid);
        }


        for (UUID uuid : arena.getClasses().keySet()) {arena.getClasses().get(uuid).onStart(Bukkit.getPlayer(uuid));}
        arena.sendMessage(
                ChatColor.translateAlternateColorCodes('&', "&a&m&l----------------------------------------"));
        arena.sendMessage(ChatColor.translateAlternateColorCodes('&', "                   &f&lQuirk Battle"));
        arena.sendMessage(ChatColor.translateAlternateColorCodes('&',
                "&e&lFight your opponent using your quirk!\n&e&lLast player standing wins."));
        arena.sendMessage(
                ChatColor.translateAlternateColorCodes('&', "&a&m&l----------------------------------------"));
        startTimer();
        startRegionCollision();
    }

    public void regenPlayerHealth(Player player) {
        int randomNumber = ThreadLocalRandom.current().nextInt(1, 2);

        double regenPercent = randomNumber *.01;

        int health = getStats(player).getHealth();
        int maxHealth = getStats(player).getMaxHealth();

        int newHealth = (int)Math.round((double)maxHealth*regenPercent);

        if (newHealth < 1) newHealth = 1;

        int newHealthReal = newHealth + health;
        if (newHealthReal > maxHealth) newHealthReal = maxHealth;

        getStats(player).setHealth(newHealthReal);
        //double healthPercent = (double)newHealthReal / (double)maxHealth;
        //double playerHealth = player.getMaxHealth() * healthPercent;


        //if(playerHealth != 0) player.setHealth(playerHealth);
    }
    public void updatePlayerStats(Player player) {
        int health = getStats(player).getHealth();
        int defense = getStats(player).getDefense();
        int mana = getStats(player).getMana();
        int intelligence = getStats(player).getIntelligence();

        double healthPercent = (double)health / (double)getStats(player).getMaxHealth();
        double playerHealth = player.getMaxHealth() * healthPercent;

        if(playerHealth > 1) player.setHealth(playerHealth);
        else player.setHealth(2);
        GameCoreMain.getInstance().sendActionBar(player, "&c" + health + "❤   &a" + defense + "❈ Defense   &3" + mana + "/" + intelligence + "⸎ Stamina");
    }

    public ArrayList<UUID> getAlivePlayers() {return alivePlayers;}
    public PlayerStats getStats(Player player) {
        return playerStatsMap.get(player.getUniqueId());
    }
    public HashMap<UUID, PlayerStats> getPlayerStatsMap() {return playerStatsMap;}
    public ClassTypes randomClass() {
        int i = 0;
        do {
            int pick2 = new Random().nextInt(ClassTypes.values().length);
            if (ClassTypes.values()[pick2].getGame().equals(Games.QUIRK_BATTTLE)
                    && !ClassTypes.values()[pick2].isUnlockable()) {
                i = 1;
                return ClassTypes.values()[pick2];
            }
        } while (i != 1);
        return null;
    }

    public void isGameOver() {
        switch (mod) {
            case ONE_V_ONE:
                if (alivePlayers.size() == 1) startEnding(Bukkit.getPlayer(alivePlayers.get(0)));
                if (alivePlayers.size() == 0) startEnding(null);
                break;
            case TWO_V_TWO:
                break;
            case THREE_V_THREE:
                break;
            case FOUR_V_FOUR:
                break;
            case HEROES_VS_VILLAINS:
                break;
            default:
                startEnding(null);
                return;
        }
    }
    public HashMap<UUID, CustomDeathCause> getCustomDeathCause() {return customDeathCause;}
    public HashMap<UUID, UUID> getLastAbilityAttacker() {return lastAbilityAttacker;}

    public void startEnding(Player winner) {
        arena.setState(GameState.LIVE_ENDING);
        for (UUID uuid : arena.getPlayers()) {

            Player player = Bukkit.getPlayer(uuid);
            SongPlayer.stopSong(player);
            if (winner == null) {
                GameCoreMain.getInstance().sendTitle(player, "&e&lDRAW", "The game ended in a draw!", 0, 3, 0);
                SongPlayer.playSong(player, Songs.QBDRAW);
            }
            if (player == winner) {
                GameCoreMain.getInstance().sendTitle(player, "&6&lVICTORY", "&7You're the last one standing!", 0,
                        3, 0);
                HerobrinePVPCore.getFileManager().setGameStats(player.getUniqueId(), Games.QUIRK_BATTTLE, "wins",
                        HerobrinePVPCore.getFileManager().getGameStats(player.getUniqueId(), Games.QUIRK_BATTTLE,
                                "wins") + 1);
                SongPlayer.playSong(player, Songs.QBWIN);
            }
            if (player != winner) {
                GameCoreMain.getInstance().sendTitle(player, "&c&lGAME OVER", "&7Better luck next time!", 0, 3, 0);
                SongPlayer.playSong(player, Songs.QBLOSE);
            }

        }
        arena.sendMessage(ChatColor.translateAlternateColorCodes('&', "&a&m&l----------------------------------------"));
        arena.sendMessage(ChatColor.translateAlternateColorCodes('&', "                   &f&lQuirk Battle - " + mod));
       if (winner != null) arena.sendMessage(HerobrinePVPCore.translateString("&fWinner - " + winner.getName()));
       else arena.sendMessage(ChatColor.WHITE + "Winner - " + ChatColor.YELLOW + "DRAW!");
       if(winner != null) arena.distributeRewards(winner.getUniqueId());
       else arena.distributeRewards((UUID)null);
       arena.sendMessage(ChatColor.translateAlternateColorCodes('&', "&a&m&l----------------------------------------"));

       endSeconds = 5;
        new BukkitRunnable() {
            @Override
            public void run() {
                if (endSeconds == 0) {
                    cancel();
                    arena.reset();
                }
                endSeconds--;
            }
        }.runTaskTimer(QuirkBattlesPlugin.getInstance(), 0L, 20L);
    }
    public void startTimer() {
        new BukkitRunnable() {
            @Override
            public void run() {
                if (!arena.getState().equals(GameState.LIVE)) cancel();
                if (seconds < 0) {
                    cancel();
                    startEnding(null);
                    return;
                }
                String time = String.format("%02d:%02d", seconds / 60, seconds % 60);
                for (UUID uuid : arena.getPlayers()) {
                    Player player = Bukkit.getPlayer(uuid);
                    updatePlayerStats(player);
                    if (player.getScoreboard().getObjective(DisplaySlot.SIDEBAR).getDisplayName().contains("Quirk Battles")) {
                        player.getScoreboard().getTeam("qbtimer").setSuffix(ChatColor.GREEN + time);
                        if (player.getScoreboard().getTeam("opponent") != null) {
                            Player opp = Bukkit.getPlayer(ChatColor.stripColor(player.getScoreboard().getTeam("opponent").getPrefix()));
                            player.getScoreboard().getTeam("opponent").setSuffix(" " + ChatColor.GREEN + getStats(opp).getHealth() + ChatColor.RED + "❤");
                        }
                        //TODO update player health on sb.
                        if(getStats(player).getHealth() < getStats(player).getMaxHealth() && seconds % 2 == 0) regenPlayerHealth(player);
                    }
                }
                seconds--;
            }
        }.runTaskTimer(QuirkBattlesPlugin.getInstance(), 0L, 20L);
    }


    public void startRegionCollision() {
        new BukkitRunnable() {
            @Override
            public void run() {
                if (!arena.getState().equals(GameState.LIVE)) {
                    cancel();
                    return;
                }

                if (seconds <= 0) {
                    cancel();
                    return;
                }

                for (UUID uuid: arena.getPlayers()) {
                    Player player = Bukkit.getPlayer(uuid);
                    if (!region.containsLocation(player.getLocation()) && collisionTicks % 10 == 0) {
                        EntityDamageEvent event = new EntityDamageEvent(player, EntityDamageEvent.DamageCause.CUSTOM, 50);
                        customDeathCause.put(player.getUniqueId(), CustomDeathCause.OUTSIDE_MAP);
                        player.sendMessage(ChatColor.RED + "Get back in the playing area!");
                        player.setLastDamageCause(event);
                        Bukkit.getPluginManager().callEvent(event);

                    }
                }
                collisionTicks++;
            }
        }.runTaskTimer(QuirkBattlesPlugin.getInstance(), 0L, 1L);
    }

}