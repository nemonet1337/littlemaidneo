package work.nemonet.littlemaidneo.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import work.nemonet.littlemaidneo.LittleMaidNeo;

public record SyncSoundPackPayload(
        int entityId,
        String soundPackName
) implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<SyncSoundPackPayload> TYPE =
            new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath(LittleMaidNeo.MODID, "sync_sound_pack"));

    public static final StreamCodec<RegistryFriendlyByteBuf, SyncSoundPackPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, SyncSoundPackPayload::entityId,
            ByteBufCodecs.STRING_UTF8, SyncSoundPackPayload::soundPackName,
            SyncSoundPackPayload::new
    ).cast();

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
