package com.jrtc27.stevechat.hooks;

import org.bukkit.plugin.Plugin;

import com.earth2me.essentials.IEssentials;
import com.earth2me.essentials.User;
import com.jrtc27.stevechat.SteveChatPlugin;

public class Essentials {
	private final SteveChatPlugin plugin;

	private final IEssentials essentials;

	public Essentials(final SteveChatPlugin plugin) throws PluginNotFoundException {
		this.plugin = plugin;
		final Plugin essentialsPlugin = this.plugin.getServer().getPluginManager().getPlugin("Essentials");
		if (essentialsPlugin instanceof IEssentials) {
			this.essentials = (IEssentials) essentialsPlugin;
		} else {
			this.essentials = null;
			throw new PluginNotFoundException("Essentials");
		}
	}

	public boolean isAfk(final String name) {
		final User user = this.essentials.getUser(this.plugin.getServer().getPlayerExact(name));
		return user.isAfk();
	}

	/**
	 * Toggles the user's AFK status and returns their new AFK status
	 *
	 * @param name User whose AFK status should be toggled
	 * @return Whether the user is now AFK
	 */
	public boolean toggleAfk(final String name) {
		final User user = this.essentials.getUser(this.plugin.getServer().getPlayerExact(name));
		return user.toggleAfk();
	}

	public void setAfk(final String name, final boolean afk) {
		final User user = this.essentials.getUser(this.plugin.getServer().getPlayerExact(name));
		user.setAfk(afk);
	}
}
