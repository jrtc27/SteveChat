package com.jrtc27.stevechat.command;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.util.StringUtil;

import com.jrtc27.stevechat.Channel;
import com.jrtc27.stevechat.Chatter;
import com.jrtc27.stevechat.MessageColor;
import com.jrtc27.stevechat.SCPermission;
import com.jrtc27.stevechat.SteveChatPlugin;

public class QuickMessageCommand extends ChatCommandBase {

	public QuickMessageCommand(final SteveChatPlugin plugin) {
		super(plugin);
		this.permission = SCPermission.QUICK_MESSAGE_COMMAND;
		this.usage = " <channel> <message>";
		this.mainCommand = "qm";
		this.aliases = new String[] { "quickmsg", "quickmessage", "say" };
		this.description = "Say something in another channel.";
	}

	@Override
	public boolean handleCommand(final CommandSender sender, final String label, final String subCommand, final String[] args) {
		if (!this.testPermission(sender)) return true;

		if (args.length < 2) {
			sender.sendMessage(MessageColor.ERROR + "Usage: " + this.getUsage(sender, label, subCommand));
			return true;
		}

		final Channel channel = this.plugin.channelHandler.getChannelByName(args[0]);
		if (channel == null) {
			sender.sendMessage(MessageColor.ERROR + "Unknown channel: " + MessageColor.UNKNOWN_CHANNEL + args[0]);
			return true;
		}

		final StringBuilder messageBuilder = new StringBuilder();

		for (int i = 1; i < args.length; i++) {
			if (i > 1) {
				messageBuilder.append(" ");
			}
			messageBuilder.append(args[i]);
		}

		final String message = messageBuilder.toString();

		if (!(sender instanceof Player)) {
			channel.broadcast(channel.formatAnnounce(ChatColor.translateAlternateColorCodes('&', message)), null);
			sender.sendMessage(MessageColor.INFO + "Message announced in " + channel.getColor() + channel.getName() + MessageColor.INFO + ".");
			return true;
		} else {
			final Chatter chatter = this.plugin.channelHandler.chatterForPlayer(sender.getName());
			final Channel activeChannel = chatter.getActiveChannel();
			chatter.setActiveChannel(channel);
			final Set<Player> players = new HashSet<Player>(Arrays.asList(this.plugin.getServer().getOnlinePlayers()));
			final AsyncPlayerChatEvent chatEvent = new AsyncPlayerChatEvent(false, (Player) sender, message, players);
			this.plugin.getServer().getPluginManager().callEvent(chatEvent);
			chatter.setActiveChannel(activeChannel);
			if (!chatEvent.isCancelled()) {
				final String formattedMessage = String.format(chatEvent.getFormat(), chatEvent.getPlayer().getDisplayName(), chatEvent.getMessage());
				this.plugin.getServer().getConsoleSender().sendMessage(formattedMessage);
				for (final Player recipient : chatEvent.getRecipients()) {
					recipient.sendMessage(formattedMessage);
				}
			}
			return true;
		}
	}

	@Override
	public List<String> onTabComplete(final CommandSender sender, final String[] args) {
		if (args.length == 1) {
			List<String> channels = new ArrayList<String>();
			String partialChannel = args[0];
			synchronized (this.plugin.channelHandler.channels) {
				for (Channel channel : this.plugin.channelHandler.channels) {
					if (!channel.canSee((Player) sender) || !channel.canSpeak((Player) sender)) continue;

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
			return null;
		}
	}

}
