package com.jrtc27.stevechat;

import java.util.HashMap;
import java.util.Map;

public class PlayerInfo {
	private final Map<String, PlayerInfoPermission> nodes = new HashMap<String, PlayerInfoPermission>();
	public final String name, group, prefix, suffix, groupPrefix, groupSuffix;

	public PlayerInfo(final String name, final String group, final String prefix, final String suffix, final String groupPrefix, final String groupSuffix) {
		this.name = name;
		this.group = group;
		this.prefix = prefix;
		this.suffix = suffix;
		this.groupPrefix = groupPrefix;
		this.groupSuffix = groupSuffix;
	}

	public PlayerInfoPermission nodeValue(final String node) {
		synchronized (this.nodes) {
			return this.nodes.get(node.toLowerCase());
		}
	}

	public void putNodeValue(final String node, final PlayerInfoPermission value) {
		synchronized (this.nodes) {
			this.nodes.put(node.toLowerCase(), value);
		}
	}
}