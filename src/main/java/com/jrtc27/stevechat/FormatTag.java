package com.jrtc27.stevechat;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public enum FormatTag {
	DEFAULT("{default}"),
	CHANNEL_NICK("{nick}"),
	CHANNEL_NAME("{name}"),
	CHANNEL_COLOR("{color}"),
	MESSAGE("{msg}", "{message}"),
	PLAYER_NICK("{sender}"),
	PLAYER_NAME("{plainsender}", "{fullsender}"),
	PLAYER_WORLD("{world}"),
	PLAYER_PREFIX("{prefix}"),
	PLAYER_SUFFIX("{suffix}"),
	PLAYER_GROUP("{group}"),
	GROUP_PREFIX("{groupprefix}"),
	GROUP_SUFFIX("{groupsuffix}"),
	TO_FROM("{tofrom}", "{convoaddress}"),
	PARTNER_NICK("{partner}"),
	PARTNER_NAME("{plainpartner}", "{fullpartner}"),
	PARTNER_WORLD("{partnerworld}"),
	PARTNER_PREFIX("{partnerprefix}"),
	PARTNER_SUFFIX("{partnersuffix}"),
	PARTNER_GROUP("{partnergroup}"),
	PARTNER_GROUP_PREFIX("{partnergroupprefix}"),
	PARTNER_GROUP_SUFFIX("{partnergroupsuffix}");

	private final String[] tags;

	private FormatTag(final String... tags) {
		this.tags = tags;
	}

	public String[] getTags() {
		return this.tags;
	}

	public static String replaceAll(final FormatTag formatTag, String subject, String replacement) {
		replacement = Matcher.quoteReplacement(replacement);
		for (String tag : formatTag.tags) {
			tag = Pattern.quote(tag);
			subject = subject.replaceAll(tag, replacement);
		}
		return subject;
	}

	public static String replaceFirst(final FormatTag formatTag, String subject, String replacement) {
		replacement = Matcher.quoteReplacement(replacement);
		for (String tag : formatTag.tags) {
			tag = Pattern.quote(tag);
			if (subject.contains(tag)) {
				subject = subject.replaceFirst(tag, replacement);
				break;
			}
		}
		return subject;
	}
}
