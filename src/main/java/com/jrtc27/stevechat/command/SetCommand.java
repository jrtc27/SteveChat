package com.jrtc27.stevechat.command;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.util.StringUtil;

import com.google.common.collect.ImmutableList;
import com.jrtc27.stevechat.Channel;
import com.jrtc27.stevechat.MessageColor;
import com.jrtc27.stevechat.SCPermission;
import com.jrtc27.stevechat.SteveChatPlugin;
import com.jrtc27.stevechat.Util;

public class SetCommand extends ChatCommandBase {
	private static List<String> knownCommands = new ArrayList<String>(Arrays.asList("name", "nick", "format", "password", "color", "radius", "qm-shortcut", "announce-activity"));
	static {
		Collections.sort(knownCommands, String.CASE_INSENSITIVE_ORDER);
	}

	public SetCommand(final SteveChatPlugin plugin) {
		super(plugin);
		this.permission = SCPermission.SET_COMMAND;
		this.usage = " <channel> <property> <value>";
		this.consoleUsage = " <channel> <property> <value>";
		this.mainCommand = "set";
		this.description = "Change a channel's settings.";
	}

	@Override
	public boolean handleCommand(final CommandSender sender, final String label, final String subCommand, final String[] args) {
		if (!testPermission(sender)) return true;
		if (args.length < 3) {
			sender.sendMessage(MessageColor.ERROR + "Usage: " + this.getUsage(sender, label, subCommand));
			return true;
		}

		final StringBuilder argBuilder = new StringBuilder(args[2]);
		for (int i = 3; i < args.length; i++) {
			argBuilder.append(" ").append(args[i]);
		}
		args[2] = argBuilder.toString();

		final Channel channel = this.plugin.channelHandler.getChannelByName(args[0]);
		if (channel == null) {
			sender.sendMessage(MessageColor.ERROR + "Unknown channel: " + MessageColor.UNKNOWN_CHANNEL + args[0]);
			return true;
		} else if (sender instanceof Player && !Util.hasCachedPermission((Player) sender, SCPermission.SET, channel)) {
			sender.sendMessage(MessageColor.ERROR + "You do not have permission to change the settings for " + channel.getColor() + channel.getName() + MessageColor.ERROR + ".");
			return true;
		}

		final String property = args[1].toLowerCase();
		final String value = args[2];
		if (property.equals("name")) {
			sender.sendMessage(MessageColor.ERROR + "Setting the name of a channel in-game is not supported due to the implications on permissions.");
			return true;
		} else if (property.equals("nick")) {
			if (this.checkNotConflicting(sender, value, "nickname", channel)) {
				channel.setShortname(value);
				sender.sendMessage(MessageColor.INFO + "Set the " + MessageColor.CHANNEL_PROPERTY + "nickname" + MessageColor.INFO + " for " + channel.getColor() + channel.getName() + MessageColor.INFO + " to " + MessageColor.CHANNEL_VALUE + value + MessageColor.INFO + ".");
				return true;
			}
			return true;
		} else if (property.equals("format")) {
			channel.setMessageFormat(value);
			sender.sendMessage(MessageColor.INFO + "Set the " + MessageColor.CHANNEL_PROPERTY + "format" + MessageColor.INFO + " of " + channel.getColor() + channel.getName() + MessageColor.INFO + " to " + MessageColor.CHANNEL_VALUE + value + MessageColor.INFO + ".");
			return true;
		} else if (property.equals("password")) {
			if (value.equals("-")) {
				channel.setPassword(null);
				sender.sendMessage(MessageColor.INFO + "Removed the " + MessageColor.CHANNEL_PROPERTY + "password" + MessageColor.INFO + " for " + channel.getColor() + channel.getName() + MessageColor.INFO + ".");
			} else {
				channel.setPassword(value);
				sender.sendMessage(MessageColor.INFO + "Set the " + MessageColor.CHANNEL_PROPERTY + "password" + MessageColor.INFO + " for " + channel.getColor() + channel.getName() + MessageColor.INFO + " to " + MessageColor.CHANNEL_VALUE + value + MessageColor.INFO + ".");
			}
			return true;
		} else if (property.equals("color")) {
			final ChatColor newColor;
			try {
				newColor = ChatColor.valueOf(value.toUpperCase());
			} catch (IllegalArgumentException e) {
				sender.sendMessage(MessageColor.ERROR + "Invalid color: " + MessageColor.CHANNEL_VALUE + value);
				return true;
			}
			final ChatColor oldColor = channel.getColor();
			channel.setColor(newColor);
			sender.sendMessage(MessageColor.INFO + "Set the " + MessageColor.CHANNEL_PROPERTY + "color" + MessageColor.INFO + " for " + oldColor + channel.getName() + MessageColor.INFO + " to " + MessageColor.CHANNEL_VALUE + newColor.name() + MessageColor.INFO + ".");
			return true;
		} else if (property.equals("radius")) {
			final long newRadius;
			try {
				newRadius = Long.parseLong(value);
			} catch (NumberFormatException e) {
				sender.sendMessage(MessageColor.ERROR + "Invalid number: " + MessageColor.CHANNEL_VALUE + value);
				return true;
			}
			channel.setRadius(newRadius);
			sender.sendMessage(MessageColor.INFO + "Set the " + MessageColor.CHANNEL_PROPERTY + "radius" + MessageColor.INFO + " for " + channel.getColor() + channel.getName() + MessageColor.INFO + " to " + MessageColor.CHANNEL_VALUE + value + MessageColor.INFO + ".");
			return true;
		} else if (property.equals("qm-shortcut")) {
			final boolean useQmShortcut;
			try {
				useQmShortcut = this.toBoolean(value);
			} catch (IllegalArgumentException e) {
				sender.sendMessage(MessageColor.ERROR + "Invalid boolean value: " + MessageColor.CHANNEL_VALUE + value);
				return true;
			}
			channel.setQMShortcutEnabled(useQmShortcut);
			sender.sendMessage(MessageColor.CHANNEL_VALUE + (useQmShortcut ? "Enabled " : "Disabled ") + MessageColor.INFO + "using the " + MessageColor.CHANNEL_PROPERTY + "Quick Message shortcut" + MessageColor.INFO + " for " + channel.getColor() + channel.getName() + MessageColor.INFO + MessageColor.INFO + ".");
			return true;
		} else if (property.equals("announce-activity")) {
			final boolean newAnnounceActivity;
			try {
				newAnnounceActivity = this.toBoolean(value);
			} catch (IllegalArgumentException e) {
				sender.sendMessage(MessageColor.ERROR + "Invalid boolean value: " + MessageColor.CHANNEL_VALUE + value);
				return true;
			}
			channel.setAnnounceActivity(newAnnounceActivity);
			sender.sendMessage(MessageColor.CHANNEL_VALUE + (newAnnounceActivity ? "Enabled " : "Disabled ") + MessageColor.CHANNEL_PROPERTY + "announcing activity" + MessageColor.INFO + " for " + channel.getColor() + channel.getName() + MessageColor.INFO + MessageColor.INFO + ".");
			return true;
		}

		sender.sendMessage(MessageColor.ERROR + "Unknown property: " + MessageColor.UNKNOWN_CHANNEL_PROPERTY + property);

		return true;
	}

	private boolean toBoolean(String string) throws IllegalArgumentException {
		string = string.toLowerCase();
		if (string.equals("true") || string.equals("yes") || string.equals("y")) {
			return true;
		} else if (string.equals("false") || string.equals("no") || string.equals("n")) {
			return false;
		}
		throw new IllegalArgumentException("Cannot represent '" + string + "' as a boolean value!");
	}

	@Override
	public List<String> onTabComplete(final CommandSender sender, final String[] args) {
		if (args.length == 1) {
			final List<String> channels = new ArrayList<String>();
			final String partialChannel = args[0];
			synchronized (this.plugin.channelHandler.channels) {
				for (final Channel channel : this.plugin.channelHandler.channels) {
					if (!channel.canSee((Player) sender) || !Util.hasCachedPermission((Player) sender, SCPermission.SET, channel))
						continue;

					final String name, shortname;

					synchronized (channel.baseAttrsLock) {
						name = channel.getName();
						shortname = channel.getShortname();
					}

					if (StringUtil.startsWithIgnoreCase(name, partialChannel) && !channels.contains(name)) {
						channels.add(name);
					}

					if (StringUtil.startsWithIgnoreCase(shortname, partialChannel) && !channels.contains(shortname)) {
						channels.add(shortname);
					}
				}
			}
			return channels;
		} else if (args.length == 2) {
			final List<String> matched = new ArrayList<String>();
			final String partialProperty = args[1];

			for (final String property : SetCommand.knownCommands) {
				if (StringUtil.startsWithIgnoreCase(property, partialProperty)) {
					matched.add(property);
				}
			}

			Collections.sort(matched, String.CASE_INSENSITIVE_ORDER);
			return matched;
		} else {
			return ImmutableList.<String>of();
		}
	}

}
