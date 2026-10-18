package com.clanclogclient;

import net.runelite.client.RuneLite;
import net.runelite.client.externalplugins.ExternalPluginManager;

/** Development launcher; automated tests live in the other test classes. */
public class ClanClogPluginTest
{
	public static void main(String[] args) throws Exception
	{
		ExternalPluginManager.loadBuiltin(ClanClogPlugin.class);
		RuneLite.main(args);
	}
}
