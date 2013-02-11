package com.jrtc27.stevechat.command;

import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.entity.Player;

import com.jrtc27.stevechat.MessageColor;
import com.jrtc27.stevechat.SCPermission;
import com.jrtc27.stevechat.SteveChatPlugin;
import com.jrtc27.stevechat.Util;

public abstract class CommandBase {
	protected final SteveChatPlugin plugin;
	protected SCPermission permission;
	protected String usage = "";
	protected String mainCommand = "";
	protected String[] aliases = new String[0];

	public CommandBase(final SteveChatPlugin plugin) {
		this.plugin = plugin;
	}

	public abstract boolean handleCommand(CommandSender sender, String label, String[] args);

	public String getUsage(final CommandSender sender) {
		return this.getUsage(sender, this.mainCommand);
	}

	public String getUsage(final CommandSender sender, final String label) {
		final StringBuilder builder = new StringBuilder();

		if (!(sender instanceof ConsoleCommandSender)) {
			builder.append("/");
		}

		builder.append(label).append(this.usage);

		return builder.toString();
	}

	public String getMainCommand() {
		return this.mainCommand;
	}

	public String[] getAliases() {
		return this.aliases;
	}

	public boolean testPermission(final CommandSender sender) {
		if (testPermissionSilent(sender)) {
			return true;
		}
		sender.sendMessage(MessageColor.ERROR + "You do not have permission to use this command.");
		return false;
	}

	public boolean testPermissionSilent(final CommandSender sender) {
		if (sender instanceof Player && this.permission != null && !Util.hasCachedPermission((Player) sender, this.permission, null)) {
			return false;
		}
		return true;
	}

	/**
	 * Returns true if the provided {@link org.bukkit.command.CommandSender} is a player.
	 * If it is not a player, they are notified.
	 *
	 * @param sender The {@link org.bukkit.command.CommandSender} to check
	 * @return true if the sender is a player, otherwise false
	 */
	protected boolean validatePlayer(final CommandSender sender) {
		if (!(sender instanceof Player)) {
			sender.sendMessage(MessageColor.ERROR + "You must be a player to perform this action.");
			return false;
		}
		return true;
	}
}
