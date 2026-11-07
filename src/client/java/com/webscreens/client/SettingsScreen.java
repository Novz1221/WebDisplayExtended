package com.webscreens.client;

import com.webscreens.ScreenData;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/** Screen settings: address, aspect ratio, always active, volume, ad blocker, resolution, FPS, DPI. */
final class SettingsScreen extends Screen {
	private static final int[] RESOLUTIONS = {64, 128, 192, 256, 384, 512, 768};
	private static final int[] FPS = {5, 10, 15, 20, 30, 45, 60};

	private final ScreenData data; // working copy
	private EditBox urlBox;
	private VolumeSlider volume;
	private EditBox dpiBox;
	private int dpiRowY;

	private final Screen parent;

	SettingsScreen(ScreenData original) {
		this(original, null);
	}

	SettingsScreen(ScreenData original, Screen parent) {
		super(Component.literal("Screen settings"));
		this.data = ScreenData.fromJson(original.toJson());
		this.parent = parent;
	}

	@Override
	public void onClose() {
		minecraft.setScreen(parent);
	}

	private static int next(int[] values, int current) {
		for (int i = 0; i < values.length; i++) {
			if (values[i] > current) return values[i];
		}
		return values[0];
	}

	@Override
	protected void init() {
		int w = 300, half = 148, x = width / 2 - w / 2, xr = x + w - half;
		int y = Math.max(20, height / 2 - 100);

		urlBox = new EditBox(font, x, y + 12, w, 20, Component.literal("Address (URL)"));
		urlBox.setMaxLength(2000);
		urlBox.setValue(data.url);
		addRenderableWidget(urlBox);

		y += 40;
		addRenderableWidget(Button.builder(aspectLabel(), b -> {
			data.keepAspect = !data.keepAspect;
			b.setMessage(aspectLabel());
		}).bounds(x, y, half, 20).build());
		addRenderableWidget(Button.builder(activeLabel(), b -> {
			data.alwaysActive = !data.alwaysActive;
			b.setMessage(activeLabel());
		}).bounds(xr, y, half, 20).build());

		y += 24;
		volume = new VolumeSlider(x, y, half, 20, data.volume);
		addRenderableWidget(volume);
		addRenderableWidget(Button.builder(adLabel(), b -> {
			data.adBlock = !data.adBlock;
			b.setMessage(adLabel());
		}).bounds(xr, y, half, 20).build());

		y += 24;
		addRenderableWidget(Button.builder(resLabel(), b -> {
			data.pxPerBlock = next(RESOLUTIONS, data.pxPerBlock);
			b.setMessage(resLabel());
		}).bounds(x, y, half, 20).build());
		addRenderableWidget(Button.builder(fpsLabel(), b -> {
			data.fps = next(FPS, data.fps);
			b.setMessage(fpsLabel());
		}).bounds(xr, y, half, 20).build());

		y += 24;
		dpiRowY = y;
		dpiBox = new EditBox(font, x + 60, y, w - 60, 20, Component.literal("DPI"));
		dpiBox.setMaxLength(3);
		dpiBox.setFilter(t -> t.matches("\\d{0,3}"));
		dpiBox.setValue(String.valueOf(data.dpi));
		dpiBox.setHint(Component.literal("96 = normal"));
		addRenderableWidget(dpiBox);

		y += 32;
		addRenderableWidget(Button.builder(Component.literal("Destroy screen"), b -> {
			ScreenActions.destroy(data.id);
			onClose();
		}).bounds(x, y, w, 20).build());
		y += 24;
		addRenderableWidget(Button.builder(Component.literal("Apply"), b -> {
			data.url = urlBox.getValue();
			data.volume = volume.volume();
			data.dpi = parseDpi(dpiBox.getValue(), data.dpi);
			ScreenActions.apply(data);
			onClose();
		}).bounds(x, y, w, 20).build());
	}

	private Component aspectLabel() {
		return Component.literal("Aspect ratio: " + (data.keepAspect ? "keep" : "stretched"));
	}

	private Component activeLabel() {
		return Component.literal("Always active: " + (data.alwaysActive ? "yes" : "no"));
	}

	private Component adLabel() {
		return Component.literal("Ad blocker: " + (data.adBlock ? "on" : "off"));
	}

	private Component resLabel() {
		return Component.literal("Resolution: " + data.pxPerBlock + " px/block");
	}

	private Component fpsLabel() {
		return Component.literal("FPS: " + data.fps);
	}

	/** Any whole number from 24 to 768; 96 is a normal page, higher makes everything bigger. */
	private static int parseDpi(String text, int fallback) {
		try {
			return Math.max(24, Math.min(768, Integer.parseInt(text.trim())));
		} catch (NumberFormatException e) {
			return fallback;
		}
	}

	@Override
	public void render(GuiGraphics g, int mouseX, int mouseY, float delta) {
		super.render(g, mouseX, mouseY, delta);
		int y = Math.max(20, height / 2 - 100);
		g.drawCenteredString(font, title, width / 2, y - 14, 0xFFFFFFFF);
		g.drawString(font, "Address (URL)", width / 2 - 150, y + 2, 0xFFAAAAAA);
		g.drawString(font, "DPI", width / 2 - 150, dpiRowY + 6, 0xFFFFFFFF);
	}

	private static final class VolumeSlider extends AbstractSliderButton {
		VolumeSlider(int x, int y, int w, int h, int vol) {
			super(x, y, w, h, Component.empty(), vol / 100.0);
			updateMessage();
		}

		int volume() {
			return (int) Math.round(value * 100);
		}

		@Override
		protected void updateMessage() {
			setMessage(Component.literal("Volume: " + volume() + " %"));
		}

		@Override
		protected void applyValue() { }
	}
}
