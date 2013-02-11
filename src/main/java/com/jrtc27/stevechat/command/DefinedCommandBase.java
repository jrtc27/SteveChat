package com.jrtc27.stevechat.command;

import org.bukkit.command.Command;

import com.jrtc27.stevechat.SteveChatPlugin;

public abstract class DefinedCommandBase extends CommandBase {

	public DefinedCommandBase(final SteveChatPlugin plugin, final Command command) {
		super(plugin);
		this.usage = command.getUsage().replaceFirst("^[^\\s]*", "");
		this.mainCommand = command.getName();
		this.aliases = command.getAliases().toArray(new String[0]);
	}

}
