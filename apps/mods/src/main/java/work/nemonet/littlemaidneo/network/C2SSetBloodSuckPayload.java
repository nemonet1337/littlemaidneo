package work.nemonet.littlemaidneo.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import work.nemonet.littlemaidneo.LittleMaidNeo;

public record C2SSetBloodSuckPayload(int entityId, boolean isBloodSuck) implements CustomPacketPayload {

    public static final Type<C2SSetBloodSuckPayload> TYPE =
            new Type<>(Identifier.fromNamespaceAndPath(LittleMaidNeo.MODID, "set_blood_suck"));

    public static final StreamCodec<FriendlyByteBuf, C2SSetBloodSuckPayload> STREAM_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.VAR_INT, C2SSetBloodSuckPayload::entityId,
                    ByteBufCodecs.BOOL, C2SSetBloodSuckPayload::isBloodSuck,
                    C2SSetBloodSuckPayload::new
            );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
