package com.LimeJerry.CoinRicochet.items;

import com.LimeJerry.CoinRicochet.entity.CoinEntity;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;

import java.util.List;

public class MarksmanItem extends Item {
    public MarksmanItem(Properties props) { super(props); }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);

        // ✅ 양손 동시 use로 인한 꼬임 방지
        if (hand != InteractionHand.MAIN_HAND) {
            return InteractionResultHolder.pass(stack);
        }

        if (!level.isClientSide && level instanceof ServerLevel serverLevel) {
            double range = 40.0;
            double thickness = 0.6; // ✅ 코인 작으니 조금 두껍게(0.5~0.8 추천)

            Vec3 start = player.getEyePosition();
            Vec3 dir = player.getLookAngle();
            Vec3 end = start.add(dir.scale(range));

            // 1) "탕!" 사운드 (플레이어 위치에서)
            serverLevel.playSound(
                    null,
                    player.getX(), player.getY(), player.getZ(),
                    SoundEvents.CROSSBOW_SHOOT,
                    SoundSource.PLAYERS,
                    1.0f,
                    1.2f  // 피치 올리면 총 느낌 조금 남
            );

            // 2) 총구 연기(눈 앞 약간)
            Vec3 muzzle = start.add(dir.scale(0.6));
            serverLevel.sendParticles(ParticleTypes.SMOKE, muzzle.x, muzzle.y, muzzle.z,
                    6, 0.03, 0.03, 0.03, 0.01);
            serverLevel.sendParticles(ParticleTypes.CRIT, muzzle.x, muzzle.y, muzzle.z,
                    2, 0.02, 0.02, 0.02, 0.01);

            // 3) 연기 궤적(레이 따라 점점 뿌리기)
            int steps = 14;
            for (int i = 1; i <= steps; i++) {
                double t = (double) i / steps;
                Vec3 p = start.add(dir.scale(range * t));
                serverLevel.sendParticles(ParticleTypes.SMOKE, p.x, p.y, p.z,
                        1, 0.0, 0.0, 0.0, 0.0);
            }

            // 4) 엔티티 히트 판정(작은 엔티티 맞추기 쉽게)
            AABB box = new AABB(start, end).inflate(thickness);
            List<Entity> hits = level.getEntities(player, box, e ->
                    e.isAlive() && e.isPickable() && e != player
            );

            EntityHitResult best = null;
            double bestDist2 = Double.MAX_VALUE;

            for (Entity e : hits) {
                // 코인/몹 모두 조금 더 크게 잡히게
                AABB eb = e.getBoundingBox().inflate(0.35);
                var clip = eb.clip(start, end);
                if (clip.isPresent()) {
                    double d2 = start.distanceToSqr(clip.get());
                    if (d2 < bestDist2) {
                        bestDist2 = d2;
                        best = new EntityHitResult(e, clip.get());
                    }
                }
            }

            // 5) 코인 맞으면 확실히 서버에서 삭제 + "팅" 느낌 사운드 추가
            if (best != null && best.getEntity() instanceof CoinEntity coin) {
                Vec3 hitPos = best.getLocation();

                serverLevel.playSound(
                        null,
                        hitPos.x, hitPos.y, hitPos.z,
                        SoundEvents.ANVIL_PLACE,   // 금속 '팅' 느낌 (원하면 다른 걸로 교체)
                        SoundSource.PLAYERS,
                        0.35f,
                        2.0f
                );

                serverLevel.sendParticles(ParticleTypes.CRIT, hitPos.x, hitPos.y, hitPos.z,
                        10, 0.08, 0.08, 0.08, 0.02);

                coin.discard(); // ✅ 서버에서 삭제해야 클라에도 사라짐
            }
        }

        return InteractionResultHolder.pass(player.getItemInHand(hand));
    }
}
