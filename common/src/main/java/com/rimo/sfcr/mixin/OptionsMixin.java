package com.rimo.sfcr.mixin;

import com.rimo.sfcr.SFCReMod;
import net.minecraft.client.OptionInstance;
import net.minecraft.client.Options;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Options.class)
public abstract class OptionsMixin {

	@Final
	@Shadow
	private OptionInstance<Integer> renderDistance;

	//Update cloudRenderDistance when view distance is changed.
	@Inject(method = "save", at = @At("RETURN"), cancellable = true)
	private void updateCloudRenderDistance(CallbackInfo ci) {
		if (SFCReMod.COMMON_CONFIG.isCloudRenderDistanceFitToView()) {
			SFCReMod.COMMON_CONFIG.setCloudRenderDistance(renderDistance.get() * 12);
			SFCReMod.COMMON_CONFIG.save();
		}
		ci.cancel();
	}
}
