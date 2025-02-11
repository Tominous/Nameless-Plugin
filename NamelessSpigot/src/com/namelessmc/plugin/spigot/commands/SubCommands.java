package com.namelessmc.plugin.spigot.commands;

import java.util.ArrayList;

import org.bukkit.command.CommandSender;

import com.namelessmc.plugin.spigot.Chat;
import com.namelessmc.plugin.spigot.Config;
import com.namelessmc.plugin.spigot.Message;

import xyz.derkades.derkutils.ListUtils;

public class SubCommands extends org.bukkit.command.Command {

	public SubCommands() {
		super(Config.COMMANDS.getConfig().getString("subcommands-name"),
				"", 
				"/" + Message.COMMAND_SUBCOMMANDS_USAGE.getMessage("command", Config.COMMANDS.getConfig().getString("subcommands-name")), 
				new ArrayList<>());
	}

	@Override
	public boolean execute(CommandSender sender, String label, String[] args) {
		if (args.length > 0) {
			String subcommand = args[0];
			
			for (Command command : Command.COMMANDS) {
				if (command.getName().equalsIgnoreCase(subcommand)) {
					return command.execute(sender, label, ListUtils.removeFirstStringFromArray(args));
				}
			}
		}
		
		String separator = Chat.convertColors("&3&m--------------------------------");

		sender.sendMessage(separator);
		sender.sendMessage("");
		
		for (Command command : Command.COMMANDS) {
			if (command.getName().equals("disabled")) {
				continue;
			}
			
			sender.sendMessage(Message.COMMAND_SUBCOMMANDS_HELP_PREFIX.getMessage("command", this.getName()) 
					+ " " + command.getUsageWithoutSlash());
			sender.sendMessage(command.getDescription());
			sender.sendMessage("");
		}
		
		sender.sendMessage(separator);
		
		return true;
	}

}