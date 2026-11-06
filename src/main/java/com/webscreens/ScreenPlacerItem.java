package com.webscreens;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;

import java.util.UUID;

/**
 * Click one corner, then the opposite corner on the same wall. Clicking an existing screen with this item
 * opens its settings (handled on the client, see WebScreensClient).
 */
public class ScreenPlacerItem extends Item {
	public ScreenPlacerItem(Properties properties) {
		super(properties);
	}

	@Override
	public InteractionResult useOn(UseOnContext context) {
		Level level = context.getLevel();
		if (level.isClientSide || !(context.getPlayer() instanceof ServerPlayer player)) {
			return InteractionResult.SUCCESS;
		}

		Direction face = context.getClickedFace();
		BlockPos pos = context.getClickedPos();
		if (face.getAxis().isVertical()) {
			player.displayClientMessage(Component.literal("Click the side of a wall, not the top or bottom."), true);
			return InteractionResult.SUCCESS;
		}

		String dim = level.dimension().location().toString();
		ScreenStore.Pending first = ScreenStore.pending().get(player.getUUID());
		boolean samePlane = first != null && first.dim().equals(dim) && first.face() == face
				&& (face.getAxis() == Direction.Axis.Z ? first.pos().getZ() == pos.getZ() : first.pos().getX() == pos.getX());

		if (!samePlane) {
			ScreenStore.pending().put(player.getUUID(), new ScreenStore.Pending(dim, pos.immutable(), face));
			player.displayClientMessage(Component.literal("First corner set. Click the opposite corner on the same face."), true);
			return InteractionResult.SUCCESS;
		}

		ScreenStore.pending().remove(player.getUUID());
		BlockPos a = first.pos();
		ScreenData s = new ScreenData();
		s.id = UUID.randomUUID().toString();
		s.dim = dim;
		s.minX = Math.min(a.getX(), pos.getX());
		s.maxX = Math.max(a.getX(), pos.getX());
		s.minY = Math.min(a.getY(), pos.getY());
		s.maxY = Math.max(a.getY(), pos.getY());
		s.minZ = Math.min(a.getZ(), pos.getZ());
		s.maxZ = Math.max(a.getZ(), pos.getZ());
		s.face = face.getName();
		s.owner = player.getUUID().toString();
		s.ownerName = player.getGameProfile().getName();

		if (s.blocksW() > ScreenStore.MAX_SIZE || s.blocksH() > ScreenStore.MAX_SIZE) {
			player.displayClientMessage(Component.literal("Too big: max " + ScreenStore.MAX_SIZE + "x" + ScreenStore.MAX_SIZE + " blocks."), true);
			return InteractionResult.SUCCESS;
		}
		long mine = ScreenStore.all().stream().filter(o -> o.owner.equals(s.owner)).count();
		if (mine >= ScreenStore.MAX_PER_PLAYER && !player.hasPermissions(2)) {
			player.displayClientMessage(Component.literal("You already have " + ScreenStore.MAX_PER_PLAYER + " screens."), true);
			return InteractionResult.SUCCESS;
		}

		ScreenStore.all().add(s);
		ScreenStore.changed(player.getServer());
		player.displayClientMessage(Component.literal("Screen created (" + s.blocksW() + "x" + s.blocksH()
				+ "). Click it with this item to open the settings."), true);
		return InteractionResult.SUCCESS;
	}
}
