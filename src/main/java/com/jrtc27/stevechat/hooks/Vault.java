package com.jrtc27.stevechat.hooks;

import org.bukkit.plugin.RegisteredServiceProvider;

import com.jrtc27.stevechat.SteveChatPlugin;

public class Vault {
	private final SteveChatPlugin plugin;

	public final net.milkbowl.vault.chat.Chat chat;

	public Vault(final SteveChatPlugin plugin) throws PluginNotFoundException {
		this.plugin = plugin;
		try {
			final RegisteredServiceProvider<net.milkbowl.vault.chat.Chat> chatProvider = plugin.getServer().getServicesManager().getRegistration(net.milkbowl.vault.chat.Chat.class);
			if (chatProvider != null) {
				this.chat = chatProvider.getProvider();
			} else {
				this.chat = null;
			}
			final RegisteredServiceProvider<net.milkbowl.vault.permission.Permission> permissionProvider = plugin.getServer().getServicesManager().getRegistration(net.milkbowl.vault.permission.Permission.class);
			if (permissionProvider != null) {
				final net.milkbowl.vault.permission.Permission permission = permissionProvider.getProvider();
				if (!permission.hasSuperPermsCompat()) {
					this.plugin.logWarning("Your permissions provider is not SuperPerms compatible - permissions may not work as expected");
				}
			}
		} catch (final NoClassDefFoundError e) {
			throw new PluginNotFoundException("Vault chat");
		}
	}

}
