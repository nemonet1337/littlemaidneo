package work.nemonet.littlemaidneo.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import work.nemonet.littlemaidneo.LittleMaidNeo;

public record C2SSetWorkItemSlotSizePayload(int entityId, int num) implements CustomPacketPayload {

    public static final Type<C2SSetWorkItemSlotSizePayload> TYPE =
            new Type<>(Identifier.fromNamespaceAndPath(LittleMaidNeo.MODID, "set_work_item_slot_size"));

    public static final StreamCodec<FriendlyByteBuf, C2SSetWorkItemSlotSizePayload> STREAM_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.VAR_INT, C2SSetWorkItemSlotSizePayload::entityId,
                    ByteBufCodecs.VAR_INT, C2SSetWorkItemSlotSizePayload::num,
                    C2SSetWorkItemSlotSizePayload::new
            );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
