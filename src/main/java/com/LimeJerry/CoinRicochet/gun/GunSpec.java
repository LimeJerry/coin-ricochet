package com.LimeJerry.CoinRicochet.gun;

import net.minecraft.sounds.SoundEvent;

public record GunSpec(
        float damage,
        double range,
        double thickness,
        SoundEvent shootSound,
        float shootVolume,
        float shootPitch
) {}
