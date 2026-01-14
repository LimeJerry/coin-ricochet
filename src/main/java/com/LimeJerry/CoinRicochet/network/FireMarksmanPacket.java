package com.LimeJerry.CoinRicochet.network;

import com.LimeJerry.CoinRicochet.entity.CoinEntity;
import com.LimeJerry.CoinRicochet.registry.ModItems;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.NetworkEvent;

import java.util.List;
import java.util.function.Supplier;

public class FireMarksmanPacket {

    // 데이터 없음 (그냥 "발사 요청" 신호만)
    public static void encode(FireMarksmanPacket msg, FriendlyByteBuf buf) {}
    public static FireMarksmanPacket decode(FriendlyByteBuf buf) { return new FireMarksmanPacket(); }

    public static void handle(FireMarksmanPacket msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ctx.get().getSender();
            if (player == null) return;

            // ✅ 메인핸드에 Marksman 들고 있을 때만
            if (!player.getMainHandItem().is(ModItems.MARKSMAN.get())) return;

            if (!(player.level() instanceof ServerLevel serverLevel)) return;

            // ===== 설정 =====
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

            // ===== 궤적(연기) =====
            int steps = 14;
            for (int i = 1; i <= steps; i++) {
                double t = (double) i / steps;
                Vec3 p = start.add(dir.scale(range * t));
                serverLevel.sendParticles(ParticleTypes.SMOKE, p.x, p.y, p.z,
                        1, 0, 0, 0, 0);
            }

            // ===== 히트 판정 =====
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

            if (best != null) {
                // ✅ 코인이면: "팅" + 반짝 + 삭제 (좌클릭에서도 소리 나도록 여기서 처리)
                if (best instanceof CoinEntity coin) {
                    Vec3 hitPos = coin.position();

                    serverLevel.playSound(null, hitPos.x, hitPos.y, hitPos.z,
                            SoundEvents.ANVIL_PLACE, SoundSource.PLAYERS,
                            0.35f, 2.0f);

                    serverLevel.sendParticles(ParticleTypes.CRIT, hitPos.x, hitPos.y, hitPos.z,
                            10, 0.08, 0.08, 0.08, 0.02);

                    coin.discard();
                } else {
                    // 일단 간단하게 1 데미지
                    best.hurt(serverLevel.damageSources().playerAttack(player), 1.0f);
                }
            }
        });

        ctx.get().setPacketHandled(true);
    }
}
