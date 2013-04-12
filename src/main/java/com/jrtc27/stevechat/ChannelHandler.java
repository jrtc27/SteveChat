package com.jrtc27.stevechat;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class ChannelHandler {
	private final SteveChatPlugin plugin;

	public final List<Channel> channels = new ArrayList<Channel>();
	private final Map<String, Chatter> chatters = new HashMap<String, Chatter>();

	private Channel mainChannel = null;

	private String defaultMessageFormat = "{color}[{nick}] &f[{group}] {sender}&f: {msg}";
	private String defaultAnnounceFormat = "{color}[{nick}]&f {msg}";
	private String defaultEmoteFormat = "{color}[{nick}] * {sender}&f {msg}";
	private String pmFormat = "&d{tofrom} {partner}&d: {msg}";
	private boolean useTwitterPM = true;

	public final Object defaultMessageFormatLock = new Object();
	public final Object defaultAnnounceFormatLock = new Object();
	public final Object defaultEmoteFormatLock = new Object();
	public final Object pmFormatLock = new Object();
	public final Object useTwitterPMLock = new Object();

	public ChannelHandler(final SteveChatPlugin plugin) {
		this.plugin = plugin;
		//this.setMainChannel(new Channel(plugin, "Global", "G", ChatColor.DARK_GREEN, this.defaultMessageFormat, this.defaultAnnounceFormat, this.defaultEmoteFormat));
		//this.getMainChannel().setQMShortcutEnabled(true);
		//this.channels.add(this.getMainChannel());
		//this.channels.add(new Channel(plugin, "Creative", "Creative", ChatColor.BLUE, this.defaultMessageFormat, this.defaultAnnounceFormat, this.defaultEmoteFormat));
	}

	public Channel getMainChannel() {
		return mainChannel;
	}

	public void setMainChannel(Channel mainChannel) {
		this.mainChannel = mainChannel;
	}

	public String getDefaultMessageFormat() {
		synchronized (this.defaultMessageFormatLock) {
			return this.defaultMessageFormat;
		}
	}

	public void setDefaultMessageFormat(final String defaultMessageFormat) {
		synchronized (this.defaultMessageFormatLock) {
			this.defaultMessageFormat = defaultMessageFormat;
		}
	}

	public String getDefaultAnnounceFormat() {
		synchronized (this.defaultAnnounceFormatLock) {
			return this.defaultAnnounceFormat;
		}
	}

	public void setDefaultAnnounceFormat(final String defaultAnnounceFormat) {
		synchronized (this.defaultAnnounceFormatLock) {
			this.defaultAnnounceFormat = defaultAnnounceFormat;
		}
	}

	public String getDefaultEmoteFormat() {
		synchronized (this.defaultEmoteFormatLock) {
			return this.defaultEmoteFormat;
		}
	}

	public void setDefaultEmoteFormat(final String defaultEmoteFormat) {
		synchronized (this.defaultEmoteFormatLock) {
			this.defaultEmoteFormat = defaultEmoteFormat;
		}
	}

	public String getPMFormat() {
		synchronized (this.pmFormatLock) {
			return this.pmFormat;
		}
	}

	public void setPMFormat(final String pmFormat) {
		synchronized (this.pmFormatLock) {
			this.pmFormat = pmFormat;
		}
	}

	public boolean useTwitterPM() {
		synchronized (this.useTwitterPMLock) {
			return this.useTwitterPM;
		}
	}

	public void setUseTwitterPM(final boolean useTwitterPM) {
		synchronized (this.useTwitterPMLock) {
			this.useTwitterPM = useTwitterPM;
		}
	}

	public void handlePlayerJoin(final Player player) {
		synchronized (this.chatters) {
			final Chatter chatter = this.plugin.configHandler.loadChatter(player.getName().toLowerCase());
			for (final String channelName : chatter.channelsToJoin) {
				final Channel channel = this.getChannelByName(channelName);
				if (channel != null && channel.canJoin(player)) {
					channel.addMember(player.getName());
				}
			}
			final Channel prevActiveChannel = chatter.getActiveChannel();
			synchronized (this.channels) {
				for (final Channel channel : this.channels) {
					if (chatter.treatAsNew() && channel.shouldAutoJoinIfNew(player)) {
						channel.addMember(player.getName());
					}

					if (channel.shouldAlwaysAutoJoin(player)) {
						channel.addMember(player.getName());
					}

					if (channel.shouldAlwaysAutoLeave(player)) {
						channel.removeMember(player.getName());
					}

					if (channel.isMember(player.getName())) {
						synchronized (chatter) {
							if (chatter.getActiveChannel() == null) {
								chatter.setActiveChannel(channel);
							}
						}
					}
				}
			}
			if ((prevActiveChannel == null || !prevActiveChannel.isMember(player.getName())) && this.getMainChannel() != null && this.getMainChannel().isMember(player.getName())) {
				chatter.setActiveChannel(this.getMainChannel());
			}
			chatter.setTreatAsNew(false);
			chatter.setModified(true);
			chatter.setChannelsToJoin(null);
			this.chatters.put(player.getName().toLowerCase(), chatter);
		}
	}

	public void handlePlayerQuit(final Player player) {
		synchronized (this.chatters) {
			final Chatter chatter = this.chatters.get(player.getName().toLowerCase());
			if (chatter == null) { // Something dodgy is going on here...
				this.plugin.logWarning("No chatter found for leaving player " + player.getName());
				return;
			}

			final String playerName = player.getName().toLowerCase();
			final Set<String> channels = new HashSet<String>();
			synchronized (this.channels) {
				for (final Channel channel : this.channels) {
					if (channel.isMember(playerName)) {
						channels.add(channel.getName());
					}
				}
			}

			chatter.setChannelsToJoin(channels);
			chatter.updateLogoutTime();
			chatter.setModified(true);
		}
	}

	public Channel channelForPlayer(final String name) {
		final Chatter chatter;
		synchronized (this.chatters) {
			chatter = this.chatters.get(name.toLowerCase());
		}
		if (chatter == null) {
			synchronized (this.channels) {
				for (final Channel channel : this.channels) {
					if (channel.isMember(name)) {
						return channel;
					}
				}
			}
			return null;
		} else {
			return chatter.getActiveChannel();
		}
	}

	public Channel getChannelByName(final String name) {
		synchronized (this.channels) {
			for (final Channel channel : this.channels) {
				synchronized (channel.baseAttrsLock) {
					if (name.equalsIgnoreCase(channel.getShortname()) || name.equalsIgnoreCase(channel.getName())) {
						return channel;
					}
				}
			}
		}
		return null;
	}

	/**
	 * Returns the <em>live</em> Chatter object for a given player's name
	 *
	 * @param name The player for whom to get the Chatter
	 * @return A <em>live</em> Chatter object
	 */
	public Chatter chatterForPlayer(final String name) {
		Chatter chatter;
		synchronized (this.chatters) {
			chatter = this.chatters.get(name.toLowerCase());
			if (chatter == null) {
				chatter = this.plugin.configHandler.loadChatter(name.toLowerCase());
				this.chatters.put(name.toLowerCase(), chatter);
			}
		}
		return chatter;
	}

	public Iterable<Chatter> getAllChatters() {
		return this.chatters.values();
	}

	public void removeChatter(final Chatter toRemove) {
		this.chatters.values().remove(toRemove);
	}

	public void removeChatters(final Collection<Chatter> toRemove) {
		this.chatters.values().removeAll(toRemove);
	}

	public String formatPM(final boolean from, final CommandSender sender, final CommandSender partner, final String message) {
		String format;
		synchronized (this.pmFormatLock) {
			format = this.pmFormat;
		}
		if (format == null) {
			this.plugin.logSevere("No valid PM format has been set! Using default format.");
			format = "&d{tofrom} {partner}&d: {msg}";
		}

		final PlayerInfoCache cache = this.plugin.infoCache;

		final String partnerFullName = partner.getName();
		String partnerDisplayName = partnerFullName;
		String partnerGroup = "";
		String partnerPrefix = "";
		String partnerSuffix = "";
		String partnerGroupPrefix = "";
		String partnerGroupSuffix = "";
		String partnerWorld = "all";

		if (partner instanceof Player) {
			final Player player = (Player) partner;

			partnerDisplayName = player.getDisplayName();
			partnerGroup = cache.getGroup(player);
			partnerPrefix = ChatColor.translateAlternateColorCodes('&', cache.getPrefix(player));
			partnerSuffix = ChatColor.translateAlternateColorCodes('&', cache.getSuffix(player));
			partnerGroupPrefix = ChatColor.translateAlternateColorCodes('&', cache.getGroupPrefix(player));
			partnerGroupSuffix = ChatColor.translateAlternateColorCodes('&', cache.getGroupSuffix(player));
			partnerWorld = player.getWorld().getName();
		} else {
			partnerDisplayName = ChatColor.GOLD + "[" + partnerDisplayName + "]";
		}

		String formattedPM = ChatColor.translateAlternateColorCodes('&', format);

		formattedPM = FormatTag.replaceAll(FormatTag.MESSAGE, formattedPM, message);
		formattedPM = FormatTag.replaceAll(FormatTag.TO_FROM, formattedPM, from ? "From" : "To");
		formattedPM = FormatTag.replaceAll(FormatTag.PARTNER_NICK, formattedPM, partnerDisplayName);
		formattedPM = FormatTag.replaceAll(FormatTag.PARTNER_NAME, formattedPM, partnerFullName);
		formattedPM = FormatTag.replaceAll(FormatTag.PARTNER_WORLD, formattedPM, partnerWorld);
		formattedPM = FormatTag.replaceAll(FormatTag.PARTNER_PREFIX, formattedPM, partnerPrefix);
		formattedPM = FormatTag.replaceAll(FormatTag.PARTNER_SUFFIX, formattedPM, partnerSuffix);
		formattedPM = FormatTag.replaceAll(FormatTag.PARTNER_GROUP, formattedPM, partnerGroup);
		formattedPM = FormatTag.replaceAll(FormatTag.PARTNER_GROUP_PREFIX, formattedPM, partnerGroupPrefix);
		formattedPM = FormatTag.replaceAll(FormatTag.PARTNER_GROUP_SUFFIX, formattedPM, partnerGroupSuffix);

		return formattedPM;
	}

}
