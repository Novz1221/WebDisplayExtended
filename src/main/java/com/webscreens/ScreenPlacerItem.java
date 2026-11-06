package com.webscreens;

import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.context.UseOnContext;

/** Milestone 0: only proves the item works. Milestone 1 turns the two clicks into a screen. */
public class ScreenPlacerItem extends Item {
	public ScreenPlacerItem(Properties properties) {
		super(properties);
	}

	@Override
	public InteractionResult useOn(UseOnContext context) {
		Player player = context.getPlayer();
		if (player != null) {
			player.displayClientMessage(Component.literal("Screen Placer: " + context.getClickedPos().toShortString()
					+ " face " + context.getClickedFace().getName()), true);
		}
		return InteractionResult.SUCCESS;
	}
}
