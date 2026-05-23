package work.nemonet.littlemaidneo.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import work.nemonet.littlemaidneo.LittleMaidNeo;

public record SyncSoundConfigPayload(int entityId, String configName) implements CustomPacketPayload {

    public static final Type<SyncSoundConfigPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(LittleMaidNeo.MODID, "sync_sound_config"));

    public static final StreamCodec<FriendlyByteBuf, SyncSoundConfigPayload> STREAM_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.VAR_INT, SyncSoundConfigPayload::entityId,
                    ByteBufCodecs.STRING_UTF8, SyncSoundConfigPayload::configName,
                    SyncSoundConfigPayload::new
            );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
