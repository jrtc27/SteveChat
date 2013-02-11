package com.jrtc27.stevechat.command;

import java.util.ArrayList;
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

public class MuteCommand extends ChatCommandBase {

	public MuteCommand(final SteveChatPlugin plugin) {
		super(plugin);
		this.permission = SCPermission.MUTE_COMMAND;
		this.usage = " <player> [channel]";
		this.mainCommand = "mute";
		this.description = "Mute a specific player.";
	}

	@Override
	public boolean handleCommand(final CommandSender sender, final String label, final String subCommand, final String[] args) {
		if (!this.testPermission(sender)) return true;

		if (args.length != 1 && args.length != 2) {
			sender.sendMessage(MessageColor.ERROR + "Usage: " + this.getUsage(sender, label, subCommand));
			return true;
		}

		final String player = args[0];
		if (args.length == 2) {
			final Channel channel = this.plugin.channelHandler.getChannelByName(args[1]);
			if (channel == null) {
				sender.sendMessage(MessageColor.ERROR + "Unknown channel: " + MessageColor.UNKNOWN_CHANNEL + args[1]);
				return true;
			}

			if (sender instanceof Player && !Util.hasCachedPermission((Player) sender, SCPermission.MUTE, channel)) {
				sender.sendMessage(MessageColor.ERROR + "You do not have permission to mute players in " + channel.getColor() + channel.getName() + MessageColor.ERROR + ".");
				return true;
			}

			final Player onlinePlayer = this.plugin.getServer().getPlayerExact(player);

			if (channel.isMuted(player)) {
				channel.unmutePlayer(player);
				sender.sendMessage(MessageColor.PLAYER + player + MessageColor.INFO + " is no longer muted in " + channel.getColor() + channel.getName() + MessageColor.INFO + ".");
				if (onlinePlayer != null) {
					onlinePlayer.sendMessage(MessageColor.INFO + "You are no longer muted in " + channel.getColor() + channel.getName() + MessageColor.INFO + ".");
				}
			} else {
				channel.mutePlayer(player);
				sender.sendMessage(MessageColor.PLAYER + player + MessageColor.INFO + " has been muted in " + channel.getColor() + channel.getName() + MessageColor.INFO + ".");
				if (onlinePlayer != null) {
					onlinePlayer.sendMessage(MessageColor.INFO + "You have been muted in " + channel.getColor() + channel.getName() + MessageColor.INFO + ".");
				}
			}
			return true;
		} else {
			if (sender instanceof Player && !Util.hasCachedPermission((Player) sender, SCPermission.MUTE, Util.DEPENDENCY_ALL)) {
				sender.sendMessage(MessageColor.ERROR + "You do not have permission to mute players server-wide.");
				return true;
			}

			final Chatter chatter = this.plugin.channelHandler.chatterForPlayer(player);
			final Player onlinePlayer = this.plugin.getServer().getPlayerExact(player);

			final boolean nowMuted;

			synchronized (chatter.mutedLock) {
				nowMuted = !chatter.isMuted();
				chatter.setMuted(nowMuted);
			}

			if (nowMuted) {
				sender.sendMessage(MessageColor.PLAYER + player + MessageColor.INFO + " has been muted server-wide.");
				if (onlinePlayer != null) {
					onlinePlayer.sendMessage(MessageColor.INFO + "You have been muted server-wide.");
				}
			} else {
				sender.sendMessage(MessageColor.PLAYER + player + MessageColor.INFO + " is no longer muted server-wide.");
				if (onlinePlayer != null) {
					onlinePlayer.sendMessage(MessageColor.INFO + "You are no longer muted server-wide.");
				}
			}
			return true;
		}
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
					if (!channel.canSee((Player) sender) || !Util.hasCachedPermission((Player) sender, SCPermission.MUTE, channel))
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
