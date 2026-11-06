package com.webscreens.client;

import com.webscreens.ScreenData;
import com.webscreens.WebScreensMod;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ServerData;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;

/** Client-only screens for servers without the mod: config/webscreens/<server>.json */
final class LocalScreens {
	private LocalScreens() { }

	private static Path file;

	private static Path fileForCurrentServer() {
		ServerData sd = Minecraft.getInstance().getCurrentServer();
		String key = sd == null || sd.ip == null ? "unknown" : sd.ip;
		key = key.replaceAll("[^A-Za-z0-9._-]", "_");
		return FabricLoader.getInstance().getConfigDir().resolve("webscreens").resolve(key + ".json");
	}

	static void loadForCurrentServer() {
		file = fileForCurrentServer();
		List<ScreenData> list = new ArrayList<>();
		try {
			if (Files.isRegularFile(file)) list = ScreenData.listFromJson(Files.readString(file, StandardCharsets.UTF_8));
		} catch (Exception e) {
			WebScreensMod.LOGGER.warn("Could not read {}: {}", file, e.toString());
		}
		ClientScreens.apply(list);
	}

	static void save(List<ScreenData> list) {
		if (file == null) return;
		try {
			Files.createDirectories(file.getParent());
			Path tmp = file.resolveSibling(file.getFileName() + ".tmp");
			Files.writeString(tmp, ScreenData.listToJson(list), StandardCharsets.UTF_8);
			Files.move(tmp, file, StandardCopyOption.REPLACE_EXISTING);
		} catch (IOException e) {
			WebScreensMod.LOGGER.warn("Could not save {}: {}", file, e.toString());
		}
	}

	static void add(ScreenData s) {
		List<ScreenData> list = new ArrayList<>(ClientScreens.screens);
		list.add(s);
		save(list);
		ClientScreens.apply(list);
	}

	static void update(ScreenData in) {
		List<ScreenData> list = new ArrayList<>(ClientScreens.screens);
		for (ScreenData s : list) {
			if (s.id.equals(in.id)) s.copySettingsFrom(in);
		}
		save(list);
		ClientScreens.apply(list);
	}

	static void remove(String id) {
		List<ScreenData> list = new ArrayList<>(ClientScreens.screens);
		list.removeIf(s -> s.id.equals(id));
		save(list);
		ClientScreens.apply(list);
	}
}
