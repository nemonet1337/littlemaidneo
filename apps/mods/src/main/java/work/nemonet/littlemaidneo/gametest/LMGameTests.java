package work.nemonet.littlemaidneo.gametest;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import work.nemonet.littlemaidneo.LittleMaidNeo;
import work.nemonet.littlemaidneo.entity.LittleMaidEntity;
import work.nemonet.littlemaidneo.entity.util.MaidJobManager;
import work.nemonet.littlemaidneo.entity.util.MaidMode;
import work.nemonet.littlemaidneo.entity.util.TameableUtil;
import work.nemonet.littlemaidneo.setup.ModRegistration;

import java.util.function.Consumer;

/**
 * 最低限の GameTest。JSON の test_instance から参照する。
 */
public final class LMGameTests {
    private LMGameTests() {
    }

    public static final DeferredRegister<Consumer<GameTestHelper>> TEST_FUNCTIONS =
            DeferredRegister.create(BuiltInRegistries.TEST_FUNCTION, LittleMaidNeo.MODID);

    public static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> JOB_SWITCH =
            TEST_FUNCTIONS.register("job_switch", () -> LMGameTests::jobSwitch);

    public static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> CONTRACT =
            TEST_FUNCTIONS.register("contract", () -> LMGameTests::contract);

    public static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> STORE_ITEMS =
            TEST_FUNCTIONS.register("store_items", () -> LMGameTests::storeItems);

    public static void jobSwitch(GameTestHelper helper) {
        LittleMaidEntity maid = spawnContracted(helper, new BlockPos(2, 1, 2));
        maid.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.IRON_SWORD));
        helper.succeedWhen(() -> helper.assertTrue(
                MaidJobManager.JOB_COMBAT.equals(maid.getActiveJobName()),
                Component.literal("expected combat job, got " + maid.getActiveJobName())));
    }

    public static void contract(GameTestHelper helper) {
        LittleMaidEntity maid = helper.spawn(ModRegistration.LITTLE_MAID_ENTITY.get(), new BlockPos(2, 1, 2));
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        maid.contract(player, new ItemStack(Items.CAKE), false);
        helper.assertTrue(maid.isContract(), "maid should be contracted");
        helper.assertTrue(
                TameableUtil.getTameOwnerUuid(maid).filter(id -> id.equals(player.getUUID())).isPresent(),
                "maid should have the mock player as owner");
        helper.succeed();
    }

    public static void storeItems(GameTestHelper helper) {
        BlockPos maidPos = new BlockPos(2, 1, 2);
        BlockPos chestPos = new BlockPos(3, 1, 2);
        LittleMaidEntity maid = spawnContracted(helper, maidPos);
        maid.setMaidMode(MaidMode.FREEDOM);
        maid.setFreedomPos(maid.blockPosition());
        maid.setWorkItemSlotNum(0);
        maid.getInventory().setItem(0, new ItemStack(Items.COBBLESTONE, 16));
        helper.setBlock(chestPos, Blocks.CHEST);
        helper.succeedWhen(() -> helper.assertContainerContains(chestPos, Items.COBBLESTONE));
    }

    private static LittleMaidEntity spawnContracted(GameTestHelper helper, BlockPos pos) {
        LittleMaidEntity maid = helper.spawn(ModRegistration.LITTLE_MAID_ENTITY.get(), pos);
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        maid.contract(player, new ItemStack(Items.CAKE), false);
        TameableUtil.setWait(maid, false);
        return maid;
    }
}
