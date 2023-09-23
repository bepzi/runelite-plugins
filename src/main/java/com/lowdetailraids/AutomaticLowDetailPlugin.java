package com.lowdetailraids;

import com.google.inject.Provides;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
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
@PluginDescriptor(name = "Automatic Low Detail", description = "Automatically turn off ground decorations while inside certain areas (like raids)", tags = {"memory", "ground", "decorations", "cox", "xeric", "tob", "theatre", "toa", "amascut", "sepulchre", "inferno"})
public class AutomaticLowDetailPlugin extends Plugin
{
	private static final int VARP_IN_RAID_ENCOUNTER = 2926;
	private static final int VARBIT_IN_PARTY_TOMBS_OF_AMASCUT = 14345;
	private static final int VARBIT_IN_INFERNO = 11878;
	private static final int VARBIT_IN_HALLOWED_SEPULCHRE = 10392;

	private static final Set<Integer> RELEVANT_EVENT_VARPS = new HashSet<>(Collections.singletonList(VARP_IN_RAID_ENCOUNTER));
	private static final Set<Integer> RELEVANT_EVENT_VARBITS = new HashSet<>(Arrays.asList(Varbits.IN_RAID, Varbits.THEATRE_OF_BLOOD, VARBIT_IN_PARTY_TOMBS_OF_AMASCUT, VARBIT_IN_INFERNO, VARBIT_IN_HALLOWED_SEPULCHRE));


	@Inject
	private Client client;

	@Inject
	private ClientThread clientThread;

	@Inject
	private ConfigManager configManager;

	@Inject
	private AutomaticLowDetailConfig config;

	private boolean lowDetailModeEnabled = false;

	@Override
	protected void startUp()
	{
		lowDetailModeEnabled = false;
		updateLowDetailMode();
	}

	@Override
	protected void shutDown()
	{
		updateLowDetailMode();
	}

	@Provides
	AutomaticLowDetailConfig getConfig(ConfigManager configManager)
	{
		return configManager.getConfig(AutomaticLowDetailConfig.class);
	}

	@Subscribe
	public void onConfigChanged(ConfigChanged event)
	{
		if (event.getGroup().equals(AutomaticLowDetailConfig.GROUP))
		{
			updateLowDetailMode();
		}
	}

	@Subscribe
	public void onVarbitChanged(VarbitChanged event)
	{
		if (varbitChangedEventIsRelevant(event))
		{
			updateLowDetailMode();
		}
	}

	private boolean varbitChangedEventIsRelevant(VarbitChanged event)
	{
		return RELEVANT_EVENT_VARPS.contains(event.getVarpId()) || RELEVANT_EVENT_VARBITS.contains(event.getVarbitId());
	}

	private void updateLowDetailMode()
	{
		clientThread.invoke(() -> {
			if (!lowDetailModeEnabled && canEnableLowDetailMode())
			{
				client.changeMemoryMode(true);
				lowDetailModeEnabled = true;
				log.debug("Automatically enabled Low Detail Mode");
				return true;
			}

			if (lowDetailModeEnabled && canDisableLowDetailMode())
			{
				client.changeMemoryMode(false);
				lowDetailModeEnabled = false;
				log.debug("Automatically disabled Low Detail Mode");
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
		else if (insideInferno())
		{
			return config.inferno();
		}
		else if (insideHallowedSepulchre())
		{
			return config.hallowedSepulchre();
		}

		return false;
	}

	private boolean canDisableLowDetailMode()
	{
		return lowDetailPluginDisabled() && !canEnableLowDetailMode();
	}

	// ====================================================================================

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

	private boolean insideInferno()
	{
		return client.getVarbitValue(VARBIT_IN_INFERNO) == 1;
	}

	private boolean insideHallowedSepulchre()
	{
		return client.getVarbitValue(VARBIT_IN_HALLOWED_SEPULCHRE) > 0;
	}

	// ====================================================================================

	private boolean lowDetailPluginDisabled()
	{
		final String pluginEnabled = configManager.getConfiguration(RuneLiteConfig.GROUP_NAME, "lowmemoryplugin");
		if (!Boolean.parseBoolean(pluginEnabled))
		{
			return true;
		}
		return !configManager.getConfig(LowMemoryConfig.class).lowDetail();
	}
}
