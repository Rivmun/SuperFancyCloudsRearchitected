package com.rimo.sfcr.network;

import com.rimo.sfcr.SFCReMod;
import com.rimo.sfcr.core.Runtime;
import dev.architectury.networking.NetworkManager;
import net.fabricmc.api.EnvType;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import org.jetbrains.annotations.NotNull;

public record RuntimePayload(double time, int fullOffset, double partialOffset) implements CustomPacketPayload {
	public static final StreamCodec<FriendlyByteBuf, RuntimePayload> CODEC = CustomPacketPayload.codec(RuntimePayload::write, RuntimePayload::new);
	public static final Type<RuntimePayload> TYPE = new Type<>(Network.CHANNEL_RUNTIME);

	private RuntimePayload(FriendlyByteBuf arg) {
		this(arg.readDouble(), arg.readInt(), arg.readDouble());
	}

	public RuntimePayload(Runtime runtime) {
		this(runtime.time, runtime.fullOffset, runtime.partialOffset);
	}

	private void write(FriendlyByteBuf arg) {
		arg.writeDouble(time);
		arg.writeInt(fullOffset);
		arg.writeDouble(partialOffset);
	}

	public CustomPacketPayload.@NotNull Type<RuntimePayload> type() {
		return TYPE;
	}

	public static void receive(RuntimePayload payload, NetworkManager.PacketContext context) {
		if (! SFCReMod.COMMON_CONFIG.isEnableServerConfig())
			return;
		if (context.getEnv() != EnvType.CLIENT)
			return;

		context.queue(() -> {
			synchronized (SFCReMod.RUNTIME) {
				try {
					SFCReMod.RUNTIME.time = payload.time;
					SFCReMod.RUNTIME.fullOffset = payload.fullOffset;
					SFCReMod.RUNTIME.partialOffset = payload.partialOffset;
				} catch (Exception e) {
					SFCReMod.COMMON_CONFIG.setEnableServerConfig(false);
					SFCReMod.COMMON_CONFIG.save();
					context.getPlayer().displayClientMessage(Component.translatable("text.sfcr.command.sync_fail"), false);
					return;
				}
			}
			if (SFCReMod.COMMON_CONFIG.isEnableDebug())
				context.getPlayer().displayClientMessage(Component.translatable("text.sfcr.command.sync_succ"), false);
		});
	}
}
