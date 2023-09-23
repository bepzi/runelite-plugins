package com.automaticlowdetail;

import net.runelite.client.RuneLite;
import net.runelite.client.externalplugins.ExternalPluginManager;

public class AutomaticLowDetailPluginTest
{
	public static void main(String[] args) throws Exception
	{
		ExternalPluginManager.loadBuiltin(AutomaticLowDetailPlugin.class);
		RuneLite.main(args);
	}
}