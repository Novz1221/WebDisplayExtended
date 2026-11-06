package com.webscreens.client;

import com.webscreens.ScreenData;
import com.webscreens.ScreenStore;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.Level;

import java.util.UUID;

/**
 * Client-side placement for servers without the mod. Turn it on with /webscreens place,
 * then right-click one corner and the opposite corner on the same wall.
 */
final class PlacementTool {
	private PlacementTool() { }

	static boolean active;
	private static String firstDim;
	private static BlockPos firstPos;
	private static Direction firstFace;

	private static void say(String text) {
		Minecraft mc = Minecraft.getInstance();
		if (mc.player != null) mc.player.displayClientMessage(Component.literal(text), true);
	}

	static void reset() {
		active = false;
		firstPos = null;
	}

	static void toggle() {
		active = !active;
		firstPos = null;
		say(active ? "Placement mode on: right-click one corner of a wall." : "Placement mode off.");
	}

	static void click(Level level, BlockPos pos, Direction face) {
		Minecraft mc = Minecraft.getInstance();
		if (mc.player == null) return;
		if (face.getAxis().isVertical()) {
			say("Click the side of a wall, not the top or bottom.");
			return;
		}
		String dim = level.dimension().location().toString();
		boolean samePlane = firstPos != null && firstDim.equals(dim) && firstFace == face
				&& (face.getAxis() == Direction.Axis.Z ? firstPos.getZ() == pos.getZ() : firstPos.getX() == pos.getX());
		if (!samePlane) {
			firstDim = dim;
			firstPos = pos.immutable();
			firstFace = face;
			say("First corner set. Click the opposite corner on the same face.");
			return;
		}

		ScreenData s = new ScreenData();
		s.id = UUID.randomUUID().toString();
		s.dim = dim;
		s.minX = Math.min(firstPos.getX(), pos.getX());
		s.maxX = Math.max(firstPos.getX(), pos.getX());
		s.minY = Math.min(firstPos.getY(), pos.getY());
		s.maxY = Math.max(firstPos.getY(), pos.getY());
		s.minZ = Math.min(firstPos.getZ(), pos.getZ());
		s.maxZ = Math.max(firstPos.getZ(), pos.getZ());
		s.face = face.getName();
		s.pxPerBlock = ClientConfig.defaultPx;
		s.fps = ClientConfig.defaultFps;
		s.dpi = ClientConfig.defaultDpi;
		s.owner = mc.player.getUUID().toString();
		s.ownerName = mc.player.getGameProfile().getName();
		firstPos = null;

		if (s.blocksW() > ScreenStore.MAX_SIZE || s.blocksH() > ScreenStore.MAX_SIZE) {
			say("Too big: max " + ScreenStore.MAX_SIZE + "x" + ScreenStore.MAX_SIZE + " blocks.");
			return;
		}
		active = false;
		LocalScreens.add(s);
		say("Screen created (" + s.blocksW() + "x" + s.blocksH() + "). Right-click it to use it, /webscreens settings to edit.");
	}
}
