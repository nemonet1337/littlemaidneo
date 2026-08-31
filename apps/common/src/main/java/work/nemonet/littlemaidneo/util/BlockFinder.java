package work.nemonet.littlemaidneo.util;

import java.util.*;
import java.util.function.Predicate;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;

/**
 * ブロック探索ロジック
 */
public class BlockFinder {

    /**
     * 特定ブロックを探索する。
     * 近い地点から探索していく。
     *
     * @param seed       開始地点
     * @param target     探索対象
     * @param linkable   探索可能ブロック
     * @param directions 探索方向
     * @param maxCount   最大探索回数
     */
    public static Optional<BlockPos> searchTargetBlock(BlockPos seed,
                                                       Predicate<BlockPos> target, Predicate<BlockPos> linkable,
                                                       Collection<Direction> directions, int maxCount) {
        BlockFinderPD finder = new BlockFinderPD(Collections.singletonList(seed), target, linkable, directions, maxCount);
        finder.tick(maxCount);
        return finder.getResult();
    }

}
