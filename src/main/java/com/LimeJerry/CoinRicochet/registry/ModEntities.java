package com.LimeJerry.CoinRicochet.registry;

import com.LimeJerry.CoinRicochet.CoinRicochet;
import com.LimeJerry.CoinRicochet.entity.CoinEntity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModEntities {

    public static final DeferredRegister<EntityType<?>> ENTITIES =
            DeferredRegister.create(ForgeRegistries.ENTITY_TYPES, CoinRicochet.MOD_ID);

    public static final RegistryObject<EntityType<CoinEntity>> COIN =
            ENTITIES.register("coin",
                    () -> EntityType.Builder
                            .<CoinEntity>of(CoinEntity::new, MobCategory.MISC)
                            .sized(0.25f, 0.25f)
                            .build("coin"));
}
