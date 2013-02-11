package com.jrtc27.stevechat.config;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;

import com.jrtc27.stevechat.Channel;
import com.jrtc27.stevechat.Chatter;
import com.jrtc27.stevechat.SteveChatPlugin;

public class ConfigHandler {
	private final SteveChatPlugin plugin;
	private final ChannelConfigHandler channelConfig;
	private final ChatterConfigHandler chatterConfig;

	public ConfigHandler(final SteveChatPlugin plugin) {
		this.plugin = plugin;
		this.channelConfig = new ChannelConfigHandler(this.plugin);
		this.chatterConfig = new ChatterConfigHandler(this.plugin);
	}

	public void load() {
		this.plugin.saveDefaultConfig();
		this.plugin.reloadConfig(); // Otherwise this.plugin.getConfig() will contain previously-loaded stuff on a reload
		final FileConfiguration fileConfig = this.plugin.getConfig();

		final String mainChannelName = fileConfig.getString("default-channel");

		final ConfigurationSection formatSection = fileConfig.getConfigurationSection("default-format");
		this.plugin.channelHandler.setDefaultMessageFormat(formatSection.getString("chat"));
		this.plugin.channelHandler.setDefaultAnnounceFormat(formatSection.getString("announce"));
		this.plugin.channelHandler.setDefaultEmoteFormat(formatSection.getString("emote"));
		this.plugin.channelHandler.setPMFormat(formatSection.getString("private-message"));

		this.plugin.channelHandler.setUseTwitterPM(fileConfig.getBoolean("twitter-style-private-messages"));
		this.plugin.checkForUpdates = fileConfig.getBoolean("check-updates", true);

		final Iterable<Channel> channels = this.channelConfig.getChannels();
		if (channels != null) {
			synchronized (this.plugin.channelHandler.channels) {
				for (Channel channel : channels) {
					this.plugin.channelHandler.channels.add(channel);
				}
			}
		}

		final Channel defaultChannel = this.plugin.channelHandler.getChannelByName(mainChannelName);
		if (defaultChannel == null) {
			this.plugin.logWarning("No valid default channel specified!");
		}
		this.plugin.channelHandler.setMainChannel(defaultChannel);
	}

	public Chatter loadChatter(final String name) {
		return this.chatterConfig.loadChatter(name.toLowerCase());
	}

	public void saveAll() {
		this.saveChatters();
		this.saveChannels();
	}

	public void saveChatters() {
		this.chatterConfig.saveModified();
	}

	public void saveChannels() {
		this.channelConfig.saveModified();
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
		return this.channelConfig.addChannel(channel);
	}

	public boolean removeChannel(final Channel channel) {
		return this.channelConfig.removeChannel(channel);
	}

}
