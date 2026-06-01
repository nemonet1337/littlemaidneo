package work.nemonet.littlemaidneo.entity.mode;

import java.util.Arrays;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraft.world.level.block.entity.BrewingStandBlockEntity;
import work.nemonet.littlemaidneo.api.mode.Mode;
import work.nemonet.littlemaidneo.api.mode.ModeType;
import work.nemonet.littlemaidneo.entity.LMHasInventory;
import work.nemonet.littlemaidneo.entity.LittleMaidEntity;
import work.nemonet.littlemaidneo.util.BlockFinder;

public class PharmcistMode extends Mode {

    private final LittleMaidEntity mob;
    private BlockPos brewingStandPos;
    private int recalcPathTimer;
    private int processTimer;

    public PharmcistMode(
        ModeType<? extends PharmcistMode> modeType,
        String name,
        LittleMaidEntity mob
    ) {
        super(modeType, name);
        this.mob = mob;
    }

    @Override
    public void startModeTask() {}

    @Override
    public boolean shouldExecute() {
        if (this.mob.isStrike()) return false;
        if (!work.nemonet.littlemaidneo.entity.util.TameableUtil.hasTameOwner(this.mob)) return false;

        if (recalcPathTimer > 0) {
            recalcPathTimer--;
            return false;
        }

        var optPos = findBrewingStandPos();
        if (optPos.isEmpty()) {
            recalcPathTimer = 40;
            return false;
        }

        BlockPos pos = optPos.get();
        var tileOpt = getBrewingStand(pos);
        if (tileOpt.isEmpty()) return false;
        var tile = tileOpt.get();

        if (hasBrewingWork(tile)) {
            this.brewingStandPos = pos;
            return true;
        }

        recalcPathTimer = 40;
        return false;
    }

    @Override
    public boolean shouldContinueExecuting() {
        if (this.mob.isStrike()) return false;
        if (this.brewingStandPos == null) return false;
        var tileOpt = getBrewingStand(brewingStandPos);
        if (tileOpt.isEmpty()) return false;
        return hasBrewingWork(tileOpt.get());
    }

    @Override
    public void tick() {
        if (brewingStandPos == null) return;
        var tileOpt = getBrewingStand(brewingStandPos);
        if (tileOpt.isEmpty()) return;
        var tile = tileOpt.get();

        double distanceSq = this.mob.distanceToSqr(brewingStandPos.getX() + 0.5, brewingStandPos.getY(), brewingStandPos.getZ() + 0.5);
        if (distanceSq > 3 * 3) {
            if (--recalcPathTimer < 0) {
                recalcPathTimer = 10;
                this.mob.getNavigation().moveTo(brewingStandPos.getX(), brewingStandPos.getY(), brewingStandPos.getZ(), 1.0);
            }
        } else {
            this.mob.getNavigation().stop();
            this.mob.getLookControl().setLookAt(brewingStandPos.getX() + 0.5, brewingStandPos.getY() + 0.5, brewingStandPos.getZ() + 0.5, 30.0f, 30.0f);

            if (processTimer++ >= 10) {
                processTimer = 0;
                boolean worked = performBrewingInteractions(tile);
                if (worked) {
                    this.mob.swing(net.minecraft.world.InteractionHand.MAIN_HAND);
                }
            }
        }
    }

    @Override
    public void resetTask() {
        this.brewingStandPos = null;
        this.recalcPathTimer = 0;
        this.processTimer = 0;
        this.mob.getNavigation().stop();
    }

    private boolean hasBrewingWork(BrewingStandBlockEntity tile) {
        var inventory = LMHasInventory.getInvAndHands(mob);
        var brewing = mob.level().potionBrewing();

        ItemStack fuelStack = tile.getItem(4);
        if (fuelStack.isEmpty() || (fuelStack.is(Items.BLAZE_POWDER) && fuelStack.getCount() < fuelStack.getMaxStackSize())) {
            for (int i = 0; i < inventory.getContainerSize(); i++) {
                if (inventory.getItem(i).is(Items.BLAZE_POWDER)) return true;
            }
        }

        for (int bSlot = 0; bSlot < 3; bSlot++) {
            if (tile.getItem(bSlot).isEmpty()) {
                for (int i = 0; i < inventory.getContainerSize(); i++) {
                    if (isBrewableBottle(inventory.getItem(i))) return true;
                }
            }
        }

        ItemStack ingredientStack = tile.getItem(3);
        if (ingredientStack.isEmpty()) {
            for (int bSlot = 0; bSlot < 3; bSlot++) {
                ItemStack bottleStack = tile.getItem(bSlot);
                if (!bottleStack.isEmpty()) {
                    for (int i = 0; i < inventory.getContainerSize(); i++) {
                        ItemStack maidStack = inventory.getItem(i);
                        if (!maidStack.isEmpty() && brewing.isIngredient(maidStack) && brewing.hasMix(bottleStack, maidStack)) return true;
                    }
                }
            }
        }

        boolean brewingFinished = tile.getItem(3).isEmpty();
        if (brewingFinished) {
            for (int bSlot = 0; bSlot < 3; bSlot++) {
                if (isFinishedPotion(tile.getItem(bSlot))) {
                    for (int i = 0; i < inventory.getContainerSize(); i++) {
                        if (inventory.getItem(i).isEmpty()) return true;
                    }
                }
            }
        }

        return false;
    }

    private boolean performBrewingInteractions(BrewingStandBlockEntity tile) {
        var inventory = LMHasInventory.getInvAndHands(mob);
        var brewing = mob.level().potionBrewing();
        boolean interacted = false;

        ItemStack fuelStack = tile.getItem(4);
        if (fuelStack.isEmpty() || (fuelStack.is(Items.BLAZE_POWDER) && fuelStack.getCount() < fuelStack.getMaxStackSize())) {
            for (int i = 0; i < inventory.getContainerSize(); i++) {
                ItemStack maidStack = inventory.getItem(i);
                if (maidStack.is(Items.BLAZE_POWDER)) {
                    int toAdd = Math.min(maidStack.getCount(), fuelStack.getMaxStackSize() - fuelStack.getCount());
                    if (fuelStack.isEmpty()) {
                        tile.setItem(4, maidStack.split(toAdd));
                    } else {
                        fuelStack.grow(toAdd);
                        maidStack.shrink(toAdd);
                    }
                    interacted = true;
                    break;
                }
            }
        }

        for (int bSlot = 0; bSlot < 3; bSlot++) {
            ItemStack bottleStack = tile.getItem(bSlot);
            if (bottleStack.isEmpty()) {
                for (int i = 0; i < inventory.getContainerSize(); i++) {
                    ItemStack maidStack = inventory.getItem(i);
                    if (isBrewableBottle(maidStack)) {
                        tile.setItem(bSlot, maidStack.split(1));
                        interacted = true;
                        break;
                    }
                }
            }
        }

        ItemStack ingredientStack = tile.getItem(3);
        if (ingredientStack.isEmpty()) {
            for (int bSlot = 0; bSlot < 3; bSlot++) {
                ItemStack bottleStack = tile.getItem(bSlot);
                if (!bottleStack.isEmpty()) {
                    for (int i = 0; i < inventory.getContainerSize(); i++) {
                        ItemStack maidStack = inventory.getItem(i);
                        if (!maidStack.isEmpty() && brewing.isIngredient(maidStack) && brewing.hasMix(bottleStack, maidStack)) {
                            tile.setItem(3, maidStack.split(1));
                            interacted = true;
                            break;
                        }
                    }
                }
                if (interacted) break;
            }
        }

        boolean brewingFinished = tile.getItem(3).isEmpty();
        if (brewingFinished) {
            for (int bSlot = 0; bSlot < 3; bSlot++) {
                ItemStack bottleStack = tile.getItem(bSlot);
                if (!bottleStack.isEmpty() && isFinishedPotion(bottleStack)) {
                    if (addStackToMaidInventory(inventory, bottleStack)) {
                        tile.setItem(bSlot, ItemStack.EMPTY);
                        interacted = true;
                    }
                }
            }
        }

        return interacted;
    }

    private boolean isBrewableBottle(ItemStack stack) {
        if (stack.isEmpty()) return false;
        if (stack.is(Items.POTION) || stack.is(Items.SPLASH_POTION) || stack.is(Items.LINGERING_POTION)) {
            var contents = stack.get(net.minecraft.core.component.DataComponents.POTION_CONTENTS);
            if (contents != null && contents.potion().isPresent()) {
                var potionHolder = contents.potion().get();
                return potionHolder.is(Potions.WATER)
                    || potionHolder.is(Potions.AWKWARD)
                    || potionHolder.is(Potions.THICK)
                    || potionHolder.is(Potions.MUNDANE);
            }
        }
        return false;
    }

    private boolean isFinishedPotion(ItemStack stack) {
        if (stack.isEmpty()) return false;
        if (stack.is(Items.POTION) || stack.is(Items.SPLASH_POTION) || stack.is(Items.LINGERING_POTION)) {
            var contents = stack.get(net.minecraft.core.component.DataComponents.POTION_CONTENTS);
            if (contents != null && contents.potion().isPresent()) {
                var potionHolder = contents.potion().get();
                return !potionHolder.is(Potions.WATER)
                    && !potionHolder.is(Potions.AWKWARD)
                    && !potionHolder.is(Potions.THICK)
                    && !potionHolder.is(Potions.MUNDANE);
            }
        }
        return false;
    }

    private boolean addStackToMaidInventory(Container inventory, ItemStack stack) {
        for (int i = 0; i < inventory.getContainerSize(); i++) {
            ItemStack slotStack = inventory.getItem(i);
            if (slotStack.isEmpty()) {
                inventory.setItem(i, stack.copy());
                stack.setCount(0);
                return true;
            } else if (ItemStack.isSameItemSameComponents(slotStack, stack) && slotStack.getCount() < slotStack.getMaxStackSize()) {
                int toAdd = Math.min(stack.getCount(), slotStack.getMaxStackSize() - slotStack.getCount());
                slotStack.grow(toAdd);
                stack.shrink(toAdd);
                if (stack.isEmpty()) {
                    return true;
                }
            }
        }
        return stack.isEmpty();
    }

    public Optional<BlockPos> findBrewingStandPos() {
        return BlockFinder.searchTargetBlock(
            mob.blockPosition(),
            this::isNotUsedBrewingStand,
            this::canSeeThrough,
            Arrays.asList(Direction.values()),
            1000
        ).filter(pos -> pos.distManhattan(mob.blockPosition()) < 8);
    }

    public boolean isNotUsedBrewingStand(BlockPos pos) {
        return getBrewingStand(pos)
            .filter(this::isNotUsedBrewingStand)
            .isPresent();
    }

    public Optional<BrewingStandBlockEntity> getBrewingStand(BlockPos pos) {
        return ModeHelpers.getBlockEntity(mob.level(), pos, BrewingStandBlockEntity.class);
    }

    public boolean isNotUsedBrewingStand(BrewingStandBlockEntity tile) {
        for (int slot : tile.getSlotsForFace(Direction.UP)) {
            ItemStack stack = tile.getItem(slot);
            if (!stack.isEmpty()) {
                return false;
            }
        }
        return true;
    }

    public boolean canUseBrewingStand(BrewingStandBlockEntity tile) {
        for (int slot : tile.getSlotsForFace(Direction.UP)) {
            ItemStack stack = tile.getItem(slot);
            if (!stack.isEmpty()) continue;
        }
        return false;
    }

    public boolean canSeeThrough(BlockPos pos) {
        return true; // !mob.world.getBlockState(pos).isSolidBlock(mob.world, pos);
    }
}
