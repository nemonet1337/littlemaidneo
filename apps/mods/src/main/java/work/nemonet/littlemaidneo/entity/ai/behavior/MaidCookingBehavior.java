package work.nemonet.littlemaidneo.entity.ai.behavior;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.Container;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.ai.memory.MemoryStatus;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.AbstractCookingRecipe;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.entity.AbstractFurnaceBlockEntity;
import net.minecraft.world.level.block.entity.HopperBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.pathfinder.PathComputationType;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import work.nemonet.littlemaidneo.entity.LMHasInventory;
import work.nemonet.littlemaidneo.entity.LittleMaidEntity;
import work.nemonet.littlemaidneo.entity.ai.WorkPoi;
import work.nemonet.littlemaidneo.entity.mode.ModeHelpers;
import work.nemonet.littlemaidneo.entity.util.MaidJob;
import work.nemonet.littlemaidneo.resource.util.LMSounds;
import work.nemonet.littlemaidneo.setup.ModRegistration;
import net.minecraft.world.entity.ai.village.poi.PoiTypes;

import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.Set;

public class MaidCookingBehavior extends AbstractMaidBehavior implements PersistentMaidBehavior {
    /**
     * 他のメイドが声称しているかまどを探す半径。
     * 仕事場 POI の探索範囲（8）に、当人のかまどまでの寄り（1.75）を足した
     * 9.75 を超えると他メイドの声称を見落とすため 10 を使う。
     */
    private static final double CLAIM_SEARCH_RADIUS = 10;

    /** バニラかまどのスロット構成（{@code AbstractFurnaceBlockEntity} と同じ配置）。 */
    private static final int SLOT_INPUT = 0;
    private static final int SLOT_FUEL = 1;
    private static final int SLOT_RESULT = 2;

    /** 焼けるアイテムのスキャン結果キャッシュ間隔（tick）。レシピ照会はここで間引く。 */
    private static final int COOKABLE_CACHE_TICKS = 20;
    /** 他のメイドのかまど再調査の間隔（tick）。 */
    private static final int CLAIM_CACHE_TICKS = 40;
    /** 焼けるアイテムのスキャン未評価。 */
    private static final int UNEVALUATED = -2;

    private BlockPos furnacePos;
    private int timeToRecalcPath;
    private int findCool;
    private int playSoundCool;
    private AbstractFurnaceBlockEntity furnace;

    /** 焼けるアイテムの作業ビュー index（未評価 {@link #UNEVALUATED}、無し -1）。 */
    private int cookableSlot = UNEVALUATED;
    private int cookableCacheCool;
    private Set<BlockPos> claimedFurnaces = Set.of();
    private int claimedFurnacesCool;

    public MaidCookingBehavior() {
        super(Map.of(
                ModRegistration.ACTIVE_JOB_NAME.get(), MemoryStatus.VALUE_PRESENT
        ));
    }

    @Override
    protected boolean checkExtraStartConditions(ServerLevel level, LittleMaidEntity mob) {
        if (!requireJob(mob, MaidJob.COOKING)) {
            return false;
        }

        if (0 < --findCool) {
            return false;
        }
        findCool = 20;

        AbstractFurnaceBlockEntity current = null;
        if (furnacePos != null && furnacePos.closerToCenterThan(mob.position(), 6)
                && !claimedFurnaces(mob).contains(furnacePos)) {
            current = getFurnaceBlockEntity(mob, furnacePos).orElse(null);
            if (current == null) {
                // チャンク再読込・破壊で stale になった主張は捨てる
                furnacePos = null;
            } else if (!current.isEmpty()) {
                setFurnace(current);
                return true;
            }
        } else {
            furnacePos = null;
        }

        // 燃料の有無より先に、今対応できるかまどがあるかを確認する。
        // （燃料がなくても、かまど内の材料が焼き上がるまで待機する価値がある。）
        AbstractFurnaceBlockEntity target = getFurnaceBlockEntity(mob, furnacePos).orElse(null);
        if (furnacePos == null || !hasFurnaceWork(mob, target)) {
            BlockPos found = findFurnacePos(mob).orElse(null);
            if (found == null) {
                furnacePos = null;
                return false;
            }
            target = getFurnaceBlockEntity(mob, found).orElse(null);
            if (target == null) {
                return false;
            }
            furnacePos = found;
        }
        setFurnace(target);
        return true;
    }

    @Override
    protected void start(ServerLevel level, LittleMaidEntity mob, long gameTime) {
        findCool = 0;
        mob.play(LMSounds.COOKING_START);
        playSoundCool = 20;
    }

    @Override
    protected boolean canStillUse(ServerLevel level, LittleMaidEntity mob, long gameTime) {
        if (!requireJob(mob, MaidJob.COOKING)) {
            return false;
        }
        if (furnacePos == null) {
            return false;
        }
        var tmp = getFurnaceBlockEntity(mob, furnacePos).orElse(null);
        if (tmp != furnace) {
            furnacePos = null;
            setFurnace(null);
            return false;
        }
        return hasFurnaceWork(mob, furnace);
    }

    @Override
    protected void tick(ServerLevel level, LittleMaidEntity mob, long gameTime) {
        mob.getLookControl().setLookAt(
                furnacePos.getX() + 0.5,
                furnacePos.getY() + 0.5,
                furnacePos.getZ() + 0.5);

        var navResult = ModeHelpers.approach(mob, furnacePos, 1.0, timeToRecalcPath, 10, 1.75, 2);
        timeToRecalcPath = navResult.nextTimer();
        if (navResult.unreachable()) {
            furnacePos = null;
            return;
        }
        if (!mob.blockPosition().closerThan(furnacePos, 1.75)) {
            return;
        }
        mob.getNavigation().stop();

        Container inventory = LMHasInventory.getWorkView(mob);
        playSoundCool--;

        int cookableIndex = cookableSlot(mob);
        if (cookableIndex != -1) {
            tryInsertCookable(mob, furnace, inventory, cookableIndex);
        }
        getFuel(mob).ifPresent(fuelIndex -> tryInsertFuel(mob, furnace, inventory, fuelIndex));
        tryExtractItem(mob, furnace, inventory);
    }

    @Override
    protected void stop(ServerLevel level, LittleMaidEntity mob, long gameTime) {
        playSoundCool = 0;
        if (furnacePos != null) {
            AbstractFurnaceBlockEntity f = getFurnaceBlockEntity(mob, furnacePos).orElse(null);
            if (f != null) {
                // 結果スロットだけ回収する。燃料（1）は他者の投入物なので放置する。
                var stack = f.getItem(SLOT_RESULT);
                if (!stack.isEmpty()) {
                    stack = HopperBlockEntity.addItem(null, LMHasInventory.getWorkView(mob), stack, null);
                    if (stack.isEmpty()) {
                        f.removeItemNoUpdate(SLOT_RESULT);
                    } else {
                        f.setItem(SLOT_RESULT, stack);
                    }
                }
            }
        }
        // 他メイドにかまどを譲るため、このメイドの声称を必ず消す
        furnacePos = null;
    }

    /**
     * このメイドさんがこのかまでできる仕事があるか。
     *
     * <ul>
     *   <li>結果スロットに完成品がある → 回収</li>
     *   <li>燃焼中で入力がある → 焼き上がり待ち</li>
     *   <li>材料があり、入力が空・燃焼中・燃料持ち のいずれか → 投入</li>
     * </ul>
     */
    private boolean hasFurnaceWork(LittleMaidEntity mob, AbstractFurnaceBlockEntity tile) {
        if (tile == null) {
            return false;
        }
        if (!tile.getItem(SLOT_RESULT).isEmpty()) {
            return true;
        }
        if (!tile.getItem(SLOT_INPUT).isEmpty() && ModeHelpers.isFurnaceLit(tile)) {
            return true;
        }
        if (cookableSlot(mob) == -1) {
            return false;
        }
        return tile.getItem(SLOT_INPUT).isEmpty()
                || ModeHelpers.isFurnaceLit(tile)
                || getFuel(mob).isPresent();
    }

    /** かまどの差し替え時に焼けるアイテムのスキャンキャッシュを破棄する。 */
    private void setFurnace(AbstractFurnaceBlockEntity tile) {
        if (this.furnace != tile) {
            this.cookableSlot = UNEVALUATED;
        }
        this.furnace = tile;
    }

    /**
     * 焼けるアイテムの作業ビュー index（無ければ -1）。
     * レシピ照会は全スロット走査になるため {@link #COOKABLE_CACHE_TICKS} tick キャッシュする。
     */
    private int cookableSlot(LittleMaidEntity mob) {
        if (this.cookableSlot != UNEVALUATED && 0 < this.cookableCacheCool) {
            return this.cookableSlot;
        }
        var recipeType = ModeHelpers.furnaceRecipeType(this.furnace);
        var inventory = LMHasInventory.getWorkView(mob);
        this.cookableSlot = -1;
        for (int i = 0; i < inventory.getContainerSize(); ++i) {
            ItemStack slotStack = inventory.getItem(i);
            if (!slotStack.isEmpty() && getRecipe(mob, slotStack, recipeType).isPresent()) {
                this.cookableSlot = i;
                break;
            }
        }
        this.cookableCacheCool = COOKABLE_CACHE_TICKS;
        return this.cookableSlot;
    }

    private OptionalInt getFuel(LittleMaidEntity mob) {
        return ModeHelpers.findSlot(LMHasInventory.getWorkView(mob), stack -> stack.has(DataComponents.COOKING_FUEL));
    }

    private Optional<BlockPos> findFurnacePos(LittleMaidEntity mob) {
        if (!(mob.level() instanceof ServerLevel level)) {
            return Optional.empty();
        }
        Set<BlockPos> claimed = claimedFurnaces(mob);
        return WorkPoi.findClosest(
                level,
                mob.blockPosition(),
                8,
                type -> type.is(ModRegistration.FURNACE_POI) || type.is(PoiTypes.ARMORER) || type.is(PoiTypes.BUTCHER),
                pos -> isSearchable(mob, pos) && isTargetFurnace(mob, pos, claimed));
    }

    /**
     * 対象かまどか。完全空は求めない — 結果が空で、入力か燃料のどちらかが空いていて、
     * 尚且つこのメイドさんに仕事があれば乗り取る（プレイヤー／他メイドさんが中断したかまどを継続）。
     */
    private boolean isTargetFurnace(LittleMaidEntity mob, BlockPos pos, Set<BlockPos> claimed) {
        if (claimed.contains(pos)) {
            return false;
        }
        return getFurnaceBlockEntity(mob, pos)
                .filter(tile -> tile.getItem(SLOT_RESULT).isEmpty())
                .filter(tile -> tile.getItem(SLOT_INPUT).isEmpty() || tile.getItem(SLOT_FUEL).isEmpty())
                .filter(tile -> hasFurnaceWork(mob, tile))
                .isPresent();
    }

    private Optional<AbstractFurnaceBlockEntity> getFurnaceBlockEntity(LittleMaidEntity mob, BlockPos pos) {
        return ModeHelpers.getBlockEntity(mob.level(), pos, AbstractFurnaceBlockEntity.class);
    }

    /**
     * 半径 {@link #CLAIM_SEARCH_RADIUS} 以内にいる他のメイドが声称しているかまどを列挙する。
     * static なマップを保持せずワールドを直接問い合わせるため、次元跨ぎの衝突も despawn 時のリークも起きない。
     * 結果は {@link #CLAIM_CACHE_TICKS} tick キャッシュする（POI 探索〜 canStillUse で共有）。
     */
    private Set<BlockPos> claimedFurnaces(LittleMaidEntity mob) {
        if (0 < --this.claimedFurnacesCool) {
            return this.claimedFurnaces;
        }
        this.claimedFurnacesCool = CLAIM_CACHE_TICKS;
        Set<BlockPos> claimed = new HashSet<>();
        for (LittleMaidEntity other : mob.level().getEntitiesOfClass(
                LittleMaidEntity.class, mob.getBoundingBox().inflate(CLAIM_SEARCH_RADIUS))) {
            var behavior = other.cookingBehavior;
            if (behavior != null && behavior.furnacePos != null) {
                claimed.add(behavior.furnacePos);
            }
        }
        this.claimedFurnaces = claimed;
        return claimed;
    }

    private Optional<? extends AbstractCookingRecipe> getRecipe(LittleMaidEntity mob, ItemStack stack,
                                                                   RecipeType<? extends AbstractCookingRecipe> recipeType) {
        var server = mob.level().getServer();
        if (server == null) return Optional.empty();
        return server.getRecipeManager()
                .getRecipeFor(recipeType, new net.minecraft.world.item.crafting.SingleRecipeInput(stack), mob.level())
                .map(net.minecraft.world.item.crafting.RecipeHolder::value);
    }

    private boolean isSearchable(LittleMaidEntity mob, BlockPos pos) {
        BlockState state;
        return Math.abs(pos.getY() - mob.getY()) < 2
                && pos.closerToCenterThan(mob.position(), 6)
                && ((state = mob.level().getBlockState(pos))
                .isPathfindable(PathComputationType.LAND)
                || (state.getBlock() instanceof DoorBlock
                && ((DoorBlock) state.getBlock()).type().canOpenByHand()));
    }

    private void tryInsertCookable(LittleMaidEntity mob, AbstractFurnaceBlockEntity furnace, Container inventory, int cookableIndex) {
        ItemStack materialSlotStack = furnace.getItem(SLOT_INPUT);
        if (!materialSlotStack.isEmpty()) {
            return;
        }
        ItemStack material = inventory.getItem(cookableIndex);
        if (!furnace.canPlaceItemThroughFace(SLOT_INPUT, material, Direction.UP)) {
            return;
        }
        furnace.setItem(SLOT_INPUT, material);
        inventory.removeItemNoUpdate(cookableIndex);
        this.cookableSlot = UNEVALUATED;
        furnace.setChanged();
        pickupAction(mob);
    }

    private void tryInsertFuel(LittleMaidEntity mob, AbstractFurnaceBlockEntity furnace, Container inventory, int fuelIndex) {
        ItemStack fuelSlotStack = furnace.getItem(SLOT_FUEL);
        if (!fuelSlotStack.isEmpty()) {
            return;
        }
        ItemStack fuel = inventory.getItem(fuelIndex);
        if (!furnace.canPlaceItemThroughFace(SLOT_FUEL, fuel, Direction.NORTH)) {
            return;
        }
        furnace.setItem(SLOT_FUEL, fuel);
        inventory.removeItemNoUpdate(fuelIndex);
        furnace.setChanged();
        pickupAction(mob);
        if (playSoundCool < 0) {
            playSoundCool = 20;
            mob.play(LMSounds.ADD_FUEL);
        }
    }

    private void tryExtractItem(LittleMaidEntity mob, AbstractFurnaceBlockEntity furnace, Container inventory) {
        ItemStack resultStack = furnace.getItem(SLOT_RESULT);
        if (resultStack.isEmpty()) {
            return;
        }
        if (!furnace.canTakeItemThroughFace(SLOT_RESULT, resultStack, Direction.DOWN)) {
            return;
        }
        pickupAction(mob);
        if (playSoundCool < 0) {
            playSoundCool = 20;
            mob.play(LMSounds.COOKING_OVER);
        }
        ItemStack copy = resultStack.copy();
        ItemStack leftover = HopperBlockEntity.addItem(furnace, inventory, furnace.removeItem(SLOT_RESULT, 1), null);
        if (leftover.isEmpty()) {
            furnace.setChanged();
            return;
        }
        furnace.setItem(SLOT_RESULT, copy);
    }

    private void pickupAction(LittleMaidEntity mob) {
        mob.swing(InteractionHand.MAIN_HAND, net.minecraft.world.item.component.SwingAnimation.DEFAULT, false);
        mob.playSound(SoundEvents.ITEM_PICKUP, 1.0F, mob.getRandom().nextFloat() * 0.1F + 1.0F);
    }

    @Override
    public void writeBehaviorData(ValueOutput output) {
        if (furnacePos != null) {
            output.putLong("FurnacePos", furnacePos.asLong());
        }
    }

    @Override
    public void readBehaviorData(ValueInput input) {
        input.getLong("FurnacePos").ifPresent(posLong -> furnacePos = BlockPos.of(posLong));
    }
}
