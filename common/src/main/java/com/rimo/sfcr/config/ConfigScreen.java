package com.rimo.sfcr.config;

import com.rimo.sfcr.SFCReMod;
import com.rimo.sfcr.util.CloudRefreshSpeed;
import com.rimo.sfcr.util.CullMode;
import me.shedaniel.clothconfig2.api.ConfigBuilder;
import me.shedaniel.clothconfig2.api.ConfigCategory;
import me.shedaniel.clothconfig2.api.ConfigEntryBuilder;
import me.shedaniel.clothconfig2.api.Requirement;
import me.shedaniel.clothconfig2.gui.entries.EnumListEntry;
import me.shedaniel.clothconfig2.impl.builders.DropdownMenuBuilder.TopCellElementBuilder;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.List;

public class ConfigScreen {

	ConfigBuilder builder = ConfigBuilder.create()
			.setParentScreen(Minecraft.getInstance().screen)
			.setTitle(Component.translatable("text.sfcr.title"));
	ConfigEntryBuilder entryBuilder = builder.entryBuilder();

	ConfigCategory general = builder.getOrCreateCategory(Component.translatable("text.sfcr.category.general"));
	ConfigCategory clouds = builder.getOrCreateCategory(Component.translatable("text.sfcr.category.clouds"));
	ConfigCategory fog = builder.getOrCreateCategory(Component.translatable("text.sfcr.category.fog"));
	ConfigCategory density = builder.getOrCreateCategory(Component.translatable("text.sfcr.category.density"));

	private final CommonConfig CONFIG = SFCReMod.COMMON_CONFIG;

	private int fogMin, fogMax;

	public Screen buildScreen() {
		buildGeneralCategory();
		buildCloudsCategory();
		buildFogCategory();
		buildDensityCategory();
		builder.setTransparentBackground(true);

		//Update when saving
		builder.setSavingRunnable(() -> {
			if (CONFIG.isCloudRenderDistanceFitToView())
				CONFIG.setCloudRenderDistance(Minecraft.getInstance().options.getEffectiveRenderDistance() * 12);
			CONFIG.setFogDisance(fogMin, fogMax);

			//Update config
			SFCReMod.COMMON_CONFIG.save();
			SFCReMod.RENDERER.updateConfig(CONFIG);
		});

		return builder.build();
	}

	private void buildGeneralCategory() {
		//enabled
		general.addEntry(entryBuilder
				.startBooleanToggle(Component.translatable("text.sfcr.option.enableMod")
						, CONFIG.isEnableMod())
				.setDefaultValue(true)
				.setTooltip(Component.translatable("text.sfcr.option.enableMod.@Tooltip"))
				.setSaveConsumer(CONFIG::setEnableMod)
				.build()
		);
		//fog enable
		general.addEntry(entryBuilder
				.startBooleanToggle(Component.translatable("text.sfcr.option.enableFog")
						, CONFIG.isEnableFog())
				.setDefaultValue(true)
				.setTooltip(Component.translatable("text.sfcr.option.enableFog.@Tooltip"))
				.setSaveConsumer(CONFIG::setEnableFog)
				.build()
		);
		//weather
		general.addEntry(entryBuilder
				.startBooleanToggle(Component.translatable("text.sfcr.option.enableWeatherDensity")
						, CONFIG.isEnableWeatherDensity())
				.setDefaultValue(true)
				.setTooltip(Component.translatable("text.sfcr.option.enableWeatherDensity.@Tooltip"))
				.setSaveConsumer(CONFIG::setEnableWeatherDensity)
				.build()
		);
		//normal cull
		general.addEntry(entryBuilder
				.startBooleanToggle(Component.translatable("text.sfcr.option.enableNormalCull")
						, CONFIG.isEnableNormalCull())
				.setDefaultValue(true)
				.setTooltip(Component.translatable("text.sfcr.option.enableNormalCull.@Tooltip"))
				.setSaveConsumer(CONFIG::setEnableNormalCull)
				.build()
		);
		//cull mode
		EnumListEntry<CullMode> cullMode = entryBuilder
				.startEnumSelector(Component.translatable("text.sfcr.option.cullMode")
						, CullMode.class
						, CONFIG.getCullMode())
				.setDefaultValue(CullMode.RECTANGULAR)
				.setEnumNameProvider(value -> ((CullMode) value).getName())
				.setTooltip(Component.translatable("text.sfcr.option.cullMode.@Tooltip"))
				.setSaveConsumer(CONFIG::setCullMode)
				.build();
		general.addEntry(cullMode);
		//cull radian multiplier
		general.addEntry(entryBuilder
				.startIntSlider(Component.translatable("text.sfcr.option.cullRadianMultiplier")
						,(int) (CONFIG.getCullRadianMultiplier() * 10)
						,5
						,15)
				.setDefaultValue(10)
				.setTextGetter(value -> Component.nullToEmpty(value / 10f + "x"))
				.setTooltip(Component.translatable("text.sfcr.option.cullRadianMultiplier.@Tooltip"))
				.setRequirement(Requirement.isValue(cullMode, CullMode.CIRCULAR, CullMode.RECTANGULAR))
				.setSaveConsumer(value -> CONFIG.setCullRadianMultiplier(value / 10f))
				.build()
		);
		//remesh interval
		general.addEntry(entryBuilder
				.startIntSlider(Component.translatable("text.sfcr.option.rebuildInterval")
						, CONFIG.getRebuildInterval()
						,0
						,30)
				.setDefaultValue(10)
				.setTextGetter(value -> value == 0 ?
						Component.translatable("text.sfcr.disabled") :
						Component.translatable("text.sfcr.frame", value)
				)
				.setTooltip(Component.translatable("text.sfcr.option.rebuildInterval.@Tooltip"))
				.setSaveConsumer(CONFIG::setRebuildInterval)
				.build()
		);
		//server control
		general.addEntry(entryBuilder
				.startBooleanToggle(Component.translatable("text.sfcr.option.enableServer")
						, CONFIG.isEnableServerConfig())
				.setDefaultValue(false)
				.setTooltip(Component.translatable("text.sfcr.option.enableServer.@Tooltip"))
				.setSaveConsumer(CONFIG::setEnableServerConfig)
				.build()
		);
		//server sync time
		general.addEntry(entryBuilder
				.startIntSlider(Component.translatable("text.sfcr.option.syncTime")
						, CONFIG.getSecPerSync() / 15
						, 1
						, 20)
				.setDefaultValue(4)
				.setTextGetter(value -> Component.translatable("text.sfcr.second", value * 15))
				.setTooltip(Component.translatable("text.sfcr.option.syncTime.@Tooltip"))
				.setSaveConsumer(value -> CONFIG.setSecPerSync(value * 15))
				.build()
		);
		//DEBUG
		general.addEntry(entryBuilder
				.startBooleanToggle(Component.translatable("text.sfcr.option.debug")
						, CONFIG.isEnableDebug())
				.setDefaultValue(false)
				.setTooltip(Component.translatable("text.sfcr.option.debug.@Tooltip"))
				.setSaveConsumer(CONFIG::setEnableDebug)
				.build()
		);
	}

	private void buildCloudsCategory() {
		//cloud height
		clouds.addEntry(entryBuilder
				.startIntSlider(Component.translatable("text.sfcr.option.cloudHeight")
						, CONFIG.getCloudHeight()
						,-1
						,384)
				.setDefaultValue(192)
				.setTextGetter(value -> value < 0 ?
						Component.translatable("text.sfcr.option.cloudHeight.followVanilla") :
						Component.nullToEmpty(value.toString())
				)
				.setTooltip(Component.translatable("text.sfcr.option.cloudHeight.@Tooltip"))
				.setSaveConsumer(CONFIG::setCloudHeight)
				.build()
		);
		//cloud block size
		clouds.addEntry(entryBuilder
				.startDropdownMenu(Component.translatable("text.sfcr.option.cloudBlockSize")
						,TopCellElementBuilder.of(CONFIG.getCloudBlockSize(), Integer::parseInt))
				.setDefaultValue(16)
				.setSuggestionMode(false)
				.setSelections(List.of(2, 4, 8, 16))
				.setTooltip(Component.translatable("text.sfcr.option.cloudBlockSize.@Tooltip"))
				.setSaveConsumer(CONFIG::setCloudBlockSize)
				.build()
		);
		//cloud thickness
		clouds.addEntry(entryBuilder
				.startIntSlider(Component.translatable("text.sfcr.option.cloudLayerThickness")
						, CONFIG.getCloudLayerThickness()
						,3
						,66)
				.setDefaultValue(32)
				.setTextGetter(value -> Component.nullToEmpty(String.valueOf(value - 2)))
				.setTooltip(Component.translatable("text.sfcr.option.cloudLayerThickness.@Tooltip"))
				.setSaveConsumer(CONFIG::setCloudLayerThickness)
				.build()
		);
		//cloud distance
		clouds.addEntry(entryBuilder
				.startIntSlider(Component.translatable("text.sfcr.option.cloudRenderDistance")
						, CONFIG.getCloudRenderDistance() / 2
						,32
						,192)
				.setDefaultValue(48)
				.setTextGetter(value -> Component.nullToEmpty(value.toString()))
				.setTooltip(Component.translatable("text.sfcr.option.cloudRenderDistance.@Tooltip"))
				.setSaveConsumer(value -> CONFIG.setCloudRenderDistance(value * 2))
				.build()
		);
		//cloud distance fit to view
		clouds.addEntry(entryBuilder
				.startBooleanToggle(Component.translatable("text.sfcr.option.cloudRenderDistanceFitToView")
						, CONFIG.isCloudRenderDistanceFitToView())
				.setDefaultValue(false)
				.setTooltip(Component.translatable("text.sfcr.option.cloudRenderDistanceFitToView.@Tooltip"))
				.setSaveConsumer(CONFIG::setCloudRenderDistanceFitToView)
				.build()
		);
		//cloud sample steps
		clouds.addEntry(entryBuilder
				.startIntSlider(Component.translatable("text.sfcr.option.sampleSteps")
						, CONFIG.getSampleSteps()
						,1
						,3)
				.setDefaultValue(2)
				.setTextGetter(value -> Component.nullToEmpty(value.toString()))
				.setTooltip(Component.translatable("text.sfcr.option.sampleSteps.@Tooltip"))
				.setSaveConsumer(CONFIG::setSampleSteps)
				.build()
		);
		//terrain dodge
		clouds.addEntry(entryBuilder
				.startBooleanToggle(Component.translatable("text.sfcr.option.enableTerrainDodge")
						, CONFIG.isEnableTerrainDodge())
				.setDefaultValue(true)
				.setTooltip(Component.translatable("text.sfcr.option.enableTerrainDodge.@Tooltip"))
				.setSaveConsumer(CONFIG::setEnableTerrainDodge)
				.build()
		);
		//cloud color
		clouds.addEntry(entryBuilder
				.startColorField(Component.translatable("text.sfcr.option.cloudColor")
						, CONFIG.getCloudColor())
				.setDefaultValue(0xFFFFFF)
				.setSaveConsumer(CONFIG::setCloudColor)
				.build()
		);
		//cloud bright multiplier
		clouds.addEntry(entryBuilder
				.startIntSlider(Component.translatable("text.sfcr.option.cloudBright")
						, (int) (CONFIG.getCloudBrightMultiplier() * 10)
						, 0
						, 10)
				.setDefaultValue(1)
				.setTextGetter(value -> Component.nullToEmpty(value * 10 + "%"))
				.setSaveConsumer(value -> CONFIG.setCloudBrightMultiplier(value / 10f))
				.build()
		);
	}

	private void buildFogCategory() {
		//fog auto distance
		fog.addEntry(entryBuilder
				.startBooleanToggle(Component.translatable("text.sfcr.option.fogAutoDistance")
						, CONFIG.isFogAutoDistance())
				.setDefaultValue(true)
				.setTooltip(Component.translatable("text.sfcr.option.fogAutoDistance.@Tooltip"))
				.setSaveConsumer(CONFIG::setFogAutoDistance)
				.build()
		);
		//fog min dist.
		fog.addEntry(entryBuilder
				.startIntSlider(Component.translatable("text.sfcr.option.fogMinDistance")
						, CONFIG.getFogMinDistance()
						,1
						,32)
				.setDefaultValue(2)
				.setTextGetter(value -> Component.nullToEmpty(value.toString()))
				.setTooltip(Component.translatable("text.sfcr.option.fogMinDistance.@Tooltip"))
				.setSaveConsumer(newValue -> fogMin = newValue)
				.build()
		);
		//fog max dist.
		fog.addEntry(entryBuilder
				.startIntSlider(Component.translatable("text.sfcr.option.fogMaxDistance")
						, CONFIG.getFogMaxDistance()
						,1
						,32)
				.setDefaultValue(4)
				.setTextGetter(value -> Component.nullToEmpty(value.toString()))
				.setTooltip(Component.translatable("text.sfcr.option.fogMaxDistance.@Tooltip"))
				.setSaveConsumer(newValue -> fogMax = newValue)
				.build()
		);
	}

	private void buildDensityCategory() {
		// threshold
		density.addEntry(entryBuilder
				.startFloatField(Component.translatable("text.sfcr.option.densityThreshold")
						, CONFIG.getDensityThreshold())
				.setDefaultValue(1.3f)
				.setMax(2f)
				.setMin(-1f)
				.setTooltip(Component.translatable("text.sfcr.option.densityThreshold.@Tooltip"))
				.setSaveConsumer(CONFIG::setDensityThreshold)
				.build()
		);
		// threshold multiplier
		density.addEntry(entryBuilder
				.startFloatField(Component.translatable("text.sfcr.option.thresholdMultiplier")
						, CONFIG.getThresholdMultiplier())
				.setDefaultValue(1.5f)
				.setMax(3f)
				.setMin(0f)
				.setTooltip(Component.translatable("text.sfcr.option.thresholdMultiplier.@Tooltip"))
				.setSaveConsumer(CONFIG::setThresholdMultiplier)
				.build()
		);
		//density
		density.addEntry(entryBuilder
				.startTextDescription(Component.translatable("text.sfcr.option.cloudDensity.@PrefixText"))
				.build()
		);
		//cloud common density
		density.addEntry(entryBuilder
				.startIntSlider(Component.translatable("text.sfcr.option.cloudDensity")
						, CONFIG.getCloudDensityPercent()
						,0
						,100)
				.setDefaultValue(25)
				.setTextGetter(value -> Component.nullToEmpty(value + "%"))
				.setSaveConsumer(CONFIG::setCloudDensityPercent)
				.build()
		);
		//rain density
		density.addEntry(entryBuilder
				.startIntSlider(Component.translatable("text.sfcr.option.rainDensity")
						, CONFIG.getRainDensityPercent()
						,0
						,100)
				.setDefaultValue(60)
				.setTextGetter(value -> Component.nullToEmpty(value + "%"))
				.setSaveConsumer(CONFIG::setRainDensityPercent)
				.build()
		);
		//thunder density
		density.addEntry(entryBuilder
				.startIntSlider(Component.translatable("text.sfcr.option.thunderDensity")
						, CONFIG.getThunderDensityPercent()
						,0
						,100)
				.setDefaultValue(90)
				.setTextGetter(value -> Component.nullToEmpty(value + "%"))
				.setSaveConsumer(CONFIG::setThunderDensityPercent)
				.build()
		);
		//weather pre-detect time
		density.addEntry(entryBuilder
				.startIntSlider(Component.translatable("text.sfcr.option.weatherPreDetectTime")
						, CONFIG.getWeatherPreDetectTime()
						,0
						,30)
				.setDefaultValue(10)
				.setTextGetter(value -> value == 0 ?
						Component.translatable("text.sfcr.disabled") :
						Component.translatable("text.sfcr.second", value)
				)
				.setTooltip(Component.translatable("text.sfcr.option.weatherPreDetectTime.@Tooltip"))
				.setSaveConsumer(CONFIG::setWeatherPreDetectTime)
				.build()
		);
		//cloud refresh speed
		density.addEntry(entryBuilder
				.startEnumSelector(Component.translatable("text.sfcr.option.cloudRefreshSpeed")
						,CloudRefreshSpeed.class
						, CONFIG.getNormalRefreshSpeed())
				.setDefaultValue(CloudRefreshSpeed.SLOW)
				.setEnumNameProvider(value -> ((CloudRefreshSpeed) value).getName())
				.setTooltip(Component.translatable("text.sfcr.option.cloudRefreshSpeed.@Tooltip"))
				.setSaveConsumer(CONFIG::setNormalRefreshSpeed)
				.build()
		);
		//weather refresh speed
		density.addEntry(entryBuilder
				.startEnumSelector(Component.translatable("text.sfcr.option.weatherRefreshSpeed")
						,CloudRefreshSpeed.class
						, CONFIG.getWeatherRefreshSpeed())
				.setDefaultValue(CloudRefreshSpeed.FAST)
				.setEnumNameProvider(value -> ((CloudRefreshSpeed) value).getName())
				.setTooltip(Component.translatable("text.sfcr.option.weatherRefreshSpeed.@Tooltip"))
				.setSaveConsumer(CONFIG::setWeatherRefreshSpeed)
				.build()
		);
		//density changing speed
		density.addEntry(entryBuilder
				.startEnumSelector(Component.translatable("text.sfcr.option.densityChangingSpeed")
						,CloudRefreshSpeed.class
						, CONFIG.getDensityChangingSpeed())
				.setDefaultValue(CloudRefreshSpeed.SLOW)
				.setEnumNameProvider(value -> ((CloudRefreshSpeed) value).getName())
				.setTooltip(Component.translatable("text.sfcr.option.densityChangingSpeed.@Tooltip"))
				.setSaveConsumer(CONFIG::setDensityChangingSpeed)
				.build()
		);
		//smooth change
		density.addEntry(entryBuilder
				.startBooleanToggle(Component.translatable("text.sfcr.option.enableSmoothChange")
						, CONFIG.isEnableSmoothChange())
				.setDefaultValue(false)
				.setTooltip(Component.translatable("text.sfcr.option.enableSmoothChange.@Tooltip"))
				.setSaveConsumer(CONFIG::setEnableSmoothChange)
				.build()
		);
		//precipitation info
		density.addEntry(entryBuilder
				.startTextDescription(Component.translatable("text.autoconfig.sfcr.option.precipitationDensity.@PrefixText"))
				.build()
		);
		//snow
		density.addEntry(entryBuilder
				.startIntSlider(Component.translatable("text.autoconfig.sfcr.option.snowDensity")
						, CONFIG.getSnowDensity()
						,0
						,100)
				.setDefaultValue(60)
				.setTextGetter(value -> Component.nullToEmpty(value + "%"))
				.setSaveConsumer(CONFIG::setSnowDensity)
				.build()
		);
		//rain
		density.addEntry(entryBuilder
				.startIntSlider(Component.translatable("text.autoconfig.sfcr.option.rainPrecipitationDensity")
						, CONFIG.getRainDensity()
						,0
						,100)
				.setDefaultValue(90)
				.setTextGetter(value -> Component.nullToEmpty(value + "%"))
				.setSaveConsumer(CONFIG::setRainDensity)
				.build()
		);
		//none
		density.addEntry(entryBuilder
				.startIntSlider(Component.translatable("text.autoconfig.sfcr.option.noneDensity")
						, CONFIG.getNoneDensity()
						,0
						,100)
				.setDefaultValue(0)
				.setTextGetter(value -> Component.nullToEmpty(value + "%"))
				.setSaveConsumer(CONFIG::setNoneDensity)
				.build()
		);
		//biome density affect by chunk
		density.addEntry(entryBuilder
				.startBooleanToggle(Component.translatable("text.sfcr.option.isBiomeDensityByChunk")
						, CONFIG.isBiomeDensityByChunk())
				.setDefaultValue(false)
				.setTooltip(Component.translatable("text.sfcr.option.isBiomeDensityByChunk.@Tooltip"))
				.setSaveConsumer(CONFIG::setBiomeDensityByChunk)
				.build()
		);
		//biome density detect loaded chunk
		density.addEntry(entryBuilder
				.startBooleanToggle(Component.translatable("text.sfcr.option.isBiomeDensityUseLoadedChunk")
						, CONFIG.isBiomeDensityUseLoadedChunk())
				.setDefaultValue(false)
				.setTooltip(Component.translatable("text.sfcr.option.isBiomeDensityUseLoadedChunk.@Tooltip"))
				.setSaveConsumer(CONFIG::setBiomeDensityUseLoadedChunk)
				.build()
		);
		//biome filter
		density.addEntry(entryBuilder
				.startStrList(Component.translatable("text.sfcr.option.biomeFilter")
						, CONFIG.getBiomeFilterList())
				.setDefaultValue(CommonConfig.DEF_BIOME_FILTER_LIST)
				.setTooltip(Component.translatable("text.sfcr.option.biomeFilter.@Tooltip"))
				.setSaveConsumer(CONFIG::setBiomeFilterList)
				.build()
		);
	}

}
