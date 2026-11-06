package com.webscreens;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class WebScreensMod implements ModInitializer {
	public static final String MOD_ID = "webscreens";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	public static ScreenPlacerItem SCREEN_PLACER;

	public static ResourceLocation id(String path) {
		return ResourceLocation.fromNamespaceAndPath(MOD_ID, path);
	}

	@Override
	public void onInitialize() {
		ResourceKey<Item> key = ResourceKey.create(Registries.ITEM, id("screen_placer"));
		SCREEN_PLACER = Registry.register(BuiltInRegistries.ITEM, key,
				new ScreenPlacerItem(new Item.Properties().setId(key).stacksTo(1)));
		ItemGroupEvents.modifyEntriesEvent(CreativeModeTabs.TOOLS_AND_UTILITIES)
				.register(entries -> entries.accept(SCREEN_PLACER));

		PayloadTypeRegistry.playS2C().register(Payloads.Sync.TYPE, Payloads.Sync.CODEC);
		PayloadTypeRegistry.playC2S().register(Payloads.Edit.TYPE, Payloads.Edit.CODEC);
		PayloadTypeRegistry.playC2S().register(Payloads.Destroy.TYPE, Payloads.Destroy.CODEC);

		ServerLifecycleEvents.SERVER_STARTED.register(ScreenStore::load);
		ServerLifecycleEvents.SERVER_STOPPING.register(server -> ScreenStore.save());
		ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> ScreenStore.sendTo(handler.getPlayer()));

		ServerPlayNetworking.registerGlobalReceiver(Payloads.Edit.TYPE, (payload, context) -> {
			ServerPlayer player = context.player();
			ScreenData in;
			try {
				in = ScreenData.fromJson(payload.json());
			} catch (Exception e) {
				return;
			}
			if (in == null || in.id == null) return;
			ScreenData s = ScreenStore.find(in.id);
			if (s == null || !canEdit(player, s)) return;

			s.copySettingsFrom(in);
			ScreenStore.changed(context.server());
		});

		ServerPlayNetworking.registerGlobalReceiver(Payloads.Destroy.TYPE, (payload, context) -> {
			ServerPlayer player = context.player();
			ScreenData s = ScreenStore.find(payload.id());
			if (s == null || !canEdit(player, s)) return;
			ScreenStore.all().remove(s);
			ScreenStore.changed(context.server());
		});

		LOGGER.info("Web Screens loaded");
	}

	/** Owner or an operator may change or destroy a screen. */
	private static boolean canEdit(ServerPlayer player, ScreenData s) {
		return player.getUUID().toString().equals(s.owner) || player.hasPermissions(2);
	}
}
