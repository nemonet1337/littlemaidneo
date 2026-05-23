package work.nemonet.littlemaidneo.network;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import work.nemonet.littlemaidneo.LittleMaidNeo;

public record OpenMaidManagerScreenS2CPayload(CompoundTag nbt) implements CustomPacketPayload {

    public static final Type<OpenMaidManagerScreenS2CPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(LittleMaidNeo.MODID, "open_maid_manager_screen_s2c"));

    public static final StreamCodec<FriendlyByteBuf, OpenMaidManagerScreenS2CPayload> STREAM_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.COMPOUND_TAG.cast(), OpenMaidManagerScreenS2CPayload::nbt,
                    OpenMaidManagerScreenS2CPayload::new
            );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
