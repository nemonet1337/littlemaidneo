package work.nemonet.littlemaidneo.entity.mode;

import com.google.common.collect.ImmutableList;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.DirectionalPlaceContext;
import net.minecraft.world.level.block.TorchBlock;
import net.minecraft.world.level.pathfinder.Path;
import org.jetbrains.annotations.Nullable;
import work.nemonet.littlemaidneo.LittleMaidNeo;
import work.nemonet.littlemaidneo.api.mode.Mode;
import work.nemonet.littlemaidneo.api.mode.ModeType;
import work.nemonet.littlemaidneo.config.LMRBConfig;
import work.nemonet.littlemaidneo.entity.LittleMaidEntity;
import work.nemonet.littlemaidneo.entity.compound.SoundPlayable;
import work.nemonet.littlemaidneo.entity.util.MaidMode;
import work.nemonet.littlemaidneo.entity.util.TameableUtil;
import work.nemonet.littlemaidneo.resource.util.LMSounds;
import work.nemonet.littlemaidneo.util.BlockFinderPD;

import java.util.Map;
import java.util.HashMap;

//暗所発見->移動->設置
//置いてすぐはライトレベルに変化が無い点に注意
public class TorcherMode extends Mode {

    protected final LittleMaidEntity mob;
    protected final float distance;
    protected BlockPos placePos;
    protected int recalcPathTimer;
    protected int failPlaceTimer;
    protected int count;

    protected final Map<BlockPos, Long> recentlyPlaced = new HashMap<>();
    @Nullable
    protected BlockFinderPD blockFinder;

    public TorcherMode(
        ModeType<? extends Mode> modeType,
        String name,
        LittleMaidEntity mob,
        float distance
    ) {
        super(modeType, name);
        this.mob = mob;
        this.distance = distance;
    }

    @Override
    public boolean shouldExecute() {
        // 手に持っているものがブロックでないといけない
        Item item = mob.getMainHandItem().getItem();
        if (!(item instanceof BlockItem)) {
            return false;
        }
        if (blockFinder == null || blockFinder.isEnd() || count++ > 100) {
            this.count = 0;
            BlockPos basePos;
            if (this.mob.getMaidMode() == MaidMode.ESCORT) {
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
                pos -> isDark(pos) && isPlaceable(pos),
                pos ->
                    Math.abs(basePos.getY() - pos.getY()) < 3 &&
                    (isPlaceable(pos) || isPlaceable(pos.below())) &&
                    pos.closerThan(basePos, distance),
                Mth.floor(distance * distance * 7)
            );
            // 探索済みブロック数の実測値に合わせてexpectedを指定
            // 半径12 seed数874
        }
        // 毎tick nブロック探索
        blockFinder.tick(10);
        placePos = blockFinder.getResult().orElse(null);
        return placePos != null;
    }

    public boolean isDark(BlockPos pos) {
        long gameTime = mob.level().getGameTime();
        recentlyPlaced.entrySet().removeIf(entry -> gameTime - entry.getValue() > 200);
        if (recentlyPlaced.containsKey(pos)) {
            return false;
        }
        return (
            mob.level().getMaxLocalRawBrightness(pos) <=
            LMRBConfig.get().work.torcherLightLevelThreshold
        );
    }

    public boolean isPlaceable(BlockPos pos) {
        return (
            mob.level().isEmptyBlock(pos) &&
            TorchBlock.canSupportCenter(
                this.mob.level(),
                pos.below(),
                Direction.UP
            )
        );
    }

    @Override
    public boolean shouldContinueExecuting() {
        return (
            placePos != null &&
            mob.getMainHandItem().getItem() instanceof BlockItem
        );
    }

    @Override
    public void startExecuting() {
        this.mob.getNavigation().stop();
        ((SoundPlayable) mob).play(LMSounds.FIND_TARGET_D);
        this.mob.setSprinting(true);
    }

    @Override
    public void tick() {
        // なぜかnullの場合があるので必須
        if (placePos == null) {
            return;
        }
        // 一定時間経過しても置けない、または明るい地点を無視
        if (
            60 < ++this.failPlaceTimer ||
            LMRBConfig.get().work.torcherLightLevelThreshold <
                mob.level().getMaxLocalRawBrightness(placePos)
        ) {
            this.placePos = null;
            this.failPlaceTimer = 0;
            return;
        }
        double distanceSq = this.mob.distanceToSqr(
            placePos.getX() + 0.5,
            placePos.getY(),
            placePos.getZ() + 0.5
        );
        // 距離が遠すぎる場合は無視
        if (this.distance * this.distance * 1.5f * 1.5f < distanceSq) {
            this.placePos = null;
            return;
        }
        // 手の届く範囲でない場合、近づく
        if (3 * 3 < distanceSq) {
            if (--recalcPathTimer < 0) {
                recalcPathTimer = 20;
                Path path = this.mob
                    .getNavigation()
                    .createPath(
                        placePos.getX(),
                        placePos.getY(),
                        placePos.getZ(),
                        2
                    );
                if (
                    path == null ||
                    path.getEndNode() == null ||
                    !path.getEndNode().asBlockPos().closerThan(placePos, 3)
                ) {
                    placePos = null;
                    return;
                }
                this.mob.getNavigation().moveTo(path, 1.0);
            }
            return;
        }

        // shouldContinueExecutingでチェック済みなので、必ずitemはブロック
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
                LittleMaidNeo.LOGGER.warn(
                    "Torcherでのブロック設置時に例外が発生しました。"
                );
                e.printStackTrace();
            }
            mob.swing(InteractionHand.MAIN_HAND);
            ((SoundPlayable) mob).play(LMSounds.INSTALLATION);
            recentlyPlaced.put(placePos.immutable(), mob.level().getGameTime());
        }
        this.placePos = null;
    }

    @Override
    public void resetTask() {
        this.count = 0;
        this.failPlaceTimer = 0;
        this.recalcPathTimer = 0;
        this.mob.setSprinting(false);
        this.mob.getNavigation().stop();
    }
}
