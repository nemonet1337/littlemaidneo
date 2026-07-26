package work.nemonet.littlemaidneo.entity.ai.behavior;

import com.google.common.collect.ImmutableMap;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.behavior.Behavior;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import work.nemonet.littlemaidneo.entity.LittleMaidEntity;
import work.nemonet.littlemaidneo.entity.util.MaidMode;
import work.nemonet.littlemaidneo.entity.util.TameableUtil;
import work.nemonet.littlemaidneo.setup.ModRegistration;

/**
 * メイドさん用の水泳・潜水挙動。
 * <ul>
 *   <li>水面上で跳ね続けない</li>
 *   <li>ターゲットや護衛対象が下にいるときは潜る</li>
 *   <li>呼吸が苦しい／頭部水没時は浮上</li>
 *   <li>護衛中にご主人が水中なら追従のため潜航する</li>
 * </ul>
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
        if (entity.isInLava()) {
            if (entity.getRandom().nextFloat() < this.chance) {
                entity.getJumpControl().jump();
            }
            return;
        }

        if (!entity.isInWater()) {
            return;
        }

        LivingEntity target = entity.getTarget();
        boolean shouldDive = shouldDive(entity, target);

        boolean isSubmerged = entity.isEyeInFluid(FluidTags.WATER);
        boolean isDrowning = entity.getAirSupply() < entity.getMaxAirSupply() / 3;

        if (isDrowning) {
            // 呼吸優先で浮上
            if (entity.getRandom().nextFloat() < this.chance) {
                entity.getJumpControl().jump();
            }
            entity.setDeltaMovement(entity.getDeltaMovement().add(0.0, 0.04, 0.0));
            return;
        }

        if (shouldDive) {
            // 潜航: 上向きジャンプを抑え、下方向へ少し押す
            entity.setDeltaMovement(entity.getDeltaMovement().add(0.0, -0.03, 0.0));
            if (!entity.isSwimming()) {
                entity.setSwimming(true);
            }
            applyHorizontalSwimToward(entity, target);
        } else if (isSubmerged) {
            // 頭部水没だが潜る理由がない → 浮上
            if (entity.getRandom().nextFloat() < this.chance) {
                entity.getJumpControl().jump();
            }
            entity.setDeltaMovement(entity.getDeltaMovement().add(0.0, 0.03, 0.0));
        } else {
            // 水面付近: 跳ねすぎないよう低頻度のみ
            if (entity.getRandom().nextFloat() < this.chance * 0.25f) {
                entity.getJumpControl().jump();
            }
        }
    }

    private static boolean shouldDive(LittleMaidEntity entity, LivingEntity target) {
        if (target != null && target.isInWater() && target.getY() < entity.getY() - 0.5) {
            return true;
        }
        if (entity.getMaidMode() == MaidMode.ESCORT && !TameableUtil.isWait(entity)) {
            Player owner = entity.getBrain().getMemory(ModRegistration.OWNER.get()).orElse(null);
            if (owner != null && owner.isInWater() && owner.getY() < entity.getY() - 0.3) {
                return true;
            }
        }
        return false;
    }

    private static void applyHorizontalSwimToward(LittleMaidEntity entity, LivingEntity target) {
        LivingEntity goal = target;
        if (goal == null && entity.getMaidMode() == MaidMode.ESCORT) {
            goal = entity.getBrain().getMemory(ModRegistration.OWNER.get()).orElse(null);
        }
        if (goal == null) {
            return;
        }
        Vec3 dir = goal.position().subtract(entity.position());
        if (dir.lengthSqr() < 1.0E-4) {
            return;
        }
        dir = dir.normalize().scale(0.05);
        entity.setDeltaMovement(entity.getDeltaMovement().add(dir.x, 0.0, dir.z));
    }
}
