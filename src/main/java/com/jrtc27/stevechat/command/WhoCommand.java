package com.jrtc27.stevechat.command;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.bukkit.ChatColor;
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

public class WhoCommand extends ChatCommandBase {
	private static final ChatColor MUTED_COLOR = ChatColor.STRIKETHROUGH;
	private static final ChatColor UNREACHABLE_WORLD_COLOR = ChatColor.GRAY;
	private static final String NOBODY_MESSAGE = "No members!";

	public WhoCommand(final SteveChatPlugin plugin) {
		super(plugin);
		this.permission = SCPermission.WHO_COMMAND;
		this.usage = " [channel]";
		this.consoleUsage = " <channel>";
		this.mainCommand = "who";
		this.aliases = new String[] { "members" };
		this.description = "List the members of a channel.";
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
		if (sender instanceof Player) {
			final Player speaker = (Player) sender;
			if (!channel.inWorld(speaker)) {
				sender.sendMessage(channel.getColor() + channel.getName() + MessageColor.ERROR + " is not available in this world.");
				return true;
			}
			if (!Util.hasCachedPermission(speaker, SCPermission.WHO, channel)) {
				sender.sendMessage(MessageColor.ERROR + "You do not have permission to list the members of " + channel.getColor() + channel.getName() + MessageColor.ERROR + ".");
				return true;
			}
		}

		sender.sendMessage(MessageColor.HEADER + "--- Members of " + channel.getColor() + channel.getName() + MessageColor.HEADER + " ---");

		final List<String> visibleMembers = new ArrayList<String>();
		for (final String member : channel.members) {
			final Player player = Util.getPlayer(member, false);
			if (player == null) {
				continue;
			}
			if (!(sender instanceof Player) || ((Player) sender).canSee(player)) {
				visibleMembers.add(player.getName());
			}
		}

		Collections.sort(visibleMembers, new Comparator<String>() {

			@Override
			public int compare(final String arg0, final String arg1) {
				return arg0.compareToIgnoreCase(arg1);
			}

		});

		if (visibleMembers.isEmpty()) {
			sender.sendMessage(WhoCommand.NOBODY_MESSAGE);
		} else {
			final int capacity = visibleMembers.size() * 10; // 16/2 + 2 -> max name length is 16 and add 2 for ', '
			final StringBuilder builder = new StringBuilder(capacity);

			boolean firstItem = true;

			for (final String member : visibleMembers) {
				boolean colored = false;

				if (firstItem) {
					firstItem = false;
				} else {
					builder.append(", ");
				}

				final Chatter chatter = this.plugin.channelHandler.chatterForPlayer(member);
				final Player player = Util.getPlayer(member, false);

				if (player != null) { // No idea why it could be null now, as we checked in the previous loop, but may as well check
					if (!channel.inWorld(player.getWorld().getName())) {
						builder.append(UNREACHABLE_WORLD_COLOR);
						colored = true;
					}
				}

				// Do this last as otherwise setting a color will cancel the strikethrough
				if (chatter.isMuted() || channel.isMuted(member)) {
					builder.append(MUTED_COLOR);
					colored = true;
				}

				builder.append(member);

				if (colored) {
					builder.append(ChatColor.RESET);
				}
			}

			sender.sendMessage(builder.toString());
		}

		return true;
	}

	@Override
	public List<String> onTabComplete(final CommandSender sender, final String[] args) {
		if (args.length == 1) {
			List<String> channels = new ArrayList<String>();
			String partialChannel = args[0];
			synchronized (this.plugin.channelHandler.channels) {
				for (Channel channel : this.plugin.channelHandler.channels) {
					if (sender instanceof Player && !Util.hasCachedPermission((Player) sender, SCPermission.WHO, channel))
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
