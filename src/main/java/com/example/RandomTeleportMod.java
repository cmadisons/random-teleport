package com.example;

import net.fabricmc.api.ModInitializer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RandomTeleportMod implements ModInitializer {
	public static final String MOD_ID = "randomteleport";

	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	@Override
	public void onInitialize() {
		LOGGER.info("Random Teleport loaded — press G in-game to teleport to a random location.");
	}
}
