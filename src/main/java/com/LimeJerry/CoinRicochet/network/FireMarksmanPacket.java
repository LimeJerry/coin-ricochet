package com.LimeJerry.CoinRicochet.network;

import com.LimeJerry.CoinRicochet.entity.CoinEntity;
import com.LimeJerry.CoinRicochet.registry.ModItems;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.NetworkEvent;

import java.util.List;
import java.util.function.Supplier;

public class FireMarksmanPacket {

    public static void encode(FireMarksmanPacket msg, FriendlyByteBuf buf) {}
    public static FireMarksmanPacket decode(FriendlyByteBuf buf) { return new FireMarksmanPacket(); }

    public static void handle(FireMarksmanPacket msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            Player player = ctx.get().getSender();
            if (player == null) return;

            // ✅ 서버에서 "진짜 들고 있는지" 확인
            if (!player.getMainHandItem().is(ModItems.MARKSMAN.get())) return;

            if (!(player.level() instanceof ServerLevel serverLevel)) return;

            // ===== 히트스캔 =====
            double range = 40.0;
            double thickness = 0.6;

            Vec3 start = player.getEyePosition();
            Vec3 dir = player.getLookAngle();
            Vec3 end = start.add(dir.scale(range));

            // ===== 총소리 + 총구 연기 =====
            serverLevel.playSound(null, player.getX(), player.getY(), player.getZ(),
                    SoundEvents.CROSSBOW_SHOOT, SoundSource.PLAYERS, 1.0f, 1.2f);

            Vec3 muzzle = start.add(dir.scale(0.6));
            serverLevel.sendParticles(ParticleTypes.SMOKE, muzzle.x, muzzle.y, muzzle.z,
                    6, 0.03, 0.03, 0.03, 0.01);
            serverLevel.sendParticles(ParticleTypes.CRIT, muzzle.x, muzzle.y, muzzle.z,
                    2, 0.02, 0.02, 0.02, 0.01);

            // ===== 연기 궤적 =====
            int steps = 14;
            for (int i = 1; i <= steps; i++) {
                double t = (double) i / steps;
                Vec3 p = start.add(dir.scale(range * t));
                serverLevel.sendParticles(ParticleTypes.SMOKE, p.x, p.y, p.z,
                        1, 0, 0, 0, 0);
            }

            // ===== 엔티티 판정 =====
            AABB box = new AABB(start, end).inflate(thickness);
            List<Entity> hits = serverLevel.getEntities(player, box, e ->
                    e.isAlive() && e.isPickable() && e != player
            );

            Entity best = null;
            double bestDist2 = Double.MAX_VALUE;

            for (Entity e : hits) {
                AABB eb = e.getBoundingBox().inflate(0.35);
                var clip = eb.clip(start, end);
                if (clip.isPresent()) {
                    double d2 = start.distanceToSqr(clip.get());
                    if (d2 < bestDist2) {
                        bestDist2 = d2;
                        best = e;
                    }
                }
            }

            // ✅ “아무 엔티티든” 맞으면 됨: 일단 데미지로 처리(근접 아닌 히트스캔)
            // 코인은 CoinEntity.hurt에서 투사체 판정만 받게 해놨으니,
            // 여기서는 "투사체 타입" 데미지로 주는 게 가장 호환 좋음.
            if (best != null) {
                if (best instanceof CoinEntity coin) {
                    // ✅ 코인은 히트스캔이면 즉시 삭제 (확실)
                    coin.discard();

                    // (원하면 여기서 "팅" 사운드도 재생)
                    // serverLevel.playSound(null, coin.getX(), coin.getY(), coin.getZ(), ...);

                } else {
                    // ✅ 코인 말고 다른 엔티티는 그냥 데미지
                    best.hurt(serverLevel.damageSources().playerAttack(player), 1.0f);
                }
            }
        });

        ctx.get().setPacketHandled(true);
    }
}