package work.nemonet.littlemaidneo.mixin;

import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(MeleeAttackGoal.class)
public interface MeleeAttackGoalAccessor {

    @Accessor("ticksUntilNextAttack")
    void setCooldown(int cooldown);

}
