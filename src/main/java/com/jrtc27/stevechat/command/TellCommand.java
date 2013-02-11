package com.jrtc27.stevechat.command;

import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.entity.Player;
import org.bukkit.util.Java15Compat;

import com.jrtc27.stevechat.Chatter;
import com.jrtc27.stevechat.MessageColor;
import com.jrtc27.stevechat.SCPermission;
import com.jrtc27.stevechat.SteveChatPlugin;
import com.jrtc27.stevechat.Util;

public class TellCommand extends ChatCommandBase {

	public TellCommand(final SteveChatPlugin plugin) {
		super(plugin);
		this.permission = SCPermission.PRIVATE_MESSAGE;
		this.usage = " <player> [message]";
		this.consoleUsage = " <player> <message>";
		this.mainCommand = "tell";
		this.aliases = new String[] { "whisper", "msg", "message", "pm" };
		this.description = "Private message a player.";
	}

	// IMPORTANT: This method *must* be thread-safe - called from AsyncPlayerChatEvent!
	@Override
	public boolean handleCommand(final CommandSender sender, final String label, final String subCommand, final String[] args) {
		if (!this.testPermission(sender)) return true;

		if (args.length < 1 || (args.length < 2 && !(sender instanceof Player))) {
			sender.sendMessage(MessageColor.ERROR + "Usage: " + this.getUsage(sender, label, subCommand));
			return true;
		}
		return this.performTell(sender, args[0], Java15Compat.Arrays_copyOfRange(args, 1, args.length));
	}

	public boolean performTell(final CommandSender sender, final String recipientName, final String[] args) {
		final CommandSender recipient;
		if (recipientName.equalsIgnoreCase(Util.CONSOLE_COMMAND_SENDER_NAME)) {
			recipient = this.plugin.getServer().getConsoleSender();
		} else {
			recipient = this.plugin.getServer().getPlayerExact(recipientName);
		}
		if (recipient == null) {
			sender.sendMessage(MessageColor.ERROR + "Unknown recipient: " + MessageColor.PLAYER + recipientName);
			return true;
		}

		if (args.length == 0 && sender instanceof Player) {
			final Chatter senderChatter = this.plugin.channelHandler.chatterForPlayer(sender.getName());
			if (recipient instanceof Player) {
				senderChatter.setConversing(recipient.getName());
				sender.sendMessage(MessageColor.INFO + "Now chatting with " + ((Player) recipient).getDisplayName() + MessageColor.INFO + ".");
			} else {
				senderChatter.setConversing(Util.CONSOLE_COMMAND_SENDER_NAME);
				sender.sendMessage(MessageColor.INFO + "Now chatting with " + ChatColor.GOLD + "[" + recipient.getName() + "]" + MessageColor.INFO + ".");
			}
			return true;
		}

		final int length = args.length;
		final StringBuilder builder = new StringBuilder(args[0]);

		for (int i = 1; i < length; i++) {
			builder.append(" ").append(args[i]);
		}

		final String message = builder.toString();

		sender.sendMessage(this.plugin.channelHandler.formatPM(false, sender, recipient, message));
		recipient.sendMessage(this.plugin.channelHandler.formatPM(true, sender, sender, message));

		if (recipient instanceof Player) {
			final Chatter chatter = this.plugin.channelHandler.chatterForPlayer(recipientName);

			if (sender instanceof Player) {
				chatter.setLastConverser(sender.getName());
			} else if (sender instanceof ConsoleCommandSender) {
				chatter.setLastConverser(Util.CONSOLE_COMMAND_SENDER_NAME);
			} else {
				chatter.setLastConverser(null);
			}

			if (chatter.isAfk() || Util.isEssentialsAfk(recipient.getName())) {
				sender.sendMessage(this.plugin.channelHandler.formatPM(true, recipient, recipient, MessageColor.INFO + "[AFK] " + chatter.getAfkMessage(false)));
			}
		}
		return true;
	}

	@Override
	public boolean canBeRoot() {
		return true;
	}

}
