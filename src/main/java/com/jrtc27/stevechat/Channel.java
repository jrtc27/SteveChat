package com.jrtc27.stevechat;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.bukkit.ChatColor;
import org.bukkit.configuration.MemoryConfiguration;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;

public class Channel implements IPermissionDependency {
	private final SteveChatPlugin plugin;

	private final String filename;

	private String name = null;
	private String shortname = null;
	private ChatColor color = ChatColor.WHITE;
	private String password = "";
	private boolean qmShortcutEnabled = false;
	private long radius = 0;
	private boolean announceActivity = true;
	private boolean serverWide = true;

	private boolean modified = false;

	public final Object baseAttrsLock = new Object();

	private String messageFormat = null;

	private String announceFormat = "{default}";
	private String emoteFormat = "{default}";

	public final Object messageFormatLock = new Object();
	public final Object announceFormatLock = new Object();
	public final Object emoteFormatLock = new Object();

	public final Set<String> members = new CopyOnWriteArraySet<String>();
	public final Set<String> muted = new CopyOnWriteArraySet<String>();
	public final Set<String> banned = new CopyOnWriteArraySet<String>();
	public final Set<String> worlds = new CopyOnWriteArraySet<String>();

	public Channel(final SteveChatPlugin plugin, final String filename) {
		this.plugin = plugin;
		this.filename = filename;
	}

	public static Channel loadFromConfiguration(final SteveChatPlugin plugin, final String filename, final MemoryConfiguration config) {
		final String name = config.getString(ConfigKeys.NAME);
		final String nick = config.getString(ConfigKeys.NICK);
		final String format = config.getString(ConfigKeys.FORMAT);
		final String colorName = config.getString(ConfigKeys.COLOR);
		final long radius = config.getLong(ConfigKeys.RADIUS);
		final boolean qmShortcut = config.getBoolean(ConfigKeys.QM_SHORTCUT);
		final boolean announceActivity = config.getBoolean(ConfigKeys.ANNOUNCE_ACTIVITY);
		final boolean serverWide = config.getBoolean(ConfigKeys.SERVER_WIDE);
		final List<String> worlds = config.getStringList(ConfigKeys.WORLDS);
		final List<String> banned = config.getStringList(ConfigKeys.BANNED);
		final List<String> muted = config.getStringList(ConfigKeys.MUTED);

		// OK, config is loaded - let's create the channel!
		Channel channel = new Channel(plugin, filename);
		channel.setName(name);
		channel.setShortname(nick);
		channel.setMessageFormat(format);
		try {
			ChatColor color = ChatColor.valueOf(colorName);
			channel.setColor(color);
		} catch (IllegalArgumentException e) {
			plugin.logWarning("Invalid channel color '" + colorName + "'");
			channel.setColor(ChatColor.WHITE);
		}
		channel.setRadius(radius);
		channel.setQMShortcutEnabled(qmShortcut);
		channel.setAnnounceActivity(announceActivity);
		channel.setServerWide(serverWide);
		for (final String world : worlds) {
			channel.addWorld(world);
		}
		for (final String banEntry : banned) {
			channel.banPlayer(banEntry);
		}
		for (final String muteEntry : muted) {
			channel.mutePlayer(muteEntry);
		}

		return channel;
	}

	public void writeToConfig(final FileConfiguration config) {
		config.set(ConfigKeys.NAME, this.getName());
		config.set(ConfigKeys.NICK, this.getShortname());
		config.set(ConfigKeys.FORMAT, this.getMessageFormat());
		config.set(ConfigKeys.COLOR, this.getColor().name());
		config.set(ConfigKeys.RADIUS, this.getRadius());
		config.set(ConfigKeys.QM_SHORTCUT, this.isQMShortcutEnabled());
		config.set(ConfigKeys.ANNOUNCE_ACTIVITY, this.shouldAnnounceActivity());
		config.set(ConfigKeys.SERVER_WIDE, this.isServerWide());
		config.set(ConfigKeys.WORLDS, new ArrayList(this.worlds));
		config.set(ConfigKeys.BANNED, new ArrayList(this.banned));
		config.set(ConfigKeys.MUTED, new ArrayList(this.muted));
	}

	public String getFilename() {
		return this.filename;
	}

	@Override
	public String permissionExtension() {
		synchronized (this.baseAttrsLock) {
			return this.name;
		}
	}

	public void broadcast(final String message, final String sender) {
		this.plugin.getServer().getConsoleSender().sendMessage(message);

		for (final String name : this.members) {
			final Player player = Util.getPlayer(name, false);
			final Chatter recipient = this.plugin.channelHandler.chatterForPlayer(name);
			if (player != null && (sender == null || !recipient.ignoring.contains(sender)) && this.inWorld(player)) {
				player.sendMessage(message);
			}
		}
	}

	public void addMember(final String name) {
		this.members.add(name.toLowerCase());
	}

	public void removeMember(final String name) {
		this.members.remove(name.toLowerCase());
	}

	public boolean isMember(final String name) {
		return this.members.contains(name.toLowerCase());
	}

	/**
	 * <em><strong>Does not verify that the player is in the correct world - use {@link #inWorld(Player)}</strong></em>
	 *
	 * @param player Player whose joining abilities are to be put to the test
	 * @return Whether the specified player can join the channel
	 */
	public boolean canJoin(final Player player) {
		return Util.hasCachedPermission(player, SCPermission.JOIN, this) && !this.isBanned(player);
	}

	public boolean canSee(final Player player) {
		return Util.hasCachedPermission(player, SCPermission.JOIN, this) && this.inWorld(player);
	}

	public boolean shouldAutoJoinIfNew(final Player player) {
		return Util.hasCachedPermission(player, SCPermission.AUTO_JOIN, this) && this.canJoin(player);
	}

	public boolean shouldAlwaysAutoJoin(final Player player) {
		return Util.hasCachedPermission(player, SCPermission.FORCE_JOIN, this) && this.canJoin(player);
	}

	public boolean shouldAlwaysAutoLeave(final Player player) {
		return Util.hasCachedPermission(player, SCPermission.FORCE_LEAVE, this);
	}

	public boolean canLeave(final Player player) {
		return Util.hasCachedPermission(player, SCPermission.LEAVE, this);
	}

	public boolean canSpeak(final Player player) {
		return Util.hasCachedPermission(player, SCPermission.SPEAK, this);
	}

	public void mutePlayer(final String name) {
		this.muted.add(name.toLowerCase());
		this.setModified(true);
	}

	public void unmutePlayer(final String name) {
		this.muted.remove(name.toLowerCase());
		this.setModified(true);
	}

	public boolean isMuted(final Player player) {
		return this.muted.contains(player.getName().toLowerCase());
	}

	public boolean isMuted(final String name) {
		return this.muted.contains(name.toLowerCase());
	}

	public void banPlayer(final String name) {
		this.banned.add(name.toLowerCase());
		this.members.remove(name.toLowerCase());
		this.setModified(true);
	}

	public void unbanPlayer(final String name) {
		this.banned.remove(name.toLowerCase());
		this.setModified(true);
	}

	public boolean isBanned(final Player player) {
		return this.banned.contains(player.getName().toLowerCase());
	}

	public boolean isBanned(final String name) {
		return this.banned.contains(name.toLowerCase());
	}

	public boolean inWorld(final Player player) {
		return this.inWorld(player.getWorld().getName()) || Util.hasCachedPermission(player, SCPermission.WORLD_OVERRIDE, this);
	}

	public boolean inWorld(final String name) {
		return this.isServerWide() || this.worlds.contains(name.toLowerCase());
	}

	public void addWorld(final String name) {
		this.worlds.add(name.toLowerCase());
		this.setModified(true);
	}

	public void removeWorld(final String name) {
		this.worlds.remove(name.toLowerCase());
		this.setModified(true);
	}

	public void announceJoin(final Player player) {
		if (this.shouldAnnounceActivity()) {
			final String message = this.formatAnnounce(player.getDisplayName() + " has joined the channel.");
			this.broadcast(message, null);
		}
	}

	public void announceLeave(final Player player) {
		if (this.shouldAnnounceActivity()) {
			final String message = this.formatAnnounce(player.getDisplayName() + " has left the channel.");
			this.broadcast(message, null);
		}
	}

	public void announceKick(final Player player) {
		if (this.shouldAnnounceActivity()) {
			final String message = this.formatAnnounce(player.getDisplayName() + " has been kicked.");
			this.broadcast(message, null);
		}
	}

	public void announceBan(final Player player) {
		if (this.shouldAnnounceActivity()) {
			final String message = this.formatAnnounce(player.getDisplayName() + " has been banned.");
			this.broadcast(message, null);
		}
	}

	public boolean canEmote(final Player player) {
		return Util.hasCachedPermission(player, SCPermission.EMOTE, this);
	}

	public String getName() {
		synchronized (this.baseAttrsLock) {
			return this.name;
		}
	}

	public void setName(final String name) {
		synchronized (this.baseAttrsLock) {
			this.name = name;
		}
		this.setModified(true);
	}

	public String getShortname() {
		synchronized (this.baseAttrsLock) {
			return this.shortname;
		}
	}

	public void setShortname(final String shortname) {
		synchronized (this.baseAttrsLock) {
			this.shortname = shortname;
		}
		this.setModified(true);
	}

	public String getPassword() {
		synchronized (this.baseAttrsLock) {
			return this.password;
		}
	}

	public void setPassword(final String password) {
		synchronized (this.baseAttrsLock) {
			this.password = password != null ? password : "";
		}
		this.setModified(true);
	}

	public ChatColor getColor() {
		synchronized (this.baseAttrsLock) {
			return this.color;
		}
	}

	public void setColor(final ChatColor color) {
		synchronized (this.baseAttrsLock) {
			this.color = color != null ? color : ChatColor.WHITE;
		}
		this.setModified(true);
	}

	public boolean isQMShortcutEnabled() {
		synchronized (this.baseAttrsLock) {
			return this.qmShortcutEnabled;
		}
	}

	public void setQMShortcutEnabled(final boolean qmShortcutEnabled) {
		synchronized (this.baseAttrsLock) {
			this.qmShortcutEnabled = qmShortcutEnabled;
		}
		this.setModified(true);
	}

	public long getRadius() {
		synchronized (this.baseAttrsLock) {
			return radius;
		}
	}

	public void setRadius(long radius) {
		synchronized (this.baseAttrsLock) {
			this.radius = radius;
		}
		this.setModified(true);
	}

	public long getRadiusSq() {
		synchronized (this.baseAttrsLock) {
			return this.radius * this.radius;
		}
	}

	public boolean shouldAnnounceActivity() {
		synchronized (this.baseAttrsLock) {
			return announceActivity;
		}
	}

	public void setAnnounceActivity(boolean announceActivity) {
		synchronized (this.baseAttrsLock) {
			this.announceActivity = announceActivity;
		}
		this.setModified(true);
	}

	public boolean isServerWide() {
		synchronized (this.baseAttrsLock) {
			return serverWide;
		}
	}

	public void setServerWide(boolean serverWide) {
		synchronized (this.baseAttrsLock) {
			this.serverWide = serverWide;
		}
		this.setModified(true);
	}

	public boolean isModified() {
		return this.modified;
	}

	public void setModified(final boolean modified) {
		this.modified = modified;
	}

	public String getMessageFormat() {
		synchronized (this.messageFormatLock) {
			if (this.messageFormat != null) {
				return this.messageFormat;
			} else {
				return this.plugin.channelHandler.getDefaultMessageFormat();
			}
		}
	}

	public void setMessageFormat(final String messageFormat) {
		// We need to include the default format when checking for validity
		/*final String semiFormatted = FormatTag.replaceAll(FormatTag.DEFAULT, messageFormat, this.plugin.channelHandler.getDefaultMessageFormat());
		int playerDisplayIndex = -1;
		for (final String tag : FormatTag.PLAYER_NICK.getTags()) {
			final int index = semiFormatted.indexOf(tag);
			if (index > 0 && (index < playerDisplayIndex || playerDisplayIndex == -1)) {
				playerDisplayIndex = index;
			}
		}

		int messageIndex = -1;
		for (final String tag : FormatTag.MESSAGE.getTags()) {
			final int index = semiFormatted.indexOf(tag);
			if (index > 0 && (index < messageIndex || messageIndex == -1)) {
				messageIndex = index;
			}
		}

		// If FormatTag.PLAYER_NICK is before FormatTag.MESSAGE and they both occur
		synchronized (this.messageFormatLock) {
			this.messageFormat = messageFormat;
			if (playerDisplayIndex < messageIndex && playerDisplayIndex > 0) {
				this.validMessageFormat = true;
			} else {
				this.validMessageFormat = false;
			}
			return this.validMessageFormat;
		}*/
		synchronized (this.messageFormatLock) {
			this.messageFormat = messageFormat;
		}
		this.setModified(true);
	}

	public String getAnnounceFormat() {
		synchronized (this.announceFormatLock) {
			if (this.announceFormat != null) {
				return this.announceFormat;
			} else {
				return this.plugin.channelHandler.getDefaultAnnounceFormat();
			}
		}
	}

	public void setAnnounceFormat(final String announceFormat) {
		synchronized (this.announceFormatLock) {
			this.announceFormat = announceFormat;
		}
		this.setModified(true);
	}

	public String getEmoteFormat() {
		synchronized (this.emoteFormatLock) {
			if (this.emoteFormat != null) {
				return this.emoteFormat;
			} else {
				return this.plugin.channelHandler.getDefaultEmoteFormat();
			}
		}
	}

	public void setEmoteFormat(final String emoteFormat) {
		synchronized (this.emoteFormatLock) {
			this.emoteFormat = emoteFormat;
		}
		this.setModified(true);
	}

	public String formatMessage(final Player player) {
		final String format = this.getMessageFormat();
		if (format == null) {
			this.plugin.logSevere("No valid format for channel " + this.getName() + " has been set! Using vanilla Minecraft chat format.");
			return "<%s> %s";
		}

		final PlayerInfoCache cache = this.plugin.infoCache;

		final String fullName = player.getName();
		final String group = cache.getGroup(player);
		final String prefix = ChatColor.translateAlternateColorCodes('&', cache.getPrefix(player));
		final String suffix = ChatColor.translateAlternateColorCodes('&', cache.getSuffix(player));
		final String groupPrefix = ChatColor.translateAlternateColorCodes('&', cache.getGroupPrefix(player));
		final String groupSuffix = ChatColor.translateAlternateColorCodes('&', cache.getGroupSuffix(player));
		final String world = player.getWorld().getName();

		String formattedMessage = FormatTag.replaceAll(FormatTag.DEFAULT, format, this.plugin.channelHandler.getDefaultMessageFormat());

		formattedMessage = FormatTag.replaceAll(FormatTag.PLAYER_NICK, formattedMessage, "%1$s");
		formattedMessage = FormatTag.replaceAll(FormatTag.MESSAGE, formattedMessage, "%2$s");

		// Order matters!
		formattedMessage = ChatColor.translateAlternateColorCodes('&', formattedMessage);

		synchronized (this.baseAttrsLock) {
			formattedMessage = FormatTag.replaceAll(FormatTag.CHANNEL_NICK, formattedMessage, this.getShortname());
			formattedMessage = FormatTag.replaceAll(FormatTag.CHANNEL_NAME, formattedMessage, this.getName());
			formattedMessage = FormatTag.replaceAll(FormatTag.CHANNEL_COLOR, formattedMessage, this.getColor().toString());
		}
		formattedMessage = FormatTag.replaceAll(FormatTag.PLAYER_NAME, formattedMessage, fullName);
		formattedMessage = FormatTag.replaceAll(FormatTag.PLAYER_WORLD, formattedMessage, world);
		formattedMessage = FormatTag.replaceAll(FormatTag.PLAYER_PREFIX, formattedMessage, prefix);
		formattedMessage = FormatTag.replaceAll(FormatTag.PLAYER_SUFFIX, formattedMessage, suffix);
		formattedMessage = FormatTag.replaceAll(FormatTag.PLAYER_GROUP, formattedMessage, group);
		formattedMessage = FormatTag.replaceAll(FormatTag.GROUP_PREFIX, formattedMessage, groupPrefix);
		formattedMessage = FormatTag.replaceAll(FormatTag.GROUP_SUFFIX, formattedMessage, groupSuffix);

		return formattedMessage;
	}

	public String formatAnnounce(final String message) {
		final String format = this.getAnnounceFormat();
		if (format == null) {
			this.plugin.logSevere("No valid activity announcement format for channel " + this.getName() + " has been set! Using built-in format.");
			return this.getColor() + "[" + this.getShortname() + "] " + message;
		}

		String formattedAnnounce = FormatTag.replaceAll(FormatTag.DEFAULT, format, this.plugin.channelHandler.getDefaultAnnounceFormat());

		formattedAnnounce = ChatColor.translateAlternateColorCodes('&', formattedAnnounce);

		synchronized (this.baseAttrsLock) {
			formattedAnnounce = FormatTag.replaceAll(FormatTag.CHANNEL_NICK, formattedAnnounce, this.shortname);
			formattedAnnounce = FormatTag.replaceAll(FormatTag.CHANNEL_NAME, formattedAnnounce, this.name);
			formattedAnnounce = FormatTag.replaceAll(FormatTag.CHANNEL_COLOR, formattedAnnounce, this.color.toString());
		}
		formattedAnnounce = FormatTag.replaceAll(FormatTag.MESSAGE, formattedAnnounce, message);

		return formattedAnnounce;
	}

	public String formatEmote(final Player player, final String message) {
		final String coloredMessage = this.translatePermittedColorCodes(player, message);
		String format = this.getEmoteFormat();
		if (format == null) {
			this.plugin.logSevere("No valid emote format for channel " + this.getName() + " has been set! Using vanilla Minecraft emote format.");
			return "* " + coloredMessage;
		}

		final PlayerInfoCache cache = this.plugin.infoCache;

		final String fullName = player.getName();
		final String displayName = player.getDisplayName();
		final String group = cache.getGroup(player);
		final String prefix = ChatColor.translateAlternateColorCodes('&', cache.getPrefix(player));
		final String suffix = ChatColor.translateAlternateColorCodes('&', cache.getSuffix(player));
		final String groupPrefix = ChatColor.translateAlternateColorCodes('&', cache.getGroupPrefix(player));
		final String groupSuffix = ChatColor.translateAlternateColorCodes('&', cache.getGroupSuffix(player));
		final String world = player.getWorld().getName();

		String formattedEmote = FormatTag.replaceAll(FormatTag.DEFAULT, format, this.plugin.channelHandler.getDefaultEmoteFormat());

		formattedEmote = ChatColor.translateAlternateColorCodes('&', formattedEmote);

		synchronized (this.baseAttrsLock) {
			formattedEmote = FormatTag.replaceAll(FormatTag.CHANNEL_NICK, formattedEmote, this.shortname);
			formattedEmote = FormatTag.replaceAll(FormatTag.CHANNEL_NAME, formattedEmote, this.name);
			formattedEmote = FormatTag.replaceAll(FormatTag.CHANNEL_COLOR, formattedEmote, this.color.toString());
		}
		formattedEmote = FormatTag.replaceAll(FormatTag.MESSAGE, formattedEmote, coloredMessage);
		formattedEmote = FormatTag.replaceAll(FormatTag.PLAYER_NICK, formattedEmote, displayName);
		formattedEmote = FormatTag.replaceAll(FormatTag.PLAYER_NAME, formattedEmote, fullName);
		formattedEmote = FormatTag.replaceAll(FormatTag.PLAYER_WORLD, formattedEmote, world);
		formattedEmote = FormatTag.replaceAll(FormatTag.PLAYER_PREFIX, formattedEmote, prefix);
		formattedEmote = FormatTag.replaceAll(FormatTag.PLAYER_SUFFIX, formattedEmote, suffix);
		formattedEmote = FormatTag.replaceAll(FormatTag.PLAYER_GROUP, formattedEmote, group);
		formattedEmote = FormatTag.replaceAll(FormatTag.GROUP_PREFIX, formattedEmote, groupPrefix);
		formattedEmote = FormatTag.replaceAll(FormatTag.GROUP_SUFFIX, formattedEmote, groupSuffix);

		return formattedEmote;
	}

	public String translatePermittedColorCodes(final Player player, String message) {
		for (final ChatColor color : ChatColor.values()) {
			final String altSeq = "&" + color.getChar();
			if (message.contains(altSeq) && (player == null || Util.hasCachedPermission(player, SCPermission.COLOR, this, color))) {
				final String repSeq = color.toString();
				message = message.replaceAll(Pattern.quote(altSeq), Matcher.quoteReplacement(repSeq));
			}
		}
		return message;
	}

	private class ConfigKeys {
		public static final String NAME = "name";
		public static final String NICK = "nick";
		public static final String FORMAT = "format";
		public static final String COLOR = "color";
		public static final String RADIUS = "radius";
		public static final String QM_SHORTCUT = "qm-shortcut";
		public static final String ANNOUNCE_ACTIVITY = "announce-activity";
		public static final String SERVER_WIDE = "server-wide";
		public static final String WORLDS = "worlds";
		public static final String BANNED = "banned";
		public static final String MUTED = "muted";
	}
}
