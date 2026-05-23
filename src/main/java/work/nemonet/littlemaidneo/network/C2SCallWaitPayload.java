package work.nemonet.littlemaidneo.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import work.nemonet.littlemaidneo.LittleMaidNeo;

public record C2SCallWaitPayload(int entityId, C2SCallWaitPayload.State state) implements CustomPacketPayload {

    public static final Type<C2SCallWaitPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(LittleMaidNeo.MODID, "call_wait"));

    public static final StreamCodec<FriendlyByteBuf, C2SCallWaitPayload> STREAM_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.VAR_INT, C2SCallWaitPayload::entityId,
                    ByteBufCodecs.VAR_INT.map(i -> State.values()[i], Enum::ordinal),
                            C2SCallWaitPayload::state,
                    C2SCallWaitPayload::new
            );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public enum State {
        WAIT,
        CALL
    }
}
