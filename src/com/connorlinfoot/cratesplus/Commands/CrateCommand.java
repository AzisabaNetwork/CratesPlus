package com.connorlinfoot.cratesplus.Commands;

import com.connorlinfoot.cratesplus.CratesPlus;
import com.connorlinfoot.cratesplus.Handlers.CrateHandler;
import com.connorlinfoot.cratesplus.Handlers.MessageHandler;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class CrateCommand implements CommandExecutor {

    @Override
    public boolean onCommand(CommandSender sender, Command command, String string, String[] args) {
        if (sender instanceof Player && !sender.hasPermission("cratesplus.admin")) {
            sender.sendMessage(CratesPlus.pluginPrefix + MessageHandler.getMessage(CratesPlus.getPlugin(), "Command No Permission", (Player) sender, "Unknown"));
            return false;
        }

        if (args.length >= 1 && args[0].equalsIgnoreCase("key")) {
            if (args.length < 2) {
                sender.sendMessage(ChatColor.RED + "Correct Usage: /crate key <player> [type]");
                return false;
            }

            Player player = Bukkit.getPlayer(args[1]);
            if (player == null) {
                sender.sendMessage(ChatColor.RED + "The player " + args[1] + " was not found");
                return false;
            }

            if (args.length >= 3) {
                CrateHandler.giveCrateKey(player, args[2]);
            } else {
                CrateHandler.giveCrateKey(player);
            }
            sender.sendMessage(ChatColor.GREEN + "Given " + player.getDisplayName() + ChatColor.RESET + ChatColor.GREEN + " a crate key");
            return true;
        }

        if (args.length >= 1 && args[0].equalsIgnoreCase("crate")) {
            Player player;
            Integer amount = null;
            String crateType;
            if (args.length == 1) {
                sender.sendMessage(ChatColor.RED + "Correct Usage: /crate crate <type> [player] [amount]");
                return false;
            }

            if (args.length == 4) {
                try {
                    amount = Integer.parseInt(args[3]);
                } catch (Exception ignore) {
                    sender.sendMessage(ChatColor.RED + "Please enter a valid number");
                    return false;
                }
                player = Bukkit.getPlayer(args[2]);
            } else if (args.length == 3) {
                player = Bukkit.getPlayer(args[2]);
            } else if (sender instanceof Player) {
                player = (Player) sender;
            } else {
                sender.sendMessage(ChatColor.RED + "Correct Usage: /crate crate <type> [player] [amount]");
                return false;
            }

            if (player == null) {
                sender.sendMessage(ChatColor.RED + "The player " + args[1] + " was not found");
                return false;
            }

            try {
                crateType = args[1];
            } catch (IllegalArgumentException e) {
                sender.sendMessage(ChatColor.RED + "Please specify a valid crate type");
                return false;
            }

            if (amount != null) {
                for (Integer i = 0; i <= amount; i++) {
                    CrateHandler.giveCrate(player, crateType);
                }
            } else {
                CrateHandler.giveCrate(player, crateType);
            }
            sender.sendMessage(ChatColor.GREEN + "Given " + player.getDisplayName() + ChatColor.RESET + ChatColor.GREEN + " a crate");
            return true;
        }

        // Help Messages
        sender.sendMessage(CratesPlus.pluginPrefix + ChatColor.AQUA + "----- CratePlus Help -----");
        sender.sendMessage(CratesPlus.pluginPrefix + ChatColor.AQUA + "/crate key <player> [type] - Give player a random crate key");
        sender.sendMessage(CratesPlus.pluginPrefix + ChatColor.AQUA + "/crate crate <type> [player] [amount] - Give player a crate to be placed");

        return true;
    }

}
