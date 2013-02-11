package com.jrtc27.stevechat.command;

import org.bukkit.Location;
import org.bukkit.Server;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import com.jrtc27.stevechat.Channel;
import com.jrtc27.stevechat.Chatter;
import com.jrtc27.stevechat.MessageColor;
import com.jrtc27.stevechat.SCPermission;
import com.jrtc27.stevechat.SteveChatPlugin;
import com.jrtc27.stevechat.Util;

public class EmoteCommand extends ChatCommandBase {

	public EmoteCommand(final SteveChatPlugin plugin) {
		super(plugin);
		this.permission = SCPermission.EMOTE_COMMAND;
		this.usage = " <action>";
		this.mainCommand = "me";
		this.aliases = new String[] { "emote", "action" };
		this.description = "Perform the specified action in chat.";
	}

	@Override
	public boolean handleCommand(final CommandSender sender, final String label, final String subCommand, final String[] args) {
		if (!this.validatePlayer(sender)) return true;
		if (!this.testPermission(sender)) return true;

		if (args.length == 0) {
			sender.sendMessage(MessageColor.ERROR + "Usage: " + this.getUsage(sender, label, subCommand));
			return true;
		}

		final Player speaker = (Player) sender;

		final Chatter chatter = this.plugin.channelHandler.chatterForPlayer(sender.getName());
		final Channel channel = this.plugin.channelHandler.channelForPlayer(sender.getName());

		if (channel == null) {
			sender.sendMessage(MessageColor.ERROR + "You must join a channel first.");
			return true;
		}

		if (!channel.inWorld(speaker)) {
			sender.sendMessage(channel.getColor() + channel.getName() + MessageColor.ERROR + " is not available in this world.");
			return true;
		}

		if (!channel.canEmote(speaker)) {
			sender.sendMessage(MessageColor.ERROR + "You do not have permission to emote in " + channel.getColor() + channel.getName() + MessageColor.ERROR + ".");
			return true;
		}

		if (chatter.isMuted()) {
			sender.sendMessage(MessageColor.INFO + "You are currently muted server-wide.");
			return true;
		} else if (channel.isMuted(speaker)) {
			sender.sendMessage(MessageColor.INFO + "You are currently muted in " + channel.getColor() + channel.getName() + MessageColor.INFO + ".");
			return true;
		}

		final StringBuilder actionBuilder = new StringBuilder();

		final int count = args.length;
		for (int i = 0; i < count; i++) {
			final String arg = args[i];
			if (i > 0) {
				actionBuilder.append(" ");
			}
			actionBuilder.append(arg);
		}

		final String action = channel.formatEmote(speaker, actionBuilder.toString()); // Handles color code translation

		this.plugin.getServer().getConsoleSender().sendMessage(action);

		final long radiusSq = channel.getRadiusSq();
		final Location origin = speaker.getLocation();
		final Server server = this.plugin.getServer();

		boolean nobodyAround = radiusSq > 0;

		for (final String name : channel.members) {
			final Player player = server.getPlayerExact(name);
			final Chatter recipient = this.plugin.channelHandler.chatterForPlayer(name);

			if (player != null && !recipient.ignoring.contains(sender.getName()) && channel.inWorld(player)) {
				final Location destination = player.getLocation();

				if (radiusSq <= 0 || destination.distanceSquared(origin) <= radiusSq) {
					player.sendMessage(action);
					if (!player.equals(sender) && !Util.hasCachedPermission(player, SCPermission.EAVESDROP, null) && speaker.canSee(player))
						nobodyAround = false;
				}
			}
		}

		if (nobodyAround) {
			sender.sendMessage(MessageColor.INFO + "Nobody hears you...");
		}

		return true;
	}

	@Override
	public boolean canBeRoot() {
		return true;
	}

}
