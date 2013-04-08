package com.jrtc27.stevechat;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.PluginDescriptionFile;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

import com.jrtc27.stevechat.config.ConfigHandler;
import com.jrtc27.stevechat.listeners.PlayerListener;

public class SteveChatPlugin extends JavaPlugin {
	public ChannelHandler channelHandler = null;
	public PlayerListener playerListener;
	public PlayerInfoCache infoCache = null;
	public final ConfigHandler configHandler;
	public final ReadWriteLock loadingLock = new ReentrantReadWriteLock();
	public String adminMessage = null;

	private Logger logger;
	private PluginDescriptionFile pdf;
	private BukkitTask cacheRefreshTask = null;
	private BukkitTask dataSaveTask = null;
	private BukkitTask updateCheckTask = null;
	private String version = null;
	private String jenkinsBuild = null;
	public boolean checkForUpdates = true;

	public SteveChatPlugin() {
		this.configHandler = new ConfigHandler(this);
	}

	@Override
	public void onEnable() {
		this.loadingLock.writeLock().lock();
		try {
			this.loadVersionInfo();
			this.logger = this.getLogger();
			this.pdf = this.getDescription();
			this.channelHandler = new ChannelHandler(this);
			this.configHandler.load();
			Util.initialise(this);
			this.playerListener = new PlayerListener(this);
			this.cancelTimers();
			this.infoCache = new PlayerInfoCache(this);
			this.setupTimers();
			this.getServer().getPluginManager().registerEvents(this.playerListener, this);
			for (final Player player : this.getServer().getOnlinePlayers()) {
				this.channelHandler.handlePlayerJoin(player);
			}
			this.logInfo("Enabled " + this.pdf.getFullName() + "!");
		} finally {
			this.loadingLock.writeLock().unlock();
		}
	}

	@Override
	public void onDisable() {
		this.cancelTimers();
		this.configHandler.saveChatters();
	}

	private void cancelTimers() {
		if (this.cacheRefreshTask != null) {
			this.cacheRefreshTask.cancel();
			this.cacheRefreshTask = null;
		}
		if (this.dataSaveTask != null) {
			this.dataSaveTask.cancel();
			this.dataSaveTask = null;
		}
		if (this.updateCheckTask != null) {
			this.updateCheckTask.cancel();
			this.updateCheckTask = null;
		}
	}

	private void setupTimers() {
		this.cacheRefreshTask = this.getServer().getScheduler().runTaskTimer(this, new Runnable() {
			@Override
			public void run() {
				Util.refreshInfoCache();
			}
		}, 10, 10);
		this.dataSaveTask = this.getServer().getScheduler().runTaskTimerAsynchronously(this, new Runnable() {
			@Override
			public void run() {
				configHandler.saveAll();
			}
		}, 20, 20);

		if (this.version == null || this.version.equalsIgnoreCase("${project.version}")) {
			this.logSevere("Error reading version info file!");
			this.adminMessage = null;
		} else if (this.version.endsWith("-SNAPSHOT")) {
			this.logWarning("You are currently running a snapshot version - please be aware that there may be (serious) bugs!");
			this.adminMessage = null;
		} else if (this.checkForUpdates) {
			this.updateCheckTask = this.getServer().getScheduler().runTaskTimerAsynchronously(this, new Runnable() {
				@Override
				public void run() {
					checkForUpdates();
				}
			}, 20, 432000); // 20 ticks * 60 seconds * 60 minutes * 6 hours => 6 hours in ticks
		} else {
			this.logInfo("Update checking has been disabled!");
		}
	}

	public void reload(final boolean readLockAcquired, final CommandSender sender) {
		this.broadcastReloadStarted(sender);
		if (readLockAcquired) {
			this.loadingLock.readLock().unlock(); // So we can acquire a write lock
		}
		try {
			if (this.loadingLock.writeLock().tryLock(5, TimeUnit.SECONDS)) {
				try {
					this.loadVersionInfo();
					this.channelHandler = new ChannelHandler(this);
					this.configHandler.load();
					Util.reload();
					this.cancelTimers();
					this.infoCache = new PlayerInfoCache(this);
					this.setupTimers();
					for (final Player player : this.getServer().getOnlinePlayers()) {
						this.channelHandler.handlePlayerJoin(player);
					}
					this.broadcastReloadFinished(sender);
				} finally {
					if (readLockAcquired) {
						this.loadingLock.readLock().lock(); // Reacquire read lock as the calling method is expecting to have to unlock it
					}
					this.loadingLock.writeLock().unlock();
				}
				return;
			}
		} catch (InterruptedException e) {
			// Do nothing
		}
		this.broadcastReloadFailed(sender);
	}

	public void loadVersionInfo() {
		final InputStream stream = this.getResource("version-info.yml");
		if (stream != null) {
			final FileConfiguration config = new YamlConfiguration();
			boolean loaded = true;
			try {
				config.load(stream);
			} catch (Exception e) {
				e.printStackTrace();
				loaded = false;
			}
			if (loaded) {
				this.version = config.getString("version");
				this.jenkinsBuild = config.getString("jenkins-build");
				return;
			}
		}
		this.version = null;
		this.jenkinsBuild = null;
	}

	public String getVersion() {
		return this.pdf.getVersion();
	}

	public void log(final Level level, final String message) {
		this.logger.log(level, message);
	}

	public void logInfo(final String message) {
		this.log(Level.INFO, message);
	}

	public void logWarning(final String message) {
		this.log(Level.WARNING, message);
	}

	public void logSevere(final String message) {
		this.log(Level.SEVERE, message);
	}

	public void broadcastAdminMessage(final String message, final boolean sendToConsole) {
		if (sendToConsole) {
			this.getServer().getConsoleSender().sendMessage(message);
		}

		for (final Player player : this.getServer().getOnlinePlayers()) {
			if (Util.hasCachedPermission(player, SCPermission.ADMIN_MESSAGES, null)) {
				player.sendMessage(message);
			}
		}
	}

	public void broadcastReloadStarted(final CommandSender sender) {
		final String starter;
		if (sender instanceof Player) {
			starter = ((Player) sender).getDisplayName();
		} else {
			starter = ChatColor.GOLD + "[" + sender.getName() + "]";
		}
		final String message = MessageColor.INFO + "[" + this.pdf.getName() + "] Reload started by " + starter;

		this.getServer().getConsoleSender().sendMessage(message);

		for (final Player player : this.getServer().getOnlinePlayers()) {
			if (Util.hasCachedPermission(player, SCPermission.ADMIN_MESSAGES, null) || player.equals(sender)) {
				player.sendMessage(message);
			}
		}
	}

	public void broadcastReloadFinished(final CommandSender sender) {
		final String message = MessageColor.INFO + "[" + this.pdf.getName() + "] Reloaded!";

		this.getServer().getConsoleSender().sendMessage(message);

		for (final Player player : this.getServer().getOnlinePlayers()) {
			if (Util.hasCachedPermission(player, SCPermission.ADMIN_MESSAGES, null) || player.equals(sender)) {
				player.sendMessage(message);
			}
		}
	}

	public void broadcastReloadFailed(final CommandSender sender) {
		final String message = MessageColor.ERROR + "[" + this.pdf.getName() + "] Failed to reload!";

		this.getServer().getConsoleSender().sendMessage(message);

		for (final Player player : this.getServer().getOnlinePlayers()) {
			if (Util.hasCachedPermission(player, SCPermission.ADMIN_MESSAGES, null) || player.equals(sender)) {
				player.sendMessage(message);
			}
		}
	}

	public void checkForUpdates() {
		BufferedReader reader = null;
		try {
			final URLConnection connection = new URL("http://jrtc27.github.com/SteveChat/version").openConnection();
			connection.setConnectTimeout(10000);
			connection.setReadTimeout(10000);
			reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
			final String version = reader.readLine();
			if (version != null) {
				if (this.isVersionNewer(this.version, version)) {
					final String message = "A new recommended version (" + version + ") is available - please update for new features and fixes!";
					this.logInfo(message);
					final String playerMessage = MessageColor.INFO + "[" + this.pdf.getName() + "] " + message;
					this.broadcastAdminMessage(playerMessage, false);
					this.adminMessage = playerMessage;
				} else {
					this.adminMessage = null;
				}
				return;
			}
		} catch (Exception e) {
			// Do nothing
		} finally {
			if (reader != null) {
				try {
					reader.close();
				} catch (IOException e) {
					// Do nothing
				}
			}
		}
		this.logWarning("Unable to check if plugin was up to date!");
	}

	private boolean isVersionNewer(final String current, final String reported) {
		final String[] currentElements = current.split("\\.");
		final String[] reportedElements = reported.split("\\.");
		final int length = Math.min(currentElements.length, reportedElements.length);
		for (int i = 0; i < length; i++) {
			final int currentInt, reportedInt;
			try {
				currentInt = Integer.valueOf(currentElements[i]);
				reportedInt = Integer.valueOf(reportedElements[i]);
			} catch (NumberFormatException e) {
				return true;
			}
			if (reportedInt > currentInt) return true;
			else if (reportedInt < currentInt) return false;
		}
		return reportedElements.length > currentElements.length;
	}

}
