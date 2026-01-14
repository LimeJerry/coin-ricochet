package com.LimeJerry.CoinRicochet.items;

import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.item.Item;

public class GunItem extends Item {

    public GunItem(Properties props) { super(props); }

    public float damage() { return 10.0f; }
    public double range() { return 40.0; }
    public double thickness() { return 0.6; }

    public net.minecraft.sounds.SoundEvent shootSound() { return net.minecraft.sounds.SoundEvents.CROSSBOW_SHOOT; }
    public float shootVolume() { return 1.0f; }
    public float shootPitch() { return 1.2f; }
}