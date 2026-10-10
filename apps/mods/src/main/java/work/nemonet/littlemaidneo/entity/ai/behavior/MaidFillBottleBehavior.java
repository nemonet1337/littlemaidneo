package work.nemonet.littlemaidneo.entity.ai.behavior;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Container;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import org.jetbrains.annotations.Nullable;
import work.nemonet.littlemaidneo.entity.LMHasInventory;
import work.nemonet.littlemaidneo.entity.LittleMaidEntity;
import work.nemonet.littlemaidneo.entity.mode.ModeHelpers;
import work.nemonet.littlemaidneo.entity.util.MaidJob;
import work.nemonet.littlemaidneo.entity.util.TameableUtil;

import java.util.Map;
import java.util.Optional;
import java.util.OptionalInt;

/**
 * 薬師ジョブの瓶補填。空のガラス瓶を水源（水流ブロック／水入り大釜）で水入り瓶に切り替える。
 *
 * <p>旧来は空き瓶を醸造台へ入れられず醸造ジョブが停止していたため、
 * 「開始キー（glass_bottle）→ ベース（水入り瓶）」の変換を行う Behavior として分離した。
 * 水源探索結果は {@link #FIND_INTERVAL} tick キャッシュする。
 */
public class MaidFillBottleBehavior extends AbstractMaidBehavior {
    /** 水源探索の間隔（tick）。 */
    private static final int FIND_INTERVAL = 40;
    /** 水源探索の水平半径／垂直半径。 */
    private static final int SEARCH_RADIUS = 6;
    private static final int SEARCH_HEIGHT = 3;

    @Nullable
    private BlockPos waterPos;
    private int findCool;
    private int recalcPathTimer;

    public MaidFillBottleBehavior() {
        super(Map.of());
    }

    @Override
    protected boolean checkExtraStartConditions(ServerLevel level, LittleMaidEntity mob) {
        if (!requireJob(mob, MaidJob.PHARMACIST)) {
            return false;
        }
        if (mob.isStrike() || !TameableUtil.hasTameOwner(mob)) {
            return false;
        }
        if (0 < --findCool) {
            return false;
        }
        findCool = FIND_INTERVAL;

        Container inventory = LMHasInventory.getWorkView(mob);
        OptionalInt emptyBottle = ModeHelpers.findSlot(inventory, stack -> stack.is(Items.GLASS_BOTTLE));
        if (emptyBottle.isEmpty()) {
            return false;
        }
        // ベース（水入り瓶等）を既に持っているなら醸造を優先する。
        if (ModeHelpers.findSlot(inventory, stack -> BrewingSupport.isBrewableBottle(level, stack)).isPresent()) {
            return false;
        }
        this.waterPos = findWaterPos(level, mob).orElse(null);
        return this.waterPos != null;
    }

    @Override
    protected boolean canStillUse(ServerLevel level, LittleMaidEntity mob, long gameTime) {
        // 補填が終わったら（水源に到達して 1 本満たしたら）醸造 Behavior に道を譲る
        return this.waterPos != null && requireJob(mob, MaidJob.PHARMACIST);
    }

    @Override
    protected void tick(ServerLevel level, LittleMaidEntity mob, long gameTime) {
        if (waterPos == null) {
            return;
        }
        var navResult = ModeHelpers.approach(mob, waterPos, 1.0, recalcPathTimer, FIND_INTERVAL, 2.0, 1);
        recalcPathTimer = navResult.nextTimer();
        if (navResult.unreachable()) {
            waterPos = null;
            return;
        }
        if (mob.distanceToSqr(waterPos.getX() + 0.5, waterPos.getY(), waterPos.getZ() + 0.5) > 2.0 * 2.0) {
            return;
        }
        mob.getNavigation().stop();
        if (fillOneBottle(mob)) {
            mob.swing(InteractionHand.MAIN_HAND, net.minecraft.world.item.component.SwingAnimation.DEFAULT, false);
            mob.level().playSound(null, waterPos, SoundEvents.BOTTLE_FILL, SoundSource.NEUTRAL, 1.0f, 1.0f);
        }
        waterPos = null;
    }

    @Override
    protected void stop(ServerLevel level, LittleMaidEntity mob, long gameTime) {
        this.waterPos = null;
        this.findCool = 0;
        this.recalcPathTimer = 0;
    }

    /** 作業ビュー先頭の空き瓶 1 本を水入り瓶に差し替える。 */
    private boolean fillOneBottle(LittleMaidEntity mob) {
        Container inventory = LMHasInventory.getWorkView(mob);
        OptionalInt slot = ModeHelpers.findSlot(inventory,
                stack -> stack.is(net.minecraft.world.item.Items.GLASS_BOTTLE));
        if (slot.isEmpty()) {
            return false;
        }
        int index = slot.getAsInt();
        ItemStack empty = inventory.getItem(index);
        if (empty.getCount() > 1) {
            empty.shrink(1);
        }
        inventory.setItem(index, BrewingSupport.createWaterBottle());
        return true;
    }

    private Optional<BlockPos> findWaterPos(ServerLevel level, LittleMaidEntity mob) {
        BlockPos origin = mob.blockPosition();
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
        for (int dx = -SEARCH_RADIUS; dx <= SEARCH_RADIUS; dx++) {
            for (int dz = -SEARCH_RADIUS; dz <= SEARCH_RADIUS; dz++) {
                for (int dy = -SEARCH_HEIGHT; dy <= SEARCH_HEIGHT; dy++) {
                    cursor.set(origin.getX() + dx, origin.getY() + dy, origin.getZ() + dz);
                    if (isWaterAccess(level, cursor.immutable())) {
                        return Optional.of(cursor.immutable());
                    }
                }
            }
        }
        return Optional.empty();
    }

    private boolean isWaterAccess(ServerLevel level, BlockPos pos) {
        var state = level.getBlockState(pos);
        return state.is(Blocks.WATER) || state.is(Blocks.WATER_CAULDRON);
    }
}
