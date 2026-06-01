package work.nemonet.littlemaidneo.entity.mode;

import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.Container;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.AbstractCookingRecipe;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.entity.AbstractFurnaceBlockEntity;
import net.minecraft.world.level.block.entity.HopperBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.pathfinder.PathComputationType;
import work.nemonet.littlemaidneo.resource.util.LMSounds;
import work.nemonet.littlemaidneo.api.mode.Mode;
import work.nemonet.littlemaidneo.api.mode.ModeType;
import work.nemonet.littlemaidneo.entity.LittleMaidEntity;
import work.nemonet.littlemaidneo.util.AbstractFurnaceAccessor;
import work.nemonet.littlemaidneo.util.BlockFinder;

import java.util.Arrays;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.function.Predicate;

public class CookingMode extends Mode {
    // 別ディメンションで同一の位置にかまどがある場合はレアケースなので考慮しない
    private static final Object2ObjectOpenHashMap<BlockPos, LittleMaidEntity> USED_FURNACE_MAP = new Object2ObjectOpenHashMap<>();
    private final LittleMaidEntity mob;
    private BlockPos furnacePos;
    private int timeToRecalcPath;
    private int findCool;
    private int playSoundCool;
    private AbstractFurnaceBlockEntity furnace;

    public CookingMode(ModeType<? extends CookingMode> modeType, String name, LittleMaidEntity mob) {
        super(modeType, name);
        this.mob = mob;
    }

    @Override
    public boolean shouldExecute() {
        if (0 < --findCool) {
            return false;
        }
        findCool = 20;
        AbstractFurnaceBlockEntity prev;
        // モードが中断されたあと、再開するときの判定
        // 注視しているかまどがあり、使用可能
        if (furnacePos != null && furnacePos.closerToCenterThan(this.mob.position(), 6)
                && (prev = getFurnaceBlockEntity(furnacePos).orElse(null)) != null
                && !isUsingFurnaceByOtherMaid(furnacePos)) {
            // アイテムが残っている場合はtrue
            if (!prev.isEmpty()) {
                furnace = prev;
                return true;
            }
        } else {
            // かまどは使用不可のためリセット
            furnacePos = null;
        }

        // 物を焼き始めるときの判定

        // 燃料がないならリターン
        if (getFuel().isEmpty()) {
            return false;
        }
        // かまどが無いか、焼けない場合は再探索
        // なお上でチェックしているため、furnacePosがあるならかまどは必ず使用可能
        if (furnacePos == null
                || !canCookingFurnace(furnace = getFurnaceBlockEntity(furnacePos).orElseThrow())) {
            furnacePos = findFurnacePos().orElse(null);
            if (furnacePos == null) {
                return false;
            }
            furnace = getFurnaceBlockEntity(furnacePos).orElseThrow();
            return true;
        }
        return true;
    }

    public OptionalInt getFuel() {
        return ModeHelpers.findSlot(this.mob.getInventory(), this::isFuel);
    }

    public boolean isFuel(ItemStack stack) {
        return mob.level().fuelValues().isFuel(stack);
    }

    /**
     * 使用可能なかまどを探索する。
     */
    public Optional<BlockPos> findFurnacePos() {
        return BlockFinder.searchTargetBlock(this.mob.blockPosition(), this::isTargetFurnace, this::isSearchable,
                Arrays.asList(Direction.values()), 128);
    }

    public boolean isTargetFurnace(BlockPos pos) {
        // 他のメイドさんが使ってるかまどはダメ
        if (isUsingFurnaceByOtherMaid(pos)) {
            return false;
        }
        return getFurnaceBlockEntity(pos)
                .filter(AbstractFurnaceBlockEntity::isEmpty)// 空のかまど
                .filter(this::canCookingFurnace)// 手持ちのアイテムを焼けるかまど
                .isPresent();
    }

    public Optional<AbstractFurnaceBlockEntity> getFurnaceBlockEntity(BlockPos pos) {
        return ModeHelpers.getBlockEntity(mob.level(), pos, AbstractFurnaceBlockEntity.class);
    }

    // 手持ちのアイテムを焼けるかまどかどうか
    public boolean canCookingFurnace(AbstractFurnaceBlockEntity tile) {
        RecipeType<? extends AbstractCookingRecipe> recipeType = ((AbstractFurnaceAccessor) tile).getRecipeType_LM();
        for (int slot : tile.getSlotsForFace(Direction.UP)) {
            ItemStack stack = tile.getItem(slot);
            if (!stack.isEmpty())
                continue;
            // 手持ちに焼けるアイテムがあればtrue
            if (getAnyCookableItem(recipeType,
                    cookable -> tile.canPlaceItemThroughFace(slot, cookable, Direction.UP))
                    .isPresent()) {
                return true;
            }
        }
        return false;
    }

    public boolean isUsingFurnaceByOtherMaid(BlockPos furnacePos) {
        var user = USED_FURNACE_MAP.get(furnacePos);
        if (user != null && user != this.mob) {
            if (!user.isAlive() || user != user.level().getEntity(user.getId())) {
                USED_FURNACE_MAP.remove(furnacePos);
                return false;
            }
            return true;
        }
        return false;
    }

    // インベントリからこのレシピタイプで焼けるアイテムを取得
    public Optional<ItemStack> getAnyCookableItem(RecipeType<? extends AbstractCookingRecipe> recipeType,
            Predicate<ItemStack> predicate) {
        Container inventory = this.mob.getInventory();
        for (int i = 0; i < inventory.getContainerSize(); ++i) {
            ItemStack slotStack = inventory.getItem(i);
            if (!slotStack.isEmpty()
                    && getRecipe(slotStack, recipeType).isPresent()
                    && predicate.test(slotStack)) {
                return Optional.of(slotStack);
            }
        }
        return Optional.empty();
    }

    public Optional<? extends AbstractCookingRecipe> getRecipe(ItemStack stack,
            RecipeType<? extends AbstractCookingRecipe> recipeType) {
        var server = mob.level().getServer();
        if (server == null) return Optional.empty();
        return server.getRecipeManager()
                .getRecipeFor(recipeType, new net.minecraft.world.item.crafting.SingleRecipeInput(stack), mob.level())
                .map(net.minecraft.world.item.crafting.RecipeHolder::value);
    }

    public boolean isSearchable(BlockPos pos) {
        BlockState state;
        return Math.abs(pos.getY() - this.mob.getY()) < 2
                && pos.closerToCenterThan(this.mob.position(), 6)
                && ((state = this.mob.level().getBlockState(pos))
                        .isPathfindable(PathComputationType.LAND)
                        // ドアも通過
                        || (state.getBlock() instanceof DoorBlock
                                && ((DoorBlock) state.getBlock()).type().canOpenByHand()));
    }

    @Override
    public void startExecuting() {
        findCool = 0;
        USED_FURNACE_MAP.put(furnacePos, mob);
        mob.play(LMSounds.COOKING_START);
        playSoundCool = 20;
    }

    @Override
    public boolean shouldContinueExecuting() {
        // かまどがなければfalse
        if (furnacePos == null) {
            return false;
        }
        // かまどが変わっていたら終了
        var tmp = getFurnaceBlockEntity(furnacePos).orElse(null);
        if (tmp != furnace) {
            furnacePos = null;
            furnace = null;
            return false;
        }
        // 結果スロットが埋まってる場合はtrue
        // getAvailableSlots(DOWN)では燃料スロットも取ってしまうため、マジックナンバーに頼らざる負えなかった
        ItemStack result = furnace.getItem(2);
        if (!result.isEmpty()) {
            return true;
        }
        // 何か焼いている場合はtrue
        boolean burning = ((AbstractFurnaceAccessor) furnace).isBurningFire_LM();
        if (burning) {
            for (int availableSlot : furnace.getSlotsForFace(Direction.UP)) {
                if (!furnace.getItem(availableSlot).isEmpty()) {
                    return true;
                }
            }
        }
        var recipeType = ((AbstractFurnaceAccessor) furnace).getRecipeType_LM();
        // 燃料と焼くものがある場合はtrue
        // どちらか無ければfalse
        return (burning || getFuel().isPresent())
                && getAnyCookableItem(recipeType, i -> true).isPresent();
    }

    @Override
    public void tick() {
        // 視線を向ける
        this.mob.getLookControl().setLookAt(
                furnacePos.getX() + 0.5,
                furnacePos.getY() + 0.5,
                furnacePos.getZ() + 0.5);

        // かまどの近くに移動
        if (!this.mob.blockPosition().closerThan(furnacePos, 1.75)) {
            if (this.mob.isShiftKeyDown()) {
                this.mob.setShiftKeyDown(false);
            }
            if (--this.timeToRecalcPath <= 0) {
                this.timeToRecalcPath = 10;
                double x = furnacePos.getX() + 0.5D;
                double y = furnacePos.getY() + 0.5D;
                double z = furnacePos.getZ() + 0.5D;
                var path = this.mob.getNavigation().createPath(x, y, z, 2);
                this.mob.getNavigation().moveTo(path, 1);
            }
            return;
        }
        this.mob.getNavigation().stop();

        // しゃがむ
        if (!this.mob.isShiftKeyDown()) {
            this.mob.setShiftKeyDown(true);
        }

        Container inventory = this.mob.getInventory();

        RecipeType<? extends AbstractCookingRecipe> recipeType = ((AbstractFurnaceAccessor) furnace).getRecipeType_LM();

        playSoundCool--;

        // 焼けるものがあれば突っ込む
        getCookable(recipeType).ifPresent(cookableIndex -> tryInsertCookable(furnace, inventory, cookableIndex));
        // 燃料があれば突っ込む
        getFuel().ifPresent(fuelIndex -> tryInsertFuel(furnace, inventory, fuelIndex));
        // 焼けてたら取り出す
        tryExtractItem(furnace, inventory);
    }

    public OptionalInt getCookable(RecipeType<? extends AbstractCookingRecipe> recipeType) {
        return ModeHelpers.findSlot(this.mob.getInventory(), stack -> getRecipe(stack, recipeType).isPresent());
    }

    private void tryInsertCookable(AbstractFurnaceBlockEntity furnace, Container inventory, int cookableIndex) {
        int[] materialSlots = furnace.getSlotsForFace(Direction.UP);
        for (int materialSlot : materialSlots) {
            ItemStack materialSlotStack = furnace.getItem(materialSlot);
            if (!materialSlotStack.isEmpty()) {
                continue;
            }
            ItemStack material = inventory.getItem(cookableIndex);
            if (!furnace.canPlaceItemThroughFace(materialSlot, material, Direction.UP)) {
                continue;
            }
            furnace.setItem(materialSlot, material);
            inventory.removeItemNoUpdate(cookableIndex);
            furnace.setChanged();
            pickupAction();
            break;
        }
    }

    private void tryInsertFuel(AbstractFurnaceBlockEntity furnace, Container inventory, int fuelIndex) {
        int[] fuelSlots = furnace.getSlotsForFace(Direction.NORTH);
        for (int fuelSlot : fuelSlots) {
            ItemStack fuelSlotStack = furnace.getItem(fuelSlot);
            if (!fuelSlotStack.isEmpty()) {
                continue;
            }
            ItemStack fuel = inventory.getItem(fuelIndex);
            if (!furnace.canPlaceItemThroughFace(fuelSlot, fuel, Direction.NORTH)) {
                continue;
            }
            furnace.setItem(fuelSlot, fuel);
            inventory.removeItemNoUpdate(fuelIndex);
            furnace.setChanged();
            pickupAction();
            if (playSoundCool < 0) {
                playSoundCool = 20;
                mob.play(LMSounds.ADD_FUEL);
            }
            break;
        }
    }

    private void tryExtractItem(AbstractFurnaceBlockEntity furnace, Container inventory) {
        int[] resultSlots = furnace.getSlotsForFace(Direction.DOWN);
        for (int resultSlot : resultSlots) {
            ItemStack resultStack = furnace.getItem(resultSlot);
            if (resultStack.isEmpty()) {
                continue;
            }
            if (!furnace.canTakeItemThroughFace(resultSlot, resultStack, Direction.DOWN)) {
                continue;
            }
            pickupAction();
            if (playSoundCool < 0) {
                playSoundCool = 20;
                mob.play(LMSounds.COOKING_OVER);
            }
            ItemStack copy = resultStack.copy();
            ItemStack leftover = HopperBlockEntity.addItem(furnace, inventory, furnace.removeItem(resultSlot, 1), null);
            if (leftover.isEmpty()) {
                furnace.setChanged();
                continue;
            }

            furnace.setItem(resultSlot, copy);
        }
    }

    public void pickupAction() {
        this.mob.swing(InteractionHand.MAIN_HAND);
        this.mob.playSound(SoundEvents.ITEM_PICKUP, 1.0F, this.mob.getRandom().nextFloat() * 0.1F + 1.0F);
    }

    @Override
    public void resetTask() {
        playSoundCool = 0;
        this.mob.setShiftKeyDown(false);
        if (furnacePos != null) {
            USED_FURNACE_MAP.remove(furnacePos, mob);
            AbstractFurnaceBlockEntity furnace = getFurnaceBlockEntity(furnacePos).orElse(null);
            if (furnace == null) {
                furnacePos = null;
                return;
            }
            // かまどからアイテムをすべて取り出す
            for (int i = 0; i < furnace.getContainerSize(); i++) {
                var stack = furnace.getItem(i);
                if (!stack.isEmpty()) {
                    stack = HopperBlockEntity.addItem(null, this.mob.getInventory(), stack, null);
                    if (stack.isEmpty()) {
                        furnace.removeItemNoUpdate(i);
                    } else {
                        furnace.setItem(i, stack);
                    }
                }
            }
        }
    }

    @Override
    public void writeModeData(CompoundTag nbt) {
        if (furnacePos != null)
            nbt.putLong("FurnacePos", furnacePos.asLong());
    }

    @Override
    public void readModeData(CompoundTag nbt) {
        if (nbt.contains("FurnacePos"))
            furnacePos = BlockPos.of(nbt.getLongOr("FurnacePos", 0L));
    }

}
