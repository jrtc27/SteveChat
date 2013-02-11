package com.jrtc27.stevechat;

import static org.bukkit.permissions.PermissionDefault.FALSE;
import static org.bukkit.permissions.PermissionDefault.OP;

import org.bukkit.ChatColor;
import org.bukkit.permissions.PermissionDefault;

public enum SCPermission {
	JOIN(OP, true, true),
	JOIN_COMMAND("join", OP, false, false, JOIN),
	AUTO_JOIN(OP, true, true),
	FORCE_JOIN(FALSE, false, true),
	LEAVE(OP, true, true),
	LEAVE_COMMAND("leave", OP, false, false, LEAVE),
	FORCE_LEAVE(FALSE, false, true),
	SPEAK(OP, true, true),
	QUICK_MESSAGE("qm", OP, true, true),
	QUICK_MESSAGE_COMMAND("qm", OP, false, false, QUICK_MESSAGE),
	EMOTE(OP, true, true),
	EMOTE_COMMAND("emote", OP, false, false, EMOTE),
	IGNORE(OP, true, false),
	MUTE(OP, true, true),
	MUTE_COMMAND("mute", OP, false, false, MUTE),
	KICK(OP, true, true),
	KICK_COMMAND("kick", OP, false, false, KICK),
	BAN(OP, true, true),
	BAN_COMMAND("ban", OP, false, false, BAN),
	MOD(OP, true, true),
	MOD_COMMAND("mod", OP, false, false, MOD),
	WHO(OP, true, true),
	WHO_COMMAND("who", OP, false, false, WHO),
	LIST(OP, true, false),
	INFO(OP, true, true),
	INFO_COMMAND("info", OP, false, false, INFO),
	AFK(OP, true, false),
	PRIVATE_MESSAGE("pm", OP, true, false),
	EAVESDROP("stealth", OP, true, false),
	COLOR(OP, true, true), // NB: make sure to use the various color-specific methods
	HELP(OP, true, false),
	CREATE(OP, true, false),
	SET(OP, true, true),
	SET_COMMAND("set", OP, false, false, SET),
	DELETE(OP, true, false),
	RELOAD(OP, true, false),
	ADMIN_MESSAGES(OP, true, false),
	WORLD_OVERRIDE(OP, true, true);

	private final String name;
	private final PermissionDefault defaultValue;
	private final boolean childOfRoot;
	private final boolean hasDependency;
	private final SCPermission parent;

	private SCPermission(final PermissionDefault defaultValue, final boolean childOfRoot, final boolean hasDependency) {
		this(defaultValue, childOfRoot, hasDependency, null);
	}

	private SCPermission(final String name, final PermissionDefault defaultValue, final boolean childOfRoot, final boolean hasDependency) {
		this(name, defaultValue, childOfRoot, hasDependency, null);
	}

	private SCPermission(final PermissionDefault defaultValue, final boolean childOfRoot, final boolean hasDependency, final SCPermission parent) {
		// NB: this.name() is not this.name!
		this.name = this.name().toLowerCase().replaceAll("_", "-");
		this.defaultValue = defaultValue;
		this.childOfRoot = childOfRoot;
		this.hasDependency = hasDependency;
		this.parent = parent;
	}

	private SCPermission(final String name, final PermissionDefault defaultValue, final boolean childOfRoot, final boolean hasDependency, final SCPermission parent) {
		this.name = name.toLowerCase();
		this.defaultValue = defaultValue;
		this.childOfRoot = childOfRoot;
		this.hasDependency = hasDependency;
		this.parent = parent;
	}

	public String nodeForDependency(final IPermissionDependency dependency) {
		return "stevechat." + this.name + "." + dependency.permissionExtension();
	}

	/**
	 * <em>Only applies if {@link #isColor()} returns true
	 */
	public String nodeForColorWithDependency(final IPermissionDependency dependency, final ChatColor color) {
		// stevechat.color.global.dark-gray
		return "stevechat." + this.name + "." + dependency.permissionExtension() + "." + color.name().toLowerCase().replaceAll("_", "-");
	}

	/**
	 * <em>Only applies if {@link #isColor()} returns true
	 */
	public String nodeAllWithDependency(final IPermissionDependency dependency) {
		// stevechat.color.global.*
		return "stevechat." + this.name + "." + dependency.permissionExtension() + ".*";
	}

	public String nodeAll() {
		return "stevechat." + this.name + ".*";
	}

	public String node() {
		return "stevechat." + this.name;
	}

	public boolean hasDependency() {
		return this.hasDependency;
	}

	public boolean isColor() {
		return this == SCPermission.COLOR;
	}

	public boolean isChildOfRoot() {
		return this.childOfRoot;
	}

	public PermissionDefault getDefaultValue() {
		return this.defaultValue;
	}

	public boolean hasParent() {
		return this.parent != null;
	}

	public SCPermission getParent() {
		return this.parent;
	}
}
