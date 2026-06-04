package work.nemonet.littlemaidneo.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import work.nemonet.littlemaidneo.LittleMaidNeo;
import work.nemonet.littlemaidneo.entity.util.MaidMode;

public record C2SSetMovingStatePayload(int entityId, MaidMode movingMode) implements CustomPacketPayload {

    public static final Type<C2SSetMovingStatePayload> TYPE =
            new Type<>(Identifier.fromNamespaceAndPath(LittleMaidNeo.MODID, "set_moving_state"));

    public static final StreamCodec<FriendlyByteBuf, C2SSetMovingStatePayload> STREAM_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.VAR_INT, C2SSetMovingStatePayload::entityId,
                    MaidMode.STREAM_CODEC, C2SSetMovingStatePayload::movingMode,
                    C2SSetMovingStatePayload::new
            );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
