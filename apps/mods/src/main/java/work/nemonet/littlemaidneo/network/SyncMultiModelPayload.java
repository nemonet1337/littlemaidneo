package work.nemonet.littlemaidneo.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.neoforged.neoforge.network.codec.NeoForgeStreamCodecs;
import work.nemonet.littlemaidneo.LittleMaidNeo;
import work.nemonet.littlemaidneo.resource.util.ArmorSets;
import work.nemonet.littlemaidneo.resource.util.TextureColors;
import work.nemonet.littlemaidneo.entity.compound.IHasMultiModel.Part;

public record SyncMultiModelPayload(
        int entityId,
        String textureName,
        ArmorSets<String> armorSets,
        TextureColors color,
        boolean isContract
) implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<SyncMultiModelPayload> TYPE =
            new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath(LittleMaidNeo.MODID, "sync_multi_model"));

    private static final StreamCodec<ByteBuf, ArmorSets<String>> ARMOR_SETS_CODEC = StreamCodec.composite(
            ByteBufCodecs.STRING_UTF8, sets -> sets.getArmor(Part.HEAD).orElse(""),
            ByteBufCodecs.STRING_UTF8, sets -> sets.getArmor(Part.BODY).orElse(""),
            ByteBufCodecs.STRING_UTF8, sets -> sets.getArmor(Part.LEGS).orElse(""),
            ByteBufCodecs.STRING_UTF8, sets -> sets.getArmor(Part.FEET).orElse(""),
            (head, body, legs, feet) -> {
                ArmorSets<String> sets = new ArmorSets<>();
                sets.setArmor(head, Part.HEAD);
                sets.setArmor(body, Part.BODY);
                sets.setArmor(legs, Part.LEGS);
                sets.setArmor(feet, Part.FEET);
                return sets;
            }
    );

    public static final StreamCodec<RegistryFriendlyByteBuf, SyncMultiModelPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, SyncMultiModelPayload::entityId,
            ByteBufCodecs.STRING_UTF8, SyncMultiModelPayload::textureName,
            ARMOR_SETS_CODEC.cast(), SyncMultiModelPayload::armorSets,
            NeoForgeStreamCodecs.enumCodec(TextureColors.class), SyncMultiModelPayload::color,
            ByteBufCodecs.BOOL, SyncMultiModelPayload::isContract,
            SyncMultiModelPayload::new
    );

    public ArmorSets<String> getArmorSets() {
        return armorSets;
    }

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
