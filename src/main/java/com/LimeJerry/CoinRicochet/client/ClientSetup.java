package com.LimeJerry.CoinRicochet.client;

import com.LimeJerry.CoinRicochet.registry.ModEntities;
import net.minecraft.client.renderer.entity.EntityRenderers;
import net.minecraft.client.renderer.entity.ThrownItemRenderer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;

import static com.LimeJerry.CoinRicochet.CoinRicochet.MOD_ID;

@Mod.EventBusSubscriber(modid = MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class ClientSetup {

    @SubscribeEvent
    public static void onClientSetup(FMLClientSetupEvent event) {
        // 던져진 코인을 "던져진 아이탬 처럼 랜더하기
        EntityRenderers.register(ModEntities.COIN.get(), context -> new ThrownItemRenderer<>(context));
    }
}
