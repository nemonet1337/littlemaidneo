package work.nemonet.littlemaidneo.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import work.nemonet.littlemaidneo.LittleMaidNeo;

public record SyncSoundPackPayload(
        int entityId,
        String soundPackName
) implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<SyncSoundPackPayload> TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(LittleMaidNeo.MODID, "sync_sound_pack"));

    public static final StreamCodec<RegistryFriendlyByteBuf, SyncSoundPackPayload> STREAM_CODEC =
            StreamCodec.of(SyncSoundPackPayload::encode, SyncSoundPackPayload::decode);

    private static void encode(RegistryFriendlyByteBuf buf, SyncSoundPackPayload payload) {
        buf.writeInt(payload.entityId());
        buf.writeUtf(payload.soundPackName());
    }

    private static SyncSoundPackPayload decode(RegistryFriendlyByteBuf buf) {
        return new SyncSoundPackPayload(buf.readInt(), buf.readUtf());
    }

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
