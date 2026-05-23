package net.sistr.littlemaidrebirth.network;

import dev.architectury.networking.NetworkManager;
import io.netty.buffer.Unpooled;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.sistr.littlemaidrebirth.LMRBMod;
import net.sistr.littlemaidrebirth.entity.LittleMaidEntity;
import net.sistr.littlemaidrebirth.entity.util.MovingMode;
import net.sistr.littlemaidrebirth.entity.util.TameableUtil;

/**
 * C2Sで移動状態をセットするパケット
 */
public class C2SSetMovingStatePacket {
    public static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath(LMRBMod.MODID, "set_moving_state");

    @Environment(EnvType.CLIENT)
    public static void sendC2SPacket(Entity entity, MovingMode state) {
        RegistryFriendlyByteBuf buf = createC2SPacket(entity, state);
        NetworkManager.sendToServer(ID, buf);
    }

    public static RegistryFriendlyByteBuf createC2SPacket(Entity entity, MovingMode state) {
        RegistryFriendlyByteBuf buf = new RegistryFriendlyByteBuf(Unpooled.buffer(),
                net.minecraft.core.RegistryAccess.EMPTY);
        buf.writeVarInt(entity.getId());
        buf.writeEnum(state);
        return buf;
    }

    public static void receiveC2SPacket(RegistryFriendlyByteBuf buf, NetworkManager.PacketContext context) {
        int id = buf.readVarInt();
        MovingMode movingMode = buf.readEnum(MovingMode.class);
        context.queue(() -> applyMovingStateServer(context.getPlayer(), id, movingMode));
    }

    private static void applyMovingStateServer(Player player, int id, MovingMode movingMode) {
        Entity entity = player.level().getEntity(id);
        if (!(entity instanceof LittleMaidEntity maid)
                || TameableUtil.getTameOwnerUuid(maid)
                        .filter(ownerId -> ownerId.equals(player.getUUID()))
                        .isEmpty()) {
            return;
        }
        if (maid.isStrike()) {
            return;
        }
        maid.setMovingMode(movingMode);
        maid.getNavigation().stop();
        if (movingMode == MovingMode.FREEDOM) {
            maid.setFreedomPos(entity.blockPosition());
        }
    }

}
