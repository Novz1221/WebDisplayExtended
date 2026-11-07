package com.webscreens.client;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/** Defaults for screens you create from now on, and how far away screens stay switched on. */
final class DefaultsScreen extends Screen {
	private static final int[] RESOLUTIONS = {64, 128, 192, 256, 384, 512, 768};
	private static final int[] FPS = {5, 10, 15, 20, 30, 45, 60};
	private static final int[] DISTANCE = {16, 24, 32, 48, 64, 96, 128};

	private final Screen parent;
	private EditBox dpiBox;

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

		dpiBox = new EditBox(font, x + 60, y + 48, w - 60, 20, Component.literal("DPI"));
		dpiBox.setMaxLength(3);
		dpiBox.setFilter(t -> t.matches("\\d{0,3}"));
		dpiBox.setValue(String.valueOf(ClientConfig.defaultDpi));
		dpiBox.setHint(Component.literal("96 = normal"));
		addRenderableWidget(dpiBox);

		addRenderableWidget(Button.builder(Component.literal("Screen distance: " + ClientConfig.viewDistance + " blocks"), b -> {
			ClientConfig.viewDistance = next(DISTANCE, ClientConfig.viewDistance);
			b.setMessage(Component.literal("Screen distance: " + ClientConfig.viewDistance + " blocks"));
		}).bounds(x, y + 72, w, 20).build());

		addRenderableWidget(Button.builder(Component.literal("Done"), b -> onClose()).bounds(x, y + 104, w, 20).build());
	}

	@Override
	public void onClose() {
		try {
			ClientConfig.defaultDpi = Math.max(24, Math.min(768, Integer.parseInt(dpiBox.getValue().trim())));
		} catch (NumberFormatException ignored) { }
		ClientConfig.save();
		minecraft.setScreen(parent);
	}

	@Override
	public void render(GuiGraphics g, int mouseX, int mouseY, float delta) {
		super.render(g, mouseX, mouseY, delta);
		int y = Math.max(40, height / 2 - 60);
		g.drawCenteredString(font, title, width / 2, y - 20, 0xFFFFFFFF);
		g.drawString(font, "DPI", width / 2 - 150, y + 54, 0xFFFFFFFF);
		g.drawCenteredString(font, Component.literal("Lower values are lighter on the phone. Existing screens keep their own settings."),
				width / 2, y + 132, 0xFF888888);
	}
}
