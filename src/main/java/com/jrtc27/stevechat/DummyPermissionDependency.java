package com.jrtc27.stevechat;

public class DummyPermissionDependency implements IPermissionDependency {
	private final String extension;

	public DummyPermissionDependency(final String extension) {
		this.extension = extension;
	}

	@Override
	public String permissionExtension() {
		return this.extension;
	}

}
