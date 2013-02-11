package com.jrtc27.stevechat.command;

import java.util.List;

import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.entity.Player;

import com.jrtc27.stevechat.Channel;
import com.jrtc27.stevechat.MessageColor;
import com.jrtc27.stevechat.SteveChatPlugin;

public abstract class ChatCommandBase extends CommandBase {
	protected String description = "";
	protected String consoleUsage = null;

	public ChatCommandBase(final SteveChatPlugin plugin) {
		super(plugin);
	}

	@Override
	public boolean handleCommand(final CommandSender sender, final String label, final String[] args) {
		return this.handleCommand(sender, label, null, args);
	}

	public abstract boolean handleCommand(final CommandSender sender, final String label, final String subCommand, final String[] args);

	@Override
	public String getUsage(final CommandSender sender) {
		return this.getUsage(sender, "ch");
	}

	@Override
	public String getUsage(final CommandSender sender, final String label) {
		return this.getUsage(sender, label, this.mainCommand);
	}

	public String getUsage(final CommandSender sender, final String label, final String subCommand) {
		final StringBuilder builder = new StringBuilder();

		if (!(sender instanceof ConsoleCommandSender)) {
			builder.append("/");
		}

		if (label != null) {
			builder.append(label);
		} else {
			builder.append("ch");
		}

		if (subCommand != null) {
			builder.append(" ").append(subCommand);
		}

		if (sender instanceof Player || this.consoleUsage == null) {
			builder.append(this.usage);
		} else {
			builder.append(this.consoleUsage);
		}

		return builder.toString();
	}

	public String[] getHelp(final CommandSender sender, final String label, final String subCommand) {
		final String[] lines = new String[2];
		lines[0] = MessageColor.HELP_PROPERTY + "Description: " + MessageColor.HELP_VALUE + this.getDescription();
		lines[1] = MessageColor.HELP_PROPERTY + "Usage: " + MessageColor.HELP_VALUE + this.getUsage(sender, label, subCommand);
		return lines;
	}

	public String getShortHelp(final CommandSender sender, final String label) {
		return MessageColor.HELP_PROPERTY + this.getUsage(sender, label) + MessageColor.HELP_VALUE + " - " + this.getDescription();
	}

	public boolean canBeRoot() {
		return false;
	}

	public boolean showsInHelp() {
		return true;
	}

	public String getDescription() {
		return this.description;
	}

	public List<String> onTabComplete(final CommandSender sender, final String[] args) {
		return null;
	}

	protected boolean checkNotConflicting(final CommandSender sender, final String name, final String nameType, final Channel ignored) {
		final Channel conflict = this.plugin.channelHandler.getChannelByName(name);
		if (conflict != null && !conflict.equals(ignored)) {
			final StringBuilder builder = new StringBuilder(MessageColor.ERROR + "That " + nameType + " is already in use as the ");
			if (conflict.getName().equalsIgnoreCase(name)) {
				builder.append("name");
			} else {
				builder.append("nickname");
			}
			builder.append(" for ").append(conflict.getColor()).append(conflict.getName()).append(MessageColor.ERROR).append(".");
			sender.sendMessage(builder.toString());
			return false;
		}
		return true;
	}
}
