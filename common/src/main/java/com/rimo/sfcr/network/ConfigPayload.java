package com.rimo.sfcr.network;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import com.rimo.sfcr.SFCReMod;
import com.rimo.sfcr.config.CoreConfig;
import dev.architectury.networking.NetworkManager;
import net.fabricmc.api.EnvType;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import org.jetbrains.annotations.NotNull;

public record ConfigPayload(long seed, String config) implements CustomPacketPayload {
	public static final StreamCodec<FriendlyByteBuf, ConfigPayload> CODEC = CustomPacketPayload.codec(ConfigPayload::write, ConfigPayload::new);
	public static final Type<ConfigPayload> TYPE = new Type<>(Network.CHANNEL_CONFIG);
	private static final Gson gson = new Gson();

	private ConfigPayload(FriendlyByteBuf arg) {
		this(arg.readLong(), arg.readUtf());
	}

	public ConfigPayload(long seed, CoreConfig config) {
		this(seed, gson.toJson(config));
	}

	private void write(FriendlyByteBuf arg) {
		arg.writeLong(seed);
		arg.writeUtf(config);
	}

	public CustomPacketPayload.@NotNull Type<ConfigPayload> type() {
		return TYPE;
	}

	public static void receive(ConfigPayload payload, NetworkManager.PacketContext context) {
		if (!SFCReMod.COMMON_CONFIG.isEnableServerConfig())
			return;
		if (context.getEnv() != EnvType.CLIENT)
			return;

		context.queue(() -> {
			try {
				synchronized (SFCReMod.COMMON_CONFIG) {
					SFCReMod.RUNTIME.seed = payload.seed;
					SFCReMod.COMMON_CONFIG.setCoreConfig(gson.fromJson(payload.config, CoreConfig.class));
				}
			} catch (JsonSyntaxException e) {
				SFCReMod.COMMON_CONFIG.load();
				SFCReMod.COMMON_CONFIG.setEnableServerConfig(false);
				SFCReMod.COMMON_CONFIG.save();
				context.getPlayer().displayClientMessage(Component.translatable("text.sfcr.command.sync_fail"), false);
				return;
			}

			SFCReMod.RENDERER.init();		// Reset renderer.
			SFCReMod.RENDERER.updateConfig(SFCReMod.COMMON_CONFIG);
			if (SFCReMod.COMMON_CONFIG.isEnableDebug())
				context.getPlayer().displayClientMessage(Component.translatable("text.sfcr.command.sync_full_succ"), false);
		});
	}
}
