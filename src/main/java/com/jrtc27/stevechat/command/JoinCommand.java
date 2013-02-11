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

public class JoinCommand extends ChatCommandBase {

	public JoinCommand(final SteveChatPlugin plugin) {
		super(plugin);
		this.permission = SCPermission.JOIN_COMMAND;
		this.usage = " <channel> [password]";
		this.mainCommand = "join";
		this.description = "Join the specified channel.";
	}

	@Override
	public boolean handleCommand(final CommandSender sender, final String label, final String subCommand, final String[] args) {
		if (!this.validatePlayer(sender)) return true;
		if (!this.testPermission(sender)) return true;

		if (args.length != 1 && args.length != 2) {
			sender.sendMessage(MessageColor.ERROR + "Usage: " + this.getUsage(sender, label, subCommand));
			return true;
		}
		final Player player = (Player) sender;
		final Channel potentialChannel = this.plugin.channelHandler.getChannelByName(args[0]);
		if (potentialChannel != null) {
			if (!potentialChannel.inWorld(player)) {
				sender.sendMessage(potentialChannel.getColor() + potentialChannel.getName() + MessageColor.ERROR + " is not available in this world.");
				return true;
			}

			if (potentialChannel.canJoin(player)) {
				final String givenPassword;
				if (args.length == 2) {
					givenPassword = args[1];
				} else {
					givenPassword = "";
				}
				if (!givenPassword.equals(potentialChannel.getPassword())) {
					sender.sendMessage(MessageColor.ERROR + "Incorrect password specified for " + potentialChannel.getColor() + potentialChannel.getName() + MessageColor.ERROR + ".");
					return true;
				}

				final Chatter chatter = this.plugin.channelHandler.chatterForPlayer(player.getName());
				final boolean member;
				final boolean alreadyActive;

				synchronized (chatter.activeChannelLock) {
					synchronized (chatter.conversingLock) {
						member = potentialChannel.isMember(player.getName());
						alreadyActive = potentialChannel.equals(chatter.getActiveChannel()) && chatter.getConversing() == null;
						chatter.setActiveChannel(potentialChannel);
						chatter.setConversing(null);
					}
				}

				if (!member) {
					potentialChannel.announceJoin((Player) sender);
					potentialChannel.addMember(player.getName());
					sender.sendMessage(MessageColor.INFO + "You have joined " + potentialChannel.getColor() + potentialChannel.getName() + MessageColor.INFO + ".");
				}

				if (!alreadyActive || !member) {
					sender.sendMessage(MessageColor.INFO + "Now talking in " + potentialChannel.getColor() + potentialChannel.getName() + MessageColor.INFO + ".");
					chatter.setModified(true);
				} else {
					sender.sendMessage(MessageColor.INFO + "You are already talking in " + potentialChannel.getColor() + potentialChannel.getName() + MessageColor.INFO + ".");
				}
			} else if (potentialChannel.isBanned(player) && potentialChannel.canSee(player)) {
				sender.sendMessage(MessageColor.ERROR + "You are currently banned from " + potentialChannel.getColor() + potentialChannel.getName() + MessageColor.ERROR + ".");
			} else {
				sender.sendMessage(MessageColor.ERROR + "You do not have permission to join this channel.");
			}
		} else {
			sender.sendMessage(MessageColor.ERROR + "Unknown channel: " + MessageColor.UNKNOWN_CHANNEL + args[0]);
		}
		return true;
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
