package com.LimeJerry.CoinRicochet.entity;

import com.LimeJerry.CoinRicochet.registry.ModEntities;
import com.LimeJerry.CoinRicochet.registry.ModItems;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.ThrowableItemProjectile;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;

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

        boolean projectileHit =
                source.getDirectEntity() instanceof net.minecraft.world.entity.projectile.Projectile;

        boolean hitscanLike =
                source.getDirectEntity() == null; // 일부 총 모드

        if (projectileHit || hitscanLike) {
            // ✅ 맞는 순간 효과
            level().playSound(
                    null, getX(), getY(), getZ(),
                    net.minecraft.sounds.SoundEvents.ANVIL_PLACE,
                    net.minecraft.sounds.SoundSource.PLAYERS,
                    0.35f,
                    2.0f
            );

            if (level() instanceof net.minecraft.server.level.ServerLevel serverLevel) {
                serverLevel.sendParticles(
                        net.minecraft.core.particles.ParticleTypes.CRIT,
                        getX(), getY(), getZ(),
                        10, 0.08, 0.08, 0.08, 0.02
                );
            }

            this.discard();
            return true;
        }

        return false;
    }
}
