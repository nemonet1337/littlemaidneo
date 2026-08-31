package work.nemonet.littlemaidneo.entity.util;

import net.minecraft.core.component.DataComponents;
import net.minecraft.world.Container;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.AxeItem;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.BowItem;
import net.minecraft.world.item.CrossbowItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.ShearsItem;
import net.minecraft.world.item.alchemy.Potions;
import work.nemonet.littlemaidneo.entity.LittleMaidEntity;
import work.nemonet.littlemaidneo.item.IRangedWeapon;
import work.nemonet.littlemaidneo.setup.LMDataMaps;
import work.nemonet.littlemaidneo.setup.ModRegistration;
import work.nemonet.littlemaidneo.tags.LMTags;

import java.util.Optional;

public class MaidJobManager {
    public static final String JOB_NONE = "none";
    public static final String JOB_COMBAT = "combat";
    public static final String JOB_COOKING = "cooking";
    public static final String JOB_RIPPER = "ripper";
    public static final String JOB_TORCHER = "torcher";
    public static final String JOB_HEALER = "healer";
    public static final String JOB_PHARMCIST = "pharmcist";

    public static final String BATTLE_NONE = "none";
    public static final String BATTLE_SWORD = "sword";
    public static final String BATTLE_BOW = "bow";

    /** インベントリ走査でジョブを新規開始する最低優先度（タグ／明示 Data Map）。 */
    private static final int INVENTORY_START_PRIORITY = 400;

    /**
     * インベントリ全走査（全スロット × タグ判定）を伴う再評価の間隔。
     * メインハンドの判定は毎 tick 行うため、手持ちアイテムの変更には即応する。
     */
    private static final int INVENTORY_SCAN_INTERVAL = 10;

    public static void tick(LittleMaidEntity maid) {
        String currentJob = maid.getBrain().getMemory(ModRegistration.ACTIVE_JOB_NAME.get()).orElse(JOB_NONE);
        boolean scanInventory = maid.tickCount % INVENTORY_SCAN_INTERVAL == 0;

        if (!currentJob.equals(JOB_NONE)) {
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

        Optional<String> newJob = getJobFromItem(maid.getMainHandItem());
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

    public static boolean isModeItemForJob(String job, ItemStack stack) {
        if (stack.isEmpty()) {
            return false;
        }
        MaidJobEntry mapped = mappedJob(stack);
        if (mapped != null && job.equals(mapped.job()) && !isSalaryBlocked(job, stack)) {
            return true;
        }
        return isFallbackForJob(job, stack);
    }

    public static boolean canContinueJobEmptyHanded(String job) {
        return JOB_PHARMCIST.equals(job)
                || JOB_COOKING.equals(job)
                || JOB_HEALER.equals(job)
                || JOB_TORCHER.equals(job)
                || JOB_RIPPER.equals(job);
    }

    private static MaidJobEntry mappedJob(ItemStack stack) {
        MaidJobEntry data = stack.getData(LMDataMaps.MAID_JOB);
        if (data != null) {
            return data;
        }
        // Data Map 未ロード時のタグフォールバック（datapack と同じ対応）
        if (stack.is(LMTags.Items.FENCER_MODE) || stack.is(LMTags.Items.ARCHER_MODE)) {
            return new MaidJobEntry(JOB_COMBAT, 400);
        }
        if (stack.is(LMTags.Items.COOKING_MODE)) {
            return new MaidJobEntry(JOB_COOKING, 400);
        }
        if (stack.is(LMTags.Items.RIPPER_MODE)) {
            return new MaidJobEntry(JOB_RIPPER, 400);
        }
        if (stack.is(LMTags.Items.TORCHER_MODE)) {
            return new MaidJobEntry(JOB_TORCHER, 400);
        }
        if (stack.is(LMTags.Items.HEALER_MODE)) {
            return new MaidJobEntry(JOB_HEALER, 400);
        }
        if (stack.is(LMTags.Items.PHARMCIST_MODE)) {
            return new MaidJobEntry(JOB_PHARMCIST, 400);
        }
        if (stack.is(LMTags.Items.PHARMCIST_INGREDIENTS)) {
            return new MaidJobEntry(JOB_PHARMCIST, 100);
        }
        return null;
    }

    private static boolean isSalaryBlocked(String job, ItemStack stack) {
        return JOB_PHARMCIST.equals(job) && stack.is(LMTags.Items.MAIDS_SALARY);
    }

    private static Optional<String> getJobFromItem(ItemStack stack) {
        if (stack.isEmpty()) {
            return Optional.empty();
        }
        MaidJobEntry mapped = mappedJob(stack);
        if (mapped != null && !isSalaryBlocked(mapped.job(), stack)) {
            return Optional.of(mapped.job());
        }
        if (isCombatFallback(stack)) {
            return Optional.of(JOB_COMBAT);
        }
        if (isRipperFallback(stack)) {
            return Optional.of(JOB_RIPPER);
        }
        if (isTorcherFallback(stack)) {
            return Optional.of(JOB_TORCHER);
        }
        if (isHealerFallback(stack)) {
            return Optional.of(JOB_HEALER);
        }
        if (isWaterBottle(stack)) {
            return Optional.of(JOB_PHARMCIST);
        }
        return Optional.empty();
    }

    private static boolean isFallbackForJob(String job, ItemStack stack) {
        return switch (job) {
            case JOB_COMBAT -> isCombatFallback(stack);
            case JOB_RIPPER -> isRipperFallback(stack);
            case JOB_TORCHER -> isTorcherFallback(stack);
            case JOB_HEALER -> isHealerFallback(stack);
            case JOB_PHARMCIST -> isWaterBottle(stack);
            default -> false;
        };
    }

    private static boolean isCombatFallback(ItemStack stack) {
        if (stack.has(DataComponents.WEAPON) || stack.getItem() instanceof AxeItem
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

    private static boolean isHealerFallback(ItemStack stack) {
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
                && contents.potion().get().is(Potions.WATER);
    }

    private static int findItemForJobInInventory(LittleMaidEntity maid, String job) {
        Container inv = maid.getInventory();
        for (int i = 0; i < inv.getContainerSize(); i++) {
            if (isModeItemForJob(job, inv.getItem(i))) {
                return i;
            }
        }
        return -1;
    }

    private record InventoryJob(int slot, String job, int priority) {}

    private static Optional<InventoryJob> findHighestPriorityJobInInventory(LittleMaidEntity maid, int minPriority) {
        Container inv = maid.getInventory();
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

    private static void switchMainHandItem(LittleMaidEntity maid, int index) {
        Container inv = maid.getInventory();
        ItemStack invStack = inv.getItem(index);
        ItemStack tmp = maid.getMainHandItem();
        maid.setItemInHand(InteractionHand.MAIN_HAND, invStack);
        inv.setItem(index, tmp);
    }

    private static void startJob(LittleMaidEntity maid, String job) {
        maid.getBrain().setMemory(ModRegistration.ACTIVE_JOB_NAME.get(), job);
        updateBattleMode(maid);

        String displayName = job.substring(0, 1).toUpperCase() + job.substring(1);
        if (JOB_COMBAT.equals(job)) {
            String battleMode = maid.getBrain().getMemory(ModRegistration.ACTIVE_BATTLE_MODE.get()).orElse("");
            displayName = BATTLE_BOW.equals(battleMode) ? "Archer" : "Fencer";
        }
        maid.setModeName(displayName);
    }

    private static void endJob(LittleMaidEntity maid) {
        maid.getBrain().eraseMemory(ModRegistration.ACTIVE_JOB_NAME.get());
        maid.getBrain().eraseMemory(ModRegistration.ACTIVE_BATTLE_MODE.get());
        maid.setModeName("");
    }

    private static void updateBattleMode(LittleMaidEntity maid) {
        String job = maid.getBrain().getMemory(ModRegistration.ACTIVE_JOB_NAME.get()).orElse(JOB_NONE);
        if (!job.equals(JOB_COMBAT)) {
            maid.getBrain().eraseMemory(ModRegistration.ACTIVE_BATTLE_MODE.get());
            return;
        }

        ItemStack main = maid.getMainHandItem();
        Item item = main.getItem();
        boolean melee = main.has(DataComponents.WEAPON)
                || item instanceof AxeItem
                || main.is(LMTags.Items.FENCER_MODE);
        if (melee) {
            maid.getBrain().setMemory(ModRegistration.ACTIVE_BATTLE_MODE.get(), BATTLE_SWORD);
            return;
        }

        boolean ranged = item instanceof BowItem
                || item instanceof CrossbowItem
                || item instanceof IRangedWeapon
                || main.is(LMTags.Items.ARCHER_MODE);
        if (ranged) {
            maid.getBrain().setMemory(ModRegistration.ACTIVE_BATTLE_MODE.get(), BATTLE_BOW);
        } else {
            maid.getBrain().setMemory(ModRegistration.ACTIVE_BATTLE_MODE.get(), BATTLE_SWORD);
        }
    }
}
