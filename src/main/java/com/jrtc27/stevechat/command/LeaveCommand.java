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

public class LeaveCommand extends ChatCommandBase {

	public LeaveCommand(final SteveChatPlugin plugin) {
		super(plugin);
		this.permission = SCPermission.LEAVE_COMMAND;
		this.usage = " [channel]";
		this.mainCommand = "leave";
		this.aliases = new String[] { "quit", "exit" };
		this.description = "Leave the specified channel.";
	}

	@Override
	public boolean handleCommand(final CommandSender sender, final String label, final String subCommand, final String[] args) {
		if (!this.validatePlayer(sender)) return true;
		if (!this.testPermission(sender)) return true;

		if (args.length > 1) {
			sender.sendMessage(MessageColor.ERROR + "Usage: " + this.getUsage(sender, label, subCommand));
			return true;
		}

		final Player speaker = (Player) sender;

		final Chatter chatter = this.plugin.channelHandler.chatterForPlayer(sender.getName());
		final Channel channel;
		String channelName = null;
		if (args.length == 1) {
			channelName = args[0];
			channel = this.plugin.channelHandler.getChannelByName(channelName);
		} else {
			channel = chatter.getActiveChannel();
		}
		if (channel == null) {
			if (channelName == null) {
				sender.sendMessage(MessageColor.ERROR + "You currently have no active channel - please specify one.");
			} else {
				sender.sendMessage(MessageColor.ERROR + "Unknown channel: " + MessageColor.UNKNOWN_CHANNEL + channelName);
			}
			return true;
		}
		if (!channel.inWorld(speaker)) {
			sender.sendMessage(channel.getColor() + channel.getName() + MessageColor.ERROR + " is not available in this world.");
			return true;
		}
		if (!channel.canLeave(speaker)) {
			sender.sendMessage(MessageColor.ERROR + "You do not have permission to leave " + channel.getColor() + channel.getName() + MessageColor.ERROR + ".");
			return true;
		}
		synchronized (chatter.activeChannelLock) {
			if (channel.equals(chatter.getActiveChannel())) {
				chatter.setActiveChannel(null);
			}
		}
		channel.removeMember(sender.getName());
		sender.sendMessage(MessageColor.INFO + "You have left " + channel.getColor() + channel.getName() + MessageColor.INFO + ".");
		chatter.setModified(true);
		channel.announceLeave(speaker);
		return true;
	}

	@Override
	public List<String> onTabComplete(final CommandSender sender, final String[] args) {
		if (args.length == 1) {
			List<String> channels = new ArrayList<String>();
			String partialChannel = args[0];
			synchronized (this.plugin.channelHandler.channels) {
				for (Channel channel : this.plugin.channelHandler.channels) {
					if (!channel.canSee((Player) sender) || !channel.canLeave((Player) sender)) continue;

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
