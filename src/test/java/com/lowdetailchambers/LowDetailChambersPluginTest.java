package com.lowdetailchambers;

import net.runelite.client.RuneLite;
import net.runelite.client.externalplugins.ExternalPluginManager;

public class LowDetailChambersPluginTest
{
	public static void main(String[] args) throws Exception
	{
		ExternalPluginManager.loadBuiltin(LowDetailChambersPlugin.class);
		RuneLite.main(args);
	}
}