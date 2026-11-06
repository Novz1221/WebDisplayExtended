package com.webscreens;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
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

		LOGGER.info("Web Screens loaded (milestone 0)");
	}
}
