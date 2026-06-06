package work.nemonet.littlemaidneo.entity.ai.behavior;

import com.google.common.collect.ImmutableMap;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.Container;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.ai.behavior.Behavior;
import net.minecraft.world.entity.ai.memory.MemoryStatus;
import net.minecraft.world.item.ItemStack;
import work.nemonet.littlemaidneo.config.LMRBConfig;
import work.nemonet.littlemaidneo.entity.LittleMaidEntity;
import work.nemonet.littlemaidneo.resource.util.LMSounds;
import work.nemonet.littlemaidneo.setup.ModRegistration;
import work.nemonet.littlemaidneo.tags.LMTags;

public class MaidHealSelfBehavior extends AbstractMaidBehavior {
    private int cool;
    private int healItemSlot = -1;

    public MaidHealSelfBehavior() {
        super(ImmutableMap.of(
                ModRegistration.IS_WAITING.get(), MemoryStatus.VALUE_ABSENT
        ));
    }

    @Override
    protected boolean checkExtraStartConditions(ServerLevel level, LittleMaidEntity entity) {
        if (isHealthFull(entity)) return false;
        if (hasHurtTime(entity) && isEnoughHealth(entity)) return false;

        this.healItemSlot = findHealItemSlot(entity);
        return this.healItemSlot != -1;
    }

    @Override
    protected boolean canStillUse(ServerLevel level, LittleMaidEntity entity, long gameTime) {
        if (isHealthFull(entity)) return false;
        this.healItemSlot = findHealItemSlot(entity);
        return this.healItemSlot != -1;
    }

    @Override
    protected void start(ServerLevel level, LittleMaidEntity entity, long gameTime) {
        entity.getNavigation().stop();
        this.cool = 0;
    }

    @Override
    protected void tick(ServerLevel level, LittleMaidEntity entity, long gameTime) {
        if (0 < cool--) return;
        cool = entity.getConfig().health.healInterval;

        var healItem = getHealItem(entity, healItemSlot);
        if (!isHealItem(healItem)) {
            healItemSlot = -1;
            return;
        }
        heal(entity, healItem);
    }

    private void heal(LittleMaidEntity entity, ItemStack healItem) {
        entity.heal(entity.getConfig().health.healAmount);
        consumeHealItem(entity, healItem);
        entity.playSound(SoundEvents.ITEM_PICKUP, 1.0F, entity.getRandom().nextFloat() * 0.1F + 1.0F);
        entity.swing(InteractionHand.MAIN_HAND);
        
        var sound = isHealthFull(entity) ? LMSounds.EAT_SUGAR_MAX_POWER : LMSounds.EAT_SUGAR;
        entity.play(sound);
    }

    private int findHealItemSlot(LittleMaidEntity entity) {
        Container inv = entity.getInventory();
        for (int i = 0; i < inv.getContainerSize(); i++) {
            ItemStack slotStack = inv.getItem(i);
            if (isHealItem(slotStack)) {
                return i;
            }
        }
        return -1;
    }

    private ItemStack getHealItem(LittleMaidEntity entity, int slot) {
        if (slot == -1) return ItemStack.EMPTY;
        var stack = entity.getInventory().getItem(slot);
        if (!isHealItem(stack)) return ItemStack.EMPTY;
        return stack;
    }

    private boolean isHealItem(ItemStack stack) {
        return stack.is(LMTags.Items.MAIDS_SALARY);
    }

    private void consumeHealItem(LittleMaidEntity entity, ItemStack healItem) {
        healItem.shrink(1);
        if (healItem.isEmpty() && healItemSlot != -1) {
            entity.getInventory().removeItemNoUpdate(healItemSlot);
        }
    }

    private boolean isHealthFull(LittleMaidEntity entity) {
        return entity.getHealth() >= entity.getMaxHealth();
    }

    private boolean hasHurtTime(LittleMaidEntity entity) {
        return entity.hurtTime > 0;
    }

    private boolean isEnoughHealth(LittleMaidEntity entity) {
        return entity.getHealth() / entity.getMaxHealth() > LMRBConfig.get().health.healDelayThreshold;
    }
}
