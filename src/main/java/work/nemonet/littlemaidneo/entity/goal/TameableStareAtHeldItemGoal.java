package work.nemonet.littlemaidneo.entity.goal;

import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.item.ItemStack;
import work.nemonet.littlemaidneo.entity.util.TameableUtil;

import java.util.function.Predicate;
import java.util.function.Supplier;

public class TameableStareAtHeldItemGoal<T extends TamableAnimal> extends StareAtHeldItemGoal<T> {
    protected final boolean isTamed;

    public TameableStareAtHeldItemGoal(T mob, Supplier<Float> stareAtRange, Predicate<ItemStack> targetItem, boolean isTamed) {
        super(mob, stareAtRange, targetItem);
        this.isTamed = isTamed;
    }

    @Override
    public boolean canUse() {
        return TameableUtil.hasTameOwner(this.mob) == isTamed && super.canUse();
    }
}
