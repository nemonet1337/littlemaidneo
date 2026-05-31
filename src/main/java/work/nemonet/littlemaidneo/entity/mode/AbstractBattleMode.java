package work.nemonet.littlemaidneo.entity.mode;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.item.ItemStack;
import work.nemonet.littlemaidneo.api.mode.Mode;
import work.nemonet.littlemaidneo.api.mode.ModeType;

import java.util.Optional;

public abstract class AbstractBattleMode<T> extends Mode {
    protected final Mob mob;
    protected LivingEntity target;
    protected ItemStack weaponStack;
    protected T weapon;

    protected AbstractBattleMode(Mob mob, ModeType<? extends Mode> modeType, String name) {
        super(modeType, name);
        this.mob = mob;
    }

    public boolean shouldExecute() {
        if (this.mob.getTarget() == null
                || !this.mob.getTarget().isAlive()) {
            return false;
        }
        this.target = this.mob.getTarget();

        var main = this.mob.getMainHandItem();
        if (isWeapon(main)) {
            this.weaponStack = main;
            this.weapon = getWeaponInstance(main).orElseThrow();
            return true;
        }

        return false;
    }

    public boolean shouldContinueExecuting() {
        return this.shouldExecute();
    }

    @Override
    public boolean isBattleMode() {
        return true;
    }

    protected boolean isWeapon(ItemStack stack) {
        return getWeaponInstance(stack).isPresent();
    }

    protected abstract Optional<T> getWeaponInstance(ItemStack stack);
}
