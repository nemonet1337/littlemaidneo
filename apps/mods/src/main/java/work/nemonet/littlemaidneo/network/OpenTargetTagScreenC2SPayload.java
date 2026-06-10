package work.nemonet.littlemaidneo.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import work.nemonet.littlemaidneo.LittleMaidNeo;

public record OpenTargetTagScreenC2SPayload(int entityId) implements CustomPacketPayload {

    public static final Type<OpenTargetTagScreenC2SPayload> TYPE =
            new Type<>(Identifier.fromNamespaceAndPath(LittleMaidNeo.MODID, "open_target_tag_screen_c2s"));

    public static final StreamCodec<FriendlyByteBuf, OpenTargetTagScreenC2SPayload> STREAM_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.VAR_INT, OpenTargetTagScreenC2SPayload::entityId,
                    OpenTargetTagScreenC2SPayload::new
            );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
