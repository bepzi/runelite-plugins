package com.lowdetailraids;

import com.google.inject.Provides;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import javax.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.VarPlayer;
import net.runelite.api.Varbits;
import net.runelite.api.events.GameStateChanged;
import net.runelite.api.events.VarbitChanged;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.config.RuneLiteConfig;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.events.ConfigChanged;
import net.runelite.client.events.PluginChanged;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.plugins.lowmemory.LowMemoryConfig;
import net.runelite.client.plugins.lowmemory.LowMemoryPlugin;

@Slf4j
@PluginDescriptor(
	name = "Automatic Low Detail",
	description = "Automatically turn off ground decorations while inside certain areas (like raids)",
	tags = {"memory", "ground", "decorations", "cox", "xeric", "tob", "theatre", "toa", "amascut", "sepulchre", "inferno"},
	configName = AutomaticLowDetailPlugin.CONFIG_NAME
)
public class AutomaticLowDetailPlugin extends Plugin
{
	public static final String CONFIG_NAME = "LowDetailRaidsPlugin";

	private static final int VARP_IN_RAID_ENCOUNTER = 2926;
	private static final int VARBIT_IN_PARTY_TOMBS_OF_AMASCUT = 14345;
	private static final int VARBIT_IN_INFERNO = 11878;
	private static final int VARBIT_IN_HALLOWED_SEPULCHRE = 10392;

	private static final Set<Integer> RELEVANT_EVENT_VARPS = new HashSet<>(Arrays.asList(VARP_IN_RAID_ENCOUNTER, VarPlayer.IN_RAID_PARTY));
	private static final Set<Integer> RELEVANT_EVENT_VARBITS = new HashSet<>(Arrays.asList(Varbits.IN_RAID, Varbits.THEATRE_OF_BLOOD, VARBIT_IN_PARTY_TOMBS_OF_AMASCUT, VARBIT_IN_INFERNO, VARBIT_IN_HALLOWED_SEPULCHRE));

	@Inject
	private Client client;

	@Inject
	private ClientThread clientThread;

	@Inject
	private ConfigManager configManager;

	@Inject
	private AutomaticLowDetailConfig config;

	enum AutomaticLowDetailRegion
	{
		CHAMBERS_OF_XERIC,
		THEATRE_OF_BLOOD,
		TOMBS_OF_AMASCUT,
		INFERNO,
		HALLOWED_SEPULCHRE
	}

	private boolean lowDetailModeEnabled = false;

	@Override
	protected void startUp()
	{
		lowDetailModeEnabled = lowDetailPluginEnabled();
		clientThread.invoke(this::updateLowDetailMode);
	}

	@Override
	protected void shutDown()
	{
		if (!lowDetailPluginEnabled() && lowDetailModeEnabled)
		{
			clientThread.invoke(() -> client.changeMemoryMode(false));
			lowDetailModeEnabled = false;
		}
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
		else if (event.getGroup().equals(LowMemoryConfig.GROUP))
		{
			// Can't call updateLowDetailMode() immediately, because the Low Detail plugin is about to call
			// client.changeMemoryMode(), which will undo anything we do now.
			//
			// If the Low Detail plugin was turned off, then we'll be free to re-disable ground decorations the next
			// time we check whether we're in a supported area. Otherwise, it doesn't matter.
			lowDetailModeEnabled = lowDetailPluginEnabled();
			// TODO: Can this be simplified to just invokeAtTickEnd?
			clientThread.invokeAtTickEnd(() -> clientThread.invokeLater(this::updateLowDetailMode));
		}
	}

	@Subscribe
	public void onPluginChanged(PluginChanged event)
	{
		if (event.getPlugin() instanceof LowMemoryPlugin)
		{
			lowDetailModeEnabled = lowDetailPluginEnabled();
			updateLowDetailMode();
		}
	}

	@Subscribe
	public void onGameStateChanged(GameStateChanged gameStateChanged)
	{
		if (lowDetailPluginEnabled())
		{
			return;
		}

		if (gameStateChanged.getGameState() == GameState.STARTING)
		{
			client.changeMemoryMode(false);
			lowDetailModeEnabled = false;
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
		if (lowDetailPluginEnabled())
		{
			return;
		}

		Optional<AutomaticLowDetailRegion> region = canEnableLowDetailMode();
		if (!lowDetailModeEnabled && region.isPresent())
		{
			client.changeMemoryMode(true);
			lowDetailModeEnabled = true;
			log.debug("Automatically enabled Low Detail Mode for region: {}", region.get());
		}
		else if (lowDetailModeEnabled && !region.isPresent())
		{
			client.changeMemoryMode(false);
			lowDetailModeEnabled = false;
			log.debug("Automatically disabled Low Detail Mode");
		}
	}

	private Optional<AutomaticLowDetailRegion> canEnableLowDetailMode()
	{
		// When the client starts it initializes the texture size based on the memory mode setting.
		// Don't set low memory before the login screen is ready to prevent loading the low detail textures,
		// which breaks the gpu plugin due to it requiring the 128x128px textures
		if (client.getGameState().getState() < GameState.LOGIN_SCREEN.getState())
		{
			return Optional.empty();
		}

		if (insideChambersOfXeric() && config.chambersOfXeric())
		{
			return Optional.of(AutomaticLowDetailRegion.CHAMBERS_OF_XERIC);
		}
		else if (insideTheatreOfBlood() && config.theatreOfBlood())
		{
			return Optional.of(AutomaticLowDetailRegion.THEATRE_OF_BLOOD);
		}
		else if (insideTombsOfAmascut() && config.tombsOfAmascut())
		{
			return Optional.of(AutomaticLowDetailRegion.TOMBS_OF_AMASCUT);
		}
		else if (insideInferno() && config.inferno())
		{
			return Optional.of(AutomaticLowDetailRegion.INFERNO);
		}
		else if (insideHallowedSepulchre() && config.hallowedSepulchre())
		{
			return Optional.of(AutomaticLowDetailRegion.HALLOWED_SEPULCHRE);
		}

		return Optional.empty();
	}

	// ====================================================================================

	private boolean insideRaidEncounter()
	{
		return client.getVarpValue(VARP_IN_RAID_ENCOUNTER) != 0;
	}

	private boolean insideChambersOfXeric()
	{
		int raidPartyID = client.getVarpValue(VarPlayer.IN_RAID_PARTY);
		boolean inRaidChambers = client.getVarbitValue(Varbits.IN_RAID) == 1;
		return raidPartyID != -1 && inRaidChambers;
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
