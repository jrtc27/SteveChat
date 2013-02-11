package com.jrtc27.stevechat.command;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.util.StringUtil;

import com.google.common.collect.ImmutableList;
import com.jrtc27.stevechat.Channel;
import com.jrtc27.stevechat.MessageColor;
import com.jrtc27.stevechat.SCPermission;
import com.jrtc27.stevechat.SteveChatPlugin;
import com.jrtc27.stevechat.Util;

public class DeleteCommand extends ChatCommandBase {

	public DeleteCommand(final SteveChatPlugin plugin) {
		super(plugin);
		this.permission = SCPermission.DELETE;
		this.usage = " <channel>";
		this.consoleUsage = " <channel>";
		this.mainCommand = "delete";
		this.description = "Deletes a channel.";
	}

	@Override
	public boolean handleCommand(final CommandSender sender, final String label, final String subCommand, final String[] args) {
		if (!this.testPermission(sender)) return true;
		if (args.length != 1) {
			sender.sendMessage(MessageColor.ERROR + "Usage: " + this.getUsage(sender, label, subCommand));
			return true;
		}
		final Channel channel = this.plugin.channelHandler.getChannelByName(args[0]);
		if (channel == null) {
			sender.sendMessage(MessageColor.ERROR + "Unknown channel: " + MessageColor.UNKNOWN_CHANNEL + args[0]);
			return true;
		}
		if (this.plugin.configHandler.removeChannel(channel)) {
			sender.sendMessage(MessageColor.INFO + "Successfully deleted " + channel.getColor() + channel.getName() + MessageColor.INFO + ".");
			return true;
		} else {
			sender.sendMessage(MessageColor.ERROR + "An error occurred when deleting " + channel.getColor() + channel.getName() + MessageColor.ERROR + "!");
			return true;
		}
	}

	@Override
	public List<String> onTabComplete(final CommandSender sender, final String[] args) {
		if (args.length == 1) {
			final List<String> channels = new ArrayList<String>();
			final String partialChannel = args[0];
			synchronized (this.plugin.channelHandler.channels) {
				for (final Channel channel : this.plugin.channelHandler.channels) {
					if (!channel.canSee((Player) sender) || !Util.hasCachedPermission((Player) sender, SCPermission.JOIN, channel))
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
