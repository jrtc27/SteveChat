package com.jrtc27.stevechat.listeners;

import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

import org.bukkit.Location;
import org.bukkit.Server;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerChatTabCompleteEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.util.Java15Compat;
import org.bukkit.util.StringUtil;
import org.reflections.Reflections;

import com.google.common.collect.ImmutableList;
import com.jrtc27.stevechat.Channel;
import com.jrtc27.stevechat.Chatter;
import com.jrtc27.stevechat.MessageColor;
import com.jrtc27.stevechat.SCPermission;
import com.jrtc27.stevechat.SteveChatPlugin;
import com.jrtc27.stevechat.Util;
import com.jrtc27.stevechat.command.ChatCommandBase;
import com.jrtc27.stevechat.command.ReloadCommand;

public class PlayerListener implements Listener, TabExecutor {
	private final SteveChatPlugin plugin;

	private final Map<String, ChatCommandBase> chatCommandsMap = new HashMap<String, ChatCommandBase>();
	public final SortedSet<String> mainCommands = new TreeSet<String>();

	// private final Map<String, DefinedCommandBase> definedCommandsMap = new HashMap<String, DefinedCommandBase>();

	public PlayerListener(final SteveChatPlugin plugin) {
		this.plugin = plugin;
		//this.plugin.logInfo("Populating commands map...");
		this.populateChatCommandsMap();
		this.registerDefinedCommands();
		//this.plugin.logInfo("Populated commands map!");
	}

	public ChatCommandBase getChatCommand(final String subCommand) {
		synchronized (this.chatCommandsMap) {
			return this.chatCommandsMap.get(subCommand);
		}
	}

	private void populateChatCommandsMap() {
		final Reflections reflections = Reflections.collect();
		final Set<Class<? extends ChatCommandBase>> commandClasses = reflections.getSubTypesOf(ChatCommandBase.class);
		synchronized (this.chatCommandsMap) {
			this.chatCommandsMap.clear();
			synchronized (this.mainCommands) {
				for (final Class<? extends ChatCommandBase> commandClass : commandClasses) {
					if (Modifier.isAbstract(commandClass.getModifiers())) {
						continue;
					}

					try {
						final ChatCommandBase command = commandClass.getConstructor(SteveChatPlugin.class).newInstance(this.plugin);

						this.chatCommandsMap.put(command.getMainCommand(), command);
						this.mainCommands.add(command.getMainCommand());
						for (final String alias : command.getAliases()) {
							this.chatCommandsMap.put(alias, command);
						}
					} catch (final Exception e) {
						this.plugin.logSevere("Error instantiating " + commandClass.getCanonicalName());
						e.printStackTrace();
					}
				}
			}
		}
	}

	private void registerDefinedCommands() {
		this.plugin.getCommand("stevechat").setExecutor(this);
		this.plugin.getCommand("me").setExecutor(this);
		this.plugin.getCommand("qm").setExecutor(this);
		this.plugin.getCommand("tell").setExecutor(this);
		this.plugin.getCommand("r").setExecutor(this);
	}

	/*private void registerDefinedCommand(final DefinedCommandBase command, final PluginCommand pCommand) {
		this.definedCommandsMap.put(command.getMainCommand(), command);
		for (final String alias : command.getAliases()) {
			this.definedCommandsMap.put(alias, command);
		}
		pCommand.setExecutor(this);
	}*/

	@EventHandler(priority = EventPriority.MONITOR)
	public void onPlayerJoin(final PlayerJoinEvent event) {
		final Player player = event.getPlayer();
		this.plugin.loadingLock.readLock().lock();
		try {
			this.plugin.infoCache.addPlayerToCache(player);
			this.plugin.channelHandler.handlePlayerJoin(player);
		} finally {
			this.plugin.loadingLock.readLock().unlock();
		}
		if (Util.hasCachedPermission(player, SCPermission.ADMIN_MESSAGES, null)) {
			final String adminMessage = this.plugin.adminMessage;
			if (adminMessage != null) {
				player.sendMessage(adminMessage);
			}
		}
	}

	@EventHandler(priority = EventPriority.MONITOR)
	public void onPlayerQuit(final PlayerQuitEvent event) {
		this.plugin.loadingLock.readLock().lock();
		try {
			this.plugin.channelHandler.handlePlayerQuit(event.getPlayer());
		} finally {
			this.plugin.loadingLock.readLock().unlock();
		}
	}

	@EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
	public void onAsyncPlayerChat(final AsyncPlayerChatEvent event) {
		this.plugin.loadingLock.readLock().lock();
		try {
			final String eventMessage = event.getMessage();
			final Player speaker = event.getPlayer();
			final Chatter chatter = this.plugin.channelHandler.chatterForPlayer(speaker.getName());

			// Important - ensure length > 1 and @ has text directly after it
			if (eventMessage.startsWith("@") && this.plugin.channelHandler.useTwitterPM() && eventMessage.length() > 1 && eventMessage.charAt(1) != ' ') {
				event.setCancelled(true);
				this.chatCommandsMap.get("tell").handleCommand(event.getPlayer(), "tell", eventMessage.substring(1).split(" "));
				return;
			}

			final String conversing = chatter.getConversing();

			if (conversing != null) {
				event.setCancelled(true);
				this.chatCommandsMap.get("tell").handleCommand(event.getPlayer(), "tell", (conversing + " " + eventMessage).split(" "));
				return;
			}

			final Set<Player> recipients = event.getRecipients();
			final Channel channel = this.plugin.channelHandler.channelForPlayer(speaker.getName());

			if (channel == null) {
				event.setCancelled(true);
				speaker.sendMessage(MessageColor.INFO + "You must join a channel first.");
				return;
			}

			if (!channel.inWorld(speaker)) {
				event.setCancelled(true);
				speaker.sendMessage(channel.getColor() + channel.getName() + MessageColor.ERROR + " is not available in this world.");
				return;
			}

			if (!channel.isMember(speaker.getName())) { // For if this event was fired from /ch qm
				event.setCancelled(true);
				speaker.sendMessage(MessageColor.INFO + "You must join " + channel.getColor() + channel.getName() + MessageColor.INFO + " first.");
				return;
			}

			if (!channel.canSpeak(speaker)) {
				event.setCancelled(true);
				speaker.sendMessage(MessageColor.INFO + "You cannot speak in " + channel.getColor() + channel.getName() + MessageColor.INFO + ".");
				return;
			}

			if (chatter.isMuted()) {
				event.setCancelled(true);
				speaker.sendMessage(MessageColor.INFO + "You are currently muted server-wide.");
				return;
			} else if (channel.isMuted(speaker)) {
				event.setCancelled(true);
				speaker.sendMessage(MessageColor.INFO + "You are currently muted in " + channel.getColor() + channel.getName() + MessageColor.INFO + ".");
				return;
			}

			event.setMessage(channel.translatePermittedColorCodes(speaker, eventMessage));

			event.setFormat(channel.formatMessage(speaker));

			try {
				recipients.clear();
			} catch (final UnsupportedOperationException e) {
				this.plugin.logWarning("Unsupported operation - set is unmodifiable!");
				event.setCancelled(true);
				event.getPlayer().sendMessage(MessageColor.ERROR + "An internal error occurred whilst sending this message!");
				return;
			}

			final long radiusSq = channel.getRadiusSq();
			final Location origin = speaker.getLocation();
			final Server server = this.plugin.getServer();

			boolean nobodyAround = radiusSq > 0;

			for (final String name : channel.members) {
				final Player player = server.getPlayerExact(name);
				final Chatter recipient = this.plugin.channelHandler.chatterForPlayer(name);

				if (player != null && !recipient.ignoring.contains(speaker.getName()) && channel.inWorld(player)) {
					final Location destination = player.getLocation();

					if (radiusSq <= 0 || destination.distanceSquared(origin) <= radiusSq) {
						recipients.add(player);
						if (!player.equals(speaker) && !Util.hasCachedPermission(player, SCPermission.EAVESDROP, null) && speaker.canSee(player))
							nobodyAround = false;
					}
				}
			}

			if (nobodyAround) {
				// Manually ensure the player gets messages in the right order
				recipients.remove(speaker);
				speaker.sendMessage(String.format(event.getFormat(), speaker.getDisplayName(), event.getMessage()));
				speaker.sendMessage(MessageColor.INFO + "Nobody hears you...");
			}
		} finally {
			this.plugin.loadingLock.readLock().unlock();
		}
	}

	@EventHandler(ignoreCancelled = true)
	public void onPlayerCommandPreProcess(final PlayerCommandPreprocessEvent event) {
		this.plugin.loadingLock.readLock().lock();
		try {
			final char firstChar = event.getMessage().charAt(0); // Special character at the start
			final String input = event.getMessage().substring(1);
			final String[] args = input.split(" ");
			final Channel potentialChannel = this.plugin.channelHandler.getChannelByName(args[0]);
			if (potentialChannel != null && potentialChannel.isQMShortcutEnabled()) {
				final int length = args.length;
				if (length > 1) { // Check there is a message as well as the channel name
					final StringBuilder messageBuilder = new StringBuilder();
					messageBuilder.append(firstChar);
					messageBuilder.append("ch qm");
					for (final String arg : args) {
						messageBuilder.append(" ").append(arg);
					}
					event.setMessage(messageBuilder.toString());
				} else {
					event.getPlayer().sendMessage(MessageColor.ERROR + "Usage: /" + args[0] + " <message>");
					event.setCancelled(true);
				}
			}
		} finally {
			this.plugin.loadingLock.readLock().unlock();
		}
	}

	@Override
	public boolean onCommand(final CommandSender sender, final Command command, final String label, final String[] args) {
		this.plugin.loadingLock.readLock().lock();
		try {
			if (command.getName().equals("stevechat")) {
				if (args.length == 0) {
					return false;
				}
				final ChatCommandBase commandBase = this.chatCommandsMap.get(args[0]);
				if (commandBase != null) {
					if (commandBase instanceof ReloadCommand) {
						return ((ReloadCommand) commandBase).handleCommand(sender, label, args[0], Java15Compat.Arrays_copyOfRange(args, 1, args.length), true);
					} else {
						return commandBase.handleCommand(sender, label, args[0], Java15Compat.Arrays_copyOfRange(args, 1, args.length));
					}
				} else {
					final Channel channel = this.plugin.channelHandler.getChannelByName(args[0]);
					if (channel != null) {
						return this.chatCommandsMap.get("join").handleCommand(sender, label, null, args);
					}
				}
				return false;
			} else {
				/*
				 * final DefinedCommandBase commandBase = this.definedCommandsMap.get(label);
				 * if (commandBase != null) {
				 * return commandBase.handleCommand(sender, label, args);
				 * }
				 */

				final ChatCommandBase commandBase = this.chatCommandsMap.get(label);
				if (commandBase != null && commandBase.canBeRoot()) {
					return commandBase.handleCommand(sender, label, null, args);
				}
			}
			sender.sendMessage(MessageColor.ERROR + "An internal error has occurred.");
			return false;
		} finally {
			this.plugin.loadingLock.readLock().unlock();
		}
	}

	@EventHandler
	public void onPlayerChatTabComplete(final PlayerChatTabCompleteEvent event) {
		this.plugin.loadingLock.readLock().lock();
		try {
			final Player player = event.getPlayer();
			final String message = event.getChatMessage();

			if (message.startsWith("@") && this.plugin.channelHandler.useTwitterPM() && message.indexOf(' ') == -1) {
				final String partialPlayer = message.substring(1);
				final Collection<String> completions = event.getTabCompletions();

				completions.clear();

				for (final Player p : this.plugin.getServer().getOnlinePlayers()) {
					if (player.canSee(p) && StringUtil.startsWithIgnoreCase(p.getName(), partialPlayer)) {
						completions.add("@" + p.getName());
					}
				}
			}
		} finally {
			this.plugin.loadingLock.readLock().unlock();
		}
	}

	@Override
	public List<String> onTabComplete(final CommandSender sender, final Command command, final String alias, final String[] args) {
		this.plugin.loadingLock.readLock().lock();
		try {
			if (command.getName().equals("stevechat")) {
				if (args.length == 1) {
					final List<String> subCommands = new ArrayList<String>();
					final String partialSubCommand = args[0];
					for (final String key : this.chatCommandsMap.keySet()) {
						final ChatCommandBase chatCommand = this.chatCommandsMap.get(key);
						if (!chatCommand.testPermissionSilent(sender)) continue;

						if (StringUtil.startsWithIgnoreCase(key, partialSubCommand) && !subCommands.contains(key)) {
							subCommands.add(key);
						}
					}
					synchronized (this.plugin.channelHandler.channels) {
						for (Channel channel : this.plugin.channelHandler.channels) {
							if (!channel.canSee((Player) sender)) continue;

							final String name, shortname;

							synchronized (channel.baseAttrsLock) {
								name = channel.getName();
								shortname = channel.getShortname();
							}

							if (StringUtil.startsWithIgnoreCase(name, partialSubCommand) && !subCommands.contains(name)) {
								subCommands.add(name);
							}

							if (StringUtil.startsWithIgnoreCase(shortname, partialSubCommand) && !subCommands.contains(shortname)) {
								subCommands.add(shortname);
							}
						}
					}
					Collections.sort(subCommands, String.CASE_INSENSITIVE_ORDER);
					return subCommands;
				} else {
					final ChatCommandBase commandBase = this.chatCommandsMap.get(args[0]);
					if (commandBase != null) {
						if (commandBase.testPermissionSilent(sender)) {
							return commandBase.onTabComplete(sender, Java15Compat.Arrays_copyOfRange(args, 1, args.length));
						} else {
							return ImmutableList.<String>of();
						}
					}
				}
			}
			return null;
		} finally {
			this.plugin.loadingLock.readLock().unlock();
		}
	}
}
