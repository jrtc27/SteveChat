package com.jrtc27.stevechat.command;

import java.util.List;

import org.bukkit.command.CommandSender;

import com.google.common.collect.ImmutableList;
import com.jrtc27.stevechat.MessageColor;
import com.jrtc27.stevechat.SCPermission;
import com.jrtc27.stevechat.SteveChatPlugin;

public class ReloadCommand extends ChatCommandBase {

	public ReloadCommand(final SteveChatPlugin plugin) {
		super(plugin);
		this.permission = SCPermission.RELOAD;
		// No need to set usage
		this.mainCommand = "reload";
		this.aliases = new String[] { "load", "refresh" };
		this.description = "Reload SteveChat's config.";
	}

	@Override
	public boolean handleCommand(final CommandSender sender, final String label, final String subCommand, final String[] args) {
		throw new UnsupportedOperationException("acquiredLock must be explicitly specified!");
	}

	public boolean handleCommand(final CommandSender sender, final String label, final String subCommand, final String[] args, final boolean acquiredLock) {
		if (!this.testPermission(sender)) return true;

		if (args.length != 0) {
			sender.sendMessage(MessageColor.ERROR + "Usage: " + this.getUsage(sender, label, subCommand));
		}
		this.plugin.reload(acquiredLock, sender);
		return true;
	}

	@Override
	public List<String> onTabComplete(final CommandSender sender, final String[] args) {
		return ImmutableList.<String>of();
	}

}
