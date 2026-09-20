package com.example;

import com.mojang.serialization.Codec;

import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents;

import net.minecraft.resources.Identifier;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.DyeColor;

/**
 * Sheep in every colour.
 *
 * Vanilla sheep are white almost all of the time: the spawn table is roughly
 * 82% white, with a few percent each of grey, light grey, black and brown and
 * a tenth of a percent of pink. A field of them reads as one colour.
 *
 * Every sheep that spawns here gets one of the sixteen dyes instead, picked
 * evenly, so a flock looks like a flock.
 *
 * <h2>Why the flag</h2>
 *
 * The only hook for "a sheep appeared" is ENTITY_LOAD, and that fires again
 * every time the chunk comes back. Recolouring on each load would mean a
 * sheep changed colour whenever you walked away and came back, which is worse
 * than them all being white. So the first time one is seen it is painted and
 * marked, the mark is saved with the sheep, and it is left alone after that.
 *
 * Shearing and dyeing still work exactly as they do in vanilla: this only ever
 * runs once, at the start of a sheep's life.
 */
public final class Sheep {
	private Sheep() {
	}

	/** Set on a sheep once it has been given its colour. */
	private static final AttachmentType<Boolean> PAINTED =
			AttachmentRegistry.<Boolean>builder()
					.initializer(() -> Boolean.FALSE)
					.persistent(Codec.BOOL)
					.buildAndRegister(Identifier.fromNamespaceAndPath(
							RandomTeleportMod.MOD_ID, "painted"));

	public static void register() {
		ServerEntityEvents.ENTITY_LOAD.register((entity, level) -> {
			if (!(entity instanceof net.minecraft.world.entity.animal.sheep.Sheep sheep)) {
				return;
			}
			if (Boolean.TRUE.equals(sheep.getAttachedOrCreate(PAINTED))) {
				return;                     // already had its turn
			}
			sheep.setAttached(PAINTED, Boolean.TRUE);

			// A lamb is born the colour of its parents, and taking that away
			// would break breeding for colour -- which is half the point of
			// having sixteen of them about. Only grown sheep that turned up on
			// their own are repainted.
			if (sheep.isBaby()) {
				return;
			}

			RandomSource random = sheep.getRandom();
			DyeColor[] all = DyeColor.values();
			sheep.setColor(all[random.nextInt(all.length)]);
		});
	}
}
