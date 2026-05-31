package work.nemonet.littlemaidneo.entity.mode;

import net.minecraft.core.component.DataComponents;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Container;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.item.consume_effects.ApplyStatusEffectsConsumeEffect;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import work.nemonet.littlemaidneo.api.mode.Mode;
import work.nemonet.littlemaidneo.api.mode.ModeType;
import work.nemonet.littlemaidneo.entity.LMHasInventory;
import work.nemonet.littlemaidneo.entity.LittleMaidEntity;
import work.nemonet.littlemaidneo.entity.util.TameableUtil;
import work.nemonet.littlemaidneo.resource.util.LMSounds;

//空腹なら食料を食わせる。ただし害のあるものは食べさせない
//有用なポーション効果があるアイテムは、常時効果が切れないように使用する
//即時回復を含む食料は普通に使う
//即時回復を含むポーションは、体力が減るまで使わない
//…ご主人がアンデッドの場合でも、即時回復を使う。ご主人は死ぬ。
public class HealerMode extends Mode {

    protected final LittleMaidEntity mob;
    protected LivingEntity owner;
    protected int foodIndex;
    protected int potionIndex;

    public HealerMode(
        ModeType<? extends Mode> modeType,
        String name,
        LittleMaidEntity mob
    ) {
        super(modeType, name);
        this.mob = mob;
    }

    @Override
    public boolean shouldExecute() {
        // ざっくり1秒に1回チェック
        if (this.mob.getRandom().nextFloat() > 1 / 20f) {
            return false;
        }
        LivingEntity owner = TameableUtil.getTameOwner(mob).orElse(null);
        if (!(owner instanceof Player)) return false;
        this.owner = owner;
        boolean isHunger = ((Player) owner).getFoodData().needsFood();
        boolean fullHealth = owner.getHealth() >= owner.getMaxHealth();
        return searchInventory(owner, isHunger, fullHealth);
    }

    @Override
    public boolean shouldContinueExecuting() {
        LivingEntity owner = TameableUtil.getTameOwner(mob).orElse(null);
        if (!(owner instanceof Player)) return false;
        boolean isHunger = ((Player) owner).getFoodData().needsFood();
        boolean fullHealth = owner.getMaxHealth() <= owner.getHealth();
        this.owner = owner;
        return searchInventory(owner, isHunger, fullHealth);
    }

    public boolean searchInventory(
        LivingEntity owner,
        boolean isHunger,
        boolean fullHealth
    ) {
        boolean result = false;
        foodIndex = -1;
        potionIndex = -1;
        Container inventory = LMHasInventory.getInvAndHands(mob);
        for (int i = 0; i < inventory.getContainerSize(); ++i) {
            ItemStack stack = inventory.getItem(i);
            if (isHunger && foodIndex == -1 && isFood(stack)) {
                foodIndex = i;
                result = true;
            }
            if (
                potionIndex == -1 &&
                isBeneficialPotion(owner, stack, fullHealth)
            ) {
                potionIndex = i;
                result = true;
            }
        }
        return result;
    }

    public boolean isFood(ItemStack stack) {
        if (!stack.has(DataComponents.FOOD)) return false;
        var consumable = stack.get(DataComponents.CONSUMABLE);
        if (consumable == null) return true;
        // 有害なステータス効果を持つ食料は除外
        return consumable.onConsumeEffects().stream()
                .filter(e -> e instanceof ApplyStatusEffectsConsumeEffect)
                .map(e -> (ApplyStatusEffectsConsumeEffect) e)
                .flatMap(e -> e.effects().stream())
                // allMatchだとエフェクトが無い場合にfalseになってしまう
                .noneMatch(e -> e.getEffect().value().getCategory() != MobEffectCategory.BENEFICIAL);
    }

    public boolean isBeneficialPotion(
        LivingEntity owner,
        ItemStack stack,
        boolean fullHealth
    ) {
        var contents = stack.get(DataComponents.POTION_CONTENTS);
        if (contents == null || contents.potion().isEmpty()) {
            return false;
        }
        var potion = contents.potion().get().value();
        // いずれかひとつでも有用でない効果がある場合はfalse
        if (
            potion
                .getEffects()
                .stream()
                .anyMatch(
                    e ->
                        e.getEffect().value().getCategory() !=
                        MobEffectCategory.BENEFICIAL
                )
        ) {
            return false;
        }
        // コンフィグで害のあるものも食えるか調整可能にする
        return potion
            .getEffects()
            .stream()
            // 即時回復ではないか、体力が減ってるならtrue
            // 即時回復は体力が減っていないとfalse
            .filter(e -> e.getEffect() != MobEffects.INSTANT_HEALTH || !fullHealth)
            // ご主人が持っていないエフェクトか、レベルが上なら適用する
            .anyMatch(
                e ->
                    owner.getActiveEffects().isEmpty() ||
                    owner
                        .getActiveEffects()
                        .stream()
                        // いずれか一つでもご主人が持ってたらダメ
                        .noneMatch(
                            oE ->
                                oE.getEffect() == e.getEffect() &&
                                e.getAmplifier() <= oE.getAmplifier()
                        )
            );
    }

    @Override
    public void tick() {
        Container inventory = LMHasInventory.getInvAndHands(mob);
        // 飯
        if (foodIndex != -1 && foodIndex >= 0 && foodIndex < inventory.getContainerSize()) {
            ItemStack stack = inventory.getItem(foodIndex);
            if (isFood(stack)) {
                stack = stack.finishUsingItem(owner.level(), owner);
                if (stack.isEmpty()) {
                    inventory.removeItemNoUpdate(foodIndex);
                } else {
                    inventory.setItem(foodIndex, stack);
                }
                this.mob.play(LMSounds.HEALING);
            }
            foodIndex = -1;
        }
        // 薬
        if (potionIndex != -1 && potionIndex >= 0 && potionIndex < inventory.getContainerSize()) {
            ItemStack stack = inventory.getItem(potionIndex);
            boolean fullHealth = owner.getHealth() >= owner.getMaxHealth();
            if (isBeneficialPotion(owner, stack, fullHealth)) {
                stack = stack.finishUsingItem(owner.level(), owner);
                if (stack.isEmpty()) {
                    inventory.removeItemNoUpdate(potionIndex);
                } else {
                    inventory.setItem(potionIndex, stack);
                }
                owner
                    .level()
                    .playSound(
                        null,
                        owner.getX(),
                        owner.getY(),
                        owner.getZ(),
                        SoundEvents.GENERIC_DRINK,
                        SoundSource.PLAYERS,
                        1.0f,
                        1.0f
                    );
                this.mob.play(LMSounds.HEALING_POTION);
            }
            potionIndex = -1;
        }
    }
}
