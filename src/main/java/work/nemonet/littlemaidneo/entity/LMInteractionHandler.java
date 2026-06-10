package work.nemonet.littlemaidneo.entity;

import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUtils;
import net.minecraft.world.item.Items;
import work.nemonet.littlemaidneo.config.LMNConfig;
import work.nemonet.littlemaidneo.entity.util.MaidMode;
import work.nemonet.littlemaidneo.entity.util.TameableUtil;
import work.nemonet.littlemaidneo.network.NetworkHandler;
import work.nemonet.littlemaidneo.resource.util.LMSounds;
import work.nemonet.littlemaidneo.tags.LMTags;

/**
 * メイドさんの右クリック操作（{@link LittleMaidEntity#mobInteract(Player, InteractionHand)}）の
 * アイテム別分岐ロジックの移譲先。
 * <p>
 * 継承元の挙動は使わず、所持アイテムを上から順に判定する。挙動は分離前と同一。
 * 戻り値の意味: SUCCESS=実行+手振り / CONSUME=実行のみ / PASS=非実行・他動作許可 / FAIL=非実行・他動作不許可。
 * 外部から直接参照できない {@code EXPERIENCE_BOTTLE_COST}（パッケージプライベート）・
 * {@code xpReward}（{@code getXpReward_LM()} ブリッジ経由）以外は public API 経由でアクセスする。
 */
final class LMInteractionHandler {

    private LMInteractionHandler() {
    }

    static InteractionResult mobInteract(LittleMaidEntity mob, Player player, InteractionHand hand) {
        if (player.isShiftKeyDown()) {
            return InteractionResult.PASS;
        }
        ItemStack stack = player.getItemInHand(hand);
        // オーナーが居ない場合
        if (TameableUtil.getTameOwnerUuid(mob).isEmpty()) {
            if (stack.is(LMTags.Items.MAIDS_EMPLOYABLE)) {
                return mob.contract(player, stack, false);
            }
            return InteractionResult.PASS;
        }
        // オーナーじゃない場合
        if (TameableUtil.getTameOwnerUuid(mob).isPresent() &&
                !TameableUtil.isTameOwner(mob, player)) {
            if (!mob.level().isClientSide()) {
                player.hurt(mob.level().damageSources().mobAttack(mob), 1.0f); // 0.5ハートダメージ
                mob.playForce(LMSounds.FIND_TARGET_D);
                mob.swing(InteractionHand.MAIN_HAND);
                mob.level().broadcastEntityEvent(mob, (byte) 6); // 怒りエフェクト
            }
            return InteractionResult.SUCCESS;
        }
        // ストライキ時
        if (mob.isStrike()) {
            if (stack.is(LMTags.Items.MAIDS_EMPLOYABLE)) {
                return mob.contract(player, stack, true);
            }
            // ストライキ時に砂糖をドカ食い
            if (stack.is(LMTags.Items.MAIDS_SALARY)) {
                int count = stack.getCount();
                if (count >= 8) {
                    if (!player.getAbilities().instabuild) {
                        stack.shrink(8);
                    }
                    mob.level().broadcastEntityEvent(mob, (byte) 71); // 再雇用エフェクト
                    mob.playSound(SoundEvents.GENERIC_EAT.value(), 1.0F, 1.0F);
                    mob.swing(InteractionHand.MAIN_HAND);

                    mob.setContractMM(true);
                    if (!mob.level().isClientSide()) {
                        NetworkHandler.sendSyncMultiModelS2C(mob, mob);
                    }
                    mob.setStrike(false);
                    mob.itemContractable.setUnpaidTimes(0);
                    mob.getNavigation().stop();
                    mob.setMaidMode(MaidMode.ESCORT);

                    return InteractionResult.SUCCESS;
                } else {
                    mob.level().broadcastEntityEvent(mob, (byte) 6); // 怒りエフェクト
                    mob.playForce(LMSounds.FIND_TARGET_D);
                    player.sendSystemMessage(Component.translatable("chat.littlemaidneo.need_more_sugar_for_strike"));
                    return InteractionResult.CONSUME;
                }
            }
            mob.level().broadcastEntityEvent(mob, (byte) 6);
            return InteractionResult.PASS;
        }
        // 本
        if (stack.is(Items.WRITABLE_BOOK)) {
            if (!mob.level().isClientSide()) {
                BookParameterParser.apply(mob, stack, player);
                player.sendSystemMessage(Component.translatable("chat.littlemaidneo.book_parameters_applied"));
            }
            mob.swing(InteractionHand.MAIN_HAND);
            return InteractionResult.SUCCESS;
        }
        // ケーキ
        if (stack.is(Items.CAKE)) {
            if (!player.getAbilities().instabuild) {
                stack.shrink(1);
            }
            mob.playSound(SoundEvents.GENERIC_EAT.value(), 1.0F, 1.0F);
            mob.swing(InteractionHand.MAIN_HAND);
            if (!mob.level().isClientSide()) {
                mob.addEffect(new MobEffectInstance(MobEffects.SPEED, 600, 1)); // Speed II
                mob.addEffect(new MobEffectInstance(MobEffects.HASTE, 600, 1));      // Haste II
                mob.level().broadcastEntityEvent(mob, (byte) 76);
            }
            return InteractionResult.SUCCESS;
        }
        // サドル持ってるとき
        if (stack.is(Items.SADDLE)) {
            if (!mob.isPassenger()) {
                if (player.isVehicle()) {
                    player.ejectPassengers();
                }
                mob.startRiding(player);
            } else {
                var vehicle = mob.getVehicle();
                if (vehicle == player) {
                    mob.stopRiding();
                }
            }
            return InteractionResult.SUCCESS;
        }
        // 肩車されてるとき
        if (mob.getVehicle() == player) {
            return InteractionResult.PASS;
        }
        // 牛乳
        if (stack.is(Items.MILK_BUCKET)) {
            if (!player.getAbilities().instabuild) {
                player.setItemInHand(hand, new ItemStack(Items.BUCKET));
            }
            mob.removeAllEffects();
            mob.playSound(SoundEvents.GENERIC_DRINK.value(), 1.0F, 1.0F);
            mob.swing(InteractionHand.MAIN_HAND);
            return InteractionResult.SUCCESS;
        }
        // 金リンゴ
        if (stack.is(Items.GOLDEN_APPLE) || stack.is(Items.ENCHANTED_GOLDEN_APPLE)) {
            if (!player.getAbilities().instabuild) {
                stack.shrink(1);
            }
            mob.heal(mob.getMaxHealth());
            mob.playSound(SoundEvents.GENERIC_EAT.value(), 1.0F, 1.0F);
            mob.swing(InteractionHand.MAIN_HAND);
            if (!mob.level().isClientSide()) {
                if (stack.is(Items.ENCHANTED_GOLDEN_APPLE)) {
                    mob.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 400, 1));
                    mob.addEffect(new MobEffectInstance(MobEffects.RESISTANCE, 6000, 0));
                    mob.addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE, 6000, 0));
                    mob.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, 2400, 3));
                } else {
                    mob.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 100, 1));
                    mob.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, 2400, 0));
                }
            }
            return InteractionResult.SUCCESS;
        }
        // 砂糖
        if (stack.is(LMTags.Items.MAIDS_SALARY)) {
            LMNConfig config = LittleMaidEntity.getConfig();
            mob.heal(config.health.healAmount);
            return mob.changeState(player, stack);
        }
        // Freedom切替
        if (stack.getItem() == Items.FEATHER) {
            if (mob.getMaidMode() == MaidMode.ESCORT) {
                mob.level().broadcastEntityEvent(mob, (byte) 73);
                mob.setMaidMode(MaidMode.FREEDOM);
                mob.setFreedomPos(mob.blockPosition());
            } else {
                mob.level().broadcastEntityEvent(mob, (byte) 74);
                mob.setMaidMode(MaidMode.ESCORT);
            }
            return InteractionResult.SUCCESS;
        }
        // Tracer切替
        if ((mob.getMaidMode() == MaidMode.FREEDOM ||
                mob.getMaidMode() == MaidMode.TRACER) &&
                stack.getItem() == Items.REDSTONE) {
            if (mob.getMaidMode() == MaidMode.FREEDOM) {
                mob.level().broadcastEntityEvent(mob, (byte) 75);
                mob.setMaidMode(MaidMode.TRACER);
            } else {
                mob.level().broadcastEntityEvent(mob, (byte) 73);
                mob.setMaidMode(MaidMode.FREEDOM);
                mob.setFreedomPos(mob.blockPosition());
            }
            return InteractionResult.SUCCESS;
        }
        // ガラス瓶->エンチャントの瓶
        if (mob.getXpReward_LM() >= LittleMaidEntity.EXPERIENCE_BOTTLE_COST &&
                stack.is(Items.GLASS_BOTTLE)) {
            mob.level().playSound(
                    null,
                    mob.getX(),
                    mob.getY(),
                    mob.getZ(),
                    SoundEvents.BOTTLE_FILL,
                    SoundSource.PLAYERS,
                    1.0f,
                    1.0f);
            ItemStack itemStack2 = ItemUtils.createFilledResult(
                    stack,
                    player,
                    Items.EXPERIENCE_BOTTLE.getDefaultInstance());
            player.setItemInHand(hand, itemStack2);
            mob.addExperience(-LittleMaidEntity.EXPERIENCE_BOTTLE_COST);
            return InteractionResult.SUCCESS;
        }
        // モブミルク
        if (LittleMaidEntity.getConfig().misc.canMilking && stack.is(Items.BUCKET)) {
            player.playSound(SoundEvents.COW_MILK, 1.0F, 1.0F);
            ItemStack itemStack2 = ItemUtils.createFilledResult(
                    stack,
                    player,
                    Items.MILK_BUCKET.getDefaultInstance());
            player.setItemInHand(hand, itemStack2);
            return InteractionResult.SUCCESS;
        }
        if (stack.getItem() == Items.GUNPOWDER) {
            int maxAccelerationStack = LittleMaidEntity.getConfig().misc.maxAccelerationStack;
            int accelerationTicks = LittleMaidEntity.getConfig().misc.accelerationTicksPerStack;
            // 同期ズレ防止のため、if条件を付加する場合は結果をパケットで送信すること
            int resumeCount = Math.min(maxAccelerationStack, stack.getCount());
            int acTicks = resumeCount * accelerationTicks;
            mob.setAccelerationTicks(acTicks);

            if (!player.getAbilities().instabuild) {
                stack.shrink(resumeCount);
                if (stack.isEmpty()) {
                    player.getInventory().removeItem(stack);
                }
            }

            return InteractionResult.SUCCESS;
        }
        mob.openInventory(player);
        return InteractionResult.SUCCESS;
    }
}
