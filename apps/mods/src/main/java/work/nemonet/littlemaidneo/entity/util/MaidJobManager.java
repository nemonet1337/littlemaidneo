package work.nemonet.littlemaidneo.entity.util;

import net.minecraft.core.component.DataComponents;
import net.minecraft.world.Container;
import net.minecraft.world.InteractionHand;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.BowItem;
import net.minecraft.world.item.CrossbowItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.ShearsItem;
import work.nemonet.littlemaidneo.entity.LMHasInventory;
import work.nemonet.littlemaidneo.entity.LittleMaidEntity;
import work.nemonet.littlemaidneo.item.IRangedWeapon;
import work.nemonet.littlemaidneo.setup.LMDataMaps;
import work.nemonet.littlemaidneo.setup.ModRegistration;
import work.nemonet.littlemaidneo.tags.LMTags;

import java.util.Optional;

/**
 * メイドさんのジョブ（お仕事）管理。
 *
 * <p>開始判定は「メインハンドのアイテム → インベントリ内の最高優先度アイテム」、
 * 継続判定は「メインハンドが現在のジョブに適合するか」。
 * ジョブ名は型安全な {@link MaidJob} で扱う（旧 String 定数は Codec 側に集約済み）。
 */
public class MaidJobManager {
    /** インベントリ走査でジョブを新規開始する最低優先度（タグ／明示 Data Map）。 */
    private static final int INVENTORY_START_PRIORITY = 400;

    /**
     * インベントリ全走査（全スロット × タグ判定）を伴う再評価の間隔。
     * メインハンドの判定は毎 tick 行うため、手持ちアイテムの変更には即応する。
     */
    private static final int INVENTORY_SCAN_INTERVAL = 10;

    public static void tick(LittleMaidEntity maid) {
        MaidJob currentJob = maid.getBrain()
                .getMemory(ModRegistration.ACTIVE_JOB_NAME.get()).orElse(MaidJob.NONE);
        boolean scanInventory = maid.tickCount % INVENTORY_SCAN_INTERVAL == 0;

        if (currentJob != MaidJob.NONE) {
            ItemStack mainHand = maid.getMainHandItem();
            boolean mainHandOk = isModeItemForJob(currentJob, mainHand);
            boolean emptyHandContinue = mainHand.isEmpty() && canContinueJobEmptyHanded(currentJob);

            if (mainHandOk || emptyHandContinue) {
                updateBattleMode(maid);
                return;
            }

            if (!scanInventory) {
                return;
            }

            int index = findItemForJobInInventory(maid, currentJob);
            if (index != -1) {
                switchMainHandItem(maid, index);
                updateBattleMode(maid);
                return;
            }

            endJob(maid);
        }

        Optional<MaidJob> newJob = getJobFromItem(maid.getMainHandItem());
        if (newJob.isPresent()) {
            startJob(maid, newJob.get());
            return;
        }

        if (!scanInventory) {
            return;
        }

        // 無職時のインベントリ開始は Data Map 優先度 400 以上（タグ明示）だけ。
        Optional<InventoryJob> fromInv = findHighestPriorityJobInInventory(maid, INVENTORY_START_PRIORITY);
        if (fromInv.isPresent()) {
            InventoryJob found = fromInv.get();
            switchMainHandItem(maid, found.slot());
            startJob(maid, found.job());
        }
    }

    public static boolean isModeItemForJob(MaidJob job, ItemStack stack) {
        if (stack.isEmpty()) {
            return false;
        }
        MaidJobEntry mapped = mappedJob(stack);
        if (mapped != null && job == mapped.job() && !isSalaryBlocked(job, stack)) {
            return true;
        }
        return isFallbackForJob(job, stack);
    }

    public static boolean canContinueJobEmptyHanded(MaidJob job) {
        return job == MaidJob.PHARMACIST
                || job == MaidJob.COOKING
                || job == MaidJob.HEALER
                || job == MaidJob.TORCHER
                || job == MaidJob.RIPPER;
    }

    private static MaidJobEntry mappedJob(ItemStack stack) {
        MaidJobEntry data = stack.getData(LMDataMaps.MAID_JOB);
        if (data != null) {
            return data;
        }
        // Data Map 未ロード時のタグフォールバック（datapack と同じ対応）
        if (stack.is(LMTags.Items.FENCER_MODE) || stack.is(LMTags.Items.ARCHER_MODE)) {
            return new MaidJobEntry(MaidJob.COMBAT, 400);
        }
        if (stack.is(LMTags.Items.COOKING_MODE)) {
            return new MaidJobEntry(MaidJob.COOKING, 400);
        }
        if (stack.is(LMTags.Items.RIPPER_MODE)) {
            return new MaidJobEntry(MaidJob.RIPPER, 400);
        }
        if (stack.is(LMTags.Items.TORCHER_MODE)) {
            return new MaidJobEntry(MaidJob.TORCHER, 400);
        }
        if (stack.is(LMTags.Items.HEALER_MODE)) {
            return new MaidJobEntry(MaidJob.HEALER, 400);
        }
        if (stack.is(LMTags.Items.PHARMACIST_MODE)) {
            return new MaidJobEntry(MaidJob.PHARMACIST, 400);
        }
        if (stack.is(LMTags.Items.PHARMACIST_INGREDIENTS)) {
            return new MaidJobEntry(MaidJob.PHARMACIST, 100);
        }
        return null;
    }

    private static boolean isSalaryBlocked(MaidJob job, ItemStack stack) {
        return job == MaidJob.PHARMACIST && stack.is(LMTags.Items.MAIDS_SALARY);
    }

    private static Optional<MaidJob> getJobFromItem(ItemStack stack) {
        if (stack.isEmpty()) {
            return Optional.empty();
        }
        MaidJobEntry mapped = mappedJob(stack);
        if (mapped != null && !isSalaryBlocked(mapped.job(), stack)) {
            return Optional.of(mapped.job());
        }
        if (isCombatFallback(stack)) {
            return Optional.of(MaidJob.COMBAT);
        }
        if (isRipperFallback(stack)) {
            return Optional.of(MaidJob.RIPPER);
        }
        if (isTorcherFallback(stack)) {
            return Optional.of(MaidJob.TORCHER);
        }
        if (isHealerFallback(stack)) {
            return Optional.of(MaidJob.HEALER);
        }
        if (isWaterBottle(stack)) {
            return Optional.of(MaidJob.PHARMACIST);
        }
        return Optional.empty();
    }

    private static boolean isFallbackForJob(MaidJob job, ItemStack stack) {
        return switch (job) {
            case COMBAT -> isCombatFallback(stack);
            case RIPPER -> isRipperFallback(stack);
            case TORCHER -> isTorcherFallback(stack);
            case HEALER -> isHealerFallback(stack);
            case PHARMACIST -> isWaterBottle(stack);
            default -> false;
        };
    }

    private static boolean isCombatFallback(ItemStack stack) {
        if (stack.has(DataComponents.WEAPON) || stack.is(ItemTags.AXES)
                || stack.getItem() instanceof IRangedWeapon) {
            return true;
        }
        var modifiers = stack.getAttributeModifiers();
        if (modifiers == null) {
            return false;
        }
        for (var entry : modifiers.modifiers()) {
            if (entry.attribute().is(net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_DAMAGE)) {
                return true;
            }
        }
        return false;
    }

    private static boolean isRipperFallback(ItemStack stack) {
        return stack.getItem() instanceof ShearsItem;
    }

    private static boolean isTorcherFallback(ItemStack stack) {
        return stack.getItem() instanceof BlockItem blockItem
                && 9 < blockItem.getBlock().defaultBlockState().getLightEmission();
    }

    /**
     * healer フォールバック（食料／ポーション）。
     * 給料アイテム（{@link LMTags.Items#MAIDS_SALARY}）は対象外 —
     * 自己回復専用で、飼い主へ渡す用途ではない。
     */
    private static boolean isHealerFallback(ItemStack stack) {
        if (stack.is(LMTags.Items.MAIDS_SALARY)) {
            return false;
        }
        if (stack.get(DataComponents.FOOD) != null) {
            return true;
        }
        var contents = stack.get(DataComponents.POTION_CONTENTS);
        return contents != null && contents.potion().isPresent();
    }

    private static boolean isWaterBottle(ItemStack stack) {
        if (!stack.is(Items.POTION)) {
            return false;
        }
        var contents = stack.get(DataComponents.POTION_CONTENTS);
        return contents != null
                && contents.potion().isPresent()
                && contents.potion().get().is(net.minecraft.world.item.alchemy.Potions.WATER);
    }

    /** ジョブ適合アイテムを手足込みの作業ビュー（メインハンド→オフハンド→18 スロット）から探す。 */
    private static int findItemForJobInInventory(LittleMaidEntity maid, MaidJob job) {
        Container inv = LMHasInventory.getWorkView(maid);
        for (int i = 0; i < inv.getContainerSize(); i++) {
            if (isModeItemForJob(job, inv.getItem(i))) {
                return i;
            }
        }
        return -1;
    }

    private record InventoryJob(int slot, MaidJob job, int priority) {}

    private static Optional<InventoryJob> findHighestPriorityJobInInventory(LittleMaidEntity maid, int minPriority) {
        Container inv = LMHasInventory.getWorkView(maid);
        InventoryJob best = null;
        for (int i = 0; i < inv.getContainerSize(); i++) {
            ItemStack stack = inv.getItem(i);
            if (stack.isEmpty()) {
                continue;
            }
            MaidJobEntry mapped = mappedJob(stack);
            if (mapped == null || mapped.priority() < minPriority || isSalaryBlocked(mapped.job(), stack)) {
                continue;
            }
            if (best == null || mapped.priority() > best.priority()) {
                best = new InventoryJob(i, mapped.job(), mapped.priority());
            }
        }
        return Optional.ofNullable(best);
    }

    /**
     * 作業ビュー {@code index} のアイテムとメインハンドを交換する。
     * index 0（メインハンド自身）は何もしない。
     */
    private static void switchMainHandItem(LittleMaidEntity maid, int index) {
        if (index == 0) {
            return;
        }
        Container inv = LMHasInventory.getWorkView(maid);
        ItemStack target = inv.getItem(index);
        ItemStack tmp = maid.getMainHandItem();
        inv.setItem(index, tmp);
        maid.setItemInHand(InteractionHand.MAIN_HAND, target);
    }

    private static void startJob(LittleMaidEntity maid, MaidJob job) {
        maid.getBrain().setMemory(ModRegistration.ACTIVE_JOB_NAME.get(), job);
        updateBattleMode(maid);
        maid.setModeName(switch (job) {
            case COMBAT -> maid.getActiveBattle() == BattleMode.BOW ? "Archer" : "Fencer";
            case COOKING -> "Cooking";
            case RIPPER -> "Ripper";
            case TORCHER -> "Torcher";
            case HEALER -> "Healer";
            case PHARMACIST -> "Pharmacist";
            case NONE -> "";
        });
    }

    private static void endJob(LittleMaidEntity maid) {
        maid.getBrain().eraseMemory(ModRegistration.ACTIVE_JOB_NAME.get());
        maid.getBrain().eraseMemory(ModRegistration.ACTIVE_BATTLE_MODE.get());
        maid.setModeName("");
    }

    private static void updateBattleMode(LittleMaidEntity maid) {
        MaidJob job = maid.getBrain().getMemory(ModRegistration.ACTIVE_JOB_NAME.get()).orElse(MaidJob.NONE);
        if (job != MaidJob.COMBAT) {
            maid.getBrain().eraseMemory(ModRegistration.ACTIVE_BATTLE_MODE.get());
            return;
        }

        ItemStack main = maid.getMainHandItem();
        Item item = main.getItem();
        boolean melee = main.has(DataComponents.WEAPON)
                || main.is(ItemTags.AXES)
                || main.is(LMTags.Items.FENCER_MODE);
        if (melee) {
            maid.getBrain().setMemory(ModRegistration.ACTIVE_BATTLE_MODE.get(), BattleMode.SWORD);
            return;
        }

        boolean ranged = item instanceof BowItem
                || item instanceof CrossbowItem
                || item instanceof IRangedWeapon
                || main.is(LMTags.Items.ARCHER_MODE);
        if (ranged) {
            maid.getBrain().setMemory(ModRegistration.ACTIVE_BATTLE_MODE.get(), BattleMode.BOW);
        } else {
            maid.getBrain().setMemory(ModRegistration.ACTIVE_BATTLE_MODE.get(), BattleMode.SWORD);
        }
    }
}
