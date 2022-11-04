package com.botdetectorantispam;

import net.runelite.client.RuneLite;
import net.runelite.client.externalplugins.ExternalPluginManager;

public class BotDetectorAntiSpamTest
{
	public static void main(String[] args) throws Exception
	{
		ExternalPluginManager.loadBuiltin(BotDetectorAntiSpamPlugin.class);
		RuneLite.main(args);
	}
}