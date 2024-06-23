package com.rimo.sfcr.register;

import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.FloatArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.rimo.sfcr.SFCReMod;
import com.rimo.sfcr.network.*;
import com.rimo.sfcr.util.CloudRefreshSpeed;
import dev.architectury.event.events.common.CommandRegistrationEvent;
import dev.architectury.networking.NetworkManager;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.network.chat.Component;

import static net.minecraft.commands.Commands.argument;
import static net.minecraft.commands.Commands.literal;

public class Command {
	@Environment(EnvType.SERVER)
	public static void register() {
		CommandRegistrationEvent.EVENT.register((dispatcher, access, env) -> {
			dispatcher.register(literal("sfcr")
					.executes(content -> {
						content.getSource().sendSystemMessage(Component.nullToEmpty("- - - - - SFCR Help Page - - - - -"));
						content.getSource().sendSystemMessage(Component.nullToEmpty("/sfcr - Show this page"));
						content.getSource().sendSystemMessage(Component.nullToEmpty("/sfcr sync [full] - Sync with server instantly"));
						if (!content.getSource().hasPermission(2))
							return 1;
						content.getSource().sendSystemMessage(Component.nullToEmpty("/sfcr statu - Show runtime config"));
						content.getSource().sendSystemMessage(Component.nullToEmpty("/sfcr [enable|disable] - Toggle SFCR server activity"));
						content.getSource().sendSystemMessage(Component.nullToEmpty("/sfcr [cloud|density|biome] - Edit config"));
						content.getSource().sendSystemMessage(Component.nullToEmpty("/sfcr biome [list|add|remove] - Manage ignored biome"));
						content.getSource().sendSystemMessage(Component.nullToEmpty("/sfcr reload - Reload config (You needs sync it toAllPlayers manually)"));
						content.getSource().sendSystemMessage(Component.nullToEmpty("/sfcr save - Save runtime config to file"));
						return 1;
					})
					.then(literal("sync")
							.executes(content -> {
								if (content.getSource().isPlayer()) {
									NetworkManager.sendToPlayer(content.getSource().getPlayer(), new RuntimePayload(SFCReMod.RUNTIME));
									SFCReMod.LOGGER.info("[SFCRe] cb: Send sync data to " + content.getSource().getDisplayName().getString());
									content.getSource().sendSystemMessage(Component.nullToEmpty("Manual requesting sync..."));
								} else {
									content.getSource().sendFailure(Component.nullToEmpty("This command must run by Player!"));
								}
								return 1;
							})
							.then(literal("full").executes(content -> {
								if (content.getSource().isPlayer()) {
									NetworkManager.sendToPlayer(content.getSource().getPlayer(), new ConfigPayload(SFCReMod.RUNTIME.seed, SFCReMod.COMMON_CONFIG));
									NetworkManager.sendToPlayer(content.getSource().getPlayer(), new RuntimePayload(SFCReMod.RUNTIME));
									SFCReMod.LOGGER.info("[SFCRe] cb: Send full sync data to " + content.getSource().getDisplayName().getString());
									content.getSource().sendSystemMessage(Component.nullToEmpty("Manual requesting sync..."));
								} else {
									content.getSource().sendFailure(Component.nullToEmpty("This command must run by Player!"));
								}
								return 1;
							}))
							.then(literal("time").requires(source -> source.hasPermission(2))
									.then(argument("sec", IntegerArgumentType.integer()).executes(content -> {
										SFCReMod.COMMON_CONFIG.setSecPerSync(content.getArgument("sec", Integer.class));
										content.getSource().sendSystemMessage(Component.nullToEmpty("Sync time changed!"));
										return 1;
									}))
									.executes(content -> {
										content.getSource().sendSystemMessage(Component.nullToEmpty("Sync per second is " + SFCReMod.COMMON_CONFIG.getSecPerSync()));
										return 1;
									})
							)
							.then(literal("toAllPlayers").requires(source -> source.hasPermission(2)).executes(content -> {
								for (ServerPlayer player : content.getSource().getServer().getPlayerList().getPlayers()) {
									NetworkManager.sendToPlayer(content.getSource().getPlayer(), new ConfigPayload(SFCReMod.RUNTIME.seed, SFCReMod.COMMON_CONFIG));
									NetworkManager.sendToPlayer(content.getSource().getPlayer(), new RuntimePayload(SFCReMod.RUNTIME));
									player.sendSystemMessage(Component.nullToEmpty("[SFCRe] Force sync request came from server..."));
								}
								content.getSource().sendSystemMessage(Component.nullToEmpty("Force sync complete!"));
								SFCReMod.LOGGER.info("[SFCRe] cb: Force sync running by " + content.getSource().getDisplayName().getString());
								return 1;
							}))
					)
					.then(literal("statu").requires(source -> source.hasPermission(2)).executes(content -> {
						content.getSource().sendSystemMessage(Component.nullToEmpty("- - - - - SFCR Mod Statu - - - - -"));
						content.getSource().sendSystemMessage(Component.nullToEmpty("§eStatu: §r"				+ SFCReMod.COMMON_CONFIG.isEnableMod()));
						content.getSource().sendSystemMessage(Component.nullToEmpty("§eCloud height: §r"		+ SFCReMod.COMMON_CONFIG.getCloudHeight()));
						content.getSource().sendSystemMessage(Component.nullToEmpty("§eCloud Block Size: §r"	+ SFCReMod.COMMON_CONFIG.getCloudBlockSize()));
						content.getSource().sendSystemMessage(Component.nullToEmpty("§eCloud Thickness: §r"	+ SFCReMod.COMMON_CONFIG.getCloudLayerThickness()));
						content.getSource().sendSystemMessage(Component.nullToEmpty("§eSample Step: §r"		+ SFCReMod.COMMON_CONFIG.getSampleSteps()));
						content.getSource().sendSystemMessage(Component.nullToEmpty("§eCloud color: §r"		+
								Integer.toHexString((SFCReMod.COMMON_CONFIG.getCloudColor() & 0xFF0000) >> 16) +
								Integer.toHexString((SFCReMod.COMMON_CONFIG.getCloudColor() & 0x00FF00) >> 8) +
								Integer.toHexString(SFCReMod.COMMON_CONFIG.getCloudColor() & 0x0000FF)
						));
						content.getSource().sendSystemMessage(Component.nullToEmpty("§eCloud Brht Multi: §r"	+ SFCReMod.COMMON_CONFIG.getCloudBrightMultiplier()));
						content.getSource().sendSystemMessage(Component.nullToEmpty("§eDynamic Density: §r"	+ SFCReMod.COMMON_CONFIG.isEnableWeatherDensity()));
						content.getSource().sendSystemMessage(Component.nullToEmpty("§eDensity Threshld: §r"	+ SFCReMod.COMMON_CONFIG.getDensityThreshold()));
						content.getSource().sendSystemMessage(Component.nullToEmpty("§eThrshld Multiplr: §r"	+ SFCReMod.COMMON_CONFIG.getThresholdMultiplier()));
						content.getSource().sendSystemMessage(Component.nullToEmpty("§ePre-Detect Time: §r"	+ SFCReMod.COMMON_CONFIG.getWeatherPreDetectTime() / 20));
						content.getSource().sendSystemMessage(Component.nullToEmpty("§eChanging Speed: §r"	+ SFCReMod.COMMON_CONFIG.getDensityChangingSpeed().getName()));
						content.getSource().sendSystemMessage(Component.nullToEmpty("§eCommon Density: §r"	+ SFCReMod.COMMON_CONFIG.getCloudDensityPercent()));
						content.getSource().sendSystemMessage(Component.nullToEmpty("§eRain Density: §r"		+ SFCReMod.COMMON_CONFIG.getRainDensityPercent()));
						content.getSource().sendSystemMessage(Component.nullToEmpty("§eThunder Density: §r"	+ SFCReMod.COMMON_CONFIG.getThunderDensityPercent()));
						content.getSource().sendSystemMessage(Component.nullToEmpty("§eSnow Area Dens.: §r"	+ SFCReMod.COMMON_CONFIG.getSnowDensity()));
						content.getSource().sendSystemMessage(Component.nullToEmpty("§eRain Area Dens.: §r"	+ SFCReMod.COMMON_CONFIG.getRainDensity()));
						content.getSource().sendSystemMessage(Component.nullToEmpty("§eOther Area Dens.: §r"	+ SFCReMod.COMMON_CONFIG.getNoneDensity()));
						content.getSource().sendSystemMessage(Component.nullToEmpty("§eUsing Chunk: §r"		+ SFCReMod.COMMON_CONFIG.isBiomeDensityByChunk()));
						content.getSource().sendSystemMessage(Component.nullToEmpty("§eUsing Loaded Chk: §r"	+ SFCReMod.COMMON_CONFIG.isBiomeDensityUseLoadedChunk()));
						content.getSource().sendSystemMessage(Component.nullToEmpty("Type [/sfcr biome list] to check ignored biome list."));
						return 1;
					}))
					.then(literal("enable").requires(source -> source.hasPermission(2))
							.then(argument("e", BoolArgumentType.bool()).executes(content -> {
								SFCReMod.COMMON_CONFIG.setEnableMod(content.getArgument("e", Boolean.class));
								content.getSource().sendSystemMessage(Component.nullToEmpty("SFCR statu changed!"));
								return 1;
							}))
					)
					.then(literal("debug").requires(source -> source.hasPermission(2))
							.then(argument("e", BoolArgumentType.bool()).executes(content -> {
								SFCReMod.COMMON_CONFIG.setEnableDebug(content.getArgument("e", Boolean.class));
								content.getSource().sendSystemMessage(Component.nullToEmpty("Debug statu changed!"));
								return 1;
							}))
					)
					.then(literal("cloud").requires(source -> source.hasPermission(2))
							.then(literal("height")
									.then(argument("height", IntegerArgumentType.integer(96, 384)).executes(content -> {
										SFCReMod.COMMON_CONFIG.setCloudHeight(content.getArgument("height", Integer.class));
										content.getSource().sendSystemMessage(Component.nullToEmpty("Cloud height changed!"));
										return 1;
									}))
									.executes(content -> {
										content.getSource().sendSystemMessage(Component.nullToEmpty("Cloud height is " + SFCReMod.COMMON_CONFIG.getCloudHeight()));
										return 1;
									})
							)
							.then(literal("size")
									.then(argument("size", IntegerArgumentType.integer(1, 4)).executes(content -> {
										switch (content.getArgument("size", Integer.class)) {
											case 1 -> SFCReMod.COMMON_CONFIG.setCloudBlockSize(2);
											case 2 -> SFCReMod.COMMON_CONFIG.setCloudBlockSize(4);
											case 3 -> SFCReMod.COMMON_CONFIG.setCloudBlockSize(8);
											case 4 -> SFCReMod.COMMON_CONFIG.setCloudBlockSize(16);
										}
										content.getSource().sendSystemMessage(Component.nullToEmpty("Cloud size changed!"));
										return 1;
									}))
									.executes(content -> {
										content.getSource().sendSystemMessage(Component.nullToEmpty("Cloud size is " + SFCReMod.COMMON_CONFIG.getCloudBlockSize()));
										return 1;
									})
							)
							.then(literal("thickness")
									.then(argument("thickness", IntegerArgumentType.integer(8, 64)).executes(content -> {
										SFCReMod.COMMON_CONFIG.setCloudLayerThickness(content.getArgument("thickness", Integer.class));
										content.getSource().sendSystemMessage(Component.nullToEmpty("Cloud thickness changed!"));
										return 1;
									}))
									.executes(content -> {
										content.getSource().sendSystemMessage(Component.nullToEmpty("Cloud thickness is " + SFCReMod.COMMON_CONFIG.getCloudLayerThickness()));
										return 1;
									})
							)
							.then(literal("sample")
									.then(argument("sample", IntegerArgumentType.integer(1, 3)).executes(content -> {
										SFCReMod.COMMON_CONFIG.setSampleSteps(content.getArgument("sample", Integer.class));
										content.getSource().sendSystemMessage(Component.nullToEmpty("Sample step changed!"));
										return 1;
									}))
									.executes(content -> {
										content.getSource().sendSystemMessage(Component.nullToEmpty("Sample steps is " + SFCReMod.COMMON_CONFIG.getSampleSteps()));
										return 1;
									})
							)
							.then(literal("color")
									.then(argument("color", StringArgumentType.string()).executes(content -> {
										int color = Integer.parseUnsignedInt(content.getArgument("color", String.class), 16);
										if (color < 0 || color > 0xFFFFFF) {
											content.getSource().sendSystemMessage(Component.nullToEmpty("Illegal color value, please check."));
										} else {
											SFCReMod.COMMON_CONFIG.setCloudColor(color);
											content.getSource().sendSystemMessage(Component.nullToEmpty("Cloud color changed!"));
										}
										return 1;
									}))
									.executes(content -> {
										content.getSource().sendSystemMessage(Component.nullToEmpty("Cloud color is " + Integer.toHexString(SFCReMod.COMMON_CONFIG.getCloudColor())));
										return 1;
									})
							)
							.then(literal("bright")
									.then(argument("bright", FloatArgumentType.floatArg(0, 1)).executes(content -> {
										SFCReMod.COMMON_CONFIG.setCloudBrightMultiplier(content.getArgument("bright", Float.class));
										content.getSource().sendSystemMessage(Component.nullToEmpty("Cloud bright changed!"));
										return 1;
									}))
									.executes(content -> {
										content.getSource().sendSystemMessage(Component.nullToEmpty("Cloud bright is " + SFCReMod.COMMON_CONFIG.getCloudBrightMultiplier()));
										return 1;
									})
							)
					)
					.then(literal("density").requires(source -> source.hasPermission(2))
							.then(literal("enable")
									.then(argument("e", BoolArgumentType.bool()).executes(content -> {
										SFCReMod.COMMON_CONFIG.setEnableWeatherDensity(content.getArgument("e", Boolean.class));
										content.getSource().sendSystemMessage(Component.nullToEmpty("Density statu changed!"));
										return 1;
									}))
							)
							.then(literal("threshold")
									.then(argument("num", FloatArgumentType.floatArg(-1, 2)).executes(content -> {
										SFCReMod.COMMON_CONFIG.setDensityThreshold(content.getArgument("num", Float.class));
										content.getSource().sendSystemMessage(Component.nullToEmpty("Density threshold changed!"));
										return 1;
									}))
							)
							.then(literal("thresholdMultiplier")
									.then(argument("num", FloatArgumentType.floatArg(-1, 2)).executes(content -> {
										SFCReMod.COMMON_CONFIG.setThresholdMultiplier(content.getArgument("num", Float.class));
										content.getSource().sendSystemMessage(Component.nullToEmpty("Threshold multiplier changed!"));
										return 1;
									}))
							)
							.then(literal("predetect")
									.then(argument("predetect", IntegerArgumentType.integer(0, 30)).executes(content -> {
										SFCReMod.COMMON_CONFIG.setWeatherPreDetectTime(content.getArgument("predetect", Integer.class));
										content.getSource().sendSystemMessage(Component.nullToEmpty("Pre-detect time changed!"));
										return 1;
									}))
									.executes(content -> {
										content.getSource().sendSystemMessage(Component.nullToEmpty("Pre-detect time is " + SFCReMod.COMMON_CONFIG.getWeatherPreDetectTime()));
										return 1;
									})
							)
							.then(literal("changingspeed")
									.then(argument("changingspeed", IntegerArgumentType.integer(1, 5)).executes(content -> {
										switch (content.getArgument("changingspeed", Integer.class)) {
											case 1 -> SFCReMod.COMMON_CONFIG.setDensityChangingSpeed(CloudRefreshSpeed.VERY_SLOW);
											case 2 -> SFCReMod.COMMON_CONFIG.setDensityChangingSpeed(CloudRefreshSpeed.SLOW);
											case 3 -> SFCReMod.COMMON_CONFIG.setDensityChangingSpeed(CloudRefreshSpeed.NORMAL);
											case 4 -> SFCReMod.COMMON_CONFIG.setDensityChangingSpeed(CloudRefreshSpeed.FAST);
											case 5 -> SFCReMod.COMMON_CONFIG.setDensityChangingSpeed(CloudRefreshSpeed.VERY_FAST);
										}
										content.getSource().sendSystemMessage(Component.nullToEmpty("Changing speed changed!"));
										return 1;
									}))
									.executes(content -> {
										content.getSource().sendSystemMessage(Component.nullToEmpty("Density changing speed is " + SFCReMod.COMMON_CONFIG.getDensityChangingSpeed().toString()));
										return 1;
									})
							)
							.then(literal("common")
									.then(argument("percent", IntegerArgumentType.integer(0, 100)).executes(content -> {
										SFCReMod.COMMON_CONFIG.setCloudDensityPercent(content.getArgument("percent", Integer.class));
										content.getSource().sendSystemMessage(Component.nullToEmpty("Common density changed!"));
										return 1;
									}))
									.executes(content -> {
										content.getSource().sendSystemMessage(Component.nullToEmpty("Common density is " + SFCReMod.COMMON_CONFIG.getCloudDensityPercent()));
										return 1;
									})
							)
							.then(literal("rain")
									.then(argument("percent", IntegerArgumentType.integer(0, 100)).executes(content -> {
										SFCReMod.COMMON_CONFIG.setRainDensityPercent(content.getArgument("percent", Integer.class));
										content.getSource().sendSystemMessage(Component.nullToEmpty("Rain density changed!"));
										return 1;
									}))
									.executes(content -> {
										content.getSource().sendSystemMessage(Component.nullToEmpty("Rain density is " + SFCReMod.COMMON_CONFIG.getRainDensityPercent()));
										return 1;
									})
							)
							.then(literal("thunder")
									.then(argument("percent", IntegerArgumentType.integer(0, 100)).executes(content -> {
										SFCReMod.COMMON_CONFIG.setThunderDensityPercent(content.getArgument("percent", Integer.class));
										content.getSource().sendSystemMessage(Component.nullToEmpty("Thunder density changed!"));
										return 1;
									}))
									.executes(content -> {
										content.getSource().sendSystemMessage(Component.nullToEmpty("Thunder density is " + SFCReMod.COMMON_CONFIG.getThunderDensityPercent()));
										return 1;
									})
							)
					)
					.then(literal("biome").requires(source -> source.hasPermission(2))
							.then(literal("snow")
									.then(argument("snow", IntegerArgumentType.integer(0, 100)).executes(content -> {
										SFCReMod.COMMON_CONFIG.setSnowDensity(content.getArgument("snow", Integer.class));
										content.getSource().sendSystemMessage(Component.nullToEmpty("Snow area density changed!"));
										return 1;
									}))
									.executes(content -> {
										content.getSource().sendSystemMessage(Component.nullToEmpty("Snow area density is " + SFCReMod.COMMON_CONFIG.getSnowDensity()));
										return 1;
									})
							)
							.then(literal("rain")
									.then(argument("rain", IntegerArgumentType.integer(0, 100)).executes(content -> {
										SFCReMod.COMMON_CONFIG.setRainDensity(content.getArgument("rain", Integer.class));
										content.getSource().sendSystemMessage(Component.nullToEmpty("Rain area density changed!"));
										return 1;
									}))
									.executes(content -> {
										content.getSource().sendSystemMessage(Component.nullToEmpty("Rain area density is " + SFCReMod.COMMON_CONFIG.getRainDensity()));
										return 1;
									})
							)
							.then(literal("none")
									.then(argument("none", IntegerArgumentType.integer(0, 100)).executes(content -> {
										SFCReMod.COMMON_CONFIG.setNoneDensity(content.getArgument("none", Integer.class));
										content.getSource().sendSystemMessage(Component.nullToEmpty("Nothing area density changed!"));
										return 1;
									}))
									.executes(content -> {
										content.getSource().sendSystemMessage(Component.nullToEmpty("Nothing area density is " + SFCReMod.COMMON_CONFIG.getNoneDensity()));
										return 1;
									})
							)
							.then(literal("byChunk")
									.then(argument("e", BoolArgumentType.bool()).executes(content -> {
										SFCReMod.COMMON_CONFIG.setBiomeDensityByChunk(content.getArgument("e", Boolean.class));
										content.getSource().sendSystemMessage(Component.nullToEmpty("Biome detect function changed!"));
										return 1;
									}))
							)
							.then(literal("byLoadedChunk")
									.then(argument("e", BoolArgumentType.bool()).executes(content -> {
										SFCReMod.COMMON_CONFIG.setBiomeDensityUseLoadedChunk(content.getArgument("e", Boolean.class));
										content.getSource().sendSystemMessage(Component.nullToEmpty("Biome detect function changed!"));
										return 1;
									}))
							)
							.then(literal("list").executes(content -> {
								content.getSource().sendSystemMessage(Component.nullToEmpty("Server Biome Filter List: "));
								for (String biome : SFCReMod.COMMON_CONFIG.getBiomeFilterList()) {
									content.getSource().sendSystemMessage(Component.nullToEmpty("- " + biome));
								}
								return 1;
							}))
							.then(literal("add")
									.then(argument("id", StringArgumentType.string()).executes(content -> {
										var list = SFCReMod.COMMON_CONFIG.getBiomeFilterList();
										list.add(content.getArgument("id", String.class));
										SFCReMod.COMMON_CONFIG.setBiomeFilterList(list);
										content.getSource().sendSystemMessage(Component.nullToEmpty("Biome added!"));
										return 1;
									}))
							)
							.then(literal("remove")
									.then(argument("id", StringArgumentType.string()).executes(content -> {
										var list = SFCReMod.COMMON_CONFIG.getBiomeFilterList();
										list.remove(content.getArgument("id", String.class));
										SFCReMod.COMMON_CONFIG.setBiomeFilterList(list);
										content.getSource().sendSystemMessage(Component.nullToEmpty("Biome removed!"));
										return 1;
									}))
							)
					)
					.then(literal("reload").requires(source -> source.hasPermission(4)).executes(content -> {
						SFCReMod.COMMON_CONFIG.load();
						SFCReMod.LOGGER.info("[SFCRe] cb: Reload config by " + content.getSource().getDisplayName().getString());
						content.getSource().sendSystemMessage(Component.nullToEmpty("Reloading complete!"));
						return 1;
					}))
					.then(literal("save").requires(source -> source.hasPermission(4)).executes(content -> {
						SFCReMod.COMMON_CONFIG.save();
						SFCReMod.LOGGER.info("[SFCRe] cb: Save config by " + content.getSource().getDisplayName().getString());
						content.getSource().sendSystemMessage(Component.nullToEmpty("Config saving complete!"));
						return 1;
					}))
			);
		});
	}
}
