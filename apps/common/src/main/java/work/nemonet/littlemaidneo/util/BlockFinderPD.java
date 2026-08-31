package work.nemonet.littlemaidneo.util;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Queues;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;
import java.util.Queue;
import java.util.Set;
import java.util.function.Predicate;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;

public class BlockFinderPD implements ProcessDivider<BlockPos> {
    private static final Iterable<Direction> DIRECTIONS = ImmutableList.of(Direction.NORTH, Direction.SOUTH,
            Direction.WEST, Direction.EAST, Direction.UP, Direction.DOWN);
    private final Queue<BlockPos> seeds;
    private final Predicate<BlockPos> target;
    private final Predicate<BlockPos> linkable;
    private final Iterable<Direction> directions;
    private final Set<BlockPos> searched;
    @Nullable
    private BlockPos result;

    public BlockFinderPD(Iterable<BlockPos> seeds, Predicate<BlockPos> target, Predicate<BlockPos> linkable,
                         Iterable<Direction> directions, int expected) {
        this.seeds = Queues.newArrayDeque(seeds);
        this.target = target;
        this.linkable = linkable;
        this.directions = ImmutableList.copyOf(directions);
        this.searched = new ObjectOpenHashSet<>(expected);
        for (BlockPos seed : seeds) {
            if (this.target.test(seed)) {
                result = seed;
                this.seeds.clear();
                return;
            }
        }
    }

    public BlockFinderPD(Iterable<BlockPos> seeds, Predicate<BlockPos> target, Predicate<BlockPos> linkable, int expected) {
        this(seeds, target, linkable, DIRECTIONS, expected);
    }

    @Override
    public boolean tick() {
        while (!isEnd()) {
            var seed = seeds.poll();
            if (seed == null) {
                return false;
            }
            if (searched.contains(seed)) {
                continue;
            }
            searched.add(seed);
            for (Direction direction : directions) {
                BlockPos linkPos = seed.relative(direction);
                if (searched.contains(linkPos)) {
                    continue;
                }
                if (target.test(linkPos)) {
                    result = linkPos;
                    seeds.clear();
                    searched.clear();
                    return true;
                }
                if (linkable.test(linkPos)) {
                    seeds.add(linkPos);
                }
            }
            if (isEnd()) {
                searched.clear();
            }
            return false;
        }
        return false;
    }

    @Override
    public Optional<BlockPos> getResult() {
        return Optional.ofNullable(result);
    }

    @Override
    public boolean isEnd() {
        return seeds.isEmpty();
    }
}
