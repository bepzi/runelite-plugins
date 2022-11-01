package com.lowdetailchambers;

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
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.plugins.PluginManager;
import net.runelite.client.plugins.lowmemory.LowMemoryConfig;

@Slf4j
@PluginDescriptor(
	name = "Low Detail Chambers",
	description = "Turn off ground decorations and certain textures only inside of Chambers of Xeric",
	tags = {"memory", "usage", "ground", "decorations", "chambers", "cox"},
)
public class LowDetailChambersPlugin extends Plugin
{
	@Inject
	private Client client;

	@Inject
	private ClientThread clientThread;

	@Inject
	private PluginManager pluginManager;

	@Inject
	private ConfigManager configManager;

	@Override
	protected void startUp() throws Exception
	{
		clientThread.invoke(() ->
		{
			if (client.getGameState().getState() >= GameState.LOGIN_SCREEN.getState())
			{
				if (client.getVarbitValue(Varbits.IN_RAID) == 1 && lowDetailDisabled())
				{
					client.changeMemoryMode(true);
				}
				return true;
			}
			return false;
		});
	}

	@Override
	protected void shutDown() throws Exception
	{
		clientThread.invoke(() ->
		{
			if (lowDetailDisabled())
			{
				client.changeMemoryMode(false);
			}
		});
	}

	@Subscribe
	public void onVarbitChanged(VarbitChanged event)
	{
		if (event.getVarbitId() == Varbits.IN_RAID && lowDetailDisabled())
		{
			if (event.getValue() == 1)
			{
				client.changeMemoryMode(true);

			}
			else
			{
				client.changeMemoryMode(false);
			}
		}
	}

	private boolean lowDetailDisabled()
	{
		final String value = configManager.getConfiguration(RuneLiteConfig.GROUP_NAME, "lowmemoryplugin");

		boolean lowMemoryPluginEnabled = value != null ? Boolean.parseBoolean(value) : false;
		boolean lowMemoryConfigEnabled = configManager.getConfig(LowMemoryConfig.class).lowDetail();

		return !lowMemoryPluginEnabled | !lowMemoryConfigEnabled;
	}

}
