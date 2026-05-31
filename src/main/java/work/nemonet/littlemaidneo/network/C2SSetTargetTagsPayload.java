package work.nemonet.littlemaidneo.network;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import work.nemonet.littlemaidneo.LittleMaidNeo;

public record C2SSetTargetTagsPayload(int entityId, CompoundTag tag) implements CustomPacketPayload {

    public static final Type<C2SSetTargetTagsPayload> TYPE =
            new Type<>(Identifier.fromNamespaceAndPath(LittleMaidNeo.MODID, "set_target_tags"));

    public static final StreamCodec<FriendlyByteBuf, C2SSetTargetTagsPayload> STREAM_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.VAR_INT, C2SSetTargetTagsPayload::entityId,
                    ByteBufCodecs.COMPOUND_TAG.cast(), C2SSetTargetTagsPayload::tag,
                    C2SSetTargetTagsPayload::new
            );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
