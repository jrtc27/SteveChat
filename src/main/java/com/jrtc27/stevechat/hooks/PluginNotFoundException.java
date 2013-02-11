package com.jrtc27.stevechat.hooks;

public class PluginNotFoundException extends Exception {
	private static final long serialVersionUID = 989943549696228505L;

	public final String pluginName;

	public PluginNotFoundException(final String pluginName) {
		this.pluginName = pluginName;
	}
}
