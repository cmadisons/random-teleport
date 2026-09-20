package com.example;

import net.fabricmc.api.ModInitializer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RandomTeleportMod implements ModInitializer {
	public static final String MOD_ID = "randomteleport";

	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	@Override
	public void onInitialize() {
		Sheep.register();
		LOGGER.info("Random Teleport loaded — press G to teleport, and the sheep come in all sixteen colours.");
	}
}
