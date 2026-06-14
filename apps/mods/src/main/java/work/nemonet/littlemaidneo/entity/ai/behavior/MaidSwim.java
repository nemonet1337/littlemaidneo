package work.nemonet.littlemaidneo.entity.ai.behavior;

import com.google.common.collect.ImmutableMap;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.behavior.Behavior;
import work.nemonet.littlemaidneo.entity.LittleMaidEntity;

/**
 * メイドさん用の水泳挙動。
 * 水の上で跳ねるのを防ぎ、ドラウンドのように静かに浮く。
 * ターゲットが自分より下にいる時は潜り、呼吸が苦しい時や頭部水没時は浮上する。
 */
public class MaidSwim extends Behavior<LittleMaidEntity> {
    private final float chance;

    public MaidSwim(float chance) {
        super(ImmutableMap.of());
        this.chance = chance;
    }

    @Override
    protected boolean checkExtraStartConditions(ServerLevel level, LittleMaidEntity entity) {
        return entity.isInWater() || entity.isInLava();
    }

    @Override
    protected boolean canStillUse(ServerLevel level, LittleMaidEntity entity, long gameTime) {
        return this.checkExtraStartConditions(level, entity);
    }

    @Override
    protected void tick(ServerLevel level, LittleMaidEntity entity, long gameTime) {
        if (entity.getRandom().nextFloat() < this.chance) {
            if (entity.isInLava()) {
                entity.getJumpControl().jump();
            } else if (entity.isInWater()) {
                LivingEntity target = entity.getTarget();
                boolean shouldDive = false;
                if (target != null) {
                    // ターゲットが水中にいて、自分より下にいる場合は潜る（ジャンプを避ける）
                    if (target.isInWater() && target.getY() < entity.getY() - 0.5) {
                        shouldDive = true;
                    }
                }

                if (!shouldDive) {
                    boolean isSubmerged = entity.isEyeInFluid(FluidTags.WATER);
                    boolean isDrowning = entity.getAirSupply() < entity.getMaxAirSupply() / 2;
                    // 頭部が水没している、または窒息しかけている時のみジャンプ（浮上）する
                    if (isSubmerged || isDrowning) {
                        entity.getJumpControl().jump();
                    }
                }
            }
        }
    }
}
