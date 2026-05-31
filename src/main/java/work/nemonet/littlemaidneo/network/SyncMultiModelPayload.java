package work.nemonet.littlemaidneo.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import work.nemonet.littlemaidneo.LittleMaidNeo;
import work.nemonet.littlemaidneo.resource.util.ArmorSets;
import work.nemonet.littlemaidneo.resource.util.TextureColors;
import work.nemonet.littlemaidneo.entity.compound.IHasMultiModel.Part;

public record SyncMultiModelPayload(
        int entityId,
        String textureName,
        String armorHead,
        String armorBody,
        String armorLegs,
        String armorFeet,
        TextureColors color,
        boolean isContract
) implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<SyncMultiModelPayload> TYPE =
            new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath(LittleMaidNeo.MODID, "sync_multi_model"));

    public static final StreamCodec<RegistryFriendlyByteBuf, SyncMultiModelPayload> STREAM_CODEC =
            StreamCodec.of(SyncMultiModelPayload::encode, SyncMultiModelPayload::decode);

    private static void encode(RegistryFriendlyByteBuf buf, SyncMultiModelPayload payload) {
        buf.writeInt(payload.entityId());
        buf.writeUtf(payload.textureName());
        buf.writeUtf(payload.armorHead());
        buf.writeUtf(payload.armorBody());
        buf.writeUtf(payload.armorLegs());
        buf.writeUtf(payload.armorFeet());
        buf.writeEnum(payload.color());
        buf.writeBoolean(payload.isContract());
    }

    private static SyncMultiModelPayload decode(RegistryFriendlyByteBuf buf) {
        int entityId = buf.readInt();
        String textureName = buf.readUtf();
        String armorHead = buf.readUtf();
        String armorBody = buf.readUtf();
        String armorLegs = buf.readUtf();
        String armorFeet = buf.readUtf();
        TextureColors color = buf.readEnum(TextureColors.class);
        boolean isContract = buf.readBoolean();
        return new SyncMultiModelPayload(entityId, textureName, armorHead, armorBody, armorLegs, armorFeet, color, isContract);
    }

    public ArmorSets<String> getArmorSets() {
        ArmorSets<String> sets = new ArmorSets<>();
        sets.setArmor(armorHead, Part.HEAD);
        sets.setArmor(armorBody, Part.BODY);
        sets.setArmor(armorLegs, Part.LEGS);
        sets.setArmor(armorFeet, Part.FEET);
        return sets;
    }

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
