package com.jrtc27.stevechat.command;

import org.bukkit.command.CommandSender;

import com.jrtc27.stevechat.Chatter;
import com.jrtc27.stevechat.MessageColor;
import com.jrtc27.stevechat.SCPermission;
import com.jrtc27.stevechat.SteveChatPlugin;

public class ReplyCommand extends ChatCommandBase {
	private final TellCommand tellCommand;

	public ReplyCommand(final SteveChatPlugin plugin) {
		super(plugin);
		this.permission = SCPermission.PRIVATE_MESSAGE;
		this.usage = " [message]";
		this.mainCommand = "r";
		this.aliases = new String[] { "reply", "respond" };
		this.description = "Reply to a private message.";
		this.tellCommand = new TellCommand(this.plugin);
	}

	@Override
	public boolean handleCommand(CommandSender sender, String label, String subCommand, String[] args) {
		if (!this.validatePlayer(sender)) return true;
		if (!this.testPermission(sender)) return true;

		final Chatter chatter = this.plugin.channelHandler.chatterForPlayer(sender.getName());
		final String lastConverser = chatter.getLastConverser();
		if (lastConverser != null) {
			return this.tellCommand.performTell(sender, lastConverser, args);
		} else {
			sender.sendMessage(MessageColor.ERROR + "You have nobody to reply to.");
			return true;
		}
	}

	@Override
	public boolean canBeRoot() {
		return true;
	}

}
