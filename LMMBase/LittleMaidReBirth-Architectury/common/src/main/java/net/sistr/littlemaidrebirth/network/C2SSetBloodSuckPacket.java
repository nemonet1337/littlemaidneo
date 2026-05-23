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
import net.sistr.littlemaidrebirth.entity.util.TameableUtil;

/**
 * クライアントからサーバーへBloodSuckを設定するパケット
 */
public class C2SSetBloodSuckPacket {
    public static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath(LMRBMod.MODID, "set_blood_suck");

    @Environment(EnvType.CLIENT)
    public static void sendC2SPacket(Entity entity, boolean isBloodSuck) {
        RegistryFriendlyByteBuf buf = createC2SPacket(entity, isBloodSuck);
        NetworkManager.sendToServer(ID, buf);
    }

    public static RegistryFriendlyByteBuf createC2SPacket(Entity entity, boolean isBloodSuck) {
        RegistryFriendlyByteBuf buf = new RegistryFriendlyByteBuf(Unpooled.buffer(),
                net.minecraft.core.RegistryAccess.EMPTY);
        buf.writeVarInt(entity.getId());
        buf.writeBoolean(isBloodSuck);
        return buf;
    }

    public static void receiveC2SPacket(RegistryFriendlyByteBuf buf, NetworkManager.PacketContext context) {
        int id = buf.readVarInt();
        boolean isBloodSuck = buf.readBoolean();
        context.queue(() -> applyBloodSuckServer(context.getPlayer(), id, isBloodSuck));
    }

    private static void applyBloodSuckServer(Player player, int id, boolean isBloodSuck) {
        Entity entity = player.level().getEntity(id);
        if (!(entity instanceof LittleMaidEntity maid)) {
            return;
        }
        // ご主人がいて、送信元のプレイヤーがご主人なら
        if (TameableUtil.getTameOwnerUuid(maid)
                .filter(uuid -> player.getUUID().equals(uuid))
                .isPresent()) {
            maid.setBloodSuck(isBloodSuck);
        }
    }
}
