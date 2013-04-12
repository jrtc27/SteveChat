package com.jrtc27.stevechat;

import java.util.Collection;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

public class Chatter {
	public final String playerName;

	private Channel activeChannel = null;
	private String conversing = null;
	private String lastConverser = null;
	private boolean muted = false;
	public final Set<String> ignoring = new CopyOnWriteArraySet<String>();
	private boolean treatAsNew = true;
	public final Set<String> channelsToJoin = new CopyOnWriteArraySet<String>();

	private boolean modified = false;
	private long logoutTime = System.currentTimeMillis();

	private boolean afk = false;
	private String afkMessage = null;

	public final Object activeChannelLock = new Object();
	public final Object conversingLock = new Object();
	public final Object lastConverserLock = new Object();
	public final Object mutedLock = new Object();
	public final Object afkLock = new Object();

	public Chatter(final String playerName) {
		this.playerName = playerName.toLowerCase();
	}

	public Channel getActiveChannel() {
		synchronized (this.activeChannelLock) {
			return this.activeChannel;
		}
	}

	public void setActiveChannel(final Channel active) {
		synchronized (this.activeChannelLock) {
			this.activeChannel = active;
		}
		this.setModified(true);
	}

	public String getConversing() {
		synchronized (this.conversingLock) {
			return this.conversing;
		}
	}

	public void setConversing(final String conversing) {
		synchronized (this.conversingLock) {
			this.conversing = conversing;
		}
	}

	public String getLastConverser() {
		synchronized (this.lastConverserLock) {
			return this.lastConverser;
		}
	}

	public void setLastConverser(final String lastConverser) {
		synchronized (this.lastConverserLock) {
			this.lastConverser = lastConverser;
		}
	}

	public boolean isMuted() {
		synchronized (this.mutedLock) {
			return this.muted;
		}
	}

	public void setMuted(final boolean muted) {
		synchronized (this.mutedLock) {
			this.muted = muted;
		}
		this.setModified(true);
	}

	public boolean treatAsNew() {
		return this.treatAsNew;
	}

	public void setTreatAsNew(final boolean treatAsNew) {
		this.treatAsNew = treatAsNew;
	}

	public void setChannelsToJoin(final Collection<String> channelsToJoin) {
		this.channelsToJoin.clear();
		if (channelsToJoin != null) {
			this.channelsToJoin.addAll(channelsToJoin);
		}
	}

	public boolean isModified() {
		return this.modified;
	}

	public void setModified(final boolean modified) {
		this.modified = modified;
	}

	public boolean isAfk() {
		synchronized (this.afkLock) {
			return this.afk;
		}
	}

	public void setAfk(final boolean afk, final String message) {
		synchronized (this.afkLock) {
			if (afk) {
				this.setAfkMessage(message);
			} else {
				this.setAfkMessage(null);
			}
			this.afk = afk;
		}
	}

	public boolean toggleAfk(final String message) {
		synchronized (this.afkLock) {
			this.setAfk(!this.afk, message);
			return this.isAfk();
		}
	}

	public void setAfkMessage(final String message) {
		synchronized (this.afkLock) {
			this.afkMessage = message;
		}
	}

	public String getAfkMessage(final boolean allowNull) {
		String message;
		synchronized (this.afkLock) {
			message = this.afkMessage;
		}
		if (message == null && !allowNull) {
			message = "I am currently afk";
		}
		return message;
	}

	public long getLogoutTime() {
		return this.logoutTime;
	}

	public void updateLogoutTime() {
		this.logoutTime = System.currentTimeMillis();
	}
}
