package work.nemonet.littlemaidneo.entity.ai.behavior;

import com.google.common.collect.ImmutableMap;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.MemoryStatus;
import net.minecraft.world.entity.ai.memory.WalkTarget;
import net.minecraft.world.entity.ai.util.LandRandomPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import work.nemonet.littlemaidneo.config.LMNConfig;
import work.nemonet.littlemaidneo.entity.LittleMaidEntity;
import work.nemonet.littlemaidneo.entity.util.MaidMode;
import work.nemonet.littlemaidneo.entity.util.TameableUtil;
import work.nemonet.littlemaidneo.setup.ModRegistration;

/**
 * お散歩モード: ご主人の周辺をゆっくり徘徊する。
 * FREEDOM が固定原点なのに対し、STROLL はご主人位置を中心にする。
 */
public class MaidStrollBehavior extends AbstractMaidBehavior {
    private int reCalcCool;

    public MaidStrollBehavior() {
        super(ImmutableMap.of(
                ModRegistration.IS_WAITING.get(), MemoryStatus.VALUE_ABSENT,
                ModRegistration.OWNER.get(), MemoryStatus.VALUE_PRESENT
        ));
    }

    @Override
    protected boolean checkExtraStartConditions(ServerLevel level, LittleMaidEntity entity) {
        return TameableUtil.hasTameOwner(entity) && entity.getMaidMode() == MaidMode.STROLL;
    }

    @Override
    protected boolean canStillUse(ServerLevel level, LittleMaidEntity entity, long gameTime) {
        return checkExtraStartConditions(level, entity)
                && !entity.getBrain().hasMemoryValue(ModRegistration.IS_WAITING.get());
    }

    @Override
    protected void tick(ServerLevel level, LittleMaidEntity entity, long gameTime) {
        Player owner = entity.getBrain().getMemory(ModRegistration.OWNER.get()).orElse(null);
        if (owner == null) {
            return;
        }

        LMNConfig config = LittleMaidEntity.getConfig();
        double range = config.movement.freedomRange;
        double rangeSq = range * range;

        // ご主人から離れすぎたら戻る
        if (entity.distanceToSqr(owner) > rangeSq) {
            entity.getBrain().setMemory(MemoryModuleType.WALK_TARGET,
                    new WalkTarget(owner.position(), config.movement.followSpeed, 2));
            return;
        }

        if (!entity.getNavigation().isDone()) {
            return;
        }

        if (reCalcCool-- > 0) {
            return;
        }
        reCalcCool = 30 + entity.getRandom().nextInt(30);

        // ご主人方向へ寄ったランダム地点へ（周辺徘徊）
        Vec3 randPos = LandRandomPos.getPosTowards(entity, 8, 5, owner.position());
        if (randPos == null) {
            randPos = LandRandomPos.getPos(entity, 6, 4);
        }
        if (randPos != null) {
            // ご主人から range 内に収める
            if (randPos.distanceToSqr(owner.position()) > rangeSq) {
                Vec3 dir = randPos.subtract(owner.position()).normalize().scale(range * 0.7);
                randPos = owner.position().add(dir);
            }
            entity.getBrain().setMemory(MemoryModuleType.WALK_TARGET,
                    new WalkTarget(randPos, config.movement.freedomSpeed * 0.85f, 1));
        }
    }
}
