package com.jrtc27.stevechat;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.bukkit.entity.Player;
import org.bukkit.permissions.Permission;

public class PlayerInfoCache {
	private final SteveChatPlugin plugin;

	private final Map<String, PlayerInfo> cached = new HashMap<String, PlayerInfo>();
	private final List<String> nodesToCheck = new ArrayList<String>();

	public final Object cachedLock = new Object();

	public PlayerInfoCache(final SteveChatPlugin plugin) {
		this.plugin = plugin;
		this.determineNodesToCheck();
		this.refreshCache();
	}

	public void determineNodesToCheck() {
		this.nodesToCheck.clear();
		for (final Permission permission : Util.registeredPermissions) {
			this.nodesToCheck.add(permission.getName().toLowerCase());
		}
	}

	public boolean isPermissionSet(final Player player, final String node) {
		final PlayerInfo cacheEntry;
		synchronized (this.cachedLock) {
			cacheEntry = this.cached.get(player.getName().toLowerCase());
		}
		if (cacheEntry != null) {
			final PlayerInfoPermission perm = cacheEntry.nodeValue(node);
			if (perm != null) {
				return perm.set;
			} else {
				this.plugin.logSevere("Player " + player.getName() + " does not have a permissions cache entry for the node '" + node + "'!");
			}
		} else {
			this.plugin.logSevere("Player " + player.getName() + " is not in the info cache!");
		}
		return true;
	}

	public boolean hasPermission(final Player player, final String node) {
		final PlayerInfo cacheEntry;
		synchronized (this.cachedLock) {
			cacheEntry = this.cached.get(player.getName().toLowerCase());
		}
		if (cacheEntry != null) {
			final PlayerInfoPermission perm = cacheEntry.nodeValue(node);
			if (perm != null) {
				return perm.has;
			} else {
				this.plugin.logSevere("Player " + player.getName() + " does not have a permissions cache entry for the node '" + node + "'!");
			}
		} else {
			this.plugin.logSevere("Player " + player.getName() + " is not in the info cache!");
		}
		return false;
	}

	public String getGroup(final Player player) {
		final PlayerInfo cacheEntry;
		synchronized (this.cachedLock) {
			cacheEntry = this.cached.get(player.getName().toLowerCase());
		}
		if (cacheEntry != null) {
			return cacheEntry.group;
		} else {
			this.plugin.logSevere("Player " + player.getName() + " is not in the info cache!");
		}
		return "";
	}

	public String getPrefix(final Player player) {
		final PlayerInfo cacheEntry;
		synchronized (this.cachedLock) {
			cacheEntry = this.cached.get(player.getName().toLowerCase());
		}
		if (cacheEntry != null) {
			return cacheEntry.prefix;
		} else {
			this.plugin.logSevere("Player " + player.getName() + " is not in the info cache!");
		}
		return "";
	}

	public String getSuffix(final Player player) {
		final PlayerInfo cacheEntry;
		synchronized (this.cachedLock) {
			cacheEntry = this.cached.get(player.getName().toLowerCase());
		}
		if (cacheEntry != null) {
			return cacheEntry.suffix;
		} else {
			this.plugin.logSevere("Player " + player.getName() + " is not in the info cache!");
		}
		return "";
	}

	public String getGroupPrefix(final Player player) {
		final PlayerInfo cacheEntry;
		synchronized (this.cachedLock) {
			cacheEntry = this.cached.get(player.getName().toLowerCase());
		}
		if (cacheEntry != null) {
			return cacheEntry.groupPrefix;
		} else {
			this.plugin.logSevere("Player " + player.getName() + " is not in the info cache!");
		}
		return "";
	}

	public String getGroupSuffix(final Player player) {
		final PlayerInfo cacheEntry;
		synchronized (this.cachedLock) {
			cacheEntry = this.cached.get(player.getName().toLowerCase());
		}
		if (cacheEntry != null) {
			return cacheEntry.groupSuffix;
		} else {
			this.plugin.logSevere("Player " + player.getName() + " is not in the info cache!");
		}
		return "";
	}

	public void addPlayerToCache(final Player player) {
		final String playerName = player.getName().toLowerCase();
		final String playerWorld = player.getWorld().getName();
		final String group = Util.getGroup(player);
		final String prefix = Util.getPrefix(player);
		final String suffix = Util.getSuffix(player);
		final String groupPrefix = Util.getGroupPrefix(playerWorld, group);
		final String groupSuffix = Util.getGroupSuffix(playerWorld, group);
		final PlayerInfo cacheEntry = new PlayerInfo(playerName, group, prefix, suffix, groupPrefix, groupSuffix);
		for (final String node : this.nodesToCheck) {
			final boolean has = Util.hasPermission(player, node);
			final boolean set = Util.isPermissionSet(player, node);
			cacheEntry.putNodeValue(node, new PlayerInfoPermission(has, set));
		}
		synchronized (this.cachedLock) {
			this.cached.put(playerName, cacheEntry);
		}
	}

	public void refreshCache() {
		synchronized (this.cachedLock) {
			this.cached.clear();
			for (final Player player : this.plugin.getServer().getOnlinePlayers()) {
				this.addPlayerToCache(player);
			}
		}
	}
}
