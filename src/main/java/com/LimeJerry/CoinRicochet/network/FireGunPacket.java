package com.LimeJerry.CoinRicochet.network;

import com.LimeJerry.CoinRicochet.entity.CoinEntity;
import com.LimeJerry.CoinRicochet.items.GunItem;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.NetworkEvent;

import java.util.List;
import java.util.function.Supplier;

public class FireGunPacket {

    // 데이터 없는 패킷: "발사 요청"만 전달
    public static void encode(FireGunPacket msg, FriendlyByteBuf buf) {}
    public static FireGunPacket decode(FriendlyByteBuf buf) { return new FireGunPacket(); }

    public static void handle(FireGunPacket msg, Supplier<NetworkEvent.Context> ctx) {
        NetworkEvent.Context context = ctx.get();
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player == null) return;

            if (!(player.level() instanceof ServerLevel serverLevel)) return;

            // ✅ 메인핸드가 GunItem일 때만 발사 허용 (총 공용화)
            var held = player.getMainHandItem().getItem();
            if (!(held instanceof GunItem gun)) return;

            // ✅ 총 스펙은 GunItem에서 읽기 (총마다 override 가능)
            double range = gun.range();
            double thickness = gun.thickness();
            float damage = gun.damage();

            Vec3 start = player.getEyePosition();
            Vec3 dir = player.getLookAngle();
            Vec3 end = start.add(dir.scale(range));

            // ✅ 발사 사운드도 총마다 바꿀 수 있게 GunItem에서 읽기
            serverLevel.playSound(
                    null,
                    player.getX(), player.getY(), player.getZ(),
                    gun.shootSound(),
                    SoundSource.PLAYERS,
                    gun.shootVolume(),
                    gun.shootPitch()
            );

            // ✅ 총구 연기 (공통, 나중에 총마다 커스터마이즈 가능)
            Vec3 muzzle = start.add(dir.scale(0.6));
            serverLevel.sendParticles(ParticleTypes.SMOKE, muzzle.x, muzzle.y, muzzle.z,
                    6, 0.03, 0.03, 0.03, 0.01);

            // ✅ 궤적 연기 (공통)
            int steps = 14;
            for (int i = 1; i <= steps; i++) {
                double t = (double) i / steps;
                Vec3 p = start.add(dir.scale(range * t));
                serverLevel.sendParticles(ParticleTypes.SMOKE, p.x, p.y, p.z,
                        1, 0, 0, 0, 0);
            }

            // ✅ 엔티티 히트 판정
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

            // ✅ 히트 처리
            if (best != null) {
                // 코인 맞으면: '팅' + 크리티컬 파티클 + 삭제 (좌클릭 히트스캔 경로에서도 확실)
                if (best instanceof CoinEntity coin) {
                    Vec3 hitPos = coin.position();

                    serverLevel.playSound(
                            null,
                            hitPos.x, hitPos.y, hitPos.z,
                            net.minecraft.sounds.SoundEvents.ANVIL_PLACE,
                            SoundSource.PLAYERS,
                            0.35f,
                            2.0f
                    );

                    serverLevel.sendParticles(ParticleTypes.CRIT, hitPos.x, hitPos.y, hitPos.z,
                            10, 0.08, 0.08, 0.08, 0.02);

                    coin.discard();
                } else {
                    // ✅ 총 데미지는 gun.getDamage() 사용 (예: 10)
                    best.hurt(serverLevel.damageSources().playerAttack(player), damage);
                }
            }
        });

        context.setPacketHandled(true);
    }
}
