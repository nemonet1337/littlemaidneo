package work.nemonet.littlemaidneo.entity;

import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.BushBlock;
import work.nemonet.littlemaidneo.config.LMNConfig;
import work.nemonet.littlemaidneo.entity.util.MaidMode;
import work.nemonet.littlemaidneo.entity.util.TameableUtil;
import work.nemonet.littlemaidneo.maidmodel.EntityCaps;

public class LittleMaidModelCaps extends EntityCaps {

    private final LittleMaidEntity maid;

    public LittleMaidModelCaps(LittleMaidEntity maid) {
        super(maid);
        this.maid = maid;
    }

    @Override
    public Object getCapsValue(int pIndex, Object... pArg) {
        return switch (pIndex) {
            case caps_aimedBow -> maid.isAimingBow();
            case caps_isLeeding -> maid.isLeashed(); // Mob 側の isLeashed を参照する
            case caps_isBloodsuck -> maid.isBloodSuck();
            case caps_isFreedom -> maid.getMaidMode() == MaidMode.FREEDOM;
            case caps_isTracer -> maid.getMaidMode() == MaidMode.TRACER;
            case caps_isPlaying -> maid.isPlayingSnow();
            case caps_isLookSuger -> maid.isBegging();
            case caps_isWait -> TameableUtil.isWait(maid) &&
                (LMNConfig.get().client.enableWaitPoseOnMoving ||
                    maid.getDeltaMovement().lengthSqr() < 0.01);
            case caps_isWorking -> !maid.getActiveJobName().equals("none");
            case caps_isContract -> maid.isContractMM();
            case caps_isClock -> maid.getMainHandItem().getItem() ==
                Items.CLOCK ||
                maid.getOffhandItem().getItem() == Items.CLOCK;
            case caps_isPlanter -> maid
                    .getInventory()
                    .getItem(17)
                    .getItem() instanceof
                BlockItem blockItem &&
                blockItem.getBlock() instanceof BushBlock;
            case caps_isOverdrive -> maid.getAccelerationTicks() > 0;
            case caps_entityIdFactor -> maid.getIdFactor();
            case caps_interestedAngle -> maid.getInterestedAngle(
                (Float) pArg[0]
            );
            case caps_job -> {
                String jobName = maid.getActiveJobName();
                if ("none".equals(jobName)) {
                    yield null;
                }
                if ("combat".equals(jobName)) {
                    yield "bow".equals(maid.getActiveBattleMode()) ? "archer" : "fencer";
                }
                yield jobName;
            }
            default -> super.getCapsValue(pIndex, pArg);
        };
    }
}
