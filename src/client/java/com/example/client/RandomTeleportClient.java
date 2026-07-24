package com.example.client;

import com.example.RandomTeleportMod;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;

import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.server.IntegratedServer;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.levelgen.Heightmap;

import org.lwjgl.glfw.GLFW;

import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

public class RandomTeleportClient implements ClientModInitializer {
	// Farthest a single teleport can move you horizontally, in blocks.
	private static final int MAX_RANGE = 2000;

	private static KeyMapping teleportKey;

	@Override
	public void onInitializeClient() {
		// Register the "G" key. Players can rebind it under Options > Controls > Miscellaneous.
		teleportKey = KeyMappingHelper.registerKeyMapping(new KeyMapping(
			"key.randomteleport.teleport",   // name (translated via the lang file)
			GLFW.GLFW_KEY_G,                 // default key
			KeyMapping.Category.MISC         // controls-screen category
		));

		// Runs every client tick; consumeClick() is true once per physical key press.
		ClientTickEvents.END_CLIENT_TICK.register(client -> {
			while (teleportKey.consumeClick()) {
				teleportRandomly(client);
			}
		});
	}

	private static void teleportRandomly(Minecraft client) {
		LocalPlayer player = client.player;
		IntegratedServer server = client.getSingleplayerServer();

		if (player == null) {
			return;
		}
		if (server == null) {
			// A client-only mod can't move you on a real server without server-side support.
			player.sendSystemMessage(Component.literal("[Random Teleport] Singleplayer worlds only."));
			return;
		}

		// The actual move must run on the server thread so the client stays in sync.
		UUID uuid = player.getUUID();
		server.execute(() -> {
			ServerPlayer serverPlayer = server.getPlayerList().getPlayer(uuid);
			if (serverPlayer == null) {
				return;
			}
			ServerLevel level = serverPlayer.level();
			ThreadLocalRandom rng = ThreadLocalRandom.current();

			int x = (int) Math.floor(serverPlayer.getX()) + rng.nextInt(-MAX_RANGE, MAX_RANGE + 1);
			int z = (int) Math.floor(serverPlayer.getZ()) + rng.nextInt(-MAX_RANGE, MAX_RANGE + 1);
			// Drop onto the highest solid/liquid block in that column so you never land in the void or inside terrain.
			int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z) + 1;

			// +0.5 centers the player on the block.
			serverPlayer.teleportTo(x + 0.5, y, z + 0.5);
			serverPlayer.sendSystemMessage(
				Component.literal("Teleported to " + x + ", " + y + ", " + z), true);
			RandomTeleportMod.LOGGER.info("Random teleport -> {} {} {}", x, y, z);
		});
	}
}
