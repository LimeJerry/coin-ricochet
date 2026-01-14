package com.LimeJerry.CoinRicochet.registry;

import com.LimeJerry.CoinRicochet.CoinRicochet;
import com.LimeJerry.CoinRicochet.items.CoinItem;
import com.LimeJerry.CoinRicochet.items.MarksmanItem;
import net.minecraft.world.item.Item;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModItems {

    public static final DeferredRegister<Item> ITEMS =
            DeferredRegister.create(ForgeRegistries.ITEMS, CoinRicochet.MOD_ID);

    public static final RegistryObject<Item> COIN =
            ITEMS.register("coin",
                    () -> new CoinItem(new Item.Properties().stacksTo(16)));
    public static final RegistryObject<Item> MARKSMAN =
            ITEMS.register("marksman", () -> new MarksmanItem(new Item.Properties().stacksTo(1)));
}
