package work.nemonet.littlemaidneo.mixin;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.HopperBlockEntity;
import work.nemonet.littlemaidneo.entity.LittleMaidEntity;
import work.nemonet.littlemaidneo.util.LMCollidable;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

import java.util.UUID;

@Mixin(ItemEntity.class)
public abstract class MixinItemEntity extends Entity implements LMCollidable {

    @Shadow
    public abstract ItemStack getItem();

    @Shadow
    private @Nullable UUID target;

    @Shadow
    public abstract boolean hasPickUpDelay();

    public MixinItemEntity(EntityType<?> type, Level world) {
        super(type, world);
    }

    @Override
    public void onCollision_LMRB(LittleMaidEntity maid) {
        if (this.level().isClientSide) {
            return;
        }

        if (this.hasPickUpDelay()
                || (this.target != null && !this.target.equals(maid.getUUID()))) {
            return;
        }

        ItemStack stack = this.getItem();
        int prevCount = stack.getCount();

        stack = HopperBlockEntity.addItem(null, maid.getInventory(), stack, null);
        if (stack.getCount() != prevCount) {
            maid.take(this, prevCount);
            if (stack.isEmpty()) {
                this.discard();
                stack.setCount(prevCount);
            }
            maid.onItemPickup((ItemEntity) (Object) this);
        }
    }
}
