package work.nemonet.littlemaidneo.entity.goal;

import net.minecraft.world.entity.TamableAnimal;
import work.nemonet.littlemaidneo.entity.util.HasMovingMode;
import work.nemonet.littlemaidneo.entity.util.MovingMode;

import java.util.function.Supplier;

public class HasMMFollowTameOwnerGoal
        <T extends TamableAnimal
                & HasMovingMode>
        extends FollowTameOwnerGoal<T> {

    public HasMMFollowTameOwnerGoal(T tameable, Supplier<Float> speed, Supplier<Float> followStart, Supplier<Float> followEnd) {
        super(tameable, speed, followStart, followEnd);
    }

    @Override
    public boolean canUse() {
        return this.tameable.getMovingMode() == MovingMode.ESCORT && super.canUse();
    }
}
