package com.webscreens;

import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.storage.LevelResource;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Server-side list of screens, saved to webscreens.json in the world folder. */
public final class ScreenStore {
	private ScreenStore() { }

	public static final int MAX_SIZE = 16;          // blocks per side
	public static final int MAX_PER_PLAYER = 8;

	public record Pending(String dim, BlockPos pos, Direction face) { }

	private static final List<ScreenData> SCREENS = new ArrayList<>();
	private static final Map<UUID, Pending> PENDING = new HashMap<>();
	private static Path file;

	public static List<ScreenData> all() {
		return SCREENS;
	}

	public static Map<UUID, Pending> pending() {
		return PENDING;
	}

	public static ScreenData find(String id) {
		for (ScreenData s : SCREENS) {
			if (s.id.equals(id)) return s;
		}
		return null;
	}

	public static void load(MinecraftServer server) {
		SCREENS.clear();
		PENDING.clear();
		file = server.getWorldPath(LevelResource.ROOT).resolve("webscreens.json");
		try {
			if (Files.isRegularFile(file)) {
				SCREENS.addAll(ScreenData.listFromJson(Files.readString(file, StandardCharsets.UTF_8)));
			}
		} catch (Exception e) {
			WebScreensMod.LOGGER.warn("Could not read {}: {}", file, e.toString());
		}
	}

	public static void save() {
		if (file == null) return;
		try {
			Path tmp = file.resolveSibling("webscreens.json.tmp");
			Files.writeString(tmp, ScreenData.listToJson(SCREENS), StandardCharsets.UTF_8);
			Files.move(tmp, file, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
		} catch (IOException e) {
			WebScreensMod.LOGGER.warn("Could not save {}: {}", file, e.toString());
		}
	}

	public static void sendTo(ServerPlayer player) {
		ServerPlayNetworking.send(player, new Payloads.Sync(ScreenData.listToJson(SCREENS)));
	}

	/** Saves and pushes the new list to every player. */
	public static void changed(MinecraftServer server) {
		save();
		for (ServerPlayer p : server.getPlayerList().getPlayers()) sendTo(p);
	}
}
