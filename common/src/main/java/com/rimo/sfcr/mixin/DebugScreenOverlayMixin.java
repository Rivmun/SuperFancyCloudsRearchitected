package com.rimo.sfcr.mixin;

import com.rimo.sfcr.SFCReMod;
import net.minecraft.client.gui.components.DebugScreenOverlay;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;

@Mixin(DebugScreenOverlay.class)
public abstract class DebugScreenOverlayMixin {
	@Inject(method = "getGameInformation", at = @At("RETURN"), cancellable = true)
	public void getLeftText(CallbackInfoReturnable<List<String>> callback) {
		List<String> list = callback.getReturnValue();

		// Add Debug Strings
		if (SFCReMod.COMMON_CONFIG.isEnableMod())
			list.add("[SFCR] Mesh Built: " +
					 SFCReMod.RENDERER.cullStateShown + " / " +
					(SFCReMod.RENDERER.cullStateSkipped + SFCReMod.RENDERER.cullStateShown) + " faces, " +
					 SFCReMod.RENDERER.cullStateSkipped + " Skipped.");

		callback.setReturnValue(list);
	}
}
