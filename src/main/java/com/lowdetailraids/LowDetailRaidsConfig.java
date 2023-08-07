package com.lowdetailraids;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;

@ConfigGroup(LowDetailRaidsConfig.GROUP)
public interface LowDetailRaidsConfig extends Config
{
	String GROUP = "lowdetailraids";

	@ConfigItem(keyName = "chambersOfXeric", name = "Chambers of Xeric", description = "Whether to enable Low Detail while inside the Chambers of Xeric")
	default boolean chambersOfXeric()
	{
		return true;
	}

	@ConfigItem(keyName = "theatreOfBlood", name = "Theatre of Blood", description = "Whether to enable Low Detail while inside the Theatre of Blood")
	default boolean theatreOfBlood()
	{
		return true;
	}

	@ConfigItem(keyName = "tombsOfAmascut", name = "Tombs of Amascut", description = "Whether to enable Low Detail while inside the Tombs of Amascut")
	default boolean tombsOfAmascut()
	{
		return true;
	}
}
