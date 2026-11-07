package com.webscreens;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;

import java.util.ArrayList;
import java.util.List;

/** One screen on a wall. Plain fields so Gson can save it to disk and send it to clients. */
public class ScreenData {
	public String id;
	public String dim;
	public int minX, minY, minZ, maxX, maxY, maxZ; // inclusive range of the wall blocks it covers
	public String face = "north";                   // side of the blocks it is attached to
	public String owner = "";
	public String ownerName = "";

	// settings
	public String url = "https://www.google.com/";
	public boolean keepAspect = false;  // false = stretched
	public boolean alwaysActive = false;
	public int volume = 50;             // 0-100
	public boolean adBlock = true;
	public int pxPerBlock = 256;        // resolution: browser pixels per block
	public int fps = 30;
	public int dpi = 96;                // page zoom, 96 = 100 %

	private static final Gson GSON = new Gson();

	public Direction dir() {
		Direction d = Direction.byName(face);
		return d == null ? Direction.NORTH : d;
	}

	public boolean contains(BlockPos p) {
		return p.getX() >= minX && p.getX() <= maxX && p.getY() >= minY && p.getY() <= maxY
				&& p.getZ() >= minZ && p.getZ() <= maxZ;
	}

	public int blocksW() {
		return dir().getAxis() == Direction.Axis.Z ? maxX - minX + 1 : maxZ - minZ + 1;
	}

	public int blocksH() {
		return maxY - minY + 1;
	}

	/** Copies the user-editable settings from another instance, clamping every value. */
	public void copySettingsFrom(ScreenData in) {
		String u = in.url == null ? "" : in.url.trim();
		if (u.isEmpty()) u = "about:blank";
		else if (!u.matches("(?i)^[a-z][a-z0-9+.-]*:.*")) u = "https://" + u;
		if (u.length() > 2000) u = u.substring(0, 2000);
		url = u;
		keepAspect = in.keepAspect;
		alwaysActive = in.alwaysActive;
		volume = Math.max(0, Math.min(100, in.volume));
		adBlock = in.adBlock;
		pxPerBlock = Math.max(64, Math.min(768, in.pxPerBlock));
		fps = Math.max(1, Math.min(60, in.fps));
		dpi = Math.max(24, Math.min(768, in.dpi));
	}

	public String toJson() {
		return GSON.toJson(this);
	}

	public static ScreenData fromJson(String json) {
		return GSON.fromJson(json, ScreenData.class);
	}

	public static String listToJson(List<ScreenData> list) {
		return GSON.toJson(list);
	}

	public static List<ScreenData> listFromJson(String json) {
		List<ScreenData> l = GSON.fromJson(json, new TypeToken<List<ScreenData>>() { }.getType());
		return l == null ? new ArrayList<>() : l;
	}
}
