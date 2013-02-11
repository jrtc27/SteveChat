package com.jrtc27.stevechat.command;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import com.jrtc27.stevechat.Chatter;
import com.jrtc27.stevechat.MessageColor;
import com.jrtc27.stevechat.SCPermission;
import com.jrtc27.stevechat.SteveChatPlugin;

public class AfkCommand extends ChatCommandBase {

	public AfkCommand(final SteveChatPlugin plugin) {
		super(plugin);
		this.permission = SCPermission.AFK;
		this.usage = " [message]";
		this.mainCommand = "afk";
		this.description = "Mark yourself as away from keyboard.";
	}

	@Override
	public boolean handleCommand(final CommandSender sender, final String label, final String subCommand, final String[] args) {
		if (!this.validatePlayer(sender)) return true;
		if (!this.testPermission(sender)) return true;

		String message = null;

		if (args.length > 0) {
			final StringBuilder builder = new StringBuilder();
			boolean addSpace = false;

			for (final String arg : args) {
				if (addSpace) {
					builder.append(" ");
				} else {
					addSpace = true;
				}

				builder.append(arg);
			}

			message = builder.toString();
		}

		String broadcastMessage;

		final Chatter chatter = this.plugin.channelHandler.chatterForPlayer(sender.getName());

		boolean nowAfk;
		String afkMessage;

		synchronized (chatter.afkLock) {
			nowAfk = chatter.toggleAfk(message);
			afkMessage = chatter.getAfkMessage(true);
		}

		if (nowAfk) {
			if (afkMessage == null) {
				afkMessage = "no reason given";
			}
			broadcastMessage = ((Player) sender).getDisplayName() + MessageColor.INFO + " is now afk (" + afkMessage + ")";
		} else {
			broadcastMessage = ((Player) sender).getDisplayName() + MessageColor.INFO + " is no longer afk";
		}

		this.plugin.getServer().getConsoleSender().sendMessage(broadcastMessage);
		for (final Player player : this.plugin.getServer().getOnlinePlayers()) {
			if (!player.canSee((Player) sender)) {
				continue;
			}
			player.sendMessage(broadcastMessage);
		}

		return true;
	}

}
