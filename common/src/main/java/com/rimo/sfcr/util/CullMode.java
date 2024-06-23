package com.rimo.sfcr.util;

import net.minecraft.network.chat.Component;

public enum CullMode {
	NONE,
	CIRCULAR,
	RECTANGULAR;

	public Component getName() {
		switch (this) {
			case NONE -> {return Component.translatable("text.sfcr.disabled");}
			case CIRCULAR -> {return Component.translatable("text.sfcr.enum.cullMode.circular");}
			case RECTANGULAR -> {return Component.translatable("text.sfcr.enum.cullMode.rectangular");}
		}
		return null;
	}
}
