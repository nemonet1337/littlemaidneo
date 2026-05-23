package work.nemonet.littlemaidneo.util;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;

import java.util.Collection;
import java.util.Collections;

public class PlayerList {
    public static Collection<ServerPlayer> tracking(Entity entity) {
        if (entity.level() instanceof ServerLevel serverLevel) {
            return serverLevel.getChunkSource().chunkMap.getPlayers(entity.chunkPosition(), false);
        }
        return Collections.emptyList();
    }
}
