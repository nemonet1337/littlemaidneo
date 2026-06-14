package work.nemonet.littlemaidneo.entity.util;

import net.minecraft.core.component.DataComponents;
import net.minecraft.world.Container;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.*;
import work.nemonet.littlemaidneo.entity.LittleMaidEntity;
import work.nemonet.littlemaidneo.item.IRangedWeapon;
import work.nemonet.littlemaidneo.tags.LMTags;

import java.util.*;
import java.util.function.Predicate;

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

    private record JobRule(String jobName, Predicate<ItemStack> predicate, int priority) {}

    private static final List<JobRule> RULES = new ArrayList<>();
    // ジョブ名 -> ルールの索引（isModeItemForJob が全ルールを線形探索しないため）
    private static final Map<String, List<JobRule>> RULES_BY_JOB = new HashMap<>();

    static {
        // Priority: HIGHER = 400, HIGH = 300, NORMAL = 200, LOW = 100, LOWER = 0
        // Combat
        RULES.add(new JobRule(JOB_COMBAT, stack -> stack.is(LMTags.Items.FENCER_MODE), 400));
        RULES.add(new JobRule(JOB_COMBAT, stack -> stack.is(LMTags.Items.ARCHER_MODE), 400));
        RULES.add(new JobRule(JOB_COMBAT, stack -> stack.has(DataComponents.WEAPON), 0));
        RULES.add(new JobRule(JOB_COMBAT, stack -> stack.getItem() instanceof AxeItem, 0));
        RULES.add(new JobRule(JOB_COMBAT, stack -> stack.getItem() instanceof IRangedWeapon, 0));
        RULES.add(new JobRule(JOB_COMBAT, stack -> {
            var modifiers = stack.getAttributeModifiers();
            if (modifiers != null) {
                for (var entry : modifiers.modifiers()) {
                    if (entry.attribute().is(net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_DAMAGE)) {
                        return true;
                    }
                }
            }
            return false;
        }, 0));

        // Cooking
        RULES.add(new JobRule(JOB_COOKING, stack -> stack.is(LMTags.Items.COOKING_MODE), 400));

        // Ripper
        RULES.add(new JobRule(JOB_RIPPER, stack -> stack.is(LMTags.Items.RIPPER_MODE), 400));
        RULES.add(new JobRule(JOB_RIPPER, stack -> stack.getItem() instanceof ShearsItem, 0));

        // Torcher
        RULES.add(new JobRule(JOB_TORCHER, stack -> stack.is(LMTags.Items.TORCHER_MODE), 400));
        RULES.add(new JobRule(JOB_TORCHER, stack -> stack.getItem() instanceof BlockItem
                && 9 < ((BlockItem) stack.getItem()).getBlock().defaultBlockState().getLightEmission(), 0));

        // Healer
        RULES.add(new JobRule(JOB_HEALER, stack -> stack.is(LMTags.Items.HEALER_MODE), 400));
        RULES.add(new JobRule(JOB_HEALER, stack -> stack.get(DataComponents.FOOD) != null, 0));
        RULES.add(new JobRule(JOB_HEALER, stack -> {
            var contents = stack.get(DataComponents.POTION_CONTENTS);
            return contents != null && contents.potion().isPresent();
        }, 0));

        // Pharmacist
        RULES.add(new JobRule(JOB_PHARMCIST, stack -> stack.is(LMTags.Items.PHARMCIST_MODE), 400));

        // 優先度が高い順にソートしておく
        RULES.sort(Comparator.comparingInt(JobRule::priority).reversed());
        for (JobRule rule : RULES) {
            RULES_BY_JOB.computeIfAbsent(rule.jobName(), k -> new ArrayList<>()).add(rule);
        }
    }

    /**
     * インベントリ全走査（全スロット × 全ルールのタグ判定）を伴う再評価の間隔。
     * メインハンドの判定は毎 tick 行うため、手持ちアイテムの変更には即応する。
     */
    private static final int INVENTORY_SCAN_INTERVAL = 10;

    public static void tick(LittleMaidEntity maid) {
        String currentJob = maid.getBrain().getMemory(work.nemonet.littlemaidneo.setup.ModRegistration.ACTIVE_JOB_NAME.get()).orElse(JOB_NONE);
        // インベントリ全走査は重い（多数のメイドさんがいるサーバーで毎 tick 行うと顕著）ため間引く
        boolean scanInventory = maid.tickCount % INVENTORY_SCAN_INTERVAL == 0;

        // 手持ちアイテムが現在のジョブを継続可能か確認
        if (!currentJob.equals(JOB_NONE)) {
            ItemStack mainHand = maid.getMainHandItem();
            if (isModeItemForJob(currentJob, mainHand)) {
                // 継続可能。戦闘の場合は戦闘モードも更新する
                updateBattleMode(maid);
                return;
            }

            if (!scanInventory) {
                return;
            }

            // 継続不可の場合、インベントリ内に現在のジョブのアイテムがあるか確認し、あれば持ち替え
            int index = findItemForJobInInventory(maid, currentJob);
            if (index != -1) {
                switchMainHandItem(maid, index);
                updateBattleMode(maid);
                return;
            }

            // どちらもなければジョブ終了
            endJob(maid);
        }

        // 新しいジョブの決定
        // メインハンドのアイテムから決定
        Optional<String> newJob = getJobFromItem(maid.getMainHandItem());
        if (newJob.isPresent()) {
            startJob(maid, newJob.get());
            return;
        }

        if (!scanInventory) {
            return;
        }

        // メインハンドに無ければ、インベントリ内から優先度の高い順に探索
        for (JobRule rule : RULES) {
            int index = findItemInInventory(maid, rule.predicate());
            if (index != -1) {
                switchMainHandItem(maid, index);
                startJob(maid, rule.jobName());
                return;
            }
        }
    }

    public static boolean isModeItemForJob(String job, ItemStack stack) {
        if (stack.isEmpty()) return false;
        for (JobRule rule : RULES_BY_JOB.getOrDefault(job, List.of())) {
            if (rule.predicate().test(stack)) {
                return true;
            }
        }
        return false;
    }

    private static int findItemForJobInInventory(LittleMaidEntity maid, String job) {
        Container inv = maid.getInventory();
        for (int i = 0; i < inv.getContainerSize(); i++) {
            ItemStack stack = inv.getItem(i);
            if (isModeItemForJob(job, stack)) {
                return i;
            }
        }
        return -1;
    }

    private static int findItemInInventory(LittleMaidEntity maid, Predicate<ItemStack> predicate) {
        Container inv = maid.getInventory();
        for (int i = 0; i < inv.getContainerSize(); i++) {
            ItemStack stack = inv.getItem(i);
            if (!stack.isEmpty() && predicate.test(stack)) {
                return i;
            }
        }
        return -1;
    }

    private static Optional<String> getJobFromItem(ItemStack stack) {
        if (stack.isEmpty()) return Optional.empty();
        for (JobRule rule : RULES) {
            if (rule.predicate().test(stack)) {
                return Optional.of(rule.jobName());
            }
        }
        return Optional.empty();
    }

    private static void switchMainHandItem(LittleMaidEntity maid, int index) {
        Container inv = maid.getInventory();
        ItemStack invStack = inv.getItem(index);
        ItemStack tmp = maid.getMainHandItem();
        maid.setItemInHand(InteractionHand.MAIN_HAND, invStack);
        inv.setItem(index, tmp);
    }

    private static void startJob(LittleMaidEntity maid, String job) {
        maid.getBrain().setMemory(work.nemonet.littlemaidneo.setup.ModRegistration.ACTIVE_JOB_NAME.get(), job);
        updateBattleMode(maid);

        String displayName = job.substring(0, 1).toUpperCase() + job.substring(1);
        if (JOB_COMBAT.equals(job)) {
            String battleMode = maid.getBrain().getMemory(work.nemonet.littlemaidneo.setup.ModRegistration.ACTIVE_BATTLE_MODE.get()).orElse("");
            displayName = BATTLE_BOW.equals(battleMode) ? "Archer" : "Fencer";
        }
        maid.setModeName(displayName);
    }

    private static void endJob(LittleMaidEntity maid) {
        maid.getBrain().eraseMemory(work.nemonet.littlemaidneo.setup.ModRegistration.ACTIVE_JOB_NAME.get());
        maid.getBrain().eraseMemory(work.nemonet.littlemaidneo.setup.ModRegistration.ACTIVE_BATTLE_MODE.get());
        maid.setModeName("");
    }

    private static void updateBattleMode(LittleMaidEntity maid) {
        String job = maid.getBrain().getMemory(work.nemonet.littlemaidneo.setup.ModRegistration.ACTIVE_JOB_NAME.get()).orElse(JOB_NONE);
        if (!job.equals(JOB_COMBAT)) {
            maid.getBrain().eraseMemory(work.nemonet.littlemaidneo.setup.ModRegistration.ACTIVE_BATTLE_MODE.get());
            return;
        }

        ItemStack main = maid.getMainHandItem();
        Item item = main.getItem();
        boolean melee = main.has(DataComponents.WEAPON)
                || item instanceof AxeItem
                || main.is(LMTags.Items.FENCER_MODE);
        if (melee) {
            maid.getBrain().setMemory(work.nemonet.littlemaidneo.setup.ModRegistration.ACTIVE_BATTLE_MODE.get(), BATTLE_SWORD);
            return;
        }

        boolean ranged = item instanceof BowItem
                || item instanceof CrossbowItem
                || item instanceof IRangedWeapon
                || main.is(LMTags.Items.ARCHER_MODE);
        if (ranged) {
            maid.getBrain().setMemory(work.nemonet.littlemaidneo.setup.ModRegistration.ACTIVE_BATTLE_MODE.get(), BATTLE_BOW);
        } else {
            maid.getBrain().setMemory(work.nemonet.littlemaidneo.setup.ModRegistration.ACTIVE_BATTLE_MODE.get(), BATTLE_SWORD);
        }
    }
}
