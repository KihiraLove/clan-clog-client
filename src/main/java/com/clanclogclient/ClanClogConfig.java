package com.clanclogclient;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;

@ConfigGroup(ClanClogConfig.GROUP)
public interface ClanClogConfig extends Config
{
	String GROUP = "clan-clog-client";
	String SUBMISSION_CHAT_NOTIFICATION = "submissionChatNotification";

	@ConfigItem(keyName = "submissionUrl", name = "Submission endpoint URL", position = 0,
		description = "Full Submission endpoint URL, you get this from your clans admin or owner.")
	default String submissionUrl()
	{
		return "";
	}

	@ConfigItem(keyName = "clanApiKey", name = "Clan API key", position = 1, secret = true,
		description = "Key to access the submissions API, you get this from your clans admin or owner.")
	default String clanApiKey()
	{
		return "";
	}

	@ConfigItem(keyName = SUBMISSION_CHAT_NOTIFICATION, name = "Submission chat notification", position = 2,
		description = "Show submission related messages in the in-game chat.")
	default boolean submissionChatNotification()
	{
		return true;
	}
}
