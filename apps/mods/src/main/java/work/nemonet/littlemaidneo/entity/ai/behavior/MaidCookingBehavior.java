package work.nemonet.littlemaidneo.entity.ai.behavior;

import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
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
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.entity.AbstractFurnaceBlockEntity;
import net.minecraft.world.level.block.entity.HopperBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.pathfinder.PathComputationType;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import work.nemonet.littlemaidneo.entity.LittleMaidEntity;
import work.nemonet.littlemaidneo.entity.ai.WorkPoi;
import work.nemonet.littlemaidneo.entity.mode.ModeHelpers;
import work.nemonet.littlemaidneo.resource.util.LMSounds;
import work.nemonet.littlemaidneo.setup.ModRegistration;
import net.minecraft.world.entity.ai.village.poi.PoiTypes;

import java.util.Map;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.function.Predicate;

public class MaidCookingBehavior extends AbstractMaidBehavior implements PersistentMaidBehavior {
    private static final Object2ObjectOpenHashMap<BlockPos, LittleMaidEntity> USED_FURNACE_MAP = new Object2ObjectOpenHashMap<>();
    private BlockPos furnacePos;
    private int timeToRecalcPath;
    private int findCool;
    private int playSoundCool;
    private AbstractFurnaceBlockEntity furnace;

    public MaidCookingBehavior() {
        super(Map.of(
                work.nemonet.littlemaidneo.setup.ModRegistration.ACTIVE_JOB_NAME.get(), MemoryStatus.VALUE_PRESENT
        ));
    }

    @Override
    protected boolean checkExtraStartConditions(ServerLevel level, LittleMaidEntity mob) {
        String job = mob.getBrain().getMemory(work.nemonet.littlemaidneo.setup.ModRegistration.ACTIVE_JOB_NAME.get()).orElse("");
        if (!job.equals("cooking")) {
            return false;
        }

        if (0 < --findCool) {
            return false;
        }
        findCool = 20;
        AbstractFurnaceBlockEntity prev;
        if (furnacePos != null && furnacePos.closerToCenterThan(mob.position(), 6)
                && (prev = getFurnaceBlockEntity(mob, furnacePos).orElse(null)) != null
                && !isUsingFurnaceByOtherMaid(mob, furnacePos)) {
            if (!prev.isEmpty()) {
                furnace = prev;
                return true;
            }
        } else {
            furnacePos = null;
        }

        if (getFuel(mob).isEmpty()) {
            return false;
        }
        if (furnacePos == null
                || !canCookingFurnace(mob, furnace = getFurnaceBlockEntity(mob, furnacePos).orElseThrow())) {
            furnacePos = findFurnacePos(mob).orElse(null);
            if (furnacePos == null) {
                return false;
            }
            furnace = getFurnaceBlockEntity(mob, furnacePos).orElseThrow();
            return true;
        }
        return true;
    }

    @Override
    protected void start(ServerLevel level, LittleMaidEntity mob, long gameTime) {
        findCool = 0;
        USED_FURNACE_MAP.put(furnacePos, mob);
        mob.play(LMSounds.COOKING_START);
        playSoundCool = 20;
    }

    @Override
    protected boolean canStillUse(ServerLevel level, LittleMaidEntity mob, long gameTime) {
        String job = mob.getBrain().getMemory(work.nemonet.littlemaidneo.setup.ModRegistration.ACTIVE_JOB_NAME.get()).orElse("");
        if (!job.equals("cooking")) {
            return false;
        }
        if (furnacePos == null) {
            return false;
        }
        var tmp = getFurnaceBlockEntity(mob, furnacePos).orElse(null);
        if (tmp != furnace) {
            furnacePos = null;
            furnace = null;
            return false;
        }
        ItemStack result = furnace.getItem(2);
        if (!result.isEmpty()) {
            return true;
        }
        boolean burning = ModeHelpers.isFurnaceLit(furnace);
        if (burning) {
            for (int availableSlot : furnace.getSlotsForFace(Direction.UP)) {
                if (!furnace.getItem(availableSlot).isEmpty()) {
                    return true;
                }
            }
        }
        var recipeType = ModeHelpers.furnaceRecipeType(furnace);
        return (burning || getFuel(mob).isPresent())
                && getAnyCookableItem(mob, recipeType, i -> true).isPresent();
    }

    @Override
    protected void tick(ServerLevel level, LittleMaidEntity mob, long gameTime) {
        mob.getLookControl().setLookAt(
                furnacePos.getX() + 0.5,
                furnacePos.getY() + 0.5,
                furnacePos.getZ() + 0.5);

        if (mob.isShiftKeyDown() && !mob.blockPosition().closerThan(furnacePos, 1.75)) {
            mob.setShiftKeyDown(false);
        }
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

        if (!mob.isShiftKeyDown()) {
            mob.setShiftKeyDown(true);
        }

        Container inventory = mob.getInventory();
        RecipeType<? extends AbstractCookingRecipe> recipeType = ModeHelpers.furnaceRecipeType(furnace);
        playSoundCool--;

        getCookable(mob, recipeType).ifPresent(cookableIndex -> tryInsertCookable(mob, furnace, inventory, cookableIndex));
        getFuel(mob).ifPresent(fuelIndex -> tryInsertFuel(mob, furnace, inventory, fuelIndex));
        tryExtractItem(mob, furnace, inventory);
    }

    @Override
    protected void stop(ServerLevel level, LittleMaidEntity mob, long gameTime) {
        playSoundCool = 0;
        mob.setShiftKeyDown(false);
        if (furnacePos != null) {
            USED_FURNACE_MAP.remove(furnacePos, mob);
            AbstractFurnaceBlockEntity f = getFurnaceBlockEntity(mob, furnacePos).orElse(null);
            if (f != null) {
                for (int i = 0; i < f.getContainerSize(); i++) {
                    var stack = f.getItem(i);
                    if (!stack.isEmpty()) {
                        stack = HopperBlockEntity.addItem(null, mob.getInventory(), stack, null);
                        if (stack.isEmpty()) {
                            f.removeItemNoUpdate(i);
                        } else {
                            f.setItem(i, stack);
                        }
                    }
                }
            }
        }
    }

    private OptionalInt getFuel(LittleMaidEntity mob) {
        return ModeHelpers.findSlot(mob.getInventory(), stack -> mob.level().fuelValues().isFuel(stack));
    }

    private Optional<BlockPos> findFurnacePos(LittleMaidEntity mob) {
        if (!(mob.level() instanceof ServerLevel level)) {
            return Optional.empty();
        }
        return WorkPoi.findClosest(
                level,
                mob.blockPosition(),
                8,
                type -> type.is(ModRegistration.FURNACE_POI) || type.is(PoiTypes.ARMORER) || type.is(PoiTypes.BUTCHER),
                pos -> isSearchable(mob, pos) && isTargetFurnace(mob, pos));
    }

    private boolean isTargetFurnace(LittleMaidEntity mob, BlockPos pos) {
        if (isUsingFurnaceByOtherMaid(mob, pos)) {
            return false;
        }
        return getFurnaceBlockEntity(mob, pos)
                .filter(AbstractFurnaceBlockEntity::isEmpty)
                .filter(tile -> canCookingFurnace(mob, tile))
                .isPresent();
    }

    private Optional<AbstractFurnaceBlockEntity> getFurnaceBlockEntity(LittleMaidEntity mob, BlockPos pos) {
        return ModeHelpers.getBlockEntity(mob.level(), pos, AbstractFurnaceBlockEntity.class);
    }

    private boolean canCookingFurnace(LittleMaidEntity mob, AbstractFurnaceBlockEntity tile) {
        RecipeType<? extends AbstractCookingRecipe> recipeType = ModeHelpers.furnaceRecipeType(tile);
        for (int slot : tile.getSlotsForFace(Direction.UP)) {
            ItemStack stack = tile.getItem(slot);
            if (!stack.isEmpty())
                continue;
            if (getAnyCookableItem(mob, recipeType, cookable -> tile.canPlaceItemThroughFace(slot, cookable, Direction.UP))
                    .isPresent()) {
                return true;
            }
        }
        return false;
    }

    private boolean isUsingFurnaceByOtherMaid(LittleMaidEntity mob, BlockPos furnacePos) {
        var user = USED_FURNACE_MAP.get(furnacePos);
        if (user != null && user != mob) {
            if (!user.isAlive() || user != user.level().getEntity(user.getId())) {
                USED_FURNACE_MAP.remove(furnacePos);
                return false;
            }
            return true;
        }
        return false;
    }

    private Optional<ItemStack> getAnyCookableItem(LittleMaidEntity mob, RecipeType<? extends AbstractCookingRecipe> recipeType,
                                                    Predicate<ItemStack> predicate) {
        Container inventory = mob.getInventory();
        for (int i = 0; i < inventory.getContainerSize(); ++i) {
            ItemStack slotStack = inventory.getItem(i);
            if (!slotStack.isEmpty()
                    && getRecipe(mob, slotStack, recipeType).isPresent()
                    && predicate.test(slotStack)) {
                return Optional.of(slotStack);
            }
        }
        return Optional.empty();
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

    private OptionalInt getCookable(LittleMaidEntity mob, RecipeType<? extends AbstractCookingRecipe> recipeType) {
        return ModeHelpers.findSlot(mob.getInventory(), stack -> getRecipe(mob, stack, recipeType).isPresent());
    }

    private void tryInsertCookable(LittleMaidEntity mob, AbstractFurnaceBlockEntity furnace, Container inventory, int cookableIndex) {
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
            pickupAction(mob);
            break;
        }
    }

    private void tryInsertFuel(LittleMaidEntity mob, AbstractFurnaceBlockEntity furnace, Container inventory, int fuelIndex) {
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
            pickupAction(mob);
            if (playSoundCool < 0) {
                playSoundCool = 20;
                mob.play(LMSounds.ADD_FUEL);
            }
            break;
        }
    }

    private void tryExtractItem(LittleMaidEntity mob, AbstractFurnaceBlockEntity furnace, Container inventory) {
        int[] resultSlots = furnace.getSlotsForFace(Direction.DOWN);
        for (int resultSlot : resultSlots) {
            ItemStack resultStack = furnace.getItem(resultSlot);
            if (resultStack.isEmpty()) {
                continue;
            }
            if (!furnace.canTakeItemThroughFace(resultSlot, resultStack, Direction.DOWN)) {
                continue;
            }
            pickupAction(mob);
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

    private void pickupAction(LittleMaidEntity mob) {
        mob.swing(InteractionHand.MAIN_HAND);
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
