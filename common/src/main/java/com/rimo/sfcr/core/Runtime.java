package com.rimo.sfcr.core;

import com.rimo.sfcr.SFCReMod;
import com.rimo.sfcr.config.CommonConfig;
import com.rimo.sfcr.mixin.ServerLevelAccessor;
import com.rimo.sfcr.network.*;
import com.rimo.sfcr.util.WeatherType;
import dev.architectury.networking.NetworkManager;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class Runtime {

	private final CommonConfig CONFIG = SFCReMod.COMMON_CONFIG;

	public long seed = new Random().nextLong();
	public double time = 0;
	public int fullOffset = 0;
	public double partialOffset = 0;

	private ResourceKey<Level> worldKey;
	public WeatherType nextWeather = WeatherType.CLEAR;
	private List<ServerPlayerManager> playerList;

	public void init(ServerLevel world) {
		seed = new Random().nextLong();
		worldKey = world.dimension();
		playerList = new ArrayList<>();
	}

	public void tick(MinecraftServer server) {

		if (!CONFIG.isEnableMod())
			return;

		if (server.isDedicatedServer()) {		// These data is updated by RENDERER, but dedicated server is not have, so we update here.
			partialOffset += 1 / 20f * CONFIG.getCloudBlockSize() / 16f;
			checkFullOffset();
			checkPartialOffset();
		}
		time += 1 / 20f;		// 20 tick per second.

		// Weather Pre-detect
		var worldProperties = ((ServerLevelAccessor) server.getLevel(worldKey)).getServerLevelData();
		var currentWeather = nextWeather;
		if (worldProperties.isRaining()) {
			if (worldProperties.isThundering()) {
				nextWeather = worldProperties.getThunderTime() / 20 < CONFIG.getWeatherPreDetectTime() ? WeatherType.RAIN : WeatherType.THUNDER;
			} else {
				nextWeather = worldProperties.getThunderTime() / 20 < CONFIG.getWeatherPreDetectTime() && worldProperties.getThunderTime() != worldProperties.getRainTime()
						? WeatherType.THUNDER
						: worldProperties.getRainTime() / 20 < CONFIG.getWeatherPreDetectTime() ? WeatherType.CLEAR : WeatherType.RAIN;
			}
		} else {
			if (worldProperties.getClearWeatherTime() != 0) {
				nextWeather = worldProperties.getClearWeatherTime() / 20 < CONFIG.getWeatherPreDetectTime() ? WeatherType.RAIN : WeatherType.CLEAR;
			} else {
				nextWeather = Math.min(worldProperties.getRainTime(), worldProperties.getThunderTime()) / 20 < CONFIG.getWeatherPreDetectTime()
						? worldProperties.getRainTime() < worldProperties.getThunderTime() ? WeatherType.RAIN : WeatherType.THUNDER
						: WeatherType.CLEAR;
			}
		}
		if (nextWeather != currentWeather)
			WeatherPayload.send(server);

		// Data Sync
		if (server.getTickCount() % 20 == 0 && (server.isDedicatedServer() || CONFIG.isEnableServerConfig())) {
			playerList.forEach(manager -> {
				if (manager.lastSyncTime < time - CONFIG.getSecPerSync()) {
					NetworkManager.sendToPlayer(manager.player, new RuntimePayload(time, fullOffset, partialOffset));
					manager.lastSyncTime = time;
				}
			});
		}

		if (CONFIG.isEnableDebug() && !server.isDedicatedServer() && server.getTickCount() % (CONFIG.getWeatherPreDetectTime() * 20) == 0) {
			SFCReMod.LOGGER.info("isThnd: " + worldProperties.isThundering() + ", isRain: " + worldProperties.isRaining());
			SFCReMod.LOGGER.info("thndTime: " + worldProperties.getThunderTime() + ", rainTime: " + worldProperties.getRainTime() + ", clearTime: " + worldProperties.getClearWeatherTime());
			SFCReMod.LOGGER.info("nextWeather: " + nextWeather.toString());
		}
	}

	public void clientTick(Level world) {
		// Weather Detect (Client) - Only runs when connected to a server without sync
		if (!Minecraft.getInstance().isLocalServer() && CONFIG.isEnableServerConfig())
			nextWeather = world.isThundering() ? WeatherType.THUNDER : world.isRaining() ? WeatherType.RAIN : WeatherType.CLEAR;
	}

	public void checkFullOffset() {
		fullOffset += (int) partialOffset / CONFIG.getCloudBlockSize();
	}

	public void checkPartialOffset() {
		partialOffset = partialOffset % CONFIG.getCloudBlockSize();
	}

	public List<ServerPlayerManager> getPlayerList() {
		return playerList;
	}

	public void addPlayer(ServerPlayer player) {
		if (playerList.stream().noneMatch(manager -> manager.getPlayer() == player))
			return;
		playerList.add(new ServerPlayerManager(player));
	}

	public void removePlayer(ServerPlayer player) {
		playerList.removeIf(manager -> manager.getPlayer() == player);
	}

	public void end(MinecraftServer server) {
		playerList.clear();
	}

	public void clientEnd() {
		//
	}

	public class ServerPlayerManager {
		private final ServerPlayer player;
		private double lastSyncTime;

		ServerPlayerManager(ServerPlayer player) {
			this.player = player;
			this.lastSyncTime = time;
			if (player.getServer().isDedicatedServer() || CONFIG.isEnableServerConfig()) {
				NetworkManager.sendToPlayer(player, new ConfigPayload(seed, SFCReMod.COMMON_CONFIG));
				NetworkManager.sendToPlayer(player, new RuntimePayload(SFCReMod.RUNTIME));
			}
		}

		public ServerPlayer getPlayer() {
			return player;
		}

		public void setLastSyncTime(double time) {
			lastSyncTime = time;
		}

		public double getLastSyncTime() {
			return lastSyncTime;
		}
	}
}
