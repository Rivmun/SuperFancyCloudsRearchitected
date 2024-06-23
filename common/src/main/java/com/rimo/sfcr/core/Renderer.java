package com.rimo.sfcr.core;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import com.rimo.sfcr.SFCReMod;
import com.rimo.sfcr.config.CommonConfig;
import com.rimo.sfcr.util.CloudDataType;
import com.rimo.sfcr.util.CullMode;
import com.rimo.sfcr.util.WeatherType;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.FogRenderer;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FastColor;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;

public class Renderer {

	private final Runtime RUNTIME = SFCReMod.RUNTIME;
	private final CommonConfig CONFIG = SFCReMod.COMMON_CONFIG;

	private float cloudDensityByWeather = 0f;
	private float cloudDensityByBiome = 0f;
	private float targetDownFall = 1f;
	private boolean isWeatherChange = false;
	private boolean isBiomeChange = false;
	public float cloudHeight;

	private int normalRefreshSpeed = CONFIG.getNormalRefreshSpeed().getValue();
	private int weatheringRefreshSpeed = CONFIG.getWeatherRefreshSpeed().getValue() / 2;
	private int densityChangingSpeed = CONFIG.getDensityChangingSpeed().getValue();

	private final ResourceLocation whiteTexture = ResourceLocation.fromNamespaceAndPath("sfcr", "white.png");

	public VertexBuffer cloudBuffer;

	public ObjectArrayList<CloudData> cloudDataGroup = new ObjectArrayList<>();

	public Thread dataProcessThread;
	public boolean isProcessingData = false;

	public int moveTimer = 40;
	public double time;
	private double timeRebuild;

	public double xScroll;
	public double zScroll;

	public int cullStateSkipped = 0;
	public int cullStateShown = 0;

	public void init() {
		CloudData.initSampler(SFCReMod.RUNTIME.seed);
		isProcessingData = false;
	}

	public void tick() {

		if (Minecraft.getInstance().player == null)
			return;

		if (!CONFIG.isEnableMod())
			return;

		if (Minecraft.getInstance().isSingleplayer() && Minecraft.getInstance().isPaused())
			return;

		if (!Minecraft.getInstance().level.dimensionType().hasSkyLight())
			return;

		//If already processing, don't start up again.
		if (isProcessingData)
			return;

		var player = Minecraft.getInstance().player;
		var world = Minecraft.getInstance().level;
		var xScroll = (int) (player.getX() / CONFIG.getCloudBlockSize()) * CONFIG.getCloudBlockSize();
		var zScroll = (int) (player.getZ() / CONFIG.getCloudBlockSize()) * CONFIG.getCloudBlockSize();

		int timeOffset = (int) (Math.floor(time / 6) * 6);

		RUNTIME.clientTick(world);

		//Detect Weather Change
		if (CONFIG.isEnableWeatherDensity()) {
			if (world.isThundering()) {
				isWeatherChange = RUNTIME.nextWeather != WeatherType.THUNDER && CONFIG.getWeatherPreDetectTime() != 0
						|| cloudDensityByWeather < CONFIG.getThunderDensityPercent() / 100f;
			} else if (world.isRaining()) {
				isWeatherChange = RUNTIME.nextWeather != WeatherType.RAIN && CONFIG.getWeatherPreDetectTime() != 0
						|| cloudDensityByWeather != CONFIG.getRainDensityPercent() / 100f;
			} else {		//Clear...
				isWeatherChange = RUNTIME.nextWeather != WeatherType.CLEAR && CONFIG.getWeatherPreDetectTime() != 0
						|| cloudDensityByWeather > CONFIG.getCloudDensityPercent() / 100f;
			}

			//Detect Biome Change
			if (!CONFIG.isBiomeDensityByChunk()) {		//Hasn't effect if use chunk data.
				if (!CONFIG.isFilterListHasBiome(world.getBiome(player.getOnPos())))
					targetDownFall = CONFIG.getDownfall(world.getBiome(player.getOnPos()).value().getPrecipitationAt(player.getOnPos()));
				isBiomeChange = cloudDensityByBiome != targetDownFall;
			}
		} else {
			isWeatherChange = false;
			isBiomeChange = false;
		}

		//Refresh Processing...
		if (timeOffset != moveTimer || xScroll != this.xScroll || zScroll != this.zScroll) {
			moveTimer = timeOffset;
			isProcessingData = true;

			//Density Change by Weather
			if (CONFIG.isEnableWeatherDensity()) {
				if (isWeatherChange) {
					switch (RUNTIME.nextWeather) {
						case THUNDER -> cloudDensityByWeather = nextDensityStep(CONFIG.getThunderDensityPercent() / 100f, cloudDensityByWeather, densityChangingSpeed);
						case RAIN -> cloudDensityByWeather = nextDensityStep(CONFIG.getRainDensityPercent() / 100f, cloudDensityByWeather, densityChangingSpeed);
						case CLEAR -> cloudDensityByWeather = nextDensityStep(CONFIG.getCloudDensityPercent() / 100f, cloudDensityByWeather, densityChangingSpeed);
					}
				} else {
					switch (RUNTIME.nextWeather) {
						case THUNDER -> cloudDensityByWeather = CONFIG.getThunderDensityPercent() / 100f;
						case RAIN -> cloudDensityByWeather = CONFIG.getRainDensityPercent() / 100f;
						case CLEAR -> cloudDensityByWeather = CONFIG.getCloudDensityPercent() / 100f;
					}
				}
				//Density Change by Biome
				if (!CONFIG.isBiomeDensityByChunk()) {
					cloudDensityByBiome = isBiomeChange ? nextDensityStep(targetDownFall, cloudDensityByBiome, densityChangingSpeed) : targetDownFall;
				} else {
					cloudDensityByBiome = 0.5f;		//Output common value if use chunk.
				}
			} else {		//Initialize if disabled detect in rain/thunder.
				cloudDensityByWeather = CONFIG.getCloudDensityPercent() / 100f;
				cloudDensityByBiome = 0f;
			}

			dataProcessThread = new Thread(() -> collectCloudData(xScroll, zScroll));
			dataProcessThread.start();

			if (CONFIG.isEnableDebug()) {
				SFCReMod.LOGGER.info("wc: " + isWeatherChange + ", bc: " + isBiomeChange + ", wd: " + cloudDensityByWeather + ", bd: " + cloudDensityByBiome);
			}
		}
	}

	public void render(ClientLevel world, PoseStack matrices, Matrix4f matrix4f, Matrix4f matrix4f2, float tickDelta, double cameraX, double cameraY, double cameraZ) {

		cloudHeight = CONFIG.getCloudHeight() < 0 ? world.effects().getCloudHeight() : CONFIG.getCloudHeight();

		if (!Float.isNaN(cloudHeight)) {
			//Setup render system
//			RenderSystem.disableCull();
//			RenderSystem.enableBlend();
//			RenderSystem.enableDepthTest();
//			RenderSystem.blendFuncSeparate(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA, GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA);
//			RenderSystem.depthMask(true);

			Vec3 cloudColor = world.getCloudColor(tickDelta);
			Vec3 cloudColor2 = new Vec3(
					cloudColor.x + (1 - cloudColor.x) * CONFIG.getCloudBrightMultiplier(),
					cloudColor.y + (1 - cloudColor.y) * CONFIG.getCloudBrightMultiplier(),
					cloudColor.z + (1 - cloudColor.z) * CONFIG.getCloudBrightMultiplier()
			);

			synchronized (this) {

				if (!Minecraft.getInstance().isSingleplayer() || !Minecraft.getInstance().isPaused())
					SFCReMod.RUNTIME.partialOffset += Minecraft.getInstance().getTimer().getGameTimeDeltaTicks() * 0.25f * 0.25f * CONFIG.getCloudBlockSize() / 16f;

				if ((isWeatherChange && cloudDensityByBiome != 0) || (isBiomeChange && cloudDensityByWeather != 0)) {
					time += Minecraft.getInstance().getTimer().getGameTimeDeltaTicks() / weatheringRefreshSpeed;
				} else {
					time += Minecraft.getInstance().getTimer().getGameTimeDeltaTicks() / normalRefreshSpeed;		//20.0f for origin
				}

				if (++timeRebuild > CONFIG.getRebuildInterval()) {
					timeRebuild = 0;
					MeshData cb = rebuildCloudMesh(Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX_COLOR_NORMAL));

					if (cb != null) {
						if (cloudBuffer != null)
							cloudBuffer.close();
						cloudBuffer = new VertexBuffer(VertexBuffer.Usage.STATIC);

						cloudBuffer.bind();
						cloudBuffer.upload(cb);
						VertexBuffer.unbind();
					}
				}

				//Setup shader
				if (cloudBuffer != null) {
//					RenderSystem.setShader(GameRenderer::getPositionShader);
					RenderSystem.setShaderTexture(0, whiteTexture);
					if (CONFIG.isEnableFog()) {
						FogRenderer.levelFogColor();
						if (!CONFIG.isFogAutoDistance()) {
							RenderSystem.setShaderFogStart(RenderSystem.getShaderFogStart() * CONFIG.getFogMinDistance() * CONFIG.getCloudBlockSize() / 16);
							RenderSystem.setShaderFogEnd(RenderSystem.getShaderFogEnd() * CONFIG.getFogMaxDistance() * CONFIG.getCloudBlockSize() / 16);
						} else {
							RenderSystem.setShaderFogStart(RenderSystem.getShaderFogStart() * CONFIG.getAutoFogMaxDistance() / 2);
							RenderSystem.setShaderFogEnd(RenderSystem.getShaderFogEnd() * CONFIG.getAutoFogMaxDistance());
						}
					} else {
						FogRenderer.setupNoFog();
					}

					matrices.pushPose();
					matrices.mulPose(matrix4f);
//					matrices.translate(-cameraX, cameraY, -cameraZ);
					matrices.translate(xScroll + 0.01f, cloudHeight - CONFIG.getCloudBlockSize() + 0.01f, zScroll + SFCReMod.RUNTIME.partialOffset);

					RenderSystem.setShaderColor((float) cloudColor2.x, (float) cloudColor2.y, (float) cloudColor2.z, 1);
					cloudBuffer.bind();

					for (int s = 0; s < 2; ++s) {
						RenderType renderType = s == 0 ? RenderType.cloudsDepthOnly() : RenderType.clouds();
						renderType.setupRenderState();
						cloudBuffer.drawWithShader(matrices.last().pose(), matrix4f2, RenderSystem.getShader());
						renderType.clearRenderState();
					}

					VertexBuffer.unbind();
					matrices.popPose();

					//Restore render system
					RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
				}
			}
//			RenderSystem.enableCull();
//			RenderSystem.disableBlend();
//			RenderSystem.defaultBlendFunc();
		}
	}

	public void clean() {
		try {
			if (dataProcessThread != null)
				dataProcessThread.join();
		} catch (Exception e) {
			//Ignore...
		}
	}

	private final float[][] normals = {
			{1, 0, 0},		//r
			{-1, 0, 0},		//l
			{0, 1, 0},		//u
			{0, -1, 0},		//d
			{0, 0, 1},		//f
			{0, 0, -1},		//b
	};

	private final int[] colors = {
			FastColor.ARGB32.color((int) (255 * 0.8f), (int) (255 * 0.95f), (int) (255 * 0.9f), (int) (255 * 0.9f)),
			FastColor.ARGB32.color((int) (255 * 0.8f), (int) (255 * 0.75f), (int) (255 * 0.75f), (int) (255 * 0.75f)),
			FastColor.ARGB32.color((int) (255 * 0.8f), 255, 255, 255),
			FastColor.ARGB32.color((int) (255 * 0.8f), (int) (255 * 0.6f), (int) (255 * 0.6f), (int) (255 * 0.6f)),
			FastColor.ARGB32.color((int) (255 * 0.8f), (int) (255 * 0.92f), (int) (255 * 0.85f), (int) (255 * 0.85f)),
			FastColor.ARGB32.color((int) (255 * 0.8f), (int) (255 * 0.8f), (int) (255 * 0.8f), (int) (255 * 0.8f)),
	};

	// Building mesh
	@Nullable
	private MeshData rebuildCloudMesh(BufferBuilder builder) {

		var client = Minecraft.getInstance();
		Vec3 camVec = null;
		Vec3[] camProjBorder = null;
		double fovCos = 0, extraAngleSin = 0;

		if (CONFIG.getCullMode().equals(CullMode.CIRCULAR)) {
			camVec = new Vec3(
					-Math.sin(Math.toRadians(client.gameRenderer.getMainCamera().getYRot())),
					-Math.tan(Math.toRadians(client.gameRenderer.getMainCamera().getXRot())),
					 Math.cos(Math.toRadians(client.gameRenderer.getMainCamera().getYRot()))
			).normalize();
			fovCos = Math.cos(Math.toRadians(client.options.fov().get() * client.player.getFieldOfViewModifier() * CONFIG.getCullRadianMultiplier()));		//multiplier 2 for better visual.
		} else if (CONFIG.getCullMode().equals(CullMode.RECTANGULAR)) {
			var camProj = client.gameRenderer.getMainCamera().getNearPlane();
			camProjBorder = new Vec3[]{
					camProj.getTopRight().cross(camProj.getTopLeft()).normalize(),			//up
					camProj.getBottomLeft().cross(camProj.getBottomRight()).normalize(),		//down
					camProj.getTopLeft().cross(camProj.getBottomLeft()).normalize(),			//left
					camProj.getBottomRight().cross(camProj.getTopRight()).normalize()		//right
			};
			extraAngleSin = Math.sin(Math.toRadians(client.options.fov().get() * (1.9f - client.player.getFieldOfViewModifier() - CONFIG.getCullRadianMultiplier())));		//increase 0.1 for better visual.
		}

//		builder.clear();
//		builder.begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_TEXTURE_COLOR_NORMAL);
		cullStateShown = 0;
		cullStateSkipped = 0;

		for (CloudData data : cloudDataGroup) {
			try {
				var colorModifier = getCloudColor(client.level.getDayTime(), data);
				int normCount = data.normalList.size();

				for (int i = 0; i < normCount; i++) {
					int normIndex = data.normalList.getByte(i);		// exacting data...
					var nx = normals[normIndex][0];
					var ny = normals[normIndex][1];
					var nz = normals[normIndex][2];
					float[][] verCache = new float[4][3];
					for (int j = 0; j < 4; j++) {
						verCache[j][0] = data.vertexList.getFloat(i * 12 + j * 3) * CONFIG.getCloudBlockSize();
						verCache[j][1] = data.vertexList.getFloat(i * 12 + j * 3 + 1) * CONFIG.getCloudBlockSize() / 2;
						verCache[j][2] = data.vertexList.getFloat(i * 12 + j * 3 + 2) * CONFIG.getCloudBlockSize();
					}

					// Normal Culling
					if (!CONFIG.isEnableNormalCull() || new Vec3(nx, ny, nz).dot(new Vec3(
							verCache[0][0] + 0.01f,
							verCache[0][1] + cloudHeight + 0.01f - CONFIG.getCloudBlockSize() - client.gameRenderer.getMainCamera().getBlockPosition().getY(),
							verCache[0][2] + SFCReMod.RUNTIME.partialOffset + 0.7f
									).normalize()) < 0.003805f) {		// clouds is moving, z-pos isn't precise, so leave some margin

						// Position Culling
						int j = -1;
						while (++j < 4) {
							Vec3 cloudVec = new Vec3(
									verCache[j][0] + 0.01f,
									verCache[j][1] + cloudHeight + 0.01f - CONFIG.getCloudBlockSize() - client.gameRenderer.getMainCamera().getBlockPosition().getY(),
									verCache[j][2] + SFCReMod.RUNTIME.partialOffset + 0.7f
							).normalize();
							boolean isInRange = true;

							if (CONFIG.getCullMode().equals(CullMode.CIRCULAR) && camVec.dot(cloudVec) < fovCos) {
								continue;
							} else if (CONFIG.getCullMode().equals(CullMode.RECTANGULAR)) {
								for (Vec3 plane : camProjBorder)
									if (plane.dot(cloudVec) < extraAngleSin) {
										isInRange = false;
										break;
									}
							}

							if (isInRange) {
								for (int k = 0; k < 4; k++)
									builder.addVertex(verCache[k][0], verCache[k][1], verCache[k][2]).setUv(0.5f, 0.5f).setColor(FastColor.ARGB32.multiply(colors[normIndex], colorModifier)).setNormal(nx, ny, nz);
								break;
							}
						}

						if (j < 4) {
							cullStateShown++;
						} else {
							cullStateSkipped++;
						}
					} else {
						cullStateSkipped++;
					}
				}
			} catch (Exception e) {
				SFCReMod.exceptionCatcher(e);
			}

			if (data.getDataType().equals(CloudDataType.NORMAL)) {
				break;
			} else if (data.getDataType().equals(CloudDataType.TRANS_MID_BODY)) {
				data.tick();
				if (data.getLifeTime() <= 0) {		// Clear if lifetime reach end
					while (!cloudDataGroup.get(0).getDataType().equals(CloudDataType.NORMAL)) {
						cloudDataGroup.remove(0);
					}
				}
				break;		// Only render IN, BODY, OUT till its life end and remove.
			} else {
				data.tick();
			}
		}

		try {
			return builder.build();
		} catch (Exception e) {
			SFCReMod.exceptionCatcher(e);
			return null;
		}
	}

	private void collectCloudData(double scrollX, double scrollZ) {

		CloudData tmp;
		CloudFadeData fadeIn = null, fadeOut = null;
		CloudMidData midBody = null;

		try {
			RUNTIME.checkFullOffset();

			tmp = new CloudData(scrollX, scrollZ, cloudDensityByWeather, cloudDensityByBiome);
			if (!cloudDataGroup.isEmpty() && CONFIG.isEnableSmoothChange()) {
				fadeIn = new CloudFadeData(cloudDataGroup.get(0), tmp, CloudDataType.TRANS_IN);
				fadeOut = new CloudFadeData(tmp, cloudDataGroup.get(0), CloudDataType.TRANS_OUT);
				midBody = new CloudMidData(cloudDataGroup.get(0), tmp, CloudDataType.TRANS_MID_BODY);
			}

			synchronized (this) {
				cloudDataGroup.clear();

				if (midBody != null) {
					cloudDataGroup.add(fadeIn);
					cloudDataGroup.add(fadeOut);
					cloudDataGroup.add(midBody);
				}
				cloudDataGroup.add(tmp);

				this.xScroll = scrollX;
				this.zScroll = scrollZ;
			}
			RUNTIME.checkPartialOffset();
			timeRebuild = CONFIG.getRebuildInterval();		//Instant refresh once to prevent flicker.
		} catch (Exception e) {		// Debug
			SFCReMod.exceptionCatcher(e);
		}

		isProcessingData = false;
	}

	private int getCloudColor(long worldTime, CloudData data) {
		var a = 255;
		var r = (CONFIG.getCloudColor() & 0xFF0000) >> 16;
		var g = (CONFIG.getCloudColor() & 0x00FF00) >> 8;
		var b = (CONFIG.getCloudColor() & 0x0000FF);
		int t = (int) (worldTime % 24000);

		// Alpha changed by cloud type and lifetime
		switch (data.getDataType()) {
			case NORMAL, TRANS_MID_BODY: break;
			case TRANS_IN: a = (int) (a - a * data.getLifeTime() / CONFIG.getNormalRefreshSpeed().getValue() * 5f); break;
			case TRANS_OUT: a = (int) (a * data.getLifeTime() / CONFIG.getNormalRefreshSpeed().getValue() * 5f); break;
		}

		// Color changed by time...
		if (t > 22500 || t < 500) {		//Dawn, scale value in [0, 2000]
			t = t > 22500 ? t - 22000 : t + 1500;
			r = (int) (r * (1 - Math.sin(t / 2000d * Math.PI) / 8));
			g = (int) (g * (1 - (Math.cos((t - 1000) / 2000d * Math.PI) / 1.2 + Math.sin(t / 1000d * Math.PI) / 3) / 2.1));
			b = (int) (b * (1 - (Math.cos((t - 1000) / 2000d * Math.PI) / 1.2 + Math.sin(t / 1000d * Math.PI) / 3) / 1.6));
		} else if (t < 13500 && t > 11500) {		//Dusk, reverse order
			t -= 11500;
			r = (int) (r * (1 - Math.sin(t / 2000d * Math.PI) / 8));
			g = (int) (g * (1 - (Math.cos((t - 1000) / 2000d * Math.PI) / 1.2 - Math.sin(t / 1000d * Math.PI) / 3) / 2.1));
			b = (int) (b * (1 - (Math.cos((t - 1000) / 2000d * Math.PI) / 1.2 - Math.sin(t / 1000d * Math.PI) / 3) / 1.6));
		}

		return FastColor.ARGB32.color(a, r, g, b);
	}

	private float nextDensityStep(float target, float current, float speed) {
		return Math.abs(target - current) > 1f / speed ? (target > current ? current + 1f / speed : current - 1f / speed) : target;
	}

	//Update Setting.
	public void updateConfig(CommonConfig config) {
		normalRefreshSpeed = config.getNormalRefreshSpeed().getValue();
		weatheringRefreshSpeed = config.getWeatherRefreshSpeed().getValue() / 2;
		densityChangingSpeed = config.getDensityChangingSpeed().getValue();
	}
}
