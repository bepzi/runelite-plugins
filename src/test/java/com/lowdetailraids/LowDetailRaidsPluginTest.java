package com.lowdetailraids;

import net.runelite.client.RuneLite;
import net.runelite.client.externalplugins.ExternalPluginManager;

public class LowDetailRaidsPluginTest
{
	public static void main(String[] args) throws Exception
	{
		ExternalPluginManager.loadBuiltin(LowDetailRaidsPlugin.class);
		RuneLite.main(args);
	}
}