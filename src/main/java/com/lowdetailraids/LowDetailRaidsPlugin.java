package com.lowdetailraids;

import com.google.inject.Provides;
import javax.inject.Inject;

import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.Varbits;
import net.runelite.api.events.VarbitChanged;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.config.RuneLiteConfig;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.events.ConfigChanged;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.plugins.lowmemory.LowMemoryConfig;

@Slf4j
@PluginDescriptor(
	name = "Low Detail Raids",
	description = "Automatically turn off ground decorations while inside raids",
	tags = {"memory", "ground", "decorations", "chambers", "cox", "theatre", "tob", "tombs", "amascut", "toa"}
)
public class LowDetailRaidsPlugin extends Plugin
{
	private static final int VARP_IN_RAID_ENCOUNTER = 2926;
	private static final int VARBIT_IN_PARTY_TOMBS_OF_AMASCUT = 14345;

	@Inject
	private Client client;

	@Inject
	private ClientThread clientThread;

	@Inject
	private ConfigManager configManager;

	@Inject
	private LowDetailRaidsConfig config;

	@Override
	protected void startUp()
	{
		updateLowDetailMode();
	}

	@Override
	protected void shutDown()
	{
		updateLowDetailMode();
	}

	@Provides
	LowDetailRaidsConfig getConfig(ConfigManager configManager)
	{
		return configManager.getConfig(LowDetailRaidsConfig.class);
	}

	@Subscribe
	public void onConfigChanged(ConfigChanged event)
	{
		if (event.getGroup().equals(LowDetailRaidsConfig.GROUP))
		{
			updateLowDetailMode();
		}
	}

	@Subscribe
	public void onVarbitChanged(VarbitChanged event)
	{
		if (varbitChangedEventRelatedToRaid(event))
		{
			updateLowDetailMode();
		}
	}

	private boolean varbitChangedEventRelatedToRaid(VarbitChanged event)
	{
		return event.getVarpId() == VARP_IN_RAID_ENCOUNTER || event.getVarbitId() == Varbits.IN_RAID || event.getVarbitId() == Varbits.THEATRE_OF_BLOOD || event.getVarbitId() == VARBIT_IN_PARTY_TOMBS_OF_AMASCUT;
	}

	private void updateLowDetailMode()
	{
		clientThread.invoke(() -> {
			if (canEnableLowDetailMode())
			{
				client.changeMemoryMode(true);
				return true;
			}
			else if (canDisableLowDetailMode())
			{
				client.changeMemoryMode(false);
				return true;
			}
			return false;
		});
	}

	private boolean canEnableLowDetailMode()
	{
		// When the client starts it initializes the texture size based on the memory mode setting.
		// Don't set low memory before the login screen is ready to prevent loading the low detail textures,
		// which breaks the gpu plugin due to it requiring the 128x128px textures
		if (client.getGameState().getState() < GameState.LOGIN_SCREEN.getState())
		{
			return false;
		}
		if (!insideRaidEncounter())
		{
			return false;
		}

		if (insideChambersOfXeric())
		{
			return config.chambersOfXeric();
		}
		else if (insideTheatreOfBlood())
		{
			return config.theatreOfBlood();
		}
		else if (insideTombsOfAmascut())
		{
			return config.tombsOfAmascut();
		}

		return false;
	}

	private boolean canDisableLowDetailMode()
	{
		return !lowDetailPluginEnabled();
	}

	private boolean insideRaidEncounter()
	{
		return client.getVarpValue(VARP_IN_RAID_ENCOUNTER) != 0;
	}

	private boolean insideChambersOfXeric()
	{
		return client.getVarbitValue(Varbits.IN_RAID) != 0;
	}

	private boolean insideTheatreOfBlood()
	{
		return client.getVarbitValue(Varbits.THEATRE_OF_BLOOD) > 1;
	}

	private boolean insideTombsOfAmascut()
	{
		// TOA doesn't seem to have a convenient Varbit we can check like we can for COX and TOB.
		return insideRaidEncounter() && client.getVarbitValue(VARBIT_IN_PARTY_TOMBS_OF_AMASCUT) != 0;
	}

	private boolean lowDetailPluginEnabled()
	{
		final String pluginEnabled = configManager.getConfiguration(RuneLiteConfig.GROUP_NAME, "lowmemoryplugin");
		if (!Boolean.parseBoolean(pluginEnabled))
		{
			return false;
		}
		return configManager.getConfig(LowMemoryConfig.class).lowDetail();
	}
}
