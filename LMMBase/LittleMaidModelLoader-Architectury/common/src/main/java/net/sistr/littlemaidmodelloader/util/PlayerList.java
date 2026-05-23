package net.sistr.littlemaidmodelloader.util;

import dev.architectury.injectables.annotations.ExpectPlatform;
import net.minecraft.world.entity.Entity;
import net.minecraft.server.level.ServerPlayer;

import java.util.Collection;

public class PlayerList {

    @ExpectPlatform
    public static Collection<ServerPlayer> tracking(Entity entity) {
        throw new AssertionError();
    }

}
