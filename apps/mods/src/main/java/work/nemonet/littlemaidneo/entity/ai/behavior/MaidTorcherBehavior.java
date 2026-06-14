package work.nemonet.littlemaidneo.entity.ai.behavior;

import com.google.common.collect.ImmutableList;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ai.memory.MemoryStatus;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.DirectionalPlaceContext;
import net.minecraft.world.level.block.TorchBlock;
import org.jetbrains.annotations.Nullable;
import work.nemonet.littlemaidneo.LittleMaidNeo;
import work.nemonet.littlemaidneo.config.LMNConfig;
import work.nemonet.littlemaidneo.entity.LittleMaidEntity;
import work.nemonet.littlemaidneo.entity.mode.ModeHelpers;
import work.nemonet.littlemaidneo.entity.util.MaidMode;
import work.nemonet.littlemaidneo.entity.util.TameableUtil;
import work.nemonet.littlemaidneo.resource.util.LMSounds;
import work.nemonet.littlemaidneo.util.BlockFinderPD;

import java.util.HashMap;
import java.util.Map;

public class MaidTorcherBehavior extends AbstractMaidBehavior {
    protected final float distance = 12F;
    protected BlockPos placePos;
    protected int recalcPathTimer;
    protected int failPlaceTimer;
    protected int count;

    protected final Map<BlockPos, Long> recentlyPlaced = new HashMap<>();
    @Nullable
    protected BlockFinderPD blockFinder;

    public MaidTorcherBehavior() {
        super(Map.of(
                work.nemonet.littlemaidneo.setup.ModRegistration.ACTIVE_JOB_NAME.get(), MemoryStatus.VALUE_PRESENT
        ));
    }

    @Override
    protected boolean checkExtraStartConditions(ServerLevel level, LittleMaidEntity mob) {
        String job = mob.getBrain().getMemory(work.nemonet.littlemaidneo.setup.ModRegistration.ACTIVE_JOB_NAME.get()).orElse("");
        if (!job.equals("torcher")) {
            return false;
        }

        Item item = mob.getMainHandItem().getItem();
        if (!(item instanceof BlockItem)) {
            return false;
        }
        if (blockFinder == null || blockFinder.isEnd() || count++ > 100) {
            this.count = 0;
            BlockPos basePos;
            if (mob.getMaidMode() == MaidMode.ESCORT) {
                Entity owner = TameableUtil.getTameOwner(mob).orElse(null);
                if (owner == null) {
                    return false;
                }
                basePos = owner.blockPosition();
            } else {
                basePos = mob.blockPosition();
            }
            blockFinder = new BlockFinderPD(
                    ImmutableList.of(basePos),
                    pos -> isDark(mob, pos) && isPlaceable(mob, pos),
                    pos -> Math.abs(basePos.getY() - pos.getY()) < 3 &&
                            (isPlaceable(mob, pos) || isPlaceable(mob, pos.below())) &&
                            pos.closerThan(basePos, distance),
                    Mth.floor(distance * distance * 7)
            );
        }
        blockFinder.tick(10);
        placePos = blockFinder.getResult().orElse(null);
        return placePos != null;
    }

    public boolean isDark(LittleMaidEntity mob, BlockPos pos) {
        long gameTime = mob.level().getGameTime();
        recentlyPlaced.entrySet().removeIf(entry -> gameTime - entry.getValue() > 200);
        if (recentlyPlaced.containsKey(pos)) {
            return false;
        }
        return (
                mob.level().getMaxLocalRawBrightness(pos) <=
                        LMNConfig.get().work.torcherLightLevelThreshold
        );
    }

    public boolean isPlaceable(LittleMaidEntity mob, BlockPos pos) {
        return (
                mob.level().isEmptyBlock(pos) &&
                        TorchBlock.canSupportCenter(
                                mob.level(),
                                pos.below(),
                                Direction.UP
                        )
        );
    }

    @Override
    protected boolean canStillUse(ServerLevel level, LittleMaidEntity mob, long gameTime) {
        String job = mob.getBrain().getMemory(work.nemonet.littlemaidneo.setup.ModRegistration.ACTIVE_JOB_NAME.get()).orElse("");
        if (!job.equals("torcher")) {
            return false;
        }
        return (
                placePos != null &&
                        mob.getMainHandItem().getItem() instanceof BlockItem
        );
    }

    @Override
    protected void start(ServerLevel level, LittleMaidEntity mob, long gameTime) {
        mob.getNavigation().stop();
        mob.play(LMSounds.FIND_TARGET_D);
        mob.setSprinting(true);
    }

    @Override
    protected void tick(ServerLevel level, LittleMaidEntity mob, long gameTime) {
        if (placePos == null) {
            return;
        }
        if (60 < ++this.failPlaceTimer ||
                LMNConfig.get().work.torcherLightLevelThreshold <
                        mob.level().getMaxLocalRawBrightness(placePos)) {
            this.placePos = null;
            this.failPlaceTimer = 0;
            return;
        }
        double distanceSq = mob.distanceToSqr(
                placePos.getX() + 0.5,
                placePos.getY(),
                placePos.getZ() + 0.5
        );
        if (this.distance * this.distance * 1.5f * 1.5f < distanceSq) {
            this.placePos = null;
            return;
        }
        var navResult = ModeHelpers.approach(mob, placePos, 1.0, recalcPathTimer, 20, 3.0, 2);
        recalcPathTimer = navResult.nextTimer();
        if (navResult.unreachable()) {
            placePos = null;
            return;
        }
        if (mob.distanceToSqr(placePos.getX() + 0.5, placePos.getY(), placePos.getZ() + 0.5) > 3.0 * 3.0) {
            return;
        }

        ItemStack itemStack = mob.getMainHandItem();
        Item item = itemStack.getItem();
        assert item instanceof BlockItem;
        if (mob.level().isEmptyBlock(placePos)) {
            try {
                ((BlockItem) item).place(
                        new DirectionalPlaceContext(
                                mob.level(),
                                placePos,
                                Direction.UP,
                                itemStack,
                                Direction.UP
                        )
                );
            } catch (Exception e) {
                LittleMaidNeo.LOGGER.warn("Torcherでのブロック設置時に例外が発生しました。");
                e.printStackTrace();
            }
            mob.swing(InteractionHand.MAIN_HAND);
            mob.play(LMSounds.INSTALLATION);
            recentlyPlaced.put(placePos.immutable(), mob.level().getGameTime());
        }
        this.placePos = null;
    }

    @Override
    protected void stop(ServerLevel level, LittleMaidEntity mob, long gameTime) {
        this.count = 0;
        this.failPlaceTimer = 0;
        this.recalcPathTimer = 0;
        mob.setSprinting(false);
        mob.getNavigation().stop();
    }
}
