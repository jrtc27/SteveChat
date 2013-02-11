package com.jrtc27.stevechat.command;

import java.util.ArrayList;
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

public class InfoCommand extends ChatCommandBase {

	public InfoCommand(final SteveChatPlugin plugin) {
		super(plugin);
		this.permission = SCPermission.INFO_COMMAND;
		this.usage = " [channel]";
		this.consoleUsage = " <channel>";
		this.mainCommand = "info";
		this.description = "View information about a channel.";
	}

	@Override
	public boolean handleCommand(final CommandSender sender, final String label, final String subCommand, final String[] args) {
		if (!this.testPermission(sender)) return true;

		if (args.length != 0 && args.length != 1) {
			sender.sendMessage(MessageColor.ERROR + "Usage: " + this.getUsage(sender, label, subCommand));
			return true;
		}
		final Channel channel;
		if (args.length == 1) {
			channel = this.plugin.channelHandler.getChannelByName(args[0]);
			if (channel == null) {
				sender.sendMessage(MessageColor.ERROR + "Unknown channel: " + MessageColor.UNKNOWN_CHANNEL + args[0]);
				return true;
			}
		} else if (sender instanceof Player) {
			channel = this.plugin.channelHandler.channelForPlayer(sender.getName());
			if (channel == null) {
				sender.sendMessage(MessageColor.ERROR + "You are not currently in a channel - either join one or specifiy one manually.");
				return true;
			}
		} else {
			sender.sendMessage(MessageColor.ERROR + "You must specify a channel manually.");
			return true;
		}
		if (sender instanceof Player && !Util.hasCachedPermission((Player) sender, SCPermission.INFO, channel)) {
			sender.sendMessage(MessageColor.ERROR + "You do not have permission to view information about " + channel.getColor() + channel.getName() + MessageColor.ERROR + ".");
			return true;
		}

		sender.sendMessage(MessageColor.HEADER + "--- " + channel.getColor() + channel.getName() + MessageColor.HEADER + " ---");
		sender.sendMessage(MessageColor.CHANNEL_PROPERTY + "File: " + MessageColor.CHANNEL_VALUE + channel.getFilename());
		sender.sendMessage(MessageColor.CHANNEL_PROPERTY + "Name: " + MessageColor.CHANNEL_VALUE + channel.getName());
		sender.sendMessage(MessageColor.CHANNEL_PROPERTY + "Nickname: " + MessageColor.CHANNEL_VALUE + channel.getShortname());
		sender.sendMessage(MessageColor.CHANNEL_PROPERTY + "Format: " + MessageColor.CHANNEL_VALUE + channel.getMessageFormat());

		final String password = channel.getPassword();
		sender.sendMessage(MessageColor.CHANNEL_PROPERTY + "Password: " + MessageColor.CHANNEL_VALUE + (password.isEmpty() ? ChatColor.ITALIC + "(none)" : password));

		final long radius = channel.getRadius();
		sender.sendMessage(MessageColor.CHANNEL_PROPERTY + "Radius: " + MessageColor.CHANNEL_VALUE + (radius <= 0 ? ChatColor.ITALIC : "") + radius);

		sender.sendMessage(MessageColor.CHANNEL_PROPERTY + "Enable Quick Message Shortcut: " + MessageColor.CHANNEL_VALUE + (channel.isQMShortcutEnabled() ? "Yes" : "No"));
		sender.sendMessage(MessageColor.CHANNEL_PROPERTY + "Announce Activity: " + MessageColor.CHANNEL_VALUE + (channel.shouldAnnounceActivity() ? "Yes" : "No"));
		return true;
	}

	@Override
	public List<String> onTabComplete(final CommandSender sender, final String args[]) {
		if (args.length == 1) {
			final List<String> channels = new ArrayList<String>();
			final String partialChannel = args[0];
			synchronized (this.plugin.channelHandler.channels) {
				for (final Channel channel : this.plugin.channelHandler.channels) {
					if (!channel.canSee((Player) sender) || !Util.hasCachedPermission((Player) sender, SCPermission.INFO, channel))
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
		} else {
			return ImmutableList.<String>of();
		}
	}

}
