package com.jrtc27.stevechat.command;

import java.util.List;

import org.bukkit.command.CommandSender;

import com.google.common.collect.ImmutableList;
import com.jrtc27.stevechat.Channel;
import com.jrtc27.stevechat.MessageColor;
import com.jrtc27.stevechat.SCPermission;
import com.jrtc27.stevechat.SteveChatPlugin;

public class CreateCommand extends ChatCommandBase {

	public CreateCommand(final SteveChatPlugin plugin) {
		super(plugin);
		this.permission = SCPermission.CREATE;
		this.usage = " <name> [nick]";
		this.consoleUsage = " <name> [nick]";
		this.mainCommand = "create";
		this.description = "Creates a new channel.";
	}

	@Override
	public boolean handleCommand(final CommandSender sender, final String label, final String subCommand, final String[] args) {
		if (!this.testPermission(sender)) return true;
		if (args.length != 1 && args.length != 2) {
			sender.sendMessage(MessageColor.ERROR + "Usage: " + this.getUsage(sender, label, subCommand));
			return true;
		}
		final String name = args[0];
		if (!this.checkNotConflicting(sender, name, "name", null)) return true;
		final String nick = args.length > 1 ? args[1] : name;
		if (!this.checkNotConflicting(sender, name, "nickname", null)) return true;
		final Channel channel = new Channel(this.plugin, name + ".yml");
		channel.setName(name);
		channel.setShortname(nick);
		if (this.plugin.configHandler.addChannel(channel)) {
			sender.sendMessage(MessageColor.INFO + "Created " + channel.getColor() + channel.getName() + MessageColor.INFO + " with nickname " + channel.getColor() + channel.getShortname() + MessageColor.INFO + ".");
		} else {
			sender.sendMessage(MessageColor.ERROR + "An error occurred when creating the channel!");
		}
		return true;
	}

	@Override
	public List<String> onTabComplete(final CommandSender sender, final String[] args) {
		return ImmutableList.<String>of();
	}

}
