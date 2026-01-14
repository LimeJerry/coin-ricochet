package com.LimeJerry.CoinRicochet.gun;

import com.LimeJerry.CoinRicochet.entity.CoinEntity;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

import java.util.Comparator;
import java.util.List;

public class CoinRicochetHelper {

    // ✅ 리코셰 데미지 최대 제한
    public static final float MAX_RICOCHET_DAMAGE = 12.0f;

    // 리코셰: 노란 레이
    private static final DustParticleOptions YELLOW =
            new DustParticleOptions(new Vector3f(1.0f, 0.95f, 0.2f), 1.0f);

    /** 코인 적중 시: 흰 스파클 + 리코셰(최대 2 타겟) */
    // ✅ 외부에서 보통 호출하는 기본 trigger (체인 기본값 포함)
    public static void trigger(ServerLevel level, Entity attacker, Entity coinEntity, Vec3 coinPos,
                               float incomingDamage, int maxTargets, double radius) {

        // 1) 코인 적중 이펙트(흰 스파클) - 기존 그대로
        //level.sendParticles(ParticleTypes.END_ROD,
        level.sendParticles(ParticleTypes.END_ROD,
                coinPos.x, coinPos.y, coinPos.z,
                16, 0.12, 0.12, 0.12, 0.02);
        level.sendParticles(ParticleTypes.FLASH,
                coinPos.x, coinPos.y, coinPos.z,
                1, 0, 0, 0, 0);

        // ✅ 여기서 “체인 포함 trigger”를 호출해야 함
        trigger(level, attacker, coinEntity, coinPos, incomingDamage, maxTargets, radius, 3);
    }

    // ✅ 실제 로직을 처리하는 trigger (depthLeft 추가)
    public static void trigger(ServerLevel level, Entity attacker, Entity coinEntity, Vec3 coinPos,
                               float incomingDamage, int maxTargets, double radius, int depthLeft) {

        float damage = Math.min(incomingDamage, MAX_RICOCHET_DAMAGE);

        // 주변 엔티티 최대 maxTargets명에게 리코셰
        ricochetToEntities(level, attacker, coinEntity, coinPos, damage, maxTargets, radius, depthLeft);
    }

    private static void ricochetToEntities(ServerLevel level, Entity attacker, Entity coinEntity, Vec3 from,
                                           float damage, int maxTargets, double radius, int depthLeft) {

        AABB area = new AABB(from, from).inflate(radius);

        List<Entity> targets = level.getEntities(attacker, area, e ->
                e.isAlive()
                        && e.isPickable()
                        && !(e instanceof Player)      // ✅ 모든 플레이어 제외
                        && e != attacker
                        && e != coinEntity
                        //&& !(e instanceof CoinEntity)  // ✅ 다른 코인 제외
        );

        targets.sort(Comparator.comparingDouble(e -> e.distanceToSqr(from)));

        DamageSource ricochetSource = makeRicochetDamageSource(level, attacker);

        int hitCount = 0;
        for (Entity target : targets) {
            if (hitCount >= maxTargets) break;

            Vec3 to = target.getBoundingBox().getCenter();
            if (!hasLineOfSight(level, from, to)) continue;

            spawnCoinTracer(level, from, to);

            // ✅ 타겟이 코인이라면: 데미지 주는 대신 "다음 리코셰" 발동
            if (target instanceof com.LimeJerry.CoinRicochet.entity.CoinEntity nextCoin) {
                if (depthLeft > 0) {
                    // 코인 적중용 흰색 스파클을 nextCoin 위치에 뿌리고,
                    // nextCoin이 또 주변으로 쏘게 함 (damage는 그대로 / 혹은 살짝 감쇠도 가능)
                    trigger(level, attacker, nextCoin, nextCoin.position(), damage, maxTargets, radius, depthLeft - 1);
                }
                nextCoin.discard(); // “맞은 코인은 사라짐” 컨셉이면 유지
                hitCount++;
                continue;
            }

            // 일반 엔티티는 데미지
            if (damage > 0.0f) {
                target.hurt(makeRicochetDamageSource(level, attacker), damage);
            }

            hitCount++;
        }
    }
    private static final DustParticleOptions YELLOW_DUST =
            new DustParticleOptions(new Vector3f(1.0f, 0.92f, 0.20f), 1.0f);

    private static DamageSource makeRicochetDamageSource(ServerLevel level, Entity attacker) {
        if (attacker instanceof Player p) {
            return level.damageSources().playerAttack(p);
        }
        if (attacker instanceof LivingEntity le) {
            return level.damageSources().mobAttack(le);
        }
        // attacker가 null/비생물일 수 있음
        return level.damageSources().generic();
    }

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

    private static void spawnCoinTracer(ServerLevel level, Vec3 from, Vec3 to) {
        Vec3 delta = to.subtract(from);
        double len = delta.length();
        if (len < 0.01) return;

        Vec3 dir = delta.scale(1.0 / len);

        int steps = Math.min(28, Math.max(10, (int)(len * 4)));
        for (int i = 1; i <= steps; i++) {
            double t = (double) i / steps;
            Vec3 p = from.add(dir.scale(len * t));

            // 1) wax_off 파편감 (색은 못 바뀜)
            level.sendParticles(ParticleTypes.WAX_OFF,
                    p.x, p.y, p.z,
                    1,          // 개수
                    0.01, 0.01, 0.01, // 약간만 퍼지게
                    0.0
            );

            // 2) 노란색을 담당하는 dust (같은 점에 같이 뿌리기)
//            level.sendParticles(YELLOW_DUST,
//                    p.x, p.y, p.z,
//                    1,
//                    0, 0, 0,
//                    0.0
//            );
            level.sendParticles(ParticleTypes.WAX_ON,
                    p.x, p.y, p.z,
                    1,          // 개수
                    0.1, 0.1, 0.1, // 약간만 퍼지게
                    0.0
            );
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
