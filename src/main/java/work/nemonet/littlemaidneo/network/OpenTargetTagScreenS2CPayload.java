package work.nemonet.littlemaidneo.network;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import work.nemonet.littlemaidneo.LittleMaidNeo;

public record OpenTargetTagScreenS2CPayload(int entityId, CompoundTag nbt) implements CustomPacketPayload {

    public static final Type<OpenTargetTagScreenS2CPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(LittleMaidNeo.MODID, "open_target_tag_screen_s2c"));

    public static final StreamCodec<FriendlyByteBuf, OpenTargetTagScreenS2CPayload> STREAM_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.VAR_INT, OpenTargetTagScreenS2CPayload::entityId,
                    ByteBufCodecs.COMPOUND_TAG.cast(), OpenTargetTagScreenS2CPayload::nbt,
                    OpenTargetTagScreenS2CPayload::new
            );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
