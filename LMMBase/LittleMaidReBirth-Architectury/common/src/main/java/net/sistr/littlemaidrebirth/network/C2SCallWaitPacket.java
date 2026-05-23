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

public class C2SCallWaitPacket {
    public static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath(LMRBMod.MODID, "call_wait");

    @Environment(EnvType.CLIENT)
    public static void sendC2SPacket(Entity entity, State state) {
        RegistryFriendlyByteBuf buf = createC2SPacket(entity, state);
        NetworkManager.sendToServer(ID, buf);
    }

    public static RegistryFriendlyByteBuf createC2SPacket(Entity entity, State state) {
        RegistryFriendlyByteBuf buf = new RegistryFriendlyByteBuf(Unpooled.buffer(),
                net.minecraft.core.RegistryAccess.EMPTY);
        buf.writeVarInt(entity.getId());
        buf.writeEnum(state);
        return buf;
    }

    public static void receiveC2SPacket(RegistryFriendlyByteBuf buf, NetworkManager.PacketContext context) {
        int id = buf.readVarInt();
        State state = buf.readEnum(State.class);
        context.queue(() -> applyMovingStateServer(context.getPlayer(), id, state));
    }

    private static void applyMovingStateServer(Player player, int id, State state) {
        Entity entity = player.level().getEntity(id);
        if (!(entity instanceof LittleMaidEntity maid)
                || !TameableUtil.isTameOwner(maid, player)) {
            return;
        }
        if (maid.isStrike()) {
            return;
        }
        if (state == State.WAIT) {
            TameableUtil.setWait(maid, true);
        } else {
            TameableUtil.setWait(maid, false);
            maid.setMovingMode(MovingMode.ESCORT);
        }
    }

    public enum State {
        WAIT,
        CALL
    }
}
