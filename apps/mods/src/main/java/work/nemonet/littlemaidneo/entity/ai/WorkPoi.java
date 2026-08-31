package work.nemonet.littlemaidneo.entity.ai;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.ai.village.poi.PoiManager;
import net.minecraft.world.entity.ai.village.poi.PoiType;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.stream.Stream;

/**
 * 仕事場（かまど・醸造台・チェスト・給料箱）の POI 探索。
 * バニラが既に持っている職場 POI は再利用し、衝突する BlockState は登録しない。
 */
public final class WorkPoi {
    private WorkPoi() {
    }

    public static Optional<BlockPos> findClosest(
            ServerLevel level,
            BlockPos origin,
            int range,
            Predicate<Holder<PoiType>> type,
            Predicate<BlockPos> posPred) {
        return level.getPoiManager().findClosest(type, posPred, origin, range, PoiManager.Occupancy.ANY);
    }

    public static Stream<BlockPos> findAll(
            ServerLevel level,
            BlockPos origin,
            int range,
            Predicate<Holder<PoiType>> type) {
        return level.getPoiManager()
                .getInRange(type, origin, range, PoiManager.Occupancy.ANY)
                .map(record -> record.getPos());
    }
}
