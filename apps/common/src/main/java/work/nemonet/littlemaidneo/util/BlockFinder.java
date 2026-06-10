package work.nemonet.littlemaidneo.util;

import com.google.common.collect.Queues;
import com.google.common.collect.Sets;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import java.util.*;
import java.util.function.Consumer;
import java.util.function.Predicate;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;

/**
 * ブロック探索ロジック
 */
public class BlockFinder {

    public static void seedFill(BlockPos seed, final int maxCount,
                                Predicate<BlockPos> fillable, Consumer<BlockPos> filler,
                                EnumSet<Direction> directions) {
        Queue<BlockPos> seeds = Queues.newArrayDeque();
        seeds.add(seed);

        int count = 0;
        while (!seeds.isEmpty()) {
            if (maxCount < count++) {
                break;
            }
            seed = seeds.poll();
            //塗りつぶせない場合break
            if (!fillable.test(seed)) {
                break;
            }
            //塗りつぶす
            filler.accept(seed);
            //周囲のマスをシードに追加する
            for (Direction direction : directions) {
                //塗りつぶせるならシードに追加
                if (fillable.test(seed)) {
                    seeds.add(seed.relative(direction));
                }
            }
        }

    }

    /**
     * 特定ブロックを探索する。
     * 近い地点から探索していく。
     * 微妙かもしらん…
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
