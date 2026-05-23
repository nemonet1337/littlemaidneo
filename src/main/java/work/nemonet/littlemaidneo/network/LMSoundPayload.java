package work.nemonet.littlemaidneo.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import work.nemonet.littlemaidneo.LittleMaidNeo;

public record LMSoundPayload(
        int entityId,
        String soundName
) implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<LMSoundPayload> TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(LittleMaidNeo.MODID, "lm_sound"));

    public static final StreamCodec<RegistryFriendlyByteBuf, LMSoundPayload> STREAM_CODEC =
            StreamCodec.of(LMSoundPayload::encode, LMSoundPayload::decode);

    private static void encode(RegistryFriendlyByteBuf buf, LMSoundPayload payload) {
        buf.writeVarInt(payload.entityId());
        buf.writeUtf(payload.soundName());
    }

    private static LMSoundPayload decode(RegistryFriendlyByteBuf buf) {
        return new LMSoundPayload(buf.readVarInt(), buf.readUtf());
    }

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
