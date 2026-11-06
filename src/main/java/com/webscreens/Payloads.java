package com.webscreens;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/** Network messages. Everything is a JSON string to keep the codecs trivial. */
public final class Payloads {
	private Payloads() { }

	/** Server -> client: the complete list of screens. */
	public record Sync(String json) implements CustomPacketPayload {
		public static final Type<Sync> TYPE = new Type<>(WebScreensMod.id("sync"));
		public static final StreamCodec<RegistryFriendlyByteBuf, Sync> CODEC =
				StreamCodec.composite(ByteBufCodecs.stringUtf8(1 << 20), Sync::json, Sync::new);

		@Override
		public Type<? extends CustomPacketPayload> type() {
			return TYPE;
		}
	}

	/** Client -> server: new settings for one screen (a ScreenData as JSON). */
	public record Edit(String json) implements CustomPacketPayload {
		public static final Type<Edit> TYPE = new Type<>(WebScreensMod.id("edit"));
		public static final StreamCodec<RegistryFriendlyByteBuf, Edit> CODEC =
				StreamCodec.composite(ByteBufCodecs.stringUtf8(1 << 16), Edit::json, Edit::new);

		@Override
		public Type<? extends CustomPacketPayload> type() {
			return TYPE;
		}
	}

	/** Client -> server: destroy the screen with this id. */
	public record Destroy(String id) implements CustomPacketPayload {
		public static final Type<Destroy> TYPE = new Type<>(WebScreensMod.id("destroy"));
		public static final StreamCodec<RegistryFriendlyByteBuf, Destroy> CODEC =
				StreamCodec.composite(ByteBufCodecs.stringUtf8(64), Destroy::id, Destroy::new);

		@Override
		public Type<? extends CustomPacketPayload> type() {
			return TYPE;
		}
	}
}
