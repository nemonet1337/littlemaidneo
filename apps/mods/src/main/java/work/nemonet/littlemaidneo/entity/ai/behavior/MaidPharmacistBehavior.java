package work.nemonet.littlemaidneo.entity.ai.behavior;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Container;
import net.minecraft.world.entity.ai.memory.MemoryStatus;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.BrewingInput;
import net.minecraft.world.item.crafting.RecipePropertySet;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.block.entity.BrewingStandBlockEntity;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import work.nemonet.littlemaidneo.entity.LMHasInventory;
import work.nemonet.littlemaidneo.entity.LittleMaidEntity;
import work.nemonet.littlemaidneo.entity.ai.WorkPoi;
import work.nemonet.littlemaidneo.entity.mode.ModeHelpers;
import work.nemonet.littlemaidneo.setup.ModRegistration;
import work.nemonet.littlemaidneo.entity.util.MaidJob;
import work.nemonet.littlemaidneo.entity.util.TameableUtil;
import net.minecraft.world.entity.ai.village.poi.PoiTypes;

import java.util.Map;
import java.util.Optional;
import java.util.OptionalInt;

public class MaidPharmacistBehavior extends AbstractMaidBehavior implements PersistentMaidBehavior {
    /** 作業判定の間隔（tick）。レシピ照会はここだけに集約する。 */
    private static final int PROCESS_INTERVAL = 10;
    /** 何も作業がなく、醸造中でもない場合に忠業を諦めるまでの tick。 */
    private static final int NO_WORK_LIMIT = 200;

    private BlockPos brewingStandPos;
    private int recalcPathTimer;
    private int processTimer;
    /** 最終作業から経過した無作業 tick。 */
    private int idleTicks;

    public MaidPharmacistBehavior() {
        super(Map.of(
                ModRegistration.ACTIVE_JOB_NAME.get(), MemoryStatus.VALUE_PRESENT
        ));
    }

    @Override
    protected boolean checkExtraStartConditions(ServerLevel level, LittleMaidEntity mob) {
        if (!requireJob(mob)) {
            return false;
        }

        if (recalcPathTimer > 0) {
            recalcPathTimer--;
            return false;
        }

        var optPos = findBrewingStandPos(mob);
        if (optPos.isEmpty()) {
            recalcPathTimer = 40;
            return false;
        }

        this.brewingStandPos = optPos.get();
        this.idleTicks = 0;
        return true;
    }

    @Override
    protected boolean canStillUse(ServerLevel level, LittleMaidEntity mob, long gameTime) {
        // 作業有無の判定は tick 側の processTimer（10 tick 間隔）で行う。
        // ここでは「対象の醸造台が現存し、薬師ジョブが継続中」だけを見る。
        return requireJob(mob) && brewingStandPos != null
                && getBrewingStand(mob, brewingStandPos).isPresent();
    }

    @Override
    protected void tick(ServerLevel level, LittleMaidEntity mob, long gameTime) {
        if (brewingStandPos == null) return;
        var tileOpt = getBrewingStand(mob, brewingStandPos);
        if (tileOpt.isEmpty()) return;
        var tile = tileOpt.get();

        var navResult = ModeHelpers.approach(mob, brewingStandPos, 1.0, recalcPathTimer, 10, 3.0, 1);
        recalcPathTimer = navResult.nextTimer();
        if (navResult.unreachable()) {
            brewingStandPos = null;
            return;
        }
        if (mob.distanceToSqr(brewingStandPos.getX() + 0.5, brewingStandPos.getY(), brewingStandPos.getZ() + 0.5) > 3 * 3) {
            return;
        }
        mob.getNavigation().stop();
        mob.getLookControl().setLookAt(brewingStandPos.getX() + 0.5, brewingStandPos.getY() + 0.5, brewingStandPos.getZ() + 0.5, 30.0f, 30.0f);

        if (processTimer++ >= PROCESS_INTERVAL) {
            processTimer = 0;
            var action = findNextBrewAction(mob, tile);
            if (action.isPresent()) {
                idleTicks = 0;
                if (performBrewAction(mob, tile, action.get())) {
                    mob.swing(net.minecraft.world.InteractionHand.MAIN_HAND,
                            net.minecraft.world.item.component.SwingAnimation.DEFAULT, false);
                }
                return;
            }
            // 醸造が進行中の台は待つ。それもなく作業も無いなら忠業を切り上げる。
            if (!isBrewing(tile) && (idleTicks += PROCESS_INTERVAL) >= NO_WORK_LIMIT) {
                brewingStandPos = null;
            }
        }
    }

    @Override
    protected void stop(ServerLevel level, LittleMaidEntity mob, long gameTime) {
        this.brewingStandPos = null;
        this.recalcPathTimer = 0;
        this.processTimer = 0;
        this.idleTicks = 0;
        mob.getNavigation().stop();
    }

    private boolean requireJob(LittleMaidEntity mob) {
        // getActiveJob() はストライキ中 NONE を返す
        return requireJob(mob, MaidJob.PHARMACIST) && TameableUtil.hasTameOwner(mob);
    }

    /**
     * 次に行うべき醸造作業 1 件を探す。作業がなければ {@link Optional#empty()}。
     * 燃料投入 → 瓶投入 → 試薬投入 → 完成ポーション回収 の順に評価する。
     */
    private Optional<BrewAction> findNextBrewAction(LittleMaidEntity mob, BrewingStandBlockEntity tile) {
        if (!(mob.level() instanceof ServerLevel level)) {
            return Optional.empty();
        }
        var inventory = LMHasInventory.getWorkView(mob);

        ItemStack fuel = tile.getItem(BrewingSupport.SLOT_FUEL);
        if (fuel.isEmpty() && findSlot(inventory, BrewingSupport::isFuel).isPresent()) {
            return Optional.of(new BrewAction(BrewAction.Kind.FUEL, BrewingSupport.SLOT_FUEL));
        }

        for (int bSlot = 0; bSlot < BrewingSupport.BOTTLE_SLOTS; bSlot++) {
            if (tile.getItem(bSlot).isEmpty() && hasBrewableBottle(level, inventory)) {
                return Optional.of(new BrewAction(BrewAction.Kind.BOTTLE, bSlot));
            }
        }

        if (tile.getItem(BrewingSupport.SLOT_REAGENT).isEmpty()) {
            for (int bSlot = 0; bSlot < BrewingSupport.BOTTLE_SLOTS; bSlot++) {
                ItemStack bottle = tile.getItem(bSlot);
                if (!bottle.isEmpty() && findReagent(level, inventory, bottle).isPresent()) {
                    // slot は試薬と混ぜる瓶のスロット
                    return Optional.of(new BrewAction(BrewAction.Kind.REAGENT, bSlot));
                }
            }
        }

        if (tile.getItem(BrewingSupport.SLOT_REAGENT).isEmpty()) {
            for (int bSlot = 0; bSlot < BrewingSupport.BOTTLE_SLOTS; bSlot++) {
                if (BrewingSupport.isFinishedPotion(level, tile.getItem(bSlot))
                        && findSlot(inventory, ItemStack::isEmpty).isPresent()) {
                    return Optional.of(new BrewAction(BrewAction.Kind.RESULT, bSlot));
                }
            }
        }

        return Optional.empty();
    }

    private boolean performBrewAction(LittleMaidEntity mob, BrewingStandBlockEntity tile, BrewAction action) {
        if (!(mob.level() instanceof ServerLevel level)) {
            return false;
        }
        var inventory = LMHasInventory.getWorkView(mob);
        return switch (action.kind()) {
            case FUEL -> insertSlot(inventory, BrewingSupport.SLOT_FUEL, tile, BrewingSupport::isFuel);
            case BOTTLE -> insertSlot(inventory, action.slot(), tile,
                    stack -> BrewingSupport.isBrewableBottle(level, stack));
            case REAGENT -> {
                // action.slot() は試薬と混ぜる瓶のスロット
                ItemStack bottle = tile.getItem(action.slot());
                yield !bottle.isEmpty() && insertSlot(inventory, BrewingSupport.SLOT_REAGENT, tile,
                        stack -> isReagentFor(level, stack, bottle));
            }
            case RESULT -> {
                ItemStack bottle = tile.getItem(action.slot());
                if (!BrewingSupport.isFinishedPotion(level, bottle)) {
                    yield false;
                }
                boolean moved = ModeHelpers.transferTo(inventory, bottle.copy()).isEmpty();
                if (moved) {
                    tile.setItem(action.slot(), ItemStack.EMPTY);
                }
                yield moved;
            }
        };
    }

    /** {@code slot} の空きスロットへ {@code predicate} に一致する手持ちアイテムを 1 個入れる。 */
    private boolean insertSlot(Container inventory, int slot, BrewingStandBlockEntity tile,
                               java.util.function.Predicate<ItemStack> predicate) {
        if (!tile.getItem(slot).isEmpty()) {
            return false;
        }
        OptionalInt found = findSlot(inventory, predicate);
        if (found.isEmpty()) {
            return false;
        }
        ItemStack maidStack = inventory.getItem(found.getAsInt());
        tile.setItem(slot, maidStack.split(1));
        return true;
    }

    private OptionalInt findSlot(Container inventory, java.util.function.Predicate<ItemStack> predicate) {
        return ModeHelpers.findSlot(inventory, predicate);
    }

    private boolean hasBrewableBottle(ServerLevel level, Container inventory) {
        return findSlot(inventory, stack -> BrewingSupport.isBrewableBottle(level, stack)).isPresent();
    }

    private OptionalInt findReagent(ServerLevel level, Container inventory, ItemStack bottle) {
        for (int i = 0; i < inventory.getContainerSize(); i++) {
            ItemStack maidStack = inventory.getItem(i);
            if (!maidStack.isEmpty() && isReagentFor(level, maidStack, bottle)) {
                return OptionalInt.of(i);
            }
        }
        return OptionalInt.empty();
    }

    private boolean isReagentFor(ServerLevel level, ItemStack stack, ItemStack bottle) {
        return level.recipeAccess().propertySet(RecipePropertySet.BREWING_REAGENTS).test(stack)
                && level.recipeAccess()
                .getRecipeFor(RecipeType.BREWING, new BrewingInput(bottle, stack), level).isPresent();
    }

    /** 瓶・試薬・燃料が揃い、醸造が進行中か（いずれかの瓶があれば進行中）。 */
    private boolean isBrewing(BrewingStandBlockEntity tile) {
        for (int bSlot = 0; bSlot < BrewingSupport.BOTTLE_SLOTS; bSlot++) {
            if (!tile.getItem(bSlot).isEmpty()) {
                return !tile.getItem(BrewingSupport.SLOT_REAGENT).isEmpty()
                        && !tile.getItem(BrewingSupport.SLOT_FUEL).isEmpty();
            }
        }
        return false;
    }

    /** この醸造台に寄る価値があるか（作業がある、または醸成待ち）。 */
    private boolean shouldApproach(LittleMaidEntity mob, BrewingStandBlockEntity tile) {
        return findNextBrewAction(mob, tile).isPresent() || isBrewing(tile);
    }

    public Optional<BlockPos> findBrewingStandPos(LittleMaidEntity mob) {
        if (!(mob.level() instanceof ServerLevel level)) {
            return Optional.empty();
        }
        return WorkPoi.findClosest(
                level,
                mob.blockPosition(),
                8,
                type -> type.is(PoiTypes.CLERIC),
                pos -> getBrewingStand(mob, pos).filter(tile -> shouldApproach(mob, tile)).isPresent());
    }

    public Optional<BrewingStandBlockEntity> getBrewingStand(LittleMaidEntity mob, BlockPos pos) {
        return ModeHelpers.getBlockEntity(mob.level(), pos, BrewingStandBlockEntity.class);
    }

    @Override
    public void writeBehaviorData(ValueOutput output) {
        if (brewingStandPos != null) {
            output.putLong("BrewingStandPos", brewingStandPos.asLong());
        }
    }

    @Override
    public void readBehaviorData(ValueInput input) {
        input.getLong("BrewingStandPos").ifPresent(posLong -> brewingStandPos = BlockPos.of(posLong));
    }

    private record BrewAction(Kind kind, int slot) {
        private enum Kind { FUEL, BOTTLE, REAGENT, RESULT }
    }
}
