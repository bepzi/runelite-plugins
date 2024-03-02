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
	configName = AutomaticLowDetailPlugin.CONFIG_NAME,
	conflicts = "Low Detail Chambers"
)
public class AutomaticLowDetailPlugin extends Plugin
{
	public static final String CONFIG_NAME = "LowDetailRaidsPlugin";

	private static final int VARP_IN_RAID_ENCOUNTER = 2926;
	private static final int VARBIT_IN_PARTY_TOMBS_OF_AMASCUT = 14345;
	private static final int VARBIT_IN_INFERNO = 11878;
	private static final int VARBIT_IN_HALLOWED_SEPULCHRE = 10392;

	private static final Set<Integer> RELEVANT_EVENT_VARPS = new HashSet<>(Arrays.asList(VARP_IN_RAID_ENCOUNTER, VarPlayer.IN_RAID_PARTY));
	private static final Set<Integer> RELEVANT_EVENT_VARBITS = new HashSet<>(Arrays.asList(Varbits.IN_RAID, Varbits.THEATRE_OF_BLOOD, Varbits.RAID_STATE, VARBIT_IN_PARTY_TOMBS_OF_AMASCUT, VARBIT_IN_INFERNO, VARBIT_IN_HALLOWED_SEPULCHRE));

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
		lowDetailModeEnabled = vanillaLowDetailPluginEnabled();
		clientThread.invoke(this::updateLowDetailMode);
	}

	@Override
	protected void shutDown()
	{
		if (!vanillaLowDetailPluginEnabled() && lowDetailModeEnabled)
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
			clientThread.invoke(this::updateLowDetailMode);
		}
		else if (event.getGroup().equals(LowMemoryConfig.GROUP))
		{
			lowDetailModeEnabled = vanillaLowDetailPluginEnabled();

			// Can't call updateLowDetailMode() immediately, because the Low Detail plugin is about to call
			// client.changeMemoryMode(), which will undo anything we do now.
			//
			// clientThread.invoke() and clientThread.invokeLater() do not work; we must wait until the end
			// of the client tick.
			clientThread.invokeAtTickEnd(this::updateLowDetailMode);
		}
	}

	@Subscribe
	public void onPluginChanged(PluginChanged event)
	{
		if (event.getPlugin() instanceof LowMemoryPlugin)
		{
			lowDetailModeEnabled = vanillaLowDetailPluginEnabled();
			clientThread.invoke(this::updateLowDetailMode);
		}
	}

	@Subscribe
	public void onGameStateChanged(GameStateChanged gameStateChanged)
	{
		if (vanillaLowDetailPluginEnabled())
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

	/**
	 * Attempts to correct the client memory mode depending on whether the player is in a valid region for automatic
	 * low detail mode and if the memory mode really has changed and needs to be adjusted.
	 * <p>
	 * If Runelite's vanilla Low Detail plugin is active, this function will do nothing.
	 * <p>
	 * <strong>This function must be called on the client thread.</strong>
	 */
	private void updateLowDetailMode()
	{
		if (vanillaLowDetailPluginEnabled())
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

	private boolean insideRaidEncounter()
	{
		return client.getVarpValue(VARP_IN_RAID_ENCOUNTER) != 0;
	}

	// ====================================================================================

	private boolean insideChambersOfXeric()
	{
		if (client.getVarbitValue(Varbits.IN_RAID) != 1)
		{
			// Not inside the lobby or the raid levels.
			return false;
		}

		int raidPartyID = client.getVarpValue(VarPlayer.IN_RAID_PARTY);
		if (raidPartyID == -1)
		{
			// Raid party ID is -1 when:
			// 1. We're not in a raid party at all (e.g. outside the raid)
			// 2. We were in a party but we're currently reloading the raid from the inside stairs
			// 3. We were in a party but then we started the raid
			//
			// Only #3 is a valid reason to enable low detail mode. The other two cases should NOT result
			// in toggling low detail.

			// The plugin crashes if we check RAID_STATE while not inside Chambers, so only check
			// RAID_STATE here now that we know it's safe.
			int raidState = client.getVarbitValue(Varbits.RAID_STATE);
			return raidState != 0 && raidState != 5;
		}

		// We're in the lobby, we haven't started the raid, and we're not currently reloading the raid.
		return true;
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

	private boolean vanillaLowDetailPluginEnabled()
	{
		final String pluginEnabled = configManager.getConfiguration(RuneLiteConfig.GROUP_NAME, "lowmemoryplugin");
		if (!Boolean.parseBoolean(pluginEnabled))
		{
			return false;
		}
		return configManager.getConfig(LowMemoryConfig.class).lowDetail();
	}

	// ====================================================================================

	enum AutomaticLowDetailRegion
	{
		CHAMBERS_OF_XERIC,
		THEATRE_OF_BLOOD,
		TOMBS_OF_AMASCUT,
		INFERNO,
		HALLOWED_SEPULCHRE
	}
}
