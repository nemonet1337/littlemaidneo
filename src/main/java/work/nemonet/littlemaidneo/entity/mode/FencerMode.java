package work.nemonet.littlemaidneo.entity.mode;

import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import work.nemonet.littlemaidneo.resource.util.LMSounds;
import work.nemonet.littlemaidneo.LMRBMod;
import work.nemonet.littlemaidneo.api.mode.ModeType;
import work.nemonet.littlemaidneo.entity.LittleMaidEntity;
import work.nemonet.littlemaidneo.util.ReachAttributeUtil;

import java.util.Optional;

public class FencerMode extends AbstractFencerMode<Item> {
    protected final LittleMaidEntity mob;
    protected int cooldown;

    // TODO 相手が無敵時間中は殴らない
    public FencerMode(ModeType<? extends FencerMode> modeType, String name, LittleMaidEntity mob, float speed) {
        super(mob, modeType, name, speed);
        this.mob = mob;
    }

    @Override
    public void startExecuting() {
        this.mob.play(LMSounds.FIND_TARGET_N);
    }

    @Override
    public void tick() {
        this.cooldown = Math.max(0, this.cooldown - 1);
        super.tick();
    }

    @Override
    protected void attack() {
        resetCooldown();
        this.mob.swing(InteractionHand.MAIN_HAND);
        this.mob.doHurtTarget(target);
    }

    protected void resetCooldown() {
        double attackSpeed = this.mob.getAttributeValue(Attributes.ATTACK_SPEED);
        this.cooldown = Mth.ceil(1 / attackSpeed * 20
                / LMRBMod.getConfig().work.fencerAttackRateFactor);
    }

    @Override
    protected boolean canAttack() {
        return this.cooldown <= 0 && super.canAttack();
    }

    @Override
    protected boolean isClose(double distanceSq) {
        return distanceSq < ReachAttributeUtil.getAttackRangeSq(mob)
                * LMRBMod.getConfig().work.fencerAttackDistanceFactor;
    }

    @Override
    protected Optional<Item> getWeaponInstance(ItemStack stack) {
        return Optional.of(stack.getItem());
    }
}
