package work.nemonet.littlemaidneo.entity;

import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.BushBlock;
import work.nemonet.littlemaidneo.maidmodel.EntityCaps;
import work.nemonet.littlemaidneo.LMRBMod;
import work.nemonet.littlemaidneo.api.mode.Mode;
import work.nemonet.littlemaidneo.entity.util.MovingMode;
import work.nemonet.littlemaidneo.entity.util.TameableUtil;

public class LittleMaidModelCaps extends EntityCaps {
    private final LittleMaidEntity maid;

    public LittleMaidModelCaps(LittleMaidEntity maid) {
        super(maid);
        this.maid = maid;
    }

    // TODO インベントリ系
    @Override
    public Object getCapsValue(int pIndex, Object... pArg) {
        return switch (pIndex) {
            case caps_aimedBow -> maid.isAimingBow();
            case caps_isLeeding -> maid.isLeashed();// MobEntityのメソッドなのでLMMLでなくこっち

            case caps_isBloodsuck -> maid.isBloodSuck();
            case caps_isFreedom -> maid.getMovingMode() == MovingMode.FREEDOM;
            case caps_isTracer -> maid.getMovingMode() == MovingMode.TRACER;
            case caps_isPlaying -> maid.isPlayingSnow();
            case caps_isLookSuger -> maid.isBegging();
            case caps_isWait -> TameableUtil.isWait(maid)
                    && (LMRBMod.getConfig().client.enableWaitPoseOnMoving
                            || maid.getDeltaMovement().lengthSqr() < 0.01);
            case caps_isWorking -> maid.getMode().isPresent();
            case caps_isContract -> maid.isContractMM();
            case caps_isClock -> maid.getMainHandItem().getItem() == Items.CLOCK
                    || maid.getOffhandItem().getItem() == Items.CLOCK;
            case caps_isPlanter -> maid.getInventory().getItem(17).getItem() instanceof BlockItem blockItem
                    && blockItem.getBlock() instanceof BushBlock;
            case caps_isOverdrive -> maid.getAccelerationTicks() > 0;

            case caps_entityIdFactor -> maid.getIdFactor();

            case caps_interestedAngle -> maid.getInterestedAngle((Float) pArg[0]);

            case caps_job -> maid.getMode()
                    .map(Mode::getName)
                    .map(String::toLowerCase)
                    .orElse(null);
            default -> super.getCapsValue(pIndex, pArg);
        };
    }
}
