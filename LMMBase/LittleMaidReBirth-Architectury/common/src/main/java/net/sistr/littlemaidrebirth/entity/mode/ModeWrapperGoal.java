package net.sistr.littlemaidrebirth.entity.mode;

import java.util.EnumSet;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;

/**
 * モードを実行するGoalクラス
 */
public class ModeWrapperGoal<T extends LivingEntity & HasMode> extends Goal {
    protected final T owner;

    public ModeWrapperGoal(T owner) {
        this.owner = owner;
        setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        if (owner.getMode().isEmpty()) return false;
        return owner.getMode().get().shouldExecute();
    }

    @Override
    public boolean canContinueToUse() {
        if (owner.getMode().isEmpty()) return false;
        return owner.getMode().get().shouldContinueExecuting();
    }

    @Override
    public void start() {
        if (owner.getMode().isEmpty()) return;
        owner.getMode().get().startExecuting();
    }

    @Override
    public void stop() {
        if (owner.getMode().isEmpty()) return;
        owner.getMode().get().resetTask();
    }

    @Override
    public void tick() {
        if (owner.getMode().isEmpty()) return;
        owner.getMode().get().tick();
    }

    @Override
    public boolean requiresUpdateEveryTick() {
        return true;
    }
}
