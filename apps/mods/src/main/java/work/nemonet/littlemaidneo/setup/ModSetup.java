package work.nemonet.littlemaidneo.setup;

import net.minecraft.world.entity.SpawnPlacementTypes;
import net.minecraft.world.level.levelgen.Heightmap;
import net.neoforged.neoforge.event.entity.RegisterSpawnPlacementsEvent;
import work.nemonet.littlemaidneo.entity.LittleMaidEntity;

public class ModSetup {

    public static void init() {
    }

    public static void onRegisterSpawnPlacements(RegisterSpawnPlacementsEvent event) {
        event.register(
            ModRegistration.LITTLE_MAID_ENTITY.get(),
            SpawnPlacementTypes.ON_GROUND,
            Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
            (type, world, spawnReason, pos, random) -> LittleMaidEntity.isValidNaturalSpawn(world, pos),
            RegisterSpawnPlacementsEvent.Operation.OR
        );
    }
}
