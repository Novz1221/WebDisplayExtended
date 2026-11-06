package com.webscreens.client;

import com.mojang.blaze3d.platform.NativeImage;
import com.webscreens.ScreenData;
import com.webscreens.WebScreensMod;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.ResourceLocation;
import org.lwjgl.system.MemoryUtil;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.util.concurrent.atomic.AtomicInteger;

/** One Android WebView driven through the bridge, shown as a dynamic texture. */
final class BridgeBrowser {
	private static final AtomicInteger NEXT_ID = new AtomicInteger(1);
	private static final int HEADER = 16;

	final int id = NEXT_ID.getAndIncrement();

	private String url;
	int reqW, reqH;
	private int fps, dpi;
	private boolean created;
	private boolean closed;

	private volatile long frameSeq;
	private volatile int frameW, frameH;
	private volatile String framePath;

	private long uploadedSeq = -1;
	private String mappedPath;
	private MappedByteBuffer map;
	private DynamicTexture tex;
	private ResourceLocation texId;
	private int texW, texH, generation;

	BridgeBrowser(ScreenData s) {
		url = s.url;
		reqW = Math.max(64, s.blocksW() * s.pxPerBlock);
		reqH = Math.max(64, s.blocksH() * s.pxPerBlock);
		fps = s.fps;
		dpi = s.dpi;
		BridgeClient.register(id, this);
		tryCreate();
	}

	/** Creates the WebView on the launcher side once the bridge is reachable. */
	void tryCreate() {
		if (created || closed || !BridgeClient.ensureConnected()) return;
		created = true;
		final String u = url;
		final int w = reqW, h = reqH;
		BridgeClient.send('C', o -> {
			o.writeInt(id);
			BridgeClient.writeStr(o, u);
			o.writeBoolean(false);
			o.writeInt(w);
			o.writeInt(h);
		});
		sendQuality();
		sendDpi();
	}

	void onDisconnected() {
		created = false;
		framePath = null;
		uploadedSeq = -1;
	}

	private void sendQuality() {
		final int f = fps;
		BridgeClient.send('P', o -> {
			o.writeInt(id);
			o.writeInt(f);
			o.writeInt(100);
			o.writeInt(0);
			o.writeInt(0);
		});
	}

	private void sendDpi() {
		final int pct = Math.round(dpi * 100f / 96f);
		BridgeClient.send('D', o -> {
			o.writeInt(id);
			o.writeInt(pct);
		});
	}

	/** Applies new settings from the server. */
	void update(ScreenData s) {
		int w = Math.max(64, s.blocksW() * s.pxPerBlock);
		int h = Math.max(64, s.blocksH() * s.pxPerBlock);
		if (!s.url.equals(url)) {
			url = s.url;
			final String u = url;
			BridgeClient.send('U', o -> {
				o.writeInt(id);
				BridgeClient.writeStr(o, u);
			});
		}
		if (w != reqW || h != reqH) {
			reqW = w;
			reqH = h;
			BridgeClient.send('S', o -> {
				o.writeInt(id);
				o.writeInt(w);
				o.writeInt(h);
			});
		}
		if (s.fps != fps) {
			fps = s.fps;
			sendQuality();
		}
		if (s.dpi != dpi) {
			dpi = s.dpi;
			sendDpi();
		}
	}

	void setVolume(int volume) {
		if (!created) return;
		final String js = "document.querySelectorAll('video,audio').forEach(function(e){e.volume=" + (volume / 100.0) + "});";
		BridgeClient.send('J', o -> {
			o.writeInt(id);
			BridgeClient.writeStr(o, js);
		});
	}

	void onFrame(long seq, int w, int h, String path) {
		frameW = w;
		frameH = h;
		framePath = path;
		frameSeq = seq;
	}

	/** Texture with the latest frame (render thread only), or null until the first frame arrived. */
	ResourceLocation texture() {
		if (closed) return null;
		String path = framePath;
		int fw = frameW, fh = frameH;
		long seq = frameSeq;
		if (path == null || fw <= 0 || fh <= 0) return tex == null ? null : texId;
		if (seq == uploadedSeq && tex != null) return texId;
		try {
			if (map == null || !path.equals(mappedPath)) {
				try (RandomAccessFile raf = new RandomAccessFile(path, "r")) {
					map = raf.getChannel().map(FileChannel.MapMode.READ_ONLY, 0, raf.length());
				}
				mappedPath = path;
			}
			long bytes = (long) fw * fh * 4;
			if (HEADER + bytes > map.capacity()) return tex == null ? null : texId;

			if (tex == null || texW != fw || texH != fh) {
				releaseTexture();
				NativeImage image = new NativeImage(NativeImage.Format.RGBA, fw, fh, false);
				final int gen = ++generation;
				tex = new DynamicTexture(() -> "webscreens/" + id + "/" + gen, image);
				texId = WebScreensMod.id("screen/" + id + "/" + gen);
				Minecraft.getInstance().getTextureManager().register(texId, tex);
				texW = fw;
				texH = fh;
			}
			NativeImage img = tex.getPixels();
			if (img != null) {
				// The bridge writes RGBA bytes, which is exactly NativeImage's RGBA layout: one straight copy.
				ByteBuffer src = map;
				MemoryUtil.memCopy(MemoryUtil.memAddress(src, HEADER), img.getPointer(), bytes);
				tex.upload();
				uploadedSeq = seq;
			}
		} catch (IOException | RuntimeException e) {
			WebScreensMod.LOGGER.warn("Frame upload failed: {}", e.toString());
		}
		return tex == null ? null : texId;
	}

	private void releaseTexture() {
		if (texId != null) Minecraft.getInstance().getTextureManager().release(texId);
		tex = null;
		texId = null;
		uploadedSeq = -1;
	}

	// -------------------------------------------------------------------------------- input (bridge coordinates)

	void setFocus(boolean focus) {
		if (!created) return;
		BridgeClient.send('F', o -> {
			o.writeInt(id);
			o.writeBoolean(focus);
		});
	}

	/** kind: 0 move, 1 press, 2 release; button: 0 left, 1 middle, 2 right. */
	void mouse(int kind, int x, int y, int button) {
		if (!created) return;
		BridgeClient.send('M', o -> {
			o.writeInt(id);
			o.writeInt(kind);
			o.writeInt(x);
			o.writeInt(y);
			o.writeInt(button);
			o.writeInt(kind == 0 ? 0 : 1);
			o.writeInt(0);
		});
	}

	void wheel(int x, int y, double delta) {
		if (!created) return;
		BridgeClient.send('W', o -> {
			o.writeInt(id);
			o.writeInt(x);
			o.writeInt(y);
			o.writeDouble(delta);
			o.writeInt(0);
		});
	}

	/** kind: 0 key down, 1 key up, 2 typed character. keyCode is a GLFW key code. */
	void key(int kind, int keyCode, char ch, int mods) {
		if (!created) return;
		BridgeClient.send('K', o -> {
			o.writeInt(id);
			o.writeInt(kind);
			o.writeInt(keyCode);
			o.writeLong(0L);
			o.writeChar(ch);
			o.writeInt(mods);
		});
	}

	void goBack() {
		if (!created) return;
		BridgeClient.send('N', o -> {
			o.writeInt(id);
			o.writeInt(0);
		});
	}

	void close() {
		if (closed) return;
		closed = true;
		if (created) {
			BridgeClient.send('X', o -> o.writeInt(id));
		}
		BridgeClient.unregister(id);
		releaseTexture();
		map = null;
	}
}
