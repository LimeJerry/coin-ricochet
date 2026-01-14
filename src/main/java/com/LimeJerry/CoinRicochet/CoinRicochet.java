package com.LimeJerry.CoinRicochet;

import com.LimeJerry.CoinRicochet.registry.ModEntities;
import com.LimeJerry.CoinRicochet.registry.ModItems;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

@Mod(CoinRicochet.MOD_ID)
public class CoinRicochet {

    public static final String MOD_ID = "coin_ricochet";

    public CoinRicochet() {
        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();

        ModItems.ITEMS.register(modEventBus);
        ModEntities.ENTITIES.register(modEventBus);
    }
}
