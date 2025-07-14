package net.herobrine.quirkbattle.commands;

import net.herobrine.core.HerobrinePVPCore;
import net.herobrine.core.Ranks;
import net.herobrine.gamecore.Arena;
import net.herobrine.gamecore.Games;
import net.herobrine.gamecore.Manager;
import net.herobrine.gamecore.Teams;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class DevCommand implements CommandExecutor {
    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (sender instanceof Player) {
            Player player = (Player) sender;
            Ranks rank = HerobrinePVPCore.getRank(player);

            if (rank.getPermLevel() < 9) {
                player.sendMessage(ChatColor.RED + "You do not have permission to use this!");
                return false;
            }

            if (!Manager.isPlaying(player)) {
                player.sendMessage(ChatColor.RED + "You must be in a game of Quirk Battles to use this command.");
                return false;
            }

            Arena arena = Manager.getArena(player);

            if (arena.getGame() != Games.QUIRK_BATTTLE) {
                player.sendMessage(ChatColor.RED + "You must be in a game of Quirk Battles to use this command.");
                return false;
            }

            if (args.length < 2) {
                printHelpMessage(player);
                return false;
            }

            String commandType = args[0];

            if (commandType.equalsIgnoreCase("changeteam")) {
                if (!arena.getType().isTeamsMode()) {
                    player.sendMessage(ChatColor.RED + "This subcommand is only available in Teams modes!");
                    return false;
                }

                try {
                    Teams team = Teams.valueOf(args[1].toUpperCase());
                    arena.setTeam(player, team);
                    player.sendMessage(ChatColor.GREEN + "You have successfully changed your team to: " + team.getDisplay());
                }

                catch(Exception e) {
                    player.sendMessage(ChatColor.RED + "Invalid team!");
                }
            }

            else if (commandType.equalsIgnoreCase("changequirk")) {
                player.sendMessage(ChatColor.RED + "This is not implemented yet!");
            }
            else {
                player.sendMessage(ChatColor.RED + "Invalid subcommand!");
                printHelpMessage(player);
            }
        }
        else sender.sendMessage(ChatColor.RED + "Only players can use this command!");


        return false;
    }


    public void printHelpMessage(Player player) {
        player.sendMessage(ChatColor.GREEN + "Quirk Battles Dev Tools");
        player.sendMessage(HerobrinePVPCore.translateString("&a&l- /qbdev changeteam <team>"));
        player.sendMessage(HerobrinePVPCore.translateString("&a&l- /qbdev changequirk <quirk>"));
    }
}
