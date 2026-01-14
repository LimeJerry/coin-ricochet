package com.LimeJerry.CoinRicochet.client;

import com.LimeJerry.CoinRicochet.CoinRicochet;
import com.LimeJerry.CoinRicochet.network.FireMarksmanPacket;
import com.LimeJerry.CoinRicochet.network.ModNetwork;
import com.LimeJerry.CoinRicochet.registry.ModItems;
import net.minecraft.client.Minecraft;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = CoinRicochet.MOD_ID, value = Dist.CLIENT)
public class ClientAttackHandler {

    @SubscribeEvent
    public static void onKey(InputEvent.InteractionKeyMappingTriggered event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;

        // 공격키(좌클릭)만 가로채기
        if (event.getKeyMapping() != mc.options.keyAttack) return;

        // 메인핸드에 marksman 들고 있으면 "기본 공격/블록파괴" 대신 발사
        if (mc.player.getMainHandItem().is(ModItems.MARKSMAN.get())) {
            event.setCanceled(true);
            mc.player.swing(net.minecraft.world.InteractionHand.MAIN_HAND);
            ModNetwork.CHANNEL.sendToServer(new FireMarksmanPacket());
        }
    }
}