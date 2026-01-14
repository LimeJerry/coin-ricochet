package com.LimeJerry.CoinRicochet.items;

import com.LimeJerry.CoinRicochet.entity.CoinEntity;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public class CoinItem extends Item {

    public CoinItem(Properties props) {
        super(props);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);

        if (!level.isClientSide) {
            CoinEntity coin = new CoinEntity(level, player);

            // 1) 시작 위치: 눈 앞(살짝 앞으로)에서 스폰
            var eye = player.getEyePosition();
            var look = player.getLookAngle();
            var start = eye.add(look.scale(0.35));

            double spawnDown = 0.22; // ✅ 아래로 내리는 정도 (0.12~0.35 추천)
            coin.setPos(start.x, start.y - spawnDown, start.z);

            // start도 동일하게 아래로 내린 값으로 맞추기(중요!)
            start = start.add(0.0, -spawnDown, 0.0);

            // 2) 목표점: "커서 방향으로 D 블록 앞" (이 점을 통과하게 만들기)
            double D = 3.0;                 // 던지는 거리 느낌 (2.0~4.0 추천)
            var target = start.add(look.scale(D));

            // 3) 몇 틱 후에 그 점을 통과할지 (궤적 높이/느낌을 결정)
            int T = 10;                      // 12틱 = 0.6초 (10~16 추천)

            // 4) 중력(가속도) - CoinEntity에서 getGravity()를 건드리지 말고 "기본값"을 가정
            // ThrowableItemProjectile 기본 중력은 보통 0.03 근처라, 일단 0.03으로 두고 튜닝
            double g = 0.03;

            // 5) 초기 속도 계산 (연속 근사식)
            // x,z: dx/T
            // y: (dy + 0.5*g*T^2)/T  -> 처음엔 위로 올라갔다가 내려오면서 target을 통과
            double dx = target.x - start.x;
            double dy = target.y - start.y;
            double dz = target.z - start.z;

            double vx = dx / T;
            double vz = dz / T;
            double vy = (dy + 0.5 * g * T * T) / T;

            coin.setDeltaMovement(vx, vy, vz);
            coin.hasImpulse = true;

            level.addFreshEntity(coin);
        }

        if (!player.getAbilities().instabuild) {
            stack.shrink(1);
        }

        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
    }
}
