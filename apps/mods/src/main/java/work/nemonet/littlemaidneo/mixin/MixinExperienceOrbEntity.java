package work.nemonet.littlemaidneo.mixin;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentEffectComponents;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.Level;
import work.nemonet.littlemaidneo.entity.LittleMaidEntity;
import work.nemonet.littlemaidneo.util.LMCollidable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;

@Mixin(ExperienceOrb.class)
public abstract class MixinExperienceOrbEntity extends Entity implements LMCollidable {

    @Shadow
    private int count;

    @Shadow
    public abstract int getValue();

    public MixinExperienceOrbEntity(EntityType<?> type, Level world) {
        super(type, world);
    }

    @Override
    public void onCollision_LM(LittleMaidEntity maid) {
        if (this.level().isClientSide() || maid.experiencePickUpDelay != 0) {
            return;
        }
        maid.experiencePickUpDelay = 2;
        maid.take(this, 1);
        int i = this.repairGears_LM(maid, this.getValue());
        if (i > 0) {
            maid.addExperience(i);
        }
        --this.count;
        if (this.count == 0) {
            this.discard();
        }
    }

    @Unique
    private int repairGears_LM(LittleMaidEntity littleMaid, int amount) {
        var optional = EnchantmentHelper.getRandomItemWith(
                EnchantmentEffectComponents.REPAIR_WITH_XP, littleMaid, ItemStack::isDamaged);
        if (optional.isPresent()) {
            ItemStack itemStack = optional.get().itemStack();
            int i = (littleMaid.level() instanceof net.minecraft.server.level.ServerLevel serverLevel)
                    ? EnchantmentHelper.modifyDurabilityToRepairFromXp(serverLevel, itemStack, amount)
                    : amount;
            int j = Math.min(i, itemStack.getDamageValue());
            itemStack.setDamageValue(itemStack.getDamageValue() - j);
            // 1.21.1: getXpRepairRatio() removed, use default ratio of 2
            int k = amount - (j > 0 ? Math.round((float) j / 2f) : 0);
            return k > 0 ? this.repairGears_LM(littleMaid, k) : 0;
        } else {
            return amount;
        }
    }
}
