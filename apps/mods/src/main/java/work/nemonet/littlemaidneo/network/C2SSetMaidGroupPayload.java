package work.nemonet.littlemaidneo.network;

import net.minecraft.core.UUIDUtil;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import work.nemonet.littlemaidneo.LittleMaidNeo;

import java.util.UUID;

public record C2SSetMaidGroupPayload(UUID maidId, String group) implements CustomPacketPayload {

    public static final Type<C2SSetMaidGroupPayload> TYPE =
            new Type<>(Identifier.fromNamespaceAndPath(LittleMaidNeo.MODID, "set_maid_group"));

    public static final StreamCodec<FriendlyByteBuf, C2SSetMaidGroupPayload> STREAM_CODEC =
            StreamCodec.composite(
                    UUIDUtil.STREAM_CODEC, C2SSetMaidGroupPayload::maidId,
                    ByteBufCodecs.STRING_UTF8, C2SSetMaidGroupPayload::group,
                    C2SSetMaidGroupPayload::new
            );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
