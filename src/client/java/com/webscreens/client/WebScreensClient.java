package com.webscreens.client;

import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.brigadier.Command;
import com.webscreens.Payloads;
import com.webscreens.ScreenData;
import com.webscreens.WebScreensMod;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.phys.HitResult;
import org.lwjgl.glfw.GLFW;

import java.util.function.Supplier;

public class WebScreensClient implements ClientModInitializer {
	/** Set by the command; opened from the tick, after the chat screen has closed itself. */
	private static volatile Supplier<Screen> pendingOpen;

	private static KeyMapping menuKey;
	private static KeyMapping settingsKey;

	@Override
	public void onInitializeClient() {
		ClientPlayNetworking.registerGlobalReceiver(Payloads.Sync.TYPE,
				(payload, context) -> ClientScreens.onSync(payload.json()));

		menuKey = KeyBindingHelper.registerKeyBinding(new KeyMapping("key.webscreens.menu",
				InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_K, "key.categories.webscreens"));
		settingsKey = KeyBindingHelper.registerKeyBinding(new KeyMapping("key.webscreens.settings",
				InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_UNKNOWN, "key.categories.webscreens"));
		ClientConfig.load();

		ClientTickEvents.END_CLIENT_TICK.register(client -> {
			ClientScreens.tick();
			while (menuKey.consumeClick()) {
				if (client.screen == null) client.setScreen(new ManagerScreen(null));
			}
			while (settingsKey.consumeClick()) {
				if (client.screen != null) continue;
				ScreenData target = lookedAtScreen(client);
				if (target == null) target = ClientScreens.nearest();
				if (target != null) client.setScreen(new SettingsScreen(target));
			}
			Supplier<Screen> open = pendingOpen;
			if (open != null && client.screen == null) {
				pendingOpen = null;
				client.setScreen(open.get());
			}
		});
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
						.then(ClientCommandManager.literal("menu").executes(ctx -> {
							pendingOpen = () -> new ManagerScreen(null);
							return Command.SINGLE_SUCCESS;
						}))
						.then(ClientCommandManager.literal("settings").executes(ctx -> {
							Minecraft mc = Minecraft.getInstance();
							ScreenData s = lookedAtScreen(mc);
							if (s == null) s = ClientScreens.nearest();
							if (s == null) {
								ctx.getSource().sendFeedback(Component.literal("No screen nearby. Look at a screen first."));
							} else {
								final ScreenData target = s;
								pendingOpen = () -> new SettingsScreen(target); // the chat closes itself right after the command
							}
							return Command.SINGLE_SUCCESS;
						}))
						.executes(ctx -> {
							ctx.getSource().sendFeedback(Component.literal("/webscreens place - create a screen (servers without the mod)"));
							ctx.getSource().sendFeedback(Component.literal("/webscreens settings - settings of the screen you are looking at"));
							ctx.getSource().sendFeedback(Component.literal("/webscreens menu - all screens and defaults (also the K key, or Mod Menu > Web Screens)"));
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
