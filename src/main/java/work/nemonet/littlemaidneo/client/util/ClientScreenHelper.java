package work.nemonet.littlemaidneo.client.util;

import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.Level;
import work.nemonet.littlemaidneo.client.screen.ModelSelectScreen;
import work.nemonet.littlemaidneo.entity.MultiModelEntity;
import work.nemonet.littlemaidneo.entity.LittleMaidEntity;
import work.nemonet.littlemaidneo.entity.util.TameableUtil;

import java.util.Optional;

public class ClientScreenHelper {
    public static void openModelSelectScreen(Level level, MultiModelEntity entity) {
        Minecraft.getInstance().setScreen(new ModelSelectScreen(
                Component.translatable("screen.littlemaidneo.model_select"), level, entity));
    }

    public static boolean shouldShowOwnerName(LittleMaidEntity maid) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null && TameableUtil.hasTameOwner(maid) && !TameableUtil.isTameOwner(maid, mc.player)) {
            if (mc.hitResult instanceof net.minecraft.world.phys.EntityHitResult hit && hit.getEntity() == maid) {
                return true;
            }
        }
        return false;
    }

    public static Optional<String> getOwnerNameForClient(LittleMaidEntity maid) {
        return TameableUtil.getTameOwnerUuid(maid).flatMap(uuid -> {
            Minecraft mc = Minecraft.getInstance();
            if (mc.player != null && !mc.player.getUUID().equals(uuid)) {
                var connection = mc.getConnection();
                if (connection != null) {
                    var info = connection.getPlayerInfo(uuid);
                    if (info != null) {
                        return Optional.of(info.getProfile().name());
                    }
                }
                var owner = maid.getOwner();
                if (owner != null) {
                    return Optional.of(owner.getName().getString());
                }
                return Optional.of("???");
            }
            return Optional.empty();
        });
    }
}
