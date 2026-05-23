package work.nemonet.littlemaidneo.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import work.nemonet.littlemaidneo.LittleMaidNeo;

public record C2SOpenInventoryPayload(int entityId) implements CustomPacketPayload {

    public static final Type<C2SOpenInventoryPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(LittleMaidNeo.MODID, "open_inventory"));

    public static final StreamCodec<FriendlyByteBuf, C2SOpenInventoryPayload> STREAM_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.VAR_INT, C2SOpenInventoryPayload::entityId,
                    C2SOpenInventoryPayload::new
            );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
