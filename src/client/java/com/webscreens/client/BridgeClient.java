package com.webscreens.client;

import com.webscreens.WebScreensMod;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Talks to the Android WebView bridge hosted by the launcher (webbridge.dex) over a loopback socket.
 * Port and token are in java.io.tmpdir/mojo_webbridge.txt.
 */
final class BridgeClient {
	private BridgeClient() { }

	static final int MAGIC = 0x4D4A5742;

	interface Writer {
		void write(DataOutputStream o) throws IOException;
	}

	private static final Map<Integer, BridgeBrowser> BROWSERS = new ConcurrentHashMap<>();
	private static final ExecutorService SENDER = Executors.newSingleThreadExecutor(r -> {
		Thread t = new Thread(r, "WebScreens-Send");
		t.setDaemon(true);
		return t;
	});
	private static final Object LOCK = new Object();
	private static final Object SEND_LOCK = new Object();
	private static volatile DataOutputStream out;
	private static volatile boolean connected;
	private static long lastTry;

	static void register(int id, BridgeBrowser b) {
		BROWSERS.put(id, b);
	}

	static void unregister(int id) {
		BROWSERS.remove(id);
	}

	static boolean isConnected() {
		return connected;
	}

	static boolean ensureConnected() {
		if (connected) return true;
		synchronized (LOCK) {
			if (connected) return true;
			long now = System.currentTimeMillis();
			if (now - lastTry < 2000) return false;
			lastTry = now;
			try {
				Path info = Path.of(System.getProperty("java.io.tmpdir"), "mojo_webbridge.txt");
				if (!Files.isRegularFile(info)) return false;
				List<String> lines = Files.readAllLines(info);
				int port = Integer.parseInt(lines.get(0).trim());
				String token = lines.get(1).trim();

				Socket s = new Socket();
				s.connect(new InetSocketAddress("127.0.0.1", port), 3000);
				s.setTcpNoDelay(true);
				DataOutputStream o = new DataOutputStream(new BufferedOutputStream(s.getOutputStream(), 65536));
				DataInputStream in = new DataInputStream(new BufferedInputStream(s.getInputStream(), 65536));
				o.writeInt(MAGIC);
				writeStr(o, token);
				o.flush();
				s.setSoTimeout(5000);
				if (in.readInt() != MAGIC) throw new IOException("bad handshake");
				s.setSoTimeout(0);
				out = o;
				connected = true;
				Thread t = new Thread(() -> readLoop(in), "WebScreens-Recv");
				t.setDaemon(true);
				t.start();
				WebScreensMod.LOGGER.info("Connected to the Android WebView bridge on port {}", port);
				return true;
			} catch (Throwable e) {
				WebScreensMod.LOGGER.warn("Bridge connect failed: {}", e.toString());
				return false;
			}
		}
	}

	static void writeStr(DataOutputStream o, String s) throws IOException {
		byte[] b = (s == null ? "" : s).getBytes(StandardCharsets.UTF_8);
		o.writeInt(b.length);
		o.write(b);
	}

	static String readStr(DataInputStream in) throws IOException {
		int n = in.readInt();
		if (n < 0 || n > 64 * 1024 * 1024) throw new IOException("bad string length " + n);
		byte[] b = new byte[n];
		in.readFully(b);
		return new String(b, StandardCharsets.UTF_8);
	}

	/** Builds the message on the caller thread and writes it on a dedicated thread. */
	static void send(char type, Writer w) {
		final DataOutputStream o = out;
		if (!connected || o == null) return;
		final byte[] data;
		try {
			ByteArrayOutputStream bos = new ByteArrayOutputStream(64);
			DataOutputStream d = new DataOutputStream(bos);
			d.writeByte(type);
			w.write(d);
			d.flush();
			data = bos.toByteArray();
		} catch (IOException e) {
			return;
		}
		SENDER.execute(() -> {
			try {
				synchronized (SEND_LOCK) {
					o.writeInt(data.length);
					o.write(data);
					o.flush();
				}
			} catch (IOException e) {
				onDisconnect(e);
			}
		});
	}

	private static void onDisconnect(Throwable e) {
		if (!connected) return;
		connected = false;
		out = null;
		WebScreensMod.LOGGER.warn("Bridge disconnected: {}", e.toString());
		for (BridgeBrowser b : BROWSERS.values()) b.onDisconnected();
	}

	private static void readLoop(DataInputStream in) {
		try {
			while (true) {
				int len = in.readInt();
				byte[] data = new byte[len];
				in.readFully(data);
				handle(new DataInputStream(new ByteArrayInputStream(data)));
			}
		} catch (IOException e) {
			onDisconnect(e);
		}
	}

	private static void handle(DataInputStream in) throws IOException {
		char type = (char) in.readUnsignedByte();
		if (type == 's') { // custom scheme request: nothing served by this mod, answer 404 so the WebView never waits
			int reqId = in.readInt();
			send('R', o -> {
				o.writeInt(reqId);
				o.writeInt(404);
				writeStr(o, "text/plain");
				o.writeInt(0);
			});
			return;
		}
		int id = in.readInt();
		BridgeBrowser b = BROWSERS.get(id);
		switch (type) {
			case 'f' -> {
				long seq = in.readLong();
				int w = in.readInt();
				int h = in.readInt();
				String path = readStr(in);
				if (b != null) b.onFrame(seq, w, h, path);
			}
			case 'q' -> { // JS cefQuery: not supported, fail it
				long qid = in.readLong();
				send('Q', o -> {
					o.writeInt(id);
					o.writeLong(qid);
					o.writeBoolean(false);
					o.writeInt(1);
					writeStr(o, "unsupported");
				});
			}
			default -> { }
		}
	}
}
