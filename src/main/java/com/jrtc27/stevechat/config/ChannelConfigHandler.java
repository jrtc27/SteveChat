package com.jrtc27.stevechat.config;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import com.jrtc27.stevechat.Channel;
import com.jrtc27.stevechat.Chatter;
import com.jrtc27.stevechat.SteveChatPlugin;
import com.jrtc27.stevechat.Util;

public class ChannelConfigHandler {
	private final SteveChatPlugin plugin;

	public ChannelConfigHandler(final SteveChatPlugin plugin) {
		this.plugin = plugin;
	}

	private File getChannelsFolder() {
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

		final File channelsFolder = new File(dataFolder, "channels");
		if (!channelsFolder.exists()) {
			if (channelsFolder.mkdir()) {
				this.saveDefaultChannels();
			} else {
				this.plugin.logSevere("Failed to create channels folder!");
				return null;
			}
		} else if (!channelsFolder.isDirectory()) {
			this.plugin.logSevere("Channels folder is not a directory!");
			return null;
		}
		return channelsFolder;
	}

	private void saveDefaultChannels() {
		this.plugin.saveResource("channels/Global.yml", false);
		this.plugin.logInfo("Created default channels!");
	}

	public Iterable<Channel> getChannels() {
		final File channelsFolder = this.getChannelsFolder();
		if (channelsFolder == null) return null;

		final File[] files = channelsFolder.listFiles(new FilenameFilter() {

			@Override
			public boolean accept(File dir, String name) {
				return name.toLowerCase().endsWith(".yml");
			}

		});

		if (files == null || files.length == 0) {
			this.plugin.logSevere("No channels found!");
			return null;
		}

		final List<Channel> channels = new ArrayList<Channel>();

		for (final File file : files) {
			final FileConfiguration configuration = new YamlConfiguration();
			try {
				configuration.load(file);
			} catch (FileNotFoundException e) {
				// This should never happen (we just got this file based on existing files) - print a stacktrace if it does
				e.printStackTrace();
				continue;
			} catch (IOException e) {
				this.plugin.logSevere("IO error reading channel file " + file.getName());
				e.printStackTrace();
				continue;
			} catch (InvalidConfigurationException e) {
				this.plugin.logSevere("Channel file " + file.getName() + " contains invalid YAML syntax! Details: " + e.getMessage());
				continue;
			}

			final Channel channel = Channel.loadFromConfiguration(this.plugin, file.getName(), configuration);

			channels.add(channel);
		}

		return channels;
	}

	public void saveModified() {
		final File channelsFolder = this.getChannelsFolder();
		if (channelsFolder == null) return;

		final Iterable<Channel> channels = this.plugin.channelHandler.channels;

		for (final Channel channel : channels) {
			if (channel.isModified()) {
				channel.setModified(false);

				final File channelFile = new File(channelsFolder, channel.getFilename());
				final FileConfiguration config = new YamlConfiguration();

				channel.writeToConfig(config);

				try {
					config.save(channelFile);
				} catch (IOException e) {
					this.plugin.logSevere("IO error writing channel file " + channel.getFilename());
					e.printStackTrace();
				}
			}
		}
	}

	/**
	 * To be called <em>after</em> initial setup.
	 *
	 * NB: Will <strong>NOT</strong> check for duplicate names
	 *
	 * @param channel Channel to add
	 * @return Whether the operation was successful
	 */
	public boolean addChannel(final Channel channel) {
		this.plugin.channelHandler.channels.add(channel);
		if (!this.recalculatePermissions()) return false;
		return true;
	}

	public boolean removeChannel(final Channel channel) {
		final File channelsFolder = this.getChannelsFolder();
		if (channelsFolder == null) return false;

		for (final Chatter chatter : this.plugin.channelHandler.getAllChatters()) {
			synchronized (chatter.activeChannelLock) {
				if (channel.equals(chatter.getActiveChannel())) {
					chatter.setActiveChannel(null);
				}
			}
		}
		this.plugin.channelHandler.channels.remove(channel);
		if (!this.recalculatePermissions()) return false;

		final File chatterFile = new File(channelsFolder, channel.getFilename());

		try {
			chatterFile.delete();
		} catch (SecurityException e) {
			e.printStackTrace();
			return false;
		}
		return true;
	}

	private boolean recalculatePermissions() {
		try {
			if (this.plugin.loadingLock.writeLock().tryLock(5, TimeUnit.SECONDS)) {
				synchronized (this.plugin.infoCache.cachedLock) { // Stop cache from updating while we recalculate permissions
					Util.registerPermissions(); // Populate permissions list, assign default values and set inheritance
					this.plugin.infoCache.determineNodesToCheck(); // Redetermine what permission nodes should be cached
				}
			}
		} catch (InterruptedException e) {
			e.printStackTrace();
			return false;
		}
		return true;
	}

}
