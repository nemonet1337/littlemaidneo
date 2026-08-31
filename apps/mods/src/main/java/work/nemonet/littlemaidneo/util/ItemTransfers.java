package work.nemonet.littlemaidneo.util;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.HopperBlockEntity;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.ResourceHandlerUtil;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.item.VanillaContainerWrapper;
import net.neoforged.neoforge.transfer.transaction.Transaction;
import org.jetbrains.annotations.Nullable;

import java.util.function.Predicate;

/**
 * ブロック側インベントリへの出し入れ。Transfer API を優先し、未対応なら {@link HopperBlockEntity} に落とす。
 */
public final class ItemTransfers {
    private ItemTransfers() {
    }

    public static ItemStack insertIntoBlock(Level level, BlockPos pos, ItemStack stack, @Nullable Direction side) {
        if (stack.isEmpty()) {
            return stack;
        }
        ResourceHandler<ItemResource> handler = level.getCapability(Capabilities.Item.BLOCK, pos, side);
        if (handler != null) {
            try (Transaction tx = Transaction.openRoot()) {
                int inserted = handler.insert(ItemResource.of(stack), stack.getCount(), tx);
                if (inserted > 0) {
                    tx.commit();
                    return stack.copyWithCount(stack.getCount() - inserted);
                }
            }
            return stack;
        }
        var container = HopperBlockEntity.getContainerAt(level, pos);
        if (container == null) {
            return stack;
        }
        return HopperBlockEntity.addItem(null, container, stack, side);
    }

    /**
     * {@code from} ブロックから {@code to} コンテナへ、述語に合うアイテムをできるだけ移す。
     *
     * @return 移動した個数
     */
    public static int moveMatchingToContainer(Level level, BlockPos from, Container to, Predicate<ItemStack> match, @Nullable Direction side) {
        ResourceHandler<ItemResource> dest = VanillaContainerWrapper.of(to);
        ResourceHandler<ItemResource> src = level.getCapability(Capabilities.Item.BLOCK, from, side);
        if (src != null) {
            try (Transaction tx = Transaction.openRoot()) {
                int moved = ResourceHandlerUtil.moveStacking(
                        src,
                        dest,
                        resource -> resource.test(match),
                        Integer.MAX_VALUE,
                        tx);
                if (moved > 0) {
                    tx.commit();
                }
                return moved;
            }
        }
        var container = HopperBlockEntity.getContainerAt(level, from);
        if (container == null) {
            return 0;
        }
        int moved = 0;
        for (int i = 0; i < container.getContainerSize(); i++) {
            ItemStack stack = container.getItem(i);
            if (stack.isEmpty() || !match.test(stack)) {
                continue;
            }
            ItemStack leftover = HopperBlockEntity.addItem(null, to, stack, null);
            if (leftover.getCount() != stack.getCount()) {
                moved += stack.getCount() - leftover.getCount();
                container.setItem(i, leftover);
            }
        }
        return moved;
    }
}
