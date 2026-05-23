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
 * クライアントからサーバーへ仕事アイテムスロット数をセットするパケット
 */
public class C2SSetWorkItemSlotSizePacket {
    public static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath(LMRBMod.MODID,
            "set_work_item_slot_size");

    @Environment(EnvType.CLIENT)
    public static void sendC2SPacket(LittleMaidEntity entity, int num) {
        RegistryFriendlyByteBuf buf = createC2SPacket(entity, num);
        NetworkManager.sendToServer(ID, buf);
    }

    public static RegistryFriendlyByteBuf createC2SPacket(LittleMaidEntity entity, int num) {
        RegistryFriendlyByteBuf buf = new RegistryFriendlyByteBuf(Unpooled.buffer(),
                net.minecraft.core.RegistryAccess.EMPTY);
        buf.writeVarInt(entity.getId());
        buf.writeByte(num);
        return buf;
    }

    public static void receiveC2SPacket(RegistryFriendlyByteBuf buf, NetworkManager.PacketContext context) {
        int id = buf.readVarInt();
        int num = buf.readByte() & 255;
        context.queue(() -> applyServer(context.getPlayer(), id, num));
    }

    private static void applyServer(Player player, int id, int num) {
        Entity entity = player.level().getEntity(id);
        if (!(entity instanceof LittleMaidEntity maid)) {
            return;
        }
        // ご主人がいて、送信元のプレイヤーがご主人なら
        if (TameableUtil.getTameOwnerUuid(maid)
                .filter(uuid -> player.getUUID().equals(uuid))
                .isPresent()) {
            maid.setWorkItemSlotNum(num);
        }
    }
}
