package com.LimeJerry.CoinRicochet.network;

import com.LimeJerry.CoinRicochet.entity.CoinEntity;
import com.LimeJerry.CoinRicochet.items.GunItem;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.NetworkEvent;
import org.joml.Vector3f;

import java.util.Comparator;
import java.util.List;
import java.util.function.Supplier;

public class FireGunPacket {

    /* ===================== PACKET ===================== */

    public static void encode(FireGunPacket msg, FriendlyByteBuf buf) {}
    public static FireGunPacket decode(FriendlyByteBuf buf) { return new FireGunPacket(); }

    public static void handle(FireGunPacket msg, Supplier<NetworkEvent.Context> ctx) {
        NetworkEvent.Context context = ctx.get();
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player == null) return;
            if (!(player.level() instanceof ServerLevel level)) return;

            // 총 체크
            if (!(player.getMainHandItem().getItem() instanceof GunItem gun)) return;

            double range = gun.range();
            double thickness = gun.thickness();
            float damage = gun.damage();

            Vec3 start = player.getEyePosition();
            Vec3 dir = player.getLookAngle();
            Vec3 end = start.add(dir.scale(range));

            // 총 발사 사운드
            level.playSound(null,
                    player.getX(), player.getY(), player.getZ(),
                    gun.shootSound(),
                    SoundSource.PLAYERS,
                    gun.shootVolume(),
                    gun.shootPitch()
            );

            // 히트스캔
            Entity hit = raycast(level, player, start, end, thickness);

            if (hit instanceof CoinEntity coin) {
                Vec3 hitPos = coin.position();

                // ✅ 코인 적중: 흰 자수정 스파클
                spawnCoinHitEffect(level, hitPos);

                // ✅ 리코셰: 주변 엔티티 최대 2명 (노란 레이)
                ricochetToEntities(level, player, coin, hitPos, damage, 2, 16.0);

                coin.discard();
            }
            else if (hit != null) {
                hit.hurt(level.damageSources().playerAttack(player), damage);
            }
        });

        context.setPacketHandled(true);
    }

    /* ===================== HITSCAN ===================== */

    private static Entity raycast(ServerLevel level, ServerPlayer shooter,
                                  Vec3 start, Vec3 end, double thickness) {

        AABB box = new AABB(start, end).inflate(thickness);
        List<Entity> list = level.getEntities(shooter, box, e ->
                e.isAlive() && e.isPickable() && e != shooter
        );

        Entity best = null;
        double bestDist = Double.MAX_VALUE;

        for (Entity e : list) {
            var clip = e.getBoundingBox().inflate(0.3).clip(start, end);
            if (clip.isPresent()) {
                double d = start.distanceToSqr(clip.get());
                if (d < bestDist) {
                    bestDist = d;
                    best = e;
                }
            }
        }
        return best;
    }

    /* ===================== RICOCHET ===================== */

    private static void ricochetToEntities(ServerLevel level, ServerPlayer shooter,
                                           Entity coinEntity, Vec3 from,
                                           float damage, int maxTargets, double radius) {

        AABB area = new AABB(from, from).inflate(radius);

        List<Entity> targets = level.getEntities(shooter, area, e ->
                e.isAlive()
                        && e.isPickable()
                        && !(e instanceof Player)
                        && e != shooter
                        && e != coinEntity
                        && !(e instanceof CoinEntity)
        );

        targets.sort(Comparator.comparingDouble(e -> e.distanceToSqr(from)));

        int hitCount = 0;
        for (Entity target : targets) {
            if (hitCount >= maxTargets) break;

            Vec3 to = target.getBoundingBox().getCenter();
            if (!hasLineOfSight(level, from, to)) continue;

            spawnYellowTracer(level, from, to);
            target.hurt(level.damageSources().playerAttack(shooter), damage);

            hitCount++;
        }
    }

    /* ===================== EFFECTS ===================== */

    // 코인 적중: 흰 자수정 느낌
    private static void spawnCoinHitEffect(ServerLevel level, Vec3 pos) {
        level.sendParticles(ParticleTypes.END_ROD,
                pos.x, pos.y, pos.z,
                16, 0.12, 0.12, 0.12, 0.02);

        level.sendParticles(ParticleTypes.ELECTRIC_SPARK,
                pos.x, pos.y, pos.z,
                10, 0.12, 0.12, 0.12, 0.03);

        level.sendParticles(ParticleTypes.FLASH,
                pos.x, pos.y, pos.z,
                1, 0, 0, 0, 0);
    }

    // 리코셰: 노란 레이
    private static final DustParticleOptions YELLOW =
            new DustParticleOptions(new Vector3f(1.0f, 0.95f, 0.2f), 1.0f);

    private static void spawnYellowTracer(ServerLevel level, Vec3 from, Vec3 to) {
        Vec3 delta = to.subtract(from);
        double len = delta.length();
        if (len < 0.01) return;

        Vec3 dir = delta.scale(1.0 / len);
        int steps = Math.min(24, Math.max(8, (int)(len * 3)));

        for (int i = 1; i <= steps; i++) {
            double t = (double) i / steps;
            Vec3 p = from.add(dir.scale(len * t));
            level.sendParticles(YELLOW, p.x, p.y, p.z, 1, 0, 0, 0, 0);
        }
    }

    private static boolean hasLineOfSight(ServerLevel level, Vec3 from, Vec3 to) {
        var ctx = new net.minecraft.world.level.ClipContext(
                from, to,
                net.minecraft.world.level.ClipContext.Block.COLLIDER,
                net.minecraft.world.level.ClipContext.Fluid.NONE,
                null
        );
        return level.clip(ctx).getType() == net.minecraft.world.phys.HitResult.Type.MISS;
    }
}
