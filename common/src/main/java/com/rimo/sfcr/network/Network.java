package com.rimo.sfcr.network;

import com.rimo.sfcr.SFCReMod;
import dev.architectury.networking.NetworkManager;
import net.minecraft.resources.ResourceLocation;

public class Network {
	public static final ResourceLocation CHANNEL_CONFIG =  ResourceLocation.fromNamespaceAndPath(SFCReMod.MOD_ID, "config_s2c");
	public static final ResourceLocation CHANNEL_RUNTIME = ResourceLocation.fromNamespaceAndPath(SFCReMod.MOD_ID, "runtime_s2c");
	public static final ResourceLocation PACKET_WEATHER =  ResourceLocation.fromNamespaceAndPath(SFCReMod.MOD_ID, "weather_s2c");

	public static void initServer() {
		NetworkManager.registerS2CPayloadType(ConfigPayload.TYPE, ConfigPayload.CODEC);
		NetworkManager.registerS2CPayloadType(RuntimePayload.TYPE, RuntimePayload.CODEC);
		NetworkManager.registerS2CPayloadType(WeatherPayload.TYPE, WeatherPayload.CODEC);
	}

	public static void initClient() {
		NetworkManager.registerReceiver(NetworkManager.Side.S2C, ConfigPayload.TYPE, ConfigPayload.CODEC, ConfigPayload::receive);
		NetworkManager.registerReceiver(NetworkManager.Side.S2C, RuntimePayload.TYPE, RuntimePayload.CODEC, RuntimePayload::receive);
		NetworkManager.registerReceiver(NetworkManager.Side.S2C, WeatherPayload.TYPE, WeatherPayload.CODEC, WeatherPayload::receive);
	}
}
