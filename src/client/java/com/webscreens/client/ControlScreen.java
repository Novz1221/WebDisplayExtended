package com.webscreens.client;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import org.lwjgl.glfw.GLFW;

/** Shows the page full screen and forwards mouse, scroll and keyboard to it. Esc closes. */
final class ControlScreen extends Screen {
	private final BridgeBrowser browser;
	private int x0, y0, dw, dh;

	ControlScreen(BridgeBrowser browser) {
		super(Component.literal("Web screen"));
		this.browser = browser;
	}

	@Override
	protected void init() {
		browser.setFocus(true);
	}

	@Override
	public boolean isPauseScreen() {
		return false;
	}

	@Override
	public void onClose() {
		browser.setFocus(false);
		super.onClose();
	}

	private void layout() {
		double aspect = browser.reqW / (double) browser.reqH;
		dw = width;
		dh = (int) Math.round(dw / aspect);
		if (dh > height) {
			dh = height;
			dw = (int) Math.round(dh * aspect);
		}
		x0 = (width - dw) / 2;
		y0 = (height - dh) / 2;
	}

	private int bx(double mx) {
		return (int) Math.max(0, Math.min(browser.reqW - 1, (mx - x0) / dw * browser.reqW));
	}

	private int by(double my) {
		return (int) Math.max(0, Math.min(browser.reqH - 1, (my - y0) / dh * browser.reqH));
	}

	@Override
	public void render(GuiGraphics g, int mouseX, int mouseY, float delta) {
		layout();
		g.fill(0, 0, width, height, 0xFF000000);
		ResourceLocation tex = browser.texture();
		if (tex != null) {
			g.blit(RenderPipelines.GUI_TEXTURED, tex, x0, y0, 0f, 0f, dw, dh, browser.reqW, browser.reqH, browser.reqW, browser.reqH);
		} else {
			g.drawCenteredString(font, Component.literal("Waiting for the Android WebView bridge..."), width / 2, height / 2, 0xFFFFFFFF);
		}
	}

	/** GLFW buttons are 0 left, 1 right, 2 middle; the bridge wants 0 left, 1 middle, 2 right. */
	private static int bridgeButton(int glfw) {
		return glfw == 0 ? 0 : (glfw == 1 ? 2 : 1);
	}

	@Override
	public boolean mouseClicked(double mx, double my, int button) {
		browser.mouse(1, bx(mx), by(my), bridgeButton(button));
		return true;
	}

	@Override
	public boolean mouseReleased(double mx, double my, int button) {
		browser.mouse(2, bx(mx), by(my), bridgeButton(button));
		return true;
	}

	@Override
	public void mouseMoved(double mx, double my) {
		browser.mouse(0, bx(mx), by(my), 0);
	}

	@Override
	public boolean mouseDragged(double mx, double my, int button, double dx, double dy) {
		browser.mouse(0, bx(mx), by(my), 0);
		return true;
	}

	@Override
	public boolean mouseScrolled(double mx, double my, double scrollX, double scrollY) {
		browser.wheel(bx(mx), by(my), scrollY);
		return true;
	}

	@Override
	public boolean keyPressed(int key, int scan, int mods) {
		if (key == GLFW.GLFW_KEY_ESCAPE) {
			onClose();
			return true;
		}
		browser.key(0, key, (char) 0, mods);
		return true;
	}

	@Override
	public boolean keyReleased(int key, int scan, int mods) {
		browser.key(1, key, (char) 0, mods);
		return true;
	}

	@Override
	public boolean charTyped(char c, int mods) {
		browser.key(2, 0, c, mods);
		return true;
	}
}
