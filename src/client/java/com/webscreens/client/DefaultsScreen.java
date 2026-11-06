package com.webscreens.client;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/** Defaults for screens you create from now on, and how far away screens stay switched on. */
final class DefaultsScreen extends Screen {
	private static final int[] RESOLUTIONS = {64, 128, 192, 256, 384, 512, 768};
	private static final int[] FPS = {5, 10, 15, 20, 30, 45, 60};
	private static final int[] DPI = {48, 72, 96, 120, 144, 192, 240, 300};
	private static final int[] DISTANCE = {16, 24, 32, 48, 64, 96, 128};

	private final Screen parent;

	DefaultsScreen(Screen parent) {
		super(Component.literal("Defaults for new screens"));
		this.parent = parent;
	}

	private static int next(int[] values, int current) {
		for (int v : values) {
			if (v > current) return v;
		}
		return values[0];
	}

	@Override
	protected void init() {
		int w = 300, x = width / 2 - w / 2;
		int y = Math.max(40, height / 2 - 60);

		addRenderableWidget(Button.builder(Component.literal("Resolution: " + ClientConfig.defaultPx + " px/block"), b -> {
			ClientConfig.defaultPx = next(RESOLUTIONS, ClientConfig.defaultPx);
			b.setMessage(Component.literal("Resolution: " + ClientConfig.defaultPx + " px/block"));
		}).bounds(x, y, w, 20).build());

		addRenderableWidget(Button.builder(Component.literal("FPS: " + ClientConfig.defaultFps), b -> {
			ClientConfig.defaultFps = next(FPS, ClientConfig.defaultFps);
			b.setMessage(Component.literal("FPS: " + ClientConfig.defaultFps));
		}).bounds(x, y + 24, w, 20).build());

		addRenderableWidget(Button.builder(dpiLabel(), b -> {
			ClientConfig.defaultDpi = next(DPI, ClientConfig.defaultDpi);
			b.setMessage(dpiLabel());
		}).bounds(x, y + 48, w, 20).build());

		addRenderableWidget(Button.builder(Component.literal("Screen distance: " + ClientConfig.viewDistance + " blocks"), b -> {
			ClientConfig.viewDistance = next(DISTANCE, ClientConfig.viewDistance);
			b.setMessage(Component.literal("Screen distance: " + ClientConfig.viewDistance + " blocks"));
		}).bounds(x, y + 72, w, 20).build());

		addRenderableWidget(Button.builder(Component.literal("Done"), b -> onClose()).bounds(x, y + 104, w, 20).build());
	}

	private static Component dpiLabel() {
		return Component.literal("DPI: " + ClientConfig.defaultDpi + " (" + Math.round(ClientConfig.defaultDpi * 100f / 96f) + " %)");
	}

	@Override
	public void onClose() {
		ClientConfig.save();
		minecraft.setScreen(parent);
	}

	@Override
	public void render(GuiGraphics g, int mouseX, int mouseY, float delta) {
		super.render(g, mouseX, mouseY, delta);
		int y = Math.max(40, height / 2 - 60);
		g.drawCenteredString(font, title, width / 2, y - 20, 0xFFFFFFFF);
		g.drawCenteredString(font, Component.literal("Lower values are lighter on the phone. Existing screens keep their own settings."),
				width / 2, y + 132, 0xFF888888);
	}
}
