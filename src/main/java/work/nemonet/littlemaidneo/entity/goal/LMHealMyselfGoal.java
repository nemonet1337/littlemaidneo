package work.nemonet.littlemaidneo.entity.goal;

import net.minecraft.world.item.ItemStack;
import work.nemonet.littlemaidneo.resource.util.LMSounds;
import work.nemonet.littlemaidneo.entity.LittleMaidEntity;

import java.util.function.Predicate;
import java.util.function.Supplier;

public class LMHealMyselfGoal extends HealMyselfGoal<LittleMaidEntity> {
    public LMHealMyselfGoal(LittleMaidEntity mob, Supplier<Integer> healInterval, Supplier<Integer> healAmount, Predicate<ItemStack> healItemPred) {
        super(mob, healInterval, healAmount, healItemPred);
    }

    @Override
    public void heal(ItemStack healItem) {
        super.heal(healItem);
        var sound = isHealthFull() ? LMSounds.EAT_SUGAR_MAX_POWER : LMSounds.EAT_SUGAR;
        mob.play(sound);
    }
}
