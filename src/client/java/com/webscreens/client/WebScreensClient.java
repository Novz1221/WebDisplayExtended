package com.webscreens.client;

import com.mojang.brigadier.Command;
import com.webscreens.Payloads;
import com.webscreens.ScreenData;
import com.webscreens.WebScreensMod;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.phys.HitResult;

public class WebScreensClient implements ClientModInitializer {
	@Override
	public void onInitializeClient() {
		ClientPlayNetworking.registerGlobalReceiver(Payloads.Sync.TYPE,
				(payload, context) -> ClientScreens.onSync(payload.json()));

		ClientTickEvents.END_CLIENT_TICK.register(client -> ClientScreens.tick());
		WorldRenderEvents.AFTER_ENTITIES.register(ClientScreens::render);

		// Server with the mod: screens come from the server. Any other server: screens stay on this client.
		ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> {
			PlacementTool.reset();
			ScreenActions.serverMode = ClientPlayNetworking.canSend(Payloads.Edit.TYPE);
			if (ScreenActions.serverMode) {
				WebScreensMod.LOGGER.info("Server has Web Screens: shared screens");
			} else {
				WebScreensMod.LOGGER.info("Server does not have Web Screens: client-only screens");
				LocalScreens.loadForCurrentServer();
			}
		});
		ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> {
			PlacementTool.reset();
			ClientScreens.closeAll();
		});

		ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> dispatcher.register(
				ClientCommandManager.literal("webscreens")
						.then(ClientCommandManager.literal("place").executes(ctx -> {
							if (ScreenActions.serverMode) {
								ctx.getSource().sendFeedback(Component.literal("This server has Web Screens: use the Screen Placer item."));
							} else {
								PlacementTool.toggle();
							}
							return Command.SINGLE_SUCCESS;
						}))
						.then(ClientCommandManager.literal("settings").executes(ctx -> {
							Minecraft mc = Minecraft.getInstance();
							ScreenData s = lookedAtScreen(mc);
							if (s == null) {
								ctx.getSource().sendFeedback(Component.literal("Look at a screen first."));
							} else {
								mc.execute(() -> mc.setScreen(new SettingsScreen(s)));
							}
							return Command.SINGLE_SUCCESS;
						}))
						.executes(ctx -> {
							ctx.getSource().sendFeedback(Component.literal("/webscreens place - create a screen (servers without the mod)"));
							ctx.getSource().sendFeedback(Component.literal("/webscreens settings - settings of the screen you are looking at"));
							return Command.SINGLE_SUCCESS;
						})));

		// Right-click: on a screen = use it (or settings when holding the Screen Placer / in placement mode).
		UseBlockCallback.EVENT.register((player, level, hand, hit) -> {
			if (!level.isClientSide || hand != InteractionHand.MAIN_HAND) return InteractionResult.PASS;
			Minecraft mc = Minecraft.getInstance();
			boolean tool = PlacementTool.active || player.getItemInHand(hand).is(WebScreensMod.SCREEN_PLACER);

			ScreenData s = ClientScreens.findAt(level, hit.getBlockPos(), hit.getDirection());
			if (s != null) {
				if (tool) {
					mc.setScreen(new SettingsScreen(s));
				} else {
					BridgeBrowser b = ClientScreens.browserOf(s);
					if (b != null) mc.setScreen(new ControlScreen(b));
				}
				return InteractionResult.SUCCESS;
			}
			if (PlacementTool.active) {
				PlacementTool.click(level, hit.getBlockPos(), hit.getDirection());
				return InteractionResult.SUCCESS;
			}
			return InteractionResult.PASS;
		});
	}

	/** The screen under the crosshair, or null. */
	private static ScreenData lookedAtScreen(Minecraft mc) {
		if (mc.level == null || !(mc.hitResult instanceof net.minecraft.world.phys.BlockHitResult hit)
				|| hit.getType() != HitResult.Type.BLOCK) return null;
		BlockPos pos = hit.getBlockPos();
		return ClientScreens.findAt(mc.level, pos, hit.getDirection());
	}
}
