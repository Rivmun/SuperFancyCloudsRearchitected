package com.rimo.sfcr.util;

import net.minecraft.network.chat.Component;

public enum CloudRefreshSpeed {
	VERY_SLOW,
	SLOW,
	NORMAL,
	FAST,
	VERY_FAST;

	public int getValue() {
		switch (this) {
			case VERY_FAST -> {return 5;}
			case FAST -> {return 10;}
			case NORMAL -> {return 20;}
			case SLOW -> {return 30;}
			case VERY_SLOW -> {return 40;}
		}
		return 20;
	}

	public Component getName() {
		switch (this) {
			case VERY_FAST -> {return Component.translatable("text.sfcr.enum.cloudRefreshSpeed.VERY_FAST");}
			case FAST -> {return Component.translatable("text.sfcr.enum.cloudRefreshSpeed.FAST");}
			case NORMAL -> {return Component.translatable("text.sfcr.enum.cloudRefreshSpeed.NORMAL");}
			case SLOW -> {return Component.translatable("text.sfcr.enum.cloudRefreshSpeed.SLOW");}
			case VERY_SLOW -> {return Component.translatable("text.sfcr.enum.cloudRefreshSpeed.VERY_SLOW");}
		}
		return Component.nullToEmpty("");
	}
}
