package com.example.client;

import com.example.RandomTeleportMod;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;

import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.server.IntegratedServer;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.material.Fluids;

import org.lwjgl.glfw.GLFW;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

public class RandomTeleportClient implements ClientModInitializer {

	/** The four places a teleport can send you. One is picked at random per key press. */
	private enum Destination { ABOVE_GROUND, WATER, NETHER, END }

	/** A resolved teleport target: which world, which block, and a label for the message. */
	private record Target(ServerLevel level, BlockPos pos, String label) {}

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

		UUID uuid = player.getUUID();
		Destination[] all = Destination.values();
		Destination destination = all[ThreadLocalRandom.current().nextInt(all.length)];

		// The move (and all world queries) must run on the server thread.
		server.execute(() -> {
			ServerPlayer sp = server.getPlayerList().getPlayer(uuid);
			if (sp == null) {
				return;
			}
			int cx = (int) Math.floor(sp.getX());
			int cz = (int) Math.floor(sp.getZ());
			ThreadLocalRandom rng = ThreadLocalRandom.current();

			Target target = switch (destination) {
				case ABOVE_GROUND -> findAboveGround(server, cx, cz, rng);
				case WATER -> findWater(server, cx, cz, rng);
				case NETHER -> findNether(server, cx, cz, rng);
				case END -> findEnd(server, rng);
			};

			// If the chosen destination couldn't be found, fall back to dry land in the overworld.
			if (target == null) {
				Target fallback = findAboveGround(server, cx, cz, rng);
				if (fallback == null) {
					sp.sendSystemMessage(Component.literal("[Random Teleport] Couldn't find a safe spot."), true);
					return;
				}
				target = new Target(fallback.level(), fallback.pos(), fallback.label() + " (fallback)");
			}

			BlockPos p = target.pos();
			boolean ok = sp.teleportTo(
				target.level(), p.getX() + 0.5, p.getY(), p.getZ() + 0.5,
				Set.of(), sp.getYRot(), sp.getXRot(), true);

			if (ok) {
				sp.sendSystemMessage(Component.literal(
					"Teleported " + target.label() + " → " + p.getX() + ", " + p.getY() + ", " + p.getZ()), true);
				RandomTeleportMod.LOGGER.info("Random teleport {} -> {} {} {}",
					target.label(), p.getX(), p.getY(), p.getZ());
			}
		});
	}

	// ---- destination finders (all run on the server thread) --------------------------------

	/** Random dry-land spot on the overworld surface (skips oceans/rivers). */
	private static Target findAboveGround(IntegratedServer server, int cx, int cz, ThreadLocalRandom rng) {
		ServerLevel level = server.overworld();
		for (int i = 0; i < 24; i++) {
			int x = cx + rng.nextInt(-3000, 3001);
			int z = cz + rng.nextInt(-3000, 3001);
			int surface = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
			BlockState ground = level.getBlockState(new BlockPos(x, surface - 1, z));
			if (ground.blocksMotion() && ground.getFluidState().isEmpty()) {
				return new Target(level, new BlockPos(x, surface, z), "above ground");
			}
		}
		return null;
	}

	/** Random spot floating at the surface of an ocean/lake in the overworld. */
	private static Target findWater(IntegratedServer server, int cx, int cz, ThreadLocalRandom rng) {
		ServerLevel level = server.overworld();
		for (int i = 0; i < 32; i++) {
			int x = cx + rng.nextInt(-4000, 4001);
			int z = cz + rng.nextInt(-4000, 4001);
			int top = level.getHeight(Heightmap.Types.MOTION_BLOCKING, x, z);
			BlockState surfaceBlock = level.getBlockState(new BlockPos(x, top - 1, z));
			if (surfaceBlock.getFluidState().isSourceOfType(Fluids.WATER)) {
				return new Target(level, new BlockPos(x, top - 1, z), "into water");
			}
		}
		return null;
	}

	/** Random air pocket in the Nether with solid, non-lava ground below and headroom above. */
	private static Target findNether(IntegratedServer server, int cx, int cz, ThreadLocalRandom rng) {
		ServerLevel level = server.getLevel(Level.NETHER);
		if (level == null) {
			return null;
		}
		for (int i = 0; i < 20; i++) {
			int x = cx + rng.nextInt(-800, 801);
			int z = cz + rng.nextInt(-800, 801);
			// Scan down from just below the bedrock roof for the first standable ledge.
			for (int y = 118; y >= level.getMinY() + 2; y--) {
				BlockState ground = level.getBlockState(new BlockPos(x, y, z));
				BlockState feet = level.getBlockState(new BlockPos(x, y + 1, z));
				BlockState head = level.getBlockState(new BlockPos(x, y + 2, z));
				if (ground.blocksMotion() && ground.getFluidState().isEmpty()
					&& feet.isAir() && head.isAir()) {
					return new Target(level, new BlockPos(x, y + 1, z), "to the Nether");
				}
			}
		}
		return null;
	}

	/** Random spot on the End's main island (stays near 0,0 so you don't drop into the void). */
	private static Target findEnd(IntegratedServer server, ThreadLocalRandom rng) {
		ServerLevel level = server.getLevel(Level.END);
		if (level == null) {
			return null;
		}
		for (int i = 0; i < 24; i++) {
			int x = rng.nextInt(-128, 129);
			int z = rng.nextInt(-128, 129);
			// Avoid landing on the central exit portal.
			if (Math.abs(x) < 8 && Math.abs(z) < 8) {
				continue;
			}
			int surface = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
			if (surface > level.getMinY()) {
				BlockState ground = level.getBlockState(new BlockPos(x, surface - 1, z));
				if (ground.blocksMotion() && ground.getFluidState().isEmpty()) {
					return new Target(level, new BlockPos(x, surface, z), "to the End");
				}
			}
		}
		return null;
	}
}
