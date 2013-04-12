package com.jrtc27.stevechat.config;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import com.jrtc27.stevechat.Channel;
import com.jrtc27.stevechat.Chatter;
import com.jrtc27.stevechat.SteveChatPlugin;
import com.jrtc27.stevechat.Util;

public class ChatterConfigHandler {
	private final SteveChatPlugin plugin;

	private static final long UNLOAD_TIME_MILLIS = 60000;

	public ChatterConfigHandler(final SteveChatPlugin plugin) {
		this.plugin = plugin;
	}

	private synchronized File getChattersFolder() {
		final File dataFolder = this.plugin.getDataFolder();
		if (!dataFolder.exists()) {
			if (!dataFolder.mkdir()) {
				this.plugin.logSevere("Failed to create data folder!");
				return null;
			}
		} else if (!dataFolder.isDirectory()) {
			this.plugin.logSevere("Data folder is not a directory!");
			return null;
		}

		final File chattersFolder = new File(dataFolder, "players");
		if (!chattersFolder.exists()) {
			if (!chattersFolder.mkdir()) {
				this.plugin.logSevere("Failed to create players folder!");
				return null;
			}
		} else if (!chattersFolder.isDirectory()) {
			this.plugin.logSevere("Players folder is not a directory!");
			return null;
		}
		return chattersFolder;
	}

	public synchronized Chatter loadChatter(final String name) {
		final File chattersFolder = this.getChattersFolder();
		if (chattersFolder == null) return null;

		final Chatter chatter = new Chatter(name.toLowerCase());
		final File chatterFile = new File(chattersFolder, name.toLowerCase() + ".yml");

		if (chatterFile.exists()) {
			final FileConfiguration configuration = new YamlConfiguration();

			try {
				configuration.load(chatterFile);
			} catch (FileNotFoundException e) {
				// This should never happen - print a stacktrace if it does
				e.printStackTrace();
				return null;
			} catch (IOException e) {
				this.plugin.logSevere("IO error reading player file " + chatterFile.getName());
				e.printStackTrace();
				return null;
			} catch (InvalidConfigurationException e) {
				this.plugin.logSevere("Player file " + chatterFile.getName() + " contains invalid YAML syntax! Details: " + e.getMessage());
				return null;
			}

			final String active = configuration.getString("active-channel");
			final List<String> channels = configuration.getStringList("channels");
			final List<String> ignoring = configuration.getStringList("ignoring");
			final boolean muted = configuration.getBoolean("muted", false);
			final boolean treatAsNew = configuration.getBoolean("new", true);

			final Channel activeChannel = this.plugin.channelHandler.getChannelByName(active);
			chatter.setActiveChannel(activeChannel);

			if (channels != null) {
				chatter.setChannelsToJoin(channels);
			}

			if (ignoring != null) {
				for (final String ignored : ignoring) {
					chatter.ignoring.add(ignored.toLowerCase());
				}
			}

			chatter.setMuted(muted);
			chatter.setTreatAsNew(treatAsNew);
		}
		chatter.setModified(true);

		return chatter;
	}

	public synchronized void saveModified() {
		final File chattersFolder = this.getChattersFolder();
		if (chattersFolder == null) return;

		final Map<String, Set<String>> membersMap = new HashMap<String, Set<String>>();

		synchronized (this.plugin.channelHandler.channels) {
			for (final Channel channel : this.plugin.channelHandler.channels) {
				for (final String member : channel.members) {
					Set<String> entry = membersMap.get(member);
					if (entry == null) {
						entry = new HashSet<String>();
					}
					entry.add(channel.getName());
					membersMap.put(member, entry);
				}
			}
		}

		final Iterable<Chatter> chatters = this.plugin.channelHandler.getAllChatters();

		for (final Chatter chatter : chatters) {
			if (chatter.isModified()) {
				chatter.setModified(false);
				final File chatterFile = new File(chattersFolder, chatter.playerName.toLowerCase() + ".yml");
				final FileConfiguration configuration = new YamlConfiguration();

				final Channel activeChannel = chatter.getActiveChannel();

				if (activeChannel != null) {
					configuration.set("active-channel", activeChannel.getName());
				} else {
					configuration.set("active-channel", "");
				}
				Set<String> channels = membersMap.get(chatter.playerName);
				if (channels == null) {
					channels = new HashSet<String>();
				}
				// If the chatter was loaded while the player was offline, they will not be in any channels
				// but will have channels that were loaded and have not yet been joined, so we must save
				// these back to the storage file as well.
				channels.addAll(chatter.channelsToJoin);
				configuration.set("channels", new ArrayList<String>(channels));

				configuration.set("ignoring", new ArrayList<String>(chatter.ignoring));
				configuration.set("muted", chatter.isMuted());
				configuration.set("new", chatter.treatAsNew());

				try {
					configuration.save(chatterFile);
				} catch (IOException e) {
					this.plugin.logSevere("IO error writing player file " + chatterFile.getName());
					e.printStackTrace();
				}
			}
		}
	}

	public synchronized void cleanup() {
		long time = System.currentTimeMillis();

		final Iterable<Chatter> chatters = this.plugin.channelHandler.getAllChatters();

		final Set<Chatter> toRemove = new HashSet<Chatter>();
		for (final Chatter chatter : chatters) {
			final Player player = Util.getPlayer(chatter.playerName, true);
			if (player == null && !chatter.isModified() && time - chatter.getLogoutTime() > UNLOAD_TIME_MILLIS) {
				toRemove.add(chatter);
			}
		}

		this.plugin.channelHandler.removeChatters(toRemove);
	}
}
