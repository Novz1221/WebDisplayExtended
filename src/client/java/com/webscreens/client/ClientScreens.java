package com.webscreens.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.webscreens.ScreenData;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Client-side copy of the screens plus one browser per nearby screen. */
final class ClientScreens {
	private ClientScreens() { }

	private static final double EPS = 0.01; // gap to the wall: smaller values z-fight on mobile GPUs
	private static double createDist() { return ClientConfig.viewDistance; }
	private static double destroyDist() { return ClientConfig.viewDistance + 16; }

	static List<ScreenData> screens = new ArrayList<>();
	private static final Map<String, BridgeBrowser> BROWSERS = new HashMap<>();
	private static int tickCount;

	static void onSync(String json) {
		Minecraft.getInstance().execute(() -> apply(json));
	}

	private static void apply(String json) {
		apply(ScreenData.listFromJson(json));
	}

	/** Replaces the screen list (from the server, or from the local file) and updates the browsers. */
	static void apply(List<ScreenData> list) {
		Set<String> ids = new HashSet<>();
		for (ScreenData s : list) ids.add(s.id);
		BROWSERS.entrySet().removeIf(e -> {
			if (!ids.contains(e.getKey())) {
				e.getValue().close();
				return true;
			}
			return false;
		});
		for (ScreenData s : list) {
			BridgeBrowser b = BROWSERS.get(s.id);
			if (b != null) b.update(s);
		}
		screens = list;
	}

	static void closeAll() {
		for (BridgeBrowser b : BROWSERS.values()) b.close();
		BROWSERS.clear();
		screens = new ArrayList<>();
	}

	/** The closest screen within 8 blocks of the player, or null. */
	static ScreenData nearest() {
		Minecraft mc = Minecraft.getInstance();
		if (mc.player == null || mc.level == null) return null;
		String dim = mc.level.dimension().location().toString();
		Vec3 me = mc.player.position();
		ScreenData best = null;
		double bestD = 8 * 8;
		for (ScreenData s : screens) {
			if (!s.dim.equals(dim)) continue;
			double cx = (s.minX + s.maxX + 1) / 2.0, cy = (s.minY + s.maxY + 1) / 2.0, cz = (s.minZ + s.maxZ + 1) / 2.0;
			double d2 = me.distanceToSqr(cx, cy, cz);
			if (d2 < bestD) {
				bestD = d2;
				best = s;
			}
		}
		return best;
	}

	static BridgeBrowser browserOf(ScreenData s) {
		return BROWSERS.get(s.id);
	}

	/** The screen attached to this block face, if any. */
	static ScreenData findAt(Level level, BlockPos pos, Direction face) {
		String dim = level.dimension().location().toString();
		for (ScreenData s : screens) {
			if (s.dim.equals(dim) && s.dir() == face && s.contains(pos)) return s;
		}
		return null;
	}

	static void tick() {
		Minecraft mc = Minecraft.getInstance();
		if (mc.player == null || mc.level == null) return;
		tickCount++;
		String dim = mc.level.dimension().location().toString();
		Vec3 me = mc.player.position();
		for (ScreenData s : screens) {
			if (!s.dim.equals(dim)) continue;
			double cx = (s.minX + s.maxX + 1) / 2.0, cy = (s.minY + s.maxY + 1) / 2.0, cz = (s.minZ + s.maxZ + 1) / 2.0;
			double d2 = me.distanceToSqr(cx, cy, cz);
			BridgeBrowser b = BROWSERS.get(s.id);
			if (b == null) {
				if (d2 < createDist() * createDist() || s.alwaysActive) BROWSERS.put(s.id, new BridgeBrowser(s));
			} else if (d2 > destroyDist() * destroyDist() && !s.alwaysActive) {
				b.close();
				BROWSERS.remove(s.id);
			} else {
				b.tryCreate();
				if (tickCount % 40 == 0) b.setVolume(s.volume);
			}
		}
	}

	static void render(WorldRenderContext ctx) {
		Minecraft mc = Minecraft.getInstance();
		if (mc.level == null) return;
		PoseStack stack = ctx.matrixStack();
		MultiBufferSource consumers = ctx.consumers();
		Camera camera = ctx.camera();
		if (stack == null || consumers == null || camera == null) return;
		String dim = mc.level.dimension().location().toString();
		Vec3 cam = camera.getPosition();

		for (ScreenData s : screens) {
			if (!s.dim.equals(dim)) continue;
			BridgeBrowser b = BROWSERS.get(s.id);
			if (b == null) continue;
			boolean visible = isVisible(s, cam, camera);
			b.setActive(visible);
			if (!visible) continue; // not in front of the camera: no texture upload, no drawing
			ResourceLocation tex = b.texture();
			if (tex == null) continue;

			RenderType type = RenderType.entityCutoutNoCull(tex);
			VertexConsumer vc = consumers.getBuffer(type);
			stack.pushPose();
			stack.translate(-cam.x, -cam.y, -cam.z);
			drawQuad(stack, vc, s, b.reqW / (double) b.reqH);
			stack.popPose();
			if (consumers instanceof MultiBufferSource.BufferSource bs) bs.endBatch(type);
		}
	}

	/** Cheap visibility test: in front of the camera (allowing for the screen's size) and on the open side of the wall. */
	private static boolean isVisible(ScreenData s, Vec3 cam, Camera camera) {
		double cx = (s.minX + s.maxX + 1) / 2.0, cy = (s.minY + s.maxY + 1) / 2.0, cz = (s.minZ + s.maxZ + 1) / 2.0;
		double dx = cx - cam.x, dy = cy - cam.y, dz = cz - cam.z;
		double radius = 0.5 * Math.sqrt(s.blocksW() * (double) s.blocksW() + s.blocksH() * (double) s.blocksH()) + 1;
		org.joml.Vector3f look = camera.getLookVector();
		double along = dx * look.x() + dy * look.y() + dz * look.z();
		if (along < -radius) return false;
		if (dx * dx + dy * dy + dz * dz > destroyDist() * destroyDist()) return false;
		// Behind the wall the screen is hidden by the wall itself.
		Direction d = s.dir();
		double plane = d.getAxis() == Direction.Axis.Z
				? (d.getStepZ() > 0 ? s.maxZ + 1 : s.minZ) : (d.getStepX() > 0 ? s.maxX + 1 : s.minX);
		double camSide = d.getAxis() == Direction.Axis.Z ? (cam.z - plane) * d.getStepZ() : (cam.x - plane) * d.getStepX();
		return camSide > -0.5;
	}

	private static void drawQuad(PoseStack stack, VertexConsumer vc, ScreenData s, double browserAspect) {
		Direction d = s.dir();
		boolean zFace = d.getAxis() == Direction.Axis.Z;
		// The viewer's right-hand side, looking at the face from outside, is the counter-clockwise neighbour.
		Direction r = d.getCounterClockWise();
		boolean rPositive = r.getAxisDirection() == Direction.AxisDirection.POSITIVE;

		double a0 = zFace ? s.minX : s.minZ;
		double a1 = zFace ? s.maxX + 1 : s.maxZ + 1;
		double left = rPositive ? a0 : a1;
		double right = rPositive ? a1 : a0;
		double bottom = s.minY;
		double top = s.maxY + 1;

		if (s.keepAspect) {
			double midS = (left + right) / 2, midT = (bottom + top) / 2;
			double hw = (right - left) / 2, hh = (top - bottom) / 2;
			double sw = Math.abs(right - left), sh = top - bottom;
			if (sw / sh > browserAspect) hw = Math.signum(hw) * sh * browserAspect / 2;
			else hh = sw / browserAspect / 2;
			left = midS - hw;
			right = midS + hw;
			bottom = midT - hh;
			top = midT + hh;
		}

		double plane;
		if (zFace) plane = d.getStepZ() > 0 ? s.maxZ + 1 + EPS : s.minZ - EPS;
		else plane = d.getStepX() > 0 ? s.maxX + 1 + EPS : s.minX - EPS;

		PoseStack.Pose pose = stack.last();
		float nx = d.getStepX(), nz = d.getStepZ();
		vertex(vc, pose, zFace, left, top, plane, 0f, 0f, nx, nz);
		vertex(vc, pose, zFace, left, bottom, plane, 0f, 1f, nx, nz);
		vertex(vc, pose, zFace, right, bottom, plane, 1f, 1f, nx, nz);
		vertex(vc, pose, zFace, right, top, plane, 1f, 0f, nx, nz);
	}

	private static void vertex(VertexConsumer vc, PoseStack.Pose pose, boolean zFace, double along, double y, double plane,
							   float u, float v, float nx, float nz) {
		double x = zFace ? along : plane;
		double z = zFace ? plane : along;
		vc.addVertex(pose, (float) x, (float) y, (float) z)
				.setColor(255, 255, 255, 255)
				.setUv(u, v)
				.setOverlay(OverlayTexture.NO_OVERLAY)
				.setLight(LightTexture.FULL_BRIGHT)
				.setNormal(pose, nx, 0f, nz);
	}
}
