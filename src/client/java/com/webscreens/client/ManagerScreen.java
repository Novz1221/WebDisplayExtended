package com.webscreens.client;

import com.webscreens.ScreenData;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/** Main Web Screens menu (keybind or Mod Menu): every screen, each one's settings, and the defaults. */
final class ManagerScreen extends Screen {
	private static final int PER_PAGE = 5;

	private final Screen parent;
	private int page;
	private final List<ScreenData> list = new ArrayList<>();

	ManagerScreen(Screen parent) {
		super(Component.literal("Web Screens"));
		this.parent = parent;
	}

	@Override
	protected void init() {
		list.clear();
		list.addAll(ClientScreens.screens);
		Minecraft mc = Minecraft.getInstance();
		if (mc.player != null) {
			Vec3 me = mc.player.position();
			list.sort(Comparator.comparingDouble(s ->
					me.distanceToSqr((s.minX + s.maxX + 1) / 2.0, (s.minY + s.maxY + 1) / 2.0, (s.minZ + s.maxZ + 1) / 2.0)));
		}
		int pages = Math.max(1, (list.size() + PER_PAGE - 1) / PER_PAGE);
		page = Math.max(0, Math.min(page, pages - 1));

		int w = 300, x = width / 2 - w / 2;
		int y = Math.max(34, height / 2 - 96);
		for (int i = 0; i < PER_PAGE; i++) {
			int idx = page * PER_PAGE + i;
			if (idx >= list.size()) break;
			ScreenData s = list.get(idx);
			String url = s.url.length() > 28 ? s.url.substring(0, 28) + "..." : s.url;
			addRenderableWidget(Button.builder(
					Component.literal((idx + 1) + ". " + s.blocksW() + "x" + s.blocksH() + "  " + url),
					b -> minecraft.setScreen(new SettingsScreen(s, this))
			).bounds(x, y + i * 24, w, 20).build());
		}

		int ny = y + PER_PAGE * 24 + 4;
		Button prev = Button.builder(Component.literal("<"), b -> { page--; rebuildWidgets(); }).bounds(x, ny, 40, 20).build();
		Button next = Button.builder(Component.literal(">"), b -> { page++; rebuildWidgets(); }).bounds(x + w - 40, ny, 40, 20).build();
		prev.active = page > 0;
		next.active = page < pages - 1;
		addRenderableWidget(prev);
		addRenderableWidget(next);

		addRenderableWidget(Button.builder(Component.literal("Defaults for new screens..."),
				b -> minecraft.setScreen(new DefaultsScreen(this))).bounds(x, ny + 26, w, 20).build());
		addRenderableWidget(Button.builder(Component.literal("Done"), b -> onClose()).bounds(x, ny + 50, w, 20).build());
	}

	@Override
	public void onClose() {
		minecraft.setScreen(parent);
	}

	@Override
	public void render(GuiGraphics g, int mouseX, int mouseY, float delta) {
		super.render(g, mouseX, mouseY, delta);
		int top = Math.max(34, height / 2 - 96);
		g.drawCenteredString(font, title, width / 2, top - 20, 0xFFFFFFFF);
		if (list.isEmpty()) {
			g.drawCenteredString(font, Component.literal("No screens yet. Use the Screen Placer item or /webscreens place."),
					width / 2, top + 40, 0xFFAAAAAA);
		} else {
			int pages = Math.max(1, (list.size() + PER_PAGE - 1) / PER_PAGE);
			g.drawCenteredString(font, Component.literal("Page " + (page + 1) + " / " + pages + "  (closest first)"),
					width / 2, top + PER_PAGE * 24 + 10, 0xFFAAAAAA);
		}
	}
}
