package com.lowdetailraids;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;

@ConfigGroup(AutomaticLowDetailConfig.GROUP)
public interface AutomaticLowDetailConfig extends Config
{
	String GROUP = "lowdetailraids";

	@ConfigItem(keyName = "chambersOfXeric", name = "Chambers of Xeric", description = "Whether to enable Low Detail while inside the Chambers of Xeric", position = 0)
	default boolean chambersOfXeric()
	{
		return true;
	}

	@ConfigItem(keyName = "theatreOfBlood", name = "Theatre of Blood", description = "Whether to enable Low Detail while inside the Theatre of Blood", position = 1)
	default boolean theatreOfBlood()
	{
		return true;
	}

	@ConfigItem(keyName = "tombsOfAmascut", name = "Tombs of Amascut", description = "Whether to enable Low Detail while inside the Tombs of Amascut", position = 2)
	default boolean tombsOfAmascut()
	{
		return true;
	}

	@ConfigItem(keyName = "inferno", name = "Inferno", description = "Whether to enable Low Detail while inside the Inferno (and TzHaar-Ket-Rak's Challenges)", position = 3)
	default boolean inferno()
	{
		return true;
	}

	@ConfigItem(keyName = "hallowedSepulchre", name = "Hallowed Sepulchre", description = "Whether to enable Low Detail while inside the Hallowed Sepulchre", position = 4)
	default boolean hallowedSepulchre()
	{
		return true;
	}
}
