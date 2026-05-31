package work.nemonet.littlemaidneo.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import work.nemonet.littlemaidneo.LittleMaidNeo;

public record OpenMaidManagerScreenC2SPayload() implements CustomPacketPayload {

    public static final OpenMaidManagerScreenC2SPayload INSTANCE = new OpenMaidManagerScreenC2SPayload();

    public static final Type<OpenMaidManagerScreenC2SPayload> TYPE =
            new Type<>(Identifier.fromNamespaceAndPath(LittleMaidNeo.MODID, "open_maid_manager_screen_c2s"));

    public static final StreamCodec<FriendlyByteBuf, OpenMaidManagerScreenC2SPayload> STREAM_CODEC =
            StreamCodec.unit(INSTANCE);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
