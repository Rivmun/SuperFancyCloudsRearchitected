package com.rimo.sfcr.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import com.rimo.sfcr.SFCReMod;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.LevelRenderer;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LevelRenderer.class)
public abstract class LevelRendererMixin {

	@Shadow
	private @Nullable ClientLevel level;

	@Inject(method = "renderClouds*", at = @At("HEAD"), cancellable = true)
	public void renderSFC(PoseStack poseStack, Matrix4f matrix4f, Matrix4f matrix4f2, float f, double d, double e, double g, CallbackInfo ci) {
		if (SFCReMod.COMMON_CONFIG.isEnableMod() && level.dimensionType().hasSkyLight()) {
			SFCReMod.RENDERER.render(level, poseStack, matrix4f, matrix4f2, f, d, e, g);
			ci.cancel();
			return;
		}
	}
}
