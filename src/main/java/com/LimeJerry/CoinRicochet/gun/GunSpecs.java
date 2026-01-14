package com.LimeJerry.CoinRicochet.gun;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;

public class GunSpecs {

    // 총 ID (나중에 다른 총도 여기에 추가)
    public static final ResourceLocation MARKSMAN =
            new ResourceLocation("coin_ricochet", "marksman");

    public static GunSpec get(ResourceLocation id) {

        if (id.equals(MARKSMAN)) {
            return new GunSpec(
                    10.0f,              // 데미지
                    40.0,               // 사거리
                    0.6,                // 히트박스 두께
                    SoundEvents.CROSSBOW_SHOOT, // 발사 사운드
                    1.0f,               // 볼륨
                    1.2f                // 피치
            );
        }

        // 기본값 (안전장치)
        return new GunSpec(
                1.0f,
                30.0,
                0.6,
                SoundEvents.ARROW_SHOOT,
                1.0f,
                1.0f
        );
    }
}