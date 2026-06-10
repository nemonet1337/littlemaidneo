package work.nemonet.littlemaidneo.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import work.nemonet.littlemaidneo.LittleMaidNeo;

public record LMSoundPayload(
        int entityId,
        String soundName
) implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<LMSoundPayload> TYPE =
            new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath(LittleMaidNeo.MODID, "lm_sound"));

    public static final StreamCodec<RegistryFriendlyByteBuf, LMSoundPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, LMSoundPayload::entityId,
            ByteBufCodecs.STRING_UTF8, LMSoundPayload::soundName,
            LMSoundPayload::new
    ).cast();

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
