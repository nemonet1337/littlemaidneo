package work.nemonet.littlemaidneo.entity.ai.behavior;

import com.google.common.collect.ImmutableMap;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.ai.memory.MemoryStatus;
import net.minecraft.world.item.ItemStack;
import work.nemonet.littlemaidneo.config.LMNConfig;
import work.nemonet.littlemaidneo.entity.LMHasInventory;
import work.nemonet.littlemaidneo.entity.LittleMaidEntity;
import work.nemonet.littlemaidneo.entity.mode.ModeHelpers;
import work.nemonet.littlemaidneo.resource.util.LMSounds;
import work.nemonet.littlemaidneo.setup.ModRegistration;
import work.nemonet.littlemaidneo.tags.LMTags;

/**
 * 自己回復（給料アイテム＝砂糖を食べる）。
 *
 * <p>唯一の自己回復経路。旧 {@code LittleMaidEntity#tryEatingFromInventory} は
 * 二重実装だったため削除し、こちらに一本化した。
 *
 * <p>方針:
 * <ul>
 *   <li>対象は「給料タグ付きアイテム」のみ。メインハンド・オフハンドを含む作業ビュー
 *       （{@link LMHasInventory#getWorkView}）を走査する</li>
 *   <li>一般の食料／ポーションは healer ジョブ（飼い主への提供）専用。
 *       {@code MaidJobManager} 側で給料タグを healer フォールバックから除外してある</li>
 *   <li>待機中（IS_WAITING）は発生しない</li>
 * </ul>
 */
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
        // healInterval は「N tick 間隔」（旧実装は +1 tick ずれていた）
        if (++cool < LittleMaidEntity.getConfig().health.healInterval) return;
        cool = 0;

        var healItem = getHealItem(entity, healItemSlot);
        if (!isHealItem(healItem)) {
            healItemSlot = -1;
            return;
        }
        heal(entity, healItem);
    }

    private void heal(LittleMaidEntity entity, ItemStack healItem) {
        entity.heal(LittleMaidEntity.getConfig().health.healAmount);
        consumeHealItem(entity, healItem);
        entity.playSound(SoundEvents.ITEM_PICKUP, 1.0F, entity.getRandom().nextFloat() * 0.1F + 1.0F);
        entity.swing(InteractionHand.MAIN_HAND, net.minecraft.world.item.component.SwingAnimation.DEFAULT, false);
        // 砂糖を食べた演出（changeState と同一のノート粒子）
        entity.level().broadcastEntityEvent(entity, (byte) 72);

        var sound = isHealthFull(entity) ? LMSounds.EAT_SUGAR_MAX_POWER : LMSounds.EAT_SUGAR;
        entity.play(sound);
    }

    /** 作業ビュー（メインハンド→オフハンド→18 スロット）から給料アイテムを探す。 */
    private int findHealItemSlot(LittleMaidEntity entity) {
        return ModeHelpers.findSlot(LMHasInventory.getWorkView(entity), this::isHealItem)
                .orElse(-1);
    }

    private ItemStack getHealItem(LittleMaidEntity entity, int slot) {
        if (slot == -1) return ItemStack.EMPTY;
        var stack = LMHasInventory.getWorkView(entity).getItem(slot);
        if (!isHealItem(stack)) return ItemStack.EMPTY;
        return stack;
    }

    private boolean isHealItem(ItemStack stack) {
        return stack.is(LMTags.Items.MAIDS_SALARY);
    }

    private void consumeHealItem(LittleMaidEntity entity, ItemStack healItem) {
        healItem.shrink(1);
        if (healItem.isEmpty() && healItemSlot != -1) {
            LMHasInventory.getWorkView(entity).removeItemNoUpdate(healItemSlot);
        }
    }

    private boolean isHealthFull(LittleMaidEntity entity) {
        return entity.getHealth() >= entity.getMaxHealth();
    }

    private boolean hasHurtTime(LittleMaidEntity entity) {
        return entity.hurtTime > 0;
    }

    private boolean isEnoughHealth(LittleMaidEntity entity) {
        return entity.getHealth() / entity.getMaxHealth() > LMNConfig.get().health.healDelayThreshold;
    }
}
