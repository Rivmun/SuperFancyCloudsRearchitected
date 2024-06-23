package com.rimo.sfcr.network;

import com.rimo.sfcr.SFCReMod;
import com.rimo.sfcr.util.WeatherType;
import dev.architectury.networking.NetworkManager;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.MinecraftServer;
import org.jetbrains.annotations.NotNull;

public record WeatherPayload(WeatherType weather) implements CustomPacketPayload {
	public static final StreamCodec<FriendlyByteBuf, WeatherPayload> CODEC = CustomPacketPayload.codec(WeatherPayload::write, WeatherPayload::new);
	public static final Type<WeatherPayload> TYPE = new Type<>(Network.PACKET_WEATHER);

	private WeatherPayload(FriendlyByteBuf arg) {
		this(arg.readEnum(WeatherType.class));
	}

	private void write(FriendlyByteBuf arg) {
		arg.writeEnum(weather);
	}

	public CustomPacketPayload.@NotNull Type<WeatherPayload> type() {
		return TYPE;
	}

	public static void send(MinecraftServer server) {
		server.getPlayerList().getPlayers().forEach(player -> NetworkManager.sendToPlayer(
				player,
				new WeatherPayload(SFCReMod.RUNTIME.nextWeather)
		));
	}

	public static void receive(WeatherPayload payload, NetworkManager.PacketContext context) {
		synchronized (SFCReMod.RUNTIME) {
			SFCReMod.RUNTIME.nextWeather = payload.weather;
		}
	}
}
