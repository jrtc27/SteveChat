package com.jrtc27.stevechat.command;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.util.StringUtil;

import com.google.common.collect.ImmutableList;
import com.jrtc27.stevechat.MessageColor;
import com.jrtc27.stevechat.SCPermission;
import com.jrtc27.stevechat.SteveChatPlugin;

public class HelpCommand extends ChatCommandBase {

	public HelpCommand(final SteveChatPlugin plugin) {
		super(plugin);
		this.permission = SCPermission.HELP;
		this.usage = " [page|command]";
		this.mainCommand = "help";
		this.aliases = new String[] { "?", "h" };
		this.description = "View the help for SteveChat.";
	}

	@Override
	public boolean handleCommand(final CommandSender sender, final String label, final String subCommand, final String[] args) {
		if (!this.testPermission(sender)) return true;

		if (args.length > 1) {
			sender.sendMessage(MessageColor.ERROR + "Usage: " + this.getUsage(sender, label, subCommand));
			return true;
		}
		int page = 1;
		boolean usePage = true;
		if (args.length == 1) {
			try {
				page = Integer.parseInt(args[0]);
			} catch (NumberFormatException e) {
				usePage = false;
			}
		}
		if (usePage) {
			final List<ChatCommandBase> availableCommands = new ArrayList<ChatCommandBase>();
			for (final String commandName : this.plugin.playerListener.mainCommands) {
				final ChatCommandBase command = this.plugin.playerListener.getChatCommand(commandName);
				if (command != null && command.testPermissionSilent(sender)) {
					availableCommands.add(command);
				}
			}
			final int numCommands = availableCommands.size();
			final int maxPage = (numCommands + 7) / 8;
			if (maxPage <= 0) {
				sender.sendMessage(MessageColor.ERROR + "No commands are available to you!");
				return true;
			}
			if (page < 1 || page > maxPage) {
				sender.sendMessage(MessageColor.ERROR + "Invalid page (" + page + ") - must be between 1 and " + maxPage + ".");
				return true;
			}
			Collections.sort(availableCommands, new Comparator<ChatCommandBase>() {
				@Override
				public int compare(final ChatCommandBase o1, final ChatCommandBase o2) {
					return o1.mainCommand.compareToIgnoreCase(o2.mainCommand);
				}
			});

			final int offset = 8 * page - 8;
			final int boundary = Math.min(offset + 8, numCommands);

			sender.sendMessage(MessageColor.HEADER + "--- SteveChat Help <Page " + page + " of " + maxPage + "> ---");
			for (int i = offset; i < boundary; i++) {
				sender.sendMessage(availableCommands.get(i).getShortHelp(sender, label));
			}
			final String slash;
			if (!(sender instanceof ConsoleCommandSender)) {
				slash = "/";
			} else {
				slash = "";
			}
			sender.sendMessage(MessageColor.HELP_VALUE + "Use " + slash + label + " " + subCommand + " <page> to view other pages.");
		} else {
			final ChatCommandBase command = this.plugin.playerListener.getChatCommand(args[0]);
			if (command == null) {
				sender.sendMessage(MessageColor.ERROR + "Unknown command: " + args[0]);
				return true;
			}
			final String[] help = command.getHelp(sender, label, args[0]);
			sender.sendMessage(help);
		}
		return true;
	}

	@Override
	public List<String> onTabComplete(final CommandSender sender, final String args[]) {
		if (args.length == 1) {
			final List<String> parameters = new ArrayList<String>();
			final String partialParameter = args[0];

			int availableCommandsCount = 0;

			for (final String commandName : this.plugin.playerListener.mainCommands) {
				final ChatCommandBase command = this.plugin.playerListener.getChatCommand(commandName);

				if (command != null && command.testPermissionSilent(sender)) {
					availableCommandsCount++;
					final String mainCommand = command.getMainCommand();

					if (StringUtil.startsWithIgnoreCase(mainCommand, partialParameter) && !parameters.contains(mainCommand)) {
						parameters.add(mainCommand);
					}

					for (final String alias : command.getAliases()) {
						if (StringUtil.startsWithIgnoreCase(alias, partialParameter) && !parameters.contains(alias)) {
							parameters.add(alias);
						}
					}
				}
			}

			final int maxPage = (availableCommandsCount + 7) / 8;

			for (int i = 1; i <= maxPage; i++) {
				final String page = String.valueOf(i);
				if (StringUtil.startsWithIgnoreCase(page, partialParameter) && !parameters.contains(page)) {
					parameters.add(page);
				}
			}

			Collections.sort(parameters, String.CASE_INSENSITIVE_ORDER);

			return parameters;
		} else {
			return ImmutableList.<String>of();
		}
	}
}
