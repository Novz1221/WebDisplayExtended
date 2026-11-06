package com.webscreens.client;

import com.webscreens.Payloads;
import com.webscreens.ScreenData;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;

/**
 * Edits go to the server when it has this mod installed (shared with every player), otherwise they are
 * kept only on this client (works on any server, but only you see the screens).
 */
final class ScreenActions {
	private ScreenActions() { }

	/** True when the server we joined has the mod. Set when joining. */
	static boolean serverMode;

	static void apply(ScreenData s) {
		if (serverMode) ClientPlayNetworking.send(new Payloads.Edit(s.toJson()));
		else LocalScreens.update(s);
	}

	static void destroy(String id) {
		if (serverMode) ClientPlayNetworking.send(new Payloads.Destroy(id));
		else LocalScreens.remove(id);
	}
}
