package work.nemonet.littlemaidneo.entity.goal;

import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import work.nemonet.littlemaidneo.entity.util.GuiEntitySupplier;
import work.nemonet.littlemaidneo.entity.util.TameableUtil;

import java.util.EnumSet;

//TODO 実装
public class WaitWhenOpenGUIGoal<T extends TamableAnimal, M extends AbstractContainerMenu & GuiEntitySupplier<T>>
        extends Goal {
    private final T mob;
    private final Class<? extends M> screenHandler;

    public WaitWhenOpenGUIGoal(T mob, Class<? extends M> screenHandler) {
        this.mob = mob;
        this.screenHandler = screenHandler;
        setFlags(EnumSet.of(Flag.MOVE));
    }

    @Override
    public boolean canUse() {
        return TameableUtil.getTameOwner(mob)
                .filter(owner -> owner instanceof Player)
                .map(owner -> ((Player) owner).containerMenu)
                .filter(screen -> this.screenHandler.isAssignableFrom(screen.getClass()))
                .map(screen -> screenHandler.cast(screen).getGuiEntity())
                .filter(guiEntity -> mob == guiEntity)
                .isPresent();
    }

    @Override
    public boolean canContinueToUse() {
        return TameableUtil.getTameOwner(mob)
                .filter(owner -> owner instanceof Player)
                .map(owner -> ((Player) owner).containerMenu)
                .filter(screen -> this.screenHandler.isAssignableFrom(screen.getClass()))
                .isPresent();
    }

    @Override
    public void start() {
        this.mob.getNavigation().stop();
    }

    @Override
    public void tick() {
        super.tick();
        TameableUtil.getTameOwner(mob)
                .ifPresent(owner -> this.mob.getLookControl().setLookAt(owner.getEyePosition(1F)));
    }
}
