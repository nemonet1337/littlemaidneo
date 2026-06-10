package work.nemonet.littlemaidneo.block;

import java.util.List;
import net.minecraft.world.level.block.entity.*;
import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
import net.minecraft.core.Vec3i;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Container;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.entity.ContainerUser;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BarrelBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.ContainerOpenersCounter;
import net.minecraft.world.level.block.entity.RandomizableContainerBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import work.nemonet.littlemaidneo.config.LMNConfig;
import work.nemonet.littlemaidneo.entity.util.SalaryBoxPosListener;
import work.nemonet.littlemaidneo.setup.ModRegistration;
import work.nemonet.littlemaidneo.tags.LMTags;
import org.jetbrains.annotations.Nullable;

public class SalaryBoxBlockEntity extends RandomizableContainerBlockEntity {
    private NonNullList<ItemStack> inventory = NonNullList.withSize(27, ItemStack.EMPTY);
    @Nullable
    private Component customName;
    private final ContainerOpenersCounter stateManager = new ContainerOpenersCounter(){

        @Override
        protected void onOpen(Level world, BlockPos pos, BlockState state) {
            SalaryBoxBlockEntity.this.playSound(state, SoundEvents.BARREL_OPEN);
            SalaryBoxBlockEntity.this.setOpen(state, true);
        }

        @Override
        protected void onClose(Level world, BlockPos pos, BlockState state) {
            SalaryBoxBlockEntity.this.playSound(state, SoundEvents.BARREL_CLOSE);
            SalaryBoxBlockEntity.this.setOpen(state, false);
        }

        @Override
        protected void openerCountChanged(Level world, BlockPos pos, BlockState state, int oldViewerCount, int newViewerCount) {
        }

        @Override
        public boolean isOwnContainer(Player player) {
            if (player.containerMenu instanceof ChestMenu) {
                Container inventory = ((ChestMenu)player.containerMenu).getContainer();
                return inventory == SalaryBoxBlockEntity.this;
            }
            return false;
        }
    };

    public SalaryBoxBlockEntity(BlockPos pos, BlockState state) {
        super(ModRegistration.SALARY_BOX_BLOCK_ENTITY.get(), pos, state);
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        if (!this.trySaveLootTable(output)) {
            ContainerHelper.saveAllItems(output, this.inventory);
        }
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        this.inventory = NonNullList.withSize(this.getContainerSize(), ItemStack.EMPTY);
        if (!this.tryLoadLootTable(input)) {
            ContainerHelper.loadAllItems(input, this.inventory);
        }
    }

    @Override
    public int getContainerSize() {
        return 27;
    }

    @Override
    protected NonNullList<ItemStack> getItems() {
        return this.inventory;
    }

    @Override
    protected void setItems(NonNullList<ItemStack> list) {
        this.inventory = list;
    }

    @Override
    protected Component getDefaultName() {
        return Component.translatable("container.littlemaidrebirth.salary_box");
    }

    @Override
    protected AbstractContainerMenu createMenu(int syncId, Inventory playerInventory) {
        return ChestMenu.threeRows(syncId, playerInventory, this);
    }

    public void setCustomName(Component name) {
        this.customName = name;
    }

    @Override
    public Component getDisplayName() {
        return this.customName != null ? this.customName : super.getDisplayName();
    }

    @Override
    public void startOpen(ContainerUser user) {
        if (!this.remove && user.getLivingEntity() instanceof Player player && !player.isSpectator()) {
            this.stateManager.incrementOpeners(player, this.getLevel(), this.getBlockPos(), this.getBlockState(), user.getContainerInteractionRange());
        }
    }

    @Override
    public void stopOpen(ContainerUser user) {
        if (!this.remove && user.getLivingEntity() instanceof Player player && !player.isSpectator()) {
            this.stateManager.decrementOpeners(player, this.getLevel(), this.getBlockPos(), this.getBlockState());
        }
    }

    public void tick() {
        if (!this.remove) {
            this.stateManager.recheckOpeners(this.getLevel(), this.getBlockPos(), this.getBlockState());
        }
    }

    void setOpen(BlockState state, boolean open) {
        this.level.setBlock(this.getBlockPos(), state.setValue(BarrelBlock.OPEN, open), Block.UPDATE_ALL);
    }

    void playSound(BlockState state, SoundEvent soundEvent) {
        Vec3i vec3i = state.getValue(BarrelBlock.FACING).getUnitVec3i();
        double d = this.worldPosition.getX() + 0.5 + vec3i.getX() / 2.0;
        double e = this.worldPosition.getY() + 0.5 + vec3i.getY() / 2.0;
        double f = this.worldPosition.getZ() + 0.5 + vec3i.getZ() / 2.0;
        this.level.playSound(null, d, e, f, soundEvent, SoundSource.BLOCKS, 0.5f, this.level.getRandom().nextFloat() * 0.1f + 0.9f);
    }

    public static boolean isinNotifyRange(Vec3i boxPos, Vec3 entityPos) {
        return boxPos.distToCenterSqr(entityPos) < getConfigNotifyRange() * getConfigNotifyRange();
    }

    public static void tick(Level world, BlockPos pos, BlockState state, SalaryBoxBlockEntity blockEntity) {
        if (!blockEntity.hasSalary()) {
            return;
        }
        if (world.getRandom().nextFloat() > (1.0f / getConfigInterval())) {
            return;
        }

        var centerPos = pos.getCenter();
        float range = getConfigNotifyRange();
        var box = new AABB(
                centerPos.x - range,
                centerPos.y - range,
                centerPos.z - range,
                centerPos.x + range,
                centerPos.y + range,
                centerPos.z + range
        );
        List<Entity> entityList = world.getEntitiesOfClass(Entity.class, box,
                e -> e instanceof SalaryBoxPosListener
                        && isinNotifyRange(pos, e.position()));
        for (Entity entity : entityList) {
            ((SalaryBoxPosListener) entity).listenSalaryBoxPos(pos);
        }
    }

    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) {
        return stack.is(LMTags.Items.MAIDS_SALARY);
    }

    public boolean hasSalary() {
        for (int i = 0; i < this.getContainerSize(); i++) {
            var stack = this.getItem(i);
            if (!stack.isEmpty()
                    && stack.is(LMTags.Items.MAIDS_SALARY)) {
                return true;
            }
        }
        return false;
    }

    private static float getConfigNotifyRange() {
        return LMNConfig.get().contract.memorySalaryBoxDistance;
    }

    private static int getConfigInterval() {
        return LMNConfig.get().contract.memorySalaryBoxInterval;
    }
}
