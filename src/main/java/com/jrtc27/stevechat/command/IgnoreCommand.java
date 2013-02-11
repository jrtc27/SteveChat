package com.jrtc27.stevechat.command;

import java.util.List;
import java.util.Set;

import org.bukkit.command.CommandSender;

import com.google.common.collect.ImmutableList;
import com.jrtc27.stevechat.Chatter;
import com.jrtc27.stevechat.MessageColor;
import com.jrtc27.stevechat.SCPermission;
import com.jrtc27.stevechat.SteveChatPlugin;

public class IgnoreCommand extends ChatCommandBase {
	private static final String NOBODY_MESSAGE = "Nobody!";

	public IgnoreCommand(final SteveChatPlugin plugin) {
		super(plugin);
		this.permission = SCPermission.IGNORE;
		this.usage = " [player]";
		this.mainCommand = "ignore";
		this.description = "Toggle whether you are ignoring a specific player.";
	}

	@Override
	public boolean handleCommand(final CommandSender sender, final String label, final String subCommand, final String[] args) {
		if (!this.validatePlayer(sender)) return true;
		if (!this.testPermission(sender)) return true;

		if (args.length > 1) {
			sender.sendMessage(MessageColor.ERROR + "Usage: " + this.getUsage(sender, label, subCommand));
			return true;
		}

		final Chatter chatter = this.plugin.channelHandler.chatterForPlayer(sender.getName());

		if (args.length == 0) {
			sender.sendMessage(MessageColor.HEADER + "--- Ignoring ---");
			final Set<String> ignoring = chatter.ignoring;
			if (ignoring.isEmpty()) {
				sender.sendMessage(NOBODY_MESSAGE);
			} else {
				boolean doneFirst = false;
				final StringBuilder builder = new StringBuilder();
				for (final String player : ignoring) {
					if (doneFirst) {
						builder.append(", ");
					} else {
						doneFirst = true;
					}
					builder.append(player);
				}
				sender.sendMessage(builder.toString());
			}
		} else {
			final String subject = args[0].toLowerCase();
			if (chatter.ignoring.contains(subject)) {
				chatter.ignoring.remove(subject);
				sender.sendMessage(MessageColor.INFO + "No longer ignoring " + MessageColor.PLAYER + subject + MessageColor.INFO + ".");
			} else {
				chatter.ignoring.add(subject);
				sender.sendMessage(MessageColor.INFO + "You are now ignoring " + MessageColor.PLAYER + subject + MessageColor.INFO + ".");
			}
			chatter.setModified(true);
		}
		return true;
	}

	@Override
	public List<String> onTabComplete(final CommandSender sender, final String args[]) {
		if (args.length == 1) {
			return null;
		} else {
			return ImmutableList.<String>of();
		}
	}

}
