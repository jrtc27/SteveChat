package com.jrtc27.stevechat.command;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.util.StringUtil;

import com.google.common.collect.ImmutableList;
import com.jrtc27.stevechat.Channel;
import com.jrtc27.stevechat.Chatter;
import com.jrtc27.stevechat.MessageColor;
import com.jrtc27.stevechat.SCPermission;
import com.jrtc27.stevechat.SteveChatPlugin;
import com.jrtc27.stevechat.Util;

public class BanCommand extends ChatCommandBase {

	public BanCommand(final SteveChatPlugin plugin) {
		super(plugin);
		this.permission = SCPermission.BAN_COMMAND;
		this.usage = " <player> [channel]";
		this.consoleUsage = " <player> <channel>";
		this.mainCommand = "ban";
		this.description = "Toggle a player's banned status for a channel.";
	}

	@Override
	public boolean handleCommand(final CommandSender sender, final String label, final String subCommand, final String[] args) {
		if (!this.testPermission(sender)) return true;

		if (args.length != 1 && args.length != 2) {
			sender.sendMessage(MessageColor.ERROR + "Usage: " + this.getUsage(sender, label, subCommand));
			return true;
		}

		final String player = args[0];
		final Channel channel;

		if (args.length == 2) {
			channel = this.plugin.channelHandler.getChannelByName(args[1]);
			if (channel == null) {
				sender.sendMessage(MessageColor.ERROR + "Unknown channel: " + MessageColor.UNKNOWN_CHANNEL + args[1]);
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

		if (sender instanceof Player && !Util.hasCachedPermission((Player) sender, SCPermission.BAN, channel)) {
			sender.sendMessage(MessageColor.ERROR + "You do not have permission to ban players from " + channel.getColor() + channel.getName() + MessageColor.ERROR + ".");
			return true;
		}

		final Player onlinePlayer = Util.getPlayer(player, true);

		if (channel.isBanned(player)) {
			channel.unbanPlayer(player);
			sender.sendMessage(MessageColor.PLAYER + player + MessageColor.INFO + " is no longer banned from " + channel.getColor() + channel.getName() + MessageColor.INFO + ".");
			if (onlinePlayer != null) {
				onlinePlayer.sendMessage(MessageColor.INFO + "You are no longer banned from " + channel.getColor() + channel.getName() + MessageColor.INFO + ".");
			}
		} else {
			channel.banPlayer(player);
			if (onlinePlayer != null) {
				channel.announceBan(onlinePlayer);
			}
			sender.sendMessage(MessageColor.PLAYER + player + MessageColor.INFO + " has been banned from " + channel.getColor() + channel.getName() + MessageColor.INFO + ".");
			final Chatter onlineChatter = this.plugin.channelHandler.chatterForPlayer(player);
			synchronized (onlineChatter) {
				final Collection<String> toRemove = new HashSet<String>();
				for (final String channelName : onlineChatter.channelsToJoin) {
					final Channel toJoin = this.plugin.channelHandler.getChannelByName(channelName);
					if (toJoin != null && toJoin.equals(channel)) {
						toRemove.add(channelName);
					}
				}
				onlineChatter.channelsToJoin.removeAll(toRemove);
				if (channel.equals(onlineChatter.getActiveChannel())) {
					onlineChatter.setActiveChannel(null);
				}
			}
			if (onlinePlayer != null) {
				onlinePlayer.sendMessage(MessageColor.INFO + "You have been banned from " + channel.getColor() + channel.getName() + MessageColor.INFO + ".");
			}
		}
		return true;
	}

	@Override
	public List<String> onTabComplete(final CommandSender sender, final String args[]) {
		if (args.length == 1) {
			return null;
		} else if (args.length == 2) {
			final List<String> channels = new ArrayList<String>();
			final String partialChannel = args[1];
			synchronized (this.plugin.channelHandler.channels) {
				for (final Channel channel : this.plugin.channelHandler.channels) {
					if (!channel.canSee((Player) sender) || !Util.hasCachedPermission((Player) sender, SCPermission.BAN, channel))
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
