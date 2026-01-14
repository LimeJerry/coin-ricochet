package com.LimeJerry.CoinRicochet.network;

import com.LimeJerry.CoinRicochet.CoinRicochet;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;

public class ModNetwork {
    private static final String PROTOCOL = "1";
    public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            new ResourceLocation(CoinRicochet.MOD_ID, "main"),
            () -> PROTOCOL, PROTOCOL::equals, PROTOCOL::equals
    );

    private static int id = 0;
    private static int nextId() { return id++; }

    public static void register() {
        CHANNEL.messageBuilder(FireGunPacket.class, nextId())
                .encoder(FireGunPacket::encode)
                .decoder(FireGunPacket::decode)
                .consumerMainThread(FireGunPacket::handle)
                .add();
    }
}