package com.jrtc27.stevechat.command;

import java.util.List;

import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import com.google.common.collect.ImmutableList;
import com.jrtc27.stevechat.Channel;
import com.jrtc27.stevechat.MessageColor;
import com.jrtc27.stevechat.SCPermission;
import com.jrtc27.stevechat.SteveChatPlugin;

public class ListCommand extends ChatCommandBase {

	public ListCommand(final SteveChatPlugin plugin) {
		super(plugin);
		this.permission = SCPermission.LIST;
		// No need to set usage
		this.mainCommand = "list";
		this.aliases = new String[] { "channels" };
		this.description = "List all channels available to you.";
	}

	@Override
	public boolean handleCommand(final CommandSender sender, final String label, final String subCommand, final String[] args) {
		if (!this.testPermission(sender)) return true;

		if (args.length != 0) {
			sender.sendMessage(MessageColor.ERROR + "Usage: " + this.getUsage(sender, label, subCommand));
			return true;
		}
		sender.sendMessage(MessageColor.HEADER + "--- Available Channels ---");
		for (final Channel channel : this.plugin.channelHandler.channels) {
			final boolean canSee = !(sender instanceof Player) || channel.canSee((Player) sender);
			if (!canSee) {
				continue;
			}
			final boolean banned = sender instanceof Player && channel.isBanned((Player) sender);
			final String prefix = sender instanceof Player && channel.isMember(sender.getName()) ? (channel.equals(this.plugin.channelHandler.chatterForPlayer(sender.getName()).getActiveChannel()) ? "*" : "+") : " ";
			final String strikethrough = banned ? ChatColor.STRIKETHROUGH.toString() : "";
			sender.sendMessage(channel.getColor() + strikethrough + " " + prefix + " [" + channel.getShortname() + "] " + channel.getName());
		}
		return true;
	}

	@Override
	public List<String> onTabComplete(final CommandSender sender, final String[] args) {
		return ImmutableList.<String>of();
	}

}
