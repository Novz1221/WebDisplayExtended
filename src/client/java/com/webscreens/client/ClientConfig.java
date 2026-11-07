package com.webscreens.client;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.webscreens.WebScreensMod;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/** Per-player defaults, config/webscreens-client.json. Edited from the Web Screens menu. */
final class ClientConfig {
	private ClientConfig() { }

	static int defaultPx = 256;     // resolution of new screens (browser pixels per block)
	static int defaultFps = 30;
	static int defaultDpi = 96;
	static int viewDistance = 48;   // screens farther away than this (+16) are switched off

	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

	private static final class Data {
		int defaultPx = 256, defaultFps = 30, defaultDpi = 96, viewDistance = 48;
	}

	private static Path file() {
		return FabricLoader.getInstance().getConfigDir().resolve("webscreens-client.json");
	}

	static void load() {
		try {
			Path f = file();
			if (!Files.isRegularFile(f)) return;
			Data d = GSON.fromJson(Files.readString(f, StandardCharsets.UTF_8), Data.class);
			if (d == null) return;
			defaultPx = Math.max(64, Math.min(768, d.defaultPx));
			defaultFps = Math.max(1, Math.min(60, d.defaultFps));
			defaultDpi = Math.max(24, Math.min(768, d.defaultDpi));
			viewDistance = Math.max(16, Math.min(128, d.viewDistance));
		} catch (Exception e) {
			WebScreensMod.LOGGER.warn("Could not read webscreens-client.json: {}", e.toString());
		}
	}

	static void save() {
		try {
			Data d = new Data();
			d.defaultPx = defaultPx;
			d.defaultFps = defaultFps;
			d.defaultDpi = defaultDpi;
			d.viewDistance = viewDistance;
			Files.writeString(file(), GSON.toJson(d), StandardCharsets.UTF_8);
		} catch (IOException e) {
			WebScreensMod.LOGGER.warn("Could not save webscreens-client.json: {}", e.toString());
		}
	}
}
