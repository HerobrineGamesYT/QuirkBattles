package net.herobrine.quirkbattle.game;

import net.herobrine.core.HerobrinePVPCore;
import net.herobrine.core.SongPlayer;
import net.herobrine.core.Songs;
import net.herobrine.gamecore.*;
import net.herobrine.quirkbattle.QuirkBattlesPlugin;
import net.herobrine.quirkbattle.files.Config;
import net.herobrine.quirkbattle.game.quirks.abilities.QuirkAbilityManager;
import net.herobrine.quirkbattle.game.stats.PlayerStats;
import org.bukkit.*;
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

    private final Map<UUID, PlayerStats> playerStatsMap = new HashMap<>();

    private final Map<UUID, CustomDeathCause> customDeathCause = new HashMap<>();

    // Victim UUID -> Attacker UUID
    private final Map<UUID, UUID> lastAbilityAttacker = new HashMap<>();

    private final List<UUID> alivePlayers = new ArrayList<>();
    private final Map<UUID, UUID[]> opponents = new HashMap<>();

    private Region region;
    private QuirkAbilityManager abilityManager;

    private boolean areRegionsInitialized;

    private WorldBorder border;

    private int aliveRedPlayers = 0;
    private int aliveBluePlayers = 0;
    private int aliveHeroPlayers = 0;
    private int aliveVillainPlayers = 0;


    public QuirkBattlesGame(Arena arena) {
        this.arena = arena;
        this.areRegionsInitialized = false;
        this.abilityManager = new QuirkAbilityManager(arena.getID());
        this.border = arena.getSpawn().getWorld().getWorldBorder();
    }

    public void initRegion() {
        this.region = new Region(Config.getFirstPosition(arena.getID()), Config.getSecondPosition(arena.getID()));
        areRegionsInitialized = true;
    }
    public void start(GameType type) {
        this.mod = type;
        this.aliveRedPlayers = 0;
        this.aliveBluePlayers = 0;
        this.aliveHeroPlayers = 0;
        this.aliveVillainPlayers = 0;
        arena.setState(GameState.LIVE);
        playerStatsMap.clear();
        customDeathCause.clear();
        lastAbilityAttacker.clear();
        alivePlayers.clear();
        opponents.clear();
        resetWorldBorder();
        if (!areRegionsInitialized) initRegion();
        collisionTicks = 0;
        seconds = 210;

        if (mod != GameType.ONE_V_ONE && mod != GameType.FFA) {
            startTeamsGame();
            return;
        }

        if (mod == GameType.FFA) {
            startFFAGame();
            return;
        }

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
                    String name  = player1.getName().substring(0, Math.min(player1.getName().length(), 13));
                    opp.setPrefix(HerobrinePVPCore.getFileManager().getRank(player1).getColor() + name);
                    opp.setSuffix(" " + ChatColor.GREEN + "100" + ChatColor.RED + "❤");
                    obj.getScore(ChatColor.RED.toString()).setScore(3);
                }
            }
            Score blank3 = obj.getScore("    ");
            blank3.setScore(2);
            Score mode = obj.getScore(ChatColor.WHITE + "Mode: " + mod.getDisplay());
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

    public void createFFAScoreboard() {

    }



    public void startTeamsGame() {
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
            obj.getScore(ChatColor.DARK_RED.toString()).setScore(11);

            Score blank1 = obj.getScore(" ");
            blank1.setScore(10);

            Team timer = board.registerNewTeam("qbtimer");
            timer.addEntry(ChatColor.LIGHT_PURPLE.toString());
            timer.setPrefix(ChatColor.WHITE + "Time Left: ");
            String time = String.format("%02d:%02d", seconds / 60, seconds % 60);
            timer.setSuffix(ChatColor.GREEN + time);
            obj.getScore(ChatColor.LIGHT_PURPLE.toString()).setScore(9);

            Score blank2 = obj.getScore("  ");
            blank2.setScore(8);

            Score opponent = obj.getScore(HerobrinePVPCore.translateString("&f&lOpponents:"));
            opponent.setScore(7);

            int score = 6;
            int loop = 0;
            List<UUID> opps = new ArrayList<>();
            ChatColor[] colors = new ChatColor[] {ChatColor.BLUE, ChatColor.DARK_PURPLE, ChatColor.BLACK, ChatColor.DARK_GRAY, ChatColor.DARK_GREEN};
            for (UUID uuid1 : arena.getPlayers()) {
                Player player1 = Bukkit.getPlayer(uuid1);
                if (arena.getTeam(player) != arena.getTeam(player1)) {
                    Team opp = board.registerNewTeam("opponent" + loop);
                    opp.addEntry(colors[loop].toString());
                    String name  = player1.getName().substring(0, Math.min(player1.getName().length(), 13));
                    opp.setPrefix(arena.getTeam(player1).getColor() + name);
                    opp.setSuffix(" " + ChatColor.GREEN + "100" + ChatColor.RED + "❤");
                    obj.getScore(colors[loop].toString()).setScore(score);
                    opps.add(player1.getUniqueId());
                    score--;
                    loop++;
                }
            }
            if(!opps.isEmpty()) opponents.put(uuid, opps.toArray(new UUID[0]));
            opps.clear();
            Score blank3 = obj.getScore("    ");
            blank3.setScore(2);
            Score mode = obj.getScore(ChatColor.WHITE + "Mode: " + mod.getDisplay());
            mode.setScore(1);

            int nameCount = 0;
            Team redTeam = board.registerNewTeam("redTeam");
            redTeam.setDisplayName(ChatColor.RED + "RED");
            redTeam.setPrefix(ChatColor.RED + "RED ");
            redTeam.setAllowFriendlyFire(false);

            Team blueTeam = board.registerNewTeam("blueTeam");

            blueTeam.setDisplayName(ChatColor.BLUE + "BLUE");
            blueTeam.setPrefix(ChatColor.BLUE + "BLUE ");
            blueTeam.setAllowFriendlyFire(false);

            Team heroTeam = board.registerNewTeam("heroTeam");

            heroTeam.setDisplayName(Teams.HERO.getDisplay());
            heroTeam.setPrefix(Teams.HERO.getDisplay().toUpperCase() + " ");
            heroTeam.setAllowFriendlyFire(false);

            Team villainTeam = board.registerNewTeam("villainTeam");

            villainTeam.setDisplayName(Teams.VILLAIN.getDisplay().toUpperCase());
            villainTeam.setPrefix(Teams.VILLAIN.getDisplay().toUpperCase() + " ");
            villainTeam.setAllowFriendlyFire(false);

            if (arena.getTeam(player).equals(Teams.RED)) aliveRedPlayers = aliveRedPlayers + 1;
            else if (arena.getTeam(player).equals(Teams.BLUE)) aliveBluePlayers = aliveBluePlayers + 1;
            else if (arena.getTeam(player).equals(Teams.HERO)) aliveHeroPlayers = aliveHeroPlayers + 1;
            else if (arena.getTeam(player).equals(Teams.VILLAIN)) aliveVillainPlayers = aliveVillainPlayers + 1;

            for (UUID uuid1 : arena.getPlayers()) {
                Player player1 = Bukkit.getPlayer(uuid1);
                if (arena.getTeam(player1).equals(Teams.RED)) redTeam.addPlayer(player1);
                else if (arena.getTeam(player1).equals(Teams.BLUE)) blueTeam.addPlayer(player1);
                else if (arena.getTeam(player1).equals(Teams.HERO)) heroTeam.addPlayer(player1);
                else if (arena.getTeam(player1).equals(Teams.VILLAIN)) villainTeam.addPlayer(player1);
                nameCount++;
            }


            player.setMaxHealth(40.0);
            player.setHealth(40.0);
            player.setScoreboard(board);
            if (mod.getAvailableTeams()[0].equals(arena.getTeam(player))) player.teleport(Config.getSpawnTeam1(arena.getID()));
            else player.teleport(Config.getSpawnTeam2(arena.getID()));
            i++;
            if (!Manager.hasKit(player)) {
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
                "&e&lFight the enemy team using your quirks!\n&e&lLast team standing wins."));
        arena.sendMessage(
                ChatColor.translateAlternateColorCodes('&', "&a&m&l----------------------------------------"));
        startTimer();
        startRegionCollision();
    }

    public void startFFAGame() {
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
            obj.getScore(ChatColor.DARK_RED.toString()).setScore(11);

            Score blank1 = obj.getScore(" ");
            blank1.setScore(10);

            Team timer = board.registerNewTeam("qbtimer");
            timer.addEntry(ChatColor.LIGHT_PURPLE.toString());
            timer.setPrefix(ChatColor.WHITE + "Time Left: ");
            String time = String.format("%02d:%02d", seconds / 60, seconds % 60);
            timer.setSuffix(ChatColor.GREEN + time);
            obj.getScore(ChatColor.LIGHT_PURPLE.toString()).setScore(9);

            Score blank2 = obj.getScore("  ");
            blank2.setScore(8);

            Score opponent = obj.getScore(HerobrinePVPCore.translateString("&f&lOpponents:"));
            opponent.setScore(7);

            int score = 6;
            int loop = 0;
            List<UUID> opps = new ArrayList<>();
            ChatColor[] colors = new ChatColor[] {ChatColor.BLUE, ChatColor.DARK_PURPLE, ChatColor.BLACK, ChatColor.DARK_GRAY, ChatColor.DARK_GREEN};
            for (UUID uuid1 : arena.getPlayers()) {
                Player player1 = Bukkit.getPlayer(uuid1);
                if (player != player1) {
                    Team opp = board.registerNewTeam("opponent" + loop);
                    opp.addEntry(colors[loop].toString());
                    String name  = player1.getName().substring(0, Math.min(player1.getName().length(), 13));
                    opp.setPrefix(HerobrinePVPCore.getRankColor(player1) + name);
                    opp.setSuffix(" " + ChatColor.GREEN + "100" + ChatColor.RED + "❤");
                    obj.getScore(colors[loop].toString()).setScore(score);
                    opps.add(player1.getUniqueId());
                    score--;
                    loop++;
                }
            }
            if(!opps.isEmpty()) opponents.put(uuid, opps.toArray(new UUID[0]));
            opps.clear();
            Score blank3 = obj.getScore("    ");
            blank3.setScore(2);
            Score mode = obj.getScore(ChatColor.WHITE + "Mode: " + mod.getDisplay());
            mode.setScore(1);

            int nameCount = 0;

            player.setMaxHealth(40.0);
            player.setHealth(40.0);
            player.setScoreboard(board);
            switch (i) {
                case 0:
                    player.teleport(Config.getSpawnTeam1(arena.getID()));
                    break;
                case 1:
                    player.teleport(Config.getSpawnTeam2(arena.getID()));
                    break;
                case 2:
                    player.teleport(Config.getSpawnTeam3(arena.getID()));
                    break;
                case 3:
                    player.teleport(Config.getSpawnTeam4(arena.getID()));
                    break;
                default:
                    player.teleport(Config.getSpawnTeam1(arena.getID()));
                    player.sendMessage(ChatColor.RED + "You were teleported to the default spawn location because there are more than 4 players!");
                    break;
            }
            i++;
            if (!Manager.hasKit(player)) {
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
                "&e&lFight all your opponents using your quirk!\n&e&lLast player standing wins."));
        arena.sendMessage(
                ChatColor.translateAlternateColorCodes('&', "&a&m&l----------------------------------------"));
        startTimer();
        startRegionCollision();
    }

    public QuirkAbilityManager getAbilityManager() {return abilityManager;}

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
    }
    public void updatePlayerStats(Player player) {
        if (!player.isOnline()) return;
        if (!arena.getPlayers().contains(player.getUniqueId())) return;
        int health = getStats(player).getHealth();
        int defense = getStats(player).getDefense();
        int mana = getStats(player).getMana();
        int intelligence = getStats(player).getIntelligence();

        double healthPercent = (double)health / (double)getStats(player).getMaxHealth();
        double playerHealth = player.getMaxHealth() * healthPercent;
        if(playerHealth > 1) player.setHealth(playerHealth);
        else player.setHealth(2);
        if (!getStats(player).useTemperature()) GameCoreMain.getInstance().sendActionBar(player, "&c" + health + "❤   &a" + defense + "❈ Defense   &3" + mana + "/" + intelligence + "⸎ Stamina");
        else {
            ChatColor color;
            int temp = getStats(player).getTemp();
            int maxTemp = getStats(player).getMaxTemp();
            int baseTemp = getStats(player).getBaseTemp();
            if (temp > baseTemp) color = ChatColor.RED;
            else if (temp < baseTemp) color = ChatColor.AQUA;
            else color = ChatColor.YELLOW;
            GameCoreMain.getInstance().sendActionBar(player, "&c" + health + "❤   &a" + defense + "❈ Defense   " + color  + temp + "/" + maxTemp + "❄ Temperature");
        }
    }

    public List<UUID> getAlivePlayers() {return alivePlayers;}
    public PlayerStats getStats(Player player) {
        return playerStatsMap.get(player.getUniqueId());
    }
    public Map<UUID, PlayerStats> getPlayerStatsMap() {return playerStatsMap;}
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
            case FFA:
            case ONE_V_ONE:
                if (alivePlayers.size() == 1) startEnding(Bukkit.getPlayer(alivePlayers.get(0)));
                if (alivePlayers.size() == 0) startEnding((Player) null);
                break;
            case TWO_V_TWO:
            case FOUR_V_FOUR:
            case THREE_V_THREE:
                if (aliveRedPlayers == 0 && aliveBluePlayers != 0) startEnding(Teams.BLUE);
                else if(aliveRedPlayers != 0) startEnding(Teams.RED);
                else startEnding((Teams)null);
                break;
            case HEROES_VS_VILLAINS:
                break;
            default:
                startEnding((Player) null);
                return;
        }
    }
    public Map<UUID, CustomDeathCause> getCustomDeathCause() {return customDeathCause;}
    public Map<UUID, UUID> getLastAbilityAttacker() {return lastAbilityAttacker;}

    public void removeAlivePlayer(Teams team) {
        switch(team) {
            case RED:
                aliveRedPlayers = aliveRedPlayers - 1;
                break;
            case BLUE:
                aliveBluePlayers = aliveBluePlayers - 1;
                break;
            case HERO:
                aliveHeroPlayers = aliveHeroPlayers - 1;
                break;
            case VILLAIN:
                aliveVillainPlayers = aliveVillainPlayers - 1;
                break;
            default: return;
        }
        if (aliveRedPlayers == 0 || aliveBluePlayers == 0) isGameOver();
    }

    public void startEnding(Player winner) {
        arena.setState(GameState.LIVE_ENDING);
        resetWorldBorder();
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
                    startEnding((Player) null);
                    return;
                }

                if (seconds == 90) startWorldBorder();

                String time = String.format("%02d:%02d", seconds / 60, seconds % 60);
                for (UUID uuid : arena.getPlayers()) {
                    Player player = Bukkit.getPlayer(uuid);
                   if (getAlivePlayers().contains(player.getUniqueId())) updatePlayerStats(player);
                   if (player.getScoreboard().getObjective(DisplaySlot.SIDEBAR).getDisplayName().contains("Quirk Battles")) {
                        player.getScoreboard().getTeam("qbtimer").setSuffix(ChatColor.GREEN + time);
                        if (mod.equals(GameType.ONE_V_ONE)) {
                            if (player.getScoreboard().getTeam("opponent") != null) {
                                Player opp = Bukkit.getPlayer(ChatColor.stripColor(player.getScoreboard().getTeam("opponent").getPrefix()));
                                if(playerStatsMap.get(opp.getUniqueId()).getHealth() == 0) player.getScoreboard().getTeam("opponent").setSuffix(" " + ChatColor.RED + "DEAD");
                                else player.getScoreboard().getTeam("opponent").setSuffix(" " + ChatColor.GREEN + getStats(opp).getHealth() + ChatColor.RED + "❤");
                            }
                        }

                        else {
                            int i = 0;
                            if (!opponents.isEmpty()) {
                                for (UUID uuid1 : opponents.get(uuid)) {
                                    if(playerStatsMap.get(uuid1).getHealth() == 0) player.getScoreboard().getTeam("opponent" + i).setSuffix(" " + ChatColor.RED + "DEAD");
                                    else player.getScoreboard().getTeam("opponent" + i).setSuffix(" " + ChatColor.GREEN + playerStatsMap.get(uuid1).getHealth() + ChatColor.RED + "❤");
                                    i++;
                                }
                            }
                        }

                        if(getStats(player).getHealth() < getStats(player).getMaxHealth() && seconds % 2 == 0 && getAlivePlayers().contains(player.getUniqueId())) regenPlayerHealth(player);
                    }
                }
                seconds--;
            }
        }.runTaskTimer(QuirkBattlesPlugin.getInstance(), 0L, 20L);
    }

    public void startWorldBorder() {
        // Will be configurable once it's decided this feature will be kept.
        border.setSize(100);
        arena.playSound(Sound.ENDERDRAGON_GROWL);
        arena.sendMessage(HerobrinePVPCore.translateString("&c&lThe World Border is now shrinking..."));
        arena.sendTitle("&c&lCAUTION", "&eThe border is shrinking!", 0,1,0);
        border.setSize(10, 60);
    }

    public void resetWorldBorder() {
        border.setCenter(1040.5, -447.5);
        border.setSize(1000);
    }


    public void startEnding(Teams winner) {
        arena.setState(GameState.LIVE_ENDING);
        resetWorldBorder();
        for (UUID uuid : arena.getPlayers()) {
            Player player = Bukkit.getPlayer(uuid);
            SongPlayer.stopSong(player);

            if (winner == null) {
                GameCoreMain.getInstance().sendTitle(player, "&e&lDRAW", "The game ended in a draw!", 0, 3, 0);
                SongPlayer.playSong(player, Songs.QBDRAW);
            }

            if (arena.getTeam(player) == winner) {
                GameCoreMain.getInstance().sendTitle(player, "&6&lVICTORY", "&7Your team is the last one alive!", 0,
                        3, 0);
                HerobrinePVPCore.getFileManager().setGameStats(player.getUniqueId(), Games.QUIRK_BATTTLE, "wins",
                        HerobrinePVPCore.getFileManager().getGameStats(player.getUniqueId(), Games.QUIRK_BATTTLE,
                                "wins") + 1);
                SongPlayer.playSong(player, Songs.QBWIN);
            }
            if (arena.getTeam(player) != winner) {
                GameCoreMain.getInstance().sendTitle(player, "&c&lGAME OVER", "&7Better luck next time!", 0, 3, 0);
                SongPlayer.playSong(player, Songs.QBLOSE);
            }

        }
        arena.sendMessage(ChatColor.translateAlternateColorCodes('&', "&a&m&l----------------------------------------"));
        arena.sendMessage(ChatColor.translateAlternateColorCodes('&', "                   &f&lQuirk Battle - " + mod));
        if (winner != null) arena.sendMessage(HerobrinePVPCore.translateString("&fWinner - " + winner.getDisplay()));
        else arena.sendMessage(ChatColor.WHITE + "Winner - " + ChatColor.YELLOW + "DRAW!");
        if (winner != null) arena.distributeRewards(winner);
        else arena.distributeRewards((UUID) null);
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

    public boolean isOutsideOfBorder(Player p) {
        Location loc = p.getLocation();
        WorldBorder border = p.getWorld().getWorldBorder();
        double size = border.getSize()/2;
        Location center = border.getCenter();
        double x = loc.getX() - center.getX(), z = loc.getZ() - center.getZ();
        return ((x > size || (-x) > size) || (z > size || (-z) > size));
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
                    if (arena.getSpectators().contains(uuid)) continue;
                    if (!region.containsLocation(player.getLocation()) || isOutsideOfBorder(player)) {
                        if (collisionTicks % 10 != 0) continue;
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
