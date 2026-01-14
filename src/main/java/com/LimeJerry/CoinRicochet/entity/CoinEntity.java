package com.LimeJerry.CoinRicochet.entity;

import com.LimeJerry.CoinRicochet.gun.CoinRicochetHelper;
import com.LimeJerry.CoinRicochet.registry.ModEntities;
import com.LimeJerry.CoinRicochet.registry.ModItems;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.ThrowableItemProjectile;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

public class CoinEntity extends ThrowableItemProjectile {

    public CoinEntity(EntityType<? extends CoinEntity> type, Level level) {
        super(type, level);
    }

    public CoinEntity(Level level, LivingEntity owner) {
        super(ModEntities.COIN.get(), owner, level);
    }

    @Override
    protected Item getDefaultItem() {
        return ModItems.COIN.get();
    }

    @Override
    protected void onHitEntity(net.minecraft.world.phys.EntityHitResult result) {
        super.onHitEntity(result);
        this.discard();
    }

    @Override
    protected void onHitBlock(net.minecraft.world.phys.BlockHitResult result) {
        super.onHitBlock(result);
        this.discard();
    }

    @Override
    public boolean isPickable() {
        return true;
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (level().isClientSide) return true;
        if (!(level() instanceof ServerLevel serverLevel)) return true;

        // ✅ 화살/눈덩이 등 투사체가 directEntity
        boolean projectileHit = source.getDirectEntity() instanceof net.minecraft.world.entity.projectile.Projectile;

        // ✅ 총(히트스캔)은 FireGunPacket에서 처리할 거라면 여기선 굳이 안 잡아도 됨
        if (projectileHit) {
            Entity attacker = source.getEntity(); // 보통 '쏜 사람/몹' (없을 수도 있음)
            Vec3 pos = this.position();

            // ✅ 리코셰 발동 (amount = 투사체 데미지, 단 최대 12로 캡됨)
            CoinRicochetHelper.trigger(serverLevel, attacker, this, pos, amount, 2, 16.0);

            this.discard();
            return true;
        }

        return false;
    }
}
