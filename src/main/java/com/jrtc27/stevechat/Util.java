package com.jrtc27.stevechat;

import java.util.EnumMap;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.permissions.Permission;
import org.bukkit.permissions.PermissionDefault;

import com.jrtc27.stevechat.hooks.Essentials;
import com.jrtc27.stevechat.hooks.PluginNotFoundException;
import com.jrtc27.stevechat.hooks.Vault;

public class Util {
	private static SteveChatPlugin plugin = null;

	public static Vault vault = null;
	private static Essentials essentials = null;

	public static Set<Permission> registeredPermissions = new HashSet<Permission>();
	public static IPermissionDependency DEPENDENCY_ALL = new DummyPermissionDependency("*");
	public static String CONSOLE_COMMAND_SENDER_NAME = "[CONSOLE]";

	private static boolean uninitialised = true;

	public static void initialise(final SteveChatPlugin plugin) {
		Util.plugin = plugin;
		Util.setupVault();
		Util.setupEssentials();
		Util.registerPermissions();
		Util.uninitialised = false;
	}

	public static void reload() {
		Util.uninitialised = true;
		Util.registerPermissions();
		Util.uninitialised = false;
	}

	private static void setupVault() {
		try {
			Util.vault = new Vault(Util.plugin);
		} catch (final PluginNotFoundException e) {
			Util.plugin.logWarning(e.pluginName + " not found - certain features will not work.");
		}
	}

	private static void setupEssentials() {
		try {
			Util.essentials = new Essentials(Util.plugin);
			Util.plugin.logInfo("Hooked into Essentials!");
		} catch (final PluginNotFoundException e) {
			// No need to do anything
		}
	}

	/**
	 * @throws UnsupportedOperationException if the supplied permission returns true for {@link SCPermission#isColor()}
	 */
	public static boolean hasCachedPermission(final Player player, final SCPermission permission, final IPermissionDependency dependency) {
		final PlayerInfoCache cache = Util.plugin.infoCache;

		if (permission.isColor()) {
			throw new UnsupportedOperationException("Need to supply a color in order to be able to determine permissions!");
		} else if (permission.hasDependency()) {
			return cache.hasPermission(player, permission.nodeForDependency(dependency));
		} else {
			return cache.hasPermission(player, permission.node());
		}
	}

	/**
	 * @throws UnsupportedOperationException if the supplied permission returns false for {@link SCPermission#isColor()}
	 */
	public static boolean hasCachedPermission(final Player player, final SCPermission permission, final IPermissionDependency dependency, final ChatColor color) {
		final PlayerInfoCache cache = Util.plugin.infoCache;

		if (!permission.isColor()) {
			throw new UnsupportedOperationException("This permission does not use a color!");
		} else {
			return cache.hasPermission(player, permission.nodeForColorWithDependency(dependency, color));
		}
	}

	/**
	 * Use {@link Util#hasCachedPermission(Player, SCPermission, IPermissionDependency)} unless you know what you're doing
	 * <p/>
	 * <em>NB: Will return false on error</em> <em>NB: Will *not* perform as expected in the context of wildcards</em>
	 *
	 * @return Whether the player has permission, or false on error
	 */
	public static boolean hasPermission(final Player player, final SCPermission permission, final Channel channel) {
		if (permission.hasDependency()) {
			final String channelNode = permission.nodeForDependency(channel);
			return Util.hasPermission(player, channelNode);
		} else {
			return Util.hasPermission(player, permission.node());
		}
	}

	public static boolean hasPermission(final Player player, final String node) {
		return player.hasPermission(node);
	}

	public static boolean isPermissionSet(final Player player, final String node) {
		return player.isPermissionSet(node);
	}

	public static Player getPlayer(final String name, final boolean exact) {
		return exact ? Bukkit.getServer().getPlayerExact(name) : Bukkit.getServer().getPlayer(name);
	}

	public static String getPrefix(final Player player) {
		if (Util.vault != null) {
			return Util.vault.chat.getPlayerPrefix(player);
		}
		return "";
	}

	public static String getSuffix(final Player player) {
		if (Util.vault != null) {
			return Util.vault.chat.getPlayerSuffix(player);
		}
		return "";
	}

	public static String getGroup(final Player player) {
		if (Util.vault != null) {
			return Util.vault.chat.getPrimaryGroup(player);
		}
		return "";
	}

	public static String getGroupPrefix(final String world, final String group) {
		if (Util.vault != null) {
			return Util.vault.chat.getGroupPrefix(world, group);
		}
		return "";
	}


	public static String getGroupSuffix(final String world, final String group) {
		if (Util.vault != null) {
			return Util.vault.chat.getGroupSuffix(world, group);
		}
		return "";
	}

	public static void refreshInfoCache() {
		Util.plugin.infoCache.refreshCache();
	}

	public static void registerPermissions() {
		for (final Permission permission : Util.registeredPermissions) {
			Util.plugin.getServer().getPluginManager().removePermission(permission);
		}
		Util.registeredPermissions.clear();

		final Set<SCPermission> toRevisit = EnumSet.noneOf(SCPermission.class);
		final Map<SCPermission, Set<Permission>> addedPermissionsMap = new EnumMap<SCPermission, Set<Permission>>(SCPermission.class);

		final Permission rootPermission = new Permission("stevechat.*", PermissionDefault.OP);

		for (final SCPermission permission : SCPermission.values()) {
			final Set<Permission> addedPermissions = new HashSet<Permission>();

			if (permission.hasParent()) {
				toRevisit.add(permission);
			}

			if (permission.isColor()) {
				/*
				 * Color Permissions Hierarchy:
				 * stevechat.color.* (subRootPermission)
				 * - stevechat.color.channel.* (channelSpecific)
				 * - stevechat.color.channel.color (channelAndColor)
				 */

				final Permission subRootPermission = new Permission(permission.nodeAll(), permission.getDefaultValue());

				synchronized (Util.plugin.channelHandler.channels) {
					for (final Channel channel : Util.plugin.channelHandler.channels) {
						final Permission channelSpecific = new Permission(permission.nodeAllWithDependency(channel), permission.getDefaultValue());
						channelSpecific.addParent(subRootPermission, true);
						Util.registerPermission(channelSpecific);
						addedPermissions.add(channelSpecific);

						for (final ChatColor color : ChatColor.values()) {
							final Permission channelAndColor = new Permission(permission.nodeForColorWithDependency(channel, color), permission.getDefaultValue());
							channelAndColor.addParent(channelSpecific, true);
							Util.registerPermission(channelAndColor);
							addedPermissions.add(channelAndColor);
						}
					}
				}

				if (permission.isChildOfRoot()) {
					subRootPermission.addParent(rootPermission, true);
				}

				Util.registerPermission(subRootPermission);
				addedPermissions.add(subRootPermission);
			} else if (permission.hasDependency()) {
				final Permission subRootPermission = new Permission(permission.nodeAll(), permission.getDefaultValue());

				for (final Channel channel : Util.plugin.channelHandler.channels) {
					final Permission childPermission = new Permission(permission.nodeForDependency(channel), permission.getDefaultValue());
					childPermission.addParent(subRootPermission, true);
					Util.registerPermission(childPermission);
					addedPermissions.add(childPermission);
				}

				if (permission.isChildOfRoot()) {
					subRootPermission.addParent(rootPermission, true);
				}

				Util.registerPermission(subRootPermission);
				addedPermissions.add(subRootPermission);
			} else {
				final Permission bukkitPermission = new Permission(permission.node(), permission.getDefaultValue());

				if (permission.isChildOfRoot()) {
					bukkitPermission.addParent(rootPermission, true);
				}

				Util.registerPermission(bukkitPermission);
				addedPermissions.add(bukkitPermission);
			}

			addedPermissionsMap.put(permission, addedPermissions);
			Util.registeredPermissions.addAll(addedPermissions);
		}

		Util.registerPermission(rootPermission);
		Util.registeredPermissions.add(rootPermission);

		for (final SCPermission permission : toRevisit) {
			if (!permission.hasParent()) {
				continue;
			}

			final Set<Permission> childPermissions = addedPermissionsMap.get(permission);
			final Set<Permission> parentPermissions = addedPermissionsMap.get(permission.getParent());

			for (final Permission parentPermission : parentPermissions) {
				for (final Permission childPermission : childPermissions) {
					childPermission.addParent(parentPermission, true);
				}
			}
		}

		/*for (final Permission permission : Util.registeredPermissions) {
			Util.plugin.logInfo(permission.getName());
			final Map<String, Boolean> childrenMap = permission.getChildren();
			final Set<String> keys = childrenMap.keySet();
			for (final String child : keys) {
				Util.plugin.logInfo(" -> " + child + " = " + (childrenMap.get(child) ? "true" : "false"));
			}
		}

		Util.plugin.logInfo(addedPermissionsMap.toString());*/
	}

	private static void registerPermission(final Permission bukkitPermission) {
		try {
			// Util.plugin.logInfo("Registering " + bukkitPermission.getName());
			Util.plugin.getServer().getPluginManager().addPermission(bukkitPermission);
		} catch (final IllegalArgumentException e) {
			if (Util.uninitialised) { // Otherwise we just added it before
				Util.plugin.logSevere("Another plugin is using the permission node " + bukkitPermission.getName() + " - expect permissions to be broken!" + "Please also notify me and/or the author(s) of the conflicting plugin to resolve this.");
			}
		}
	}

	public static boolean isEssentialsAfk(final String name) {
		if (Util.essentials != null) {
			return Util.essentials.isAfk(name);
		}
		return false;
	}

	/*
	 * public static void addPermission(Player player, String node, List<World> worlds) {
	 * if (permission != null) {
	 * for (World world : worlds) {
	 * permission.playerAdd(world, player.getName(), node);
	 * }
	 * } else {
	 * player.addAttachment(plugin, node, true);
	 * }
	 * }
	 * public static void removePermission(Player player, String node, List<World> worlds) {
	 * if (permission != null) {
	 * for (World world : worlds) {
	 * permission.playerRemove(world, player.getName(), node);
	 * }
	 * } else {
	 * player.addAttachment(plugin, node, false);
	 * }
	 * }
	 */
}
