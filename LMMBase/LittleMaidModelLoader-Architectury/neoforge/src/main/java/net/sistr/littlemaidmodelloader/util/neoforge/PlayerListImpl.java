package net.sistr.littlemaidmodelloader.util.neoforge;

import net.minecraft.world.entity.Entity;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerLevel;

import java.util.Collection;

public class PlayerListImpl {

    public static Collection<ServerPlayer> tracking(Entity entity) {
        if (entity.level() instanceof ServerLevel serverLevel) {
            return serverLevel.getChunkSource().chunkMap.getPlayers(entity.chunkPosition(), false);
        }
        return java.util.Collections.emptyList();
    }

}
