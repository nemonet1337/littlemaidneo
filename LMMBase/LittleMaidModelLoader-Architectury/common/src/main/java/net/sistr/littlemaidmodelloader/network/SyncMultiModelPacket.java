package net.sistr.littlemaidmodelloader.network;

import dev.architectury.networking.NetworkManager;
import net.minecraft.network.RegistryFriendlyByteBuf;
import io.netty.buffer.Unpooled;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.sistr.littlemaidmodelloader.LMMLMod;
import net.sistr.littlemaidmodelloader.entity.compound.IHasMultiModel;
import net.sistr.littlemaidmodelloader.entity.compound.IHasMultiModel.Layer;
import net.sistr.littlemaidmodelloader.entity.compound.IHasMultiModel.Part;
import net.sistr.littlemaidmodelloader.resource.manager.LMTextureManager;
import net.sistr.littlemaidmodelloader.resource.util.ArmorSets;
import net.sistr.littlemaidmodelloader.resource.util.TextureColors;
import net.sistr.littlemaidmodelloader.util.PlayerList;

public class SyncMultiModelPacket {
    // 1.21.1変更: ResourceLocationコンストラクタが非公開になり、fromNamespaceAndPathを使用
    public static final ResourceLocation ID =
            ResourceLocation.fromNamespaceAndPath(LMMLMod.MODID, "sync_multi_model");

    @Environment(EnvType.CLIENT)
    public static void sendC2SPacket(Entity entity, IHasMultiModel hasMultiModel) {
        RegistryFriendlyByteBuf passedData = createC2SPacket(entity, hasMultiModel);
        NetworkManager.sendToServer(ID, passedData);
    }

    public static RegistryFriendlyByteBuf createC2SPacket(Entity entity, IHasMultiModel hasMultiModel) {
        RegistryFriendlyByteBuf passedData = new RegistryFriendlyByteBuf(Unpooled.buffer(), null);
        passedData.writeInt(entity.getId());
        passedData.writeUtf(hasMultiModel.getTextureHolder(Layer.SKIN, Part.HEAD)
                .getTextureName());
        for (Part part : Part.values()) {
            passedData.writeUtf(hasMultiModel.getTextureHolder(Layer.INNER, part).getTextureName());
        }
        passedData.writeEnum(hasMultiModel.getColorMM());
        passedData.writeBoolean(hasMultiModel.isContractMM());
        return passedData;
    }

    public static void sendS2CPacket(Entity entity, IHasMultiModel hasMultiModel) {
        RegistryFriendlyByteBuf passedData = createS2CPacket(entity, hasMultiModel);
        NetworkManager.sendToPlayers(PlayerList.tracking(entity), ID, passedData);
    }

    public static RegistryFriendlyByteBuf createS2CPacket(Entity entity, IHasMultiModel hasMultiModel) {
        RegistryFriendlyByteBuf passedData = new RegistryFriendlyByteBuf(Unpooled.buffer(), null);
        passedData.writeInt(entity.getId());
        passedData.writeUtf(hasMultiModel.getTextureHolder(Layer.SKIN, Part.HEAD).getTextureName());
        for (Part part : Part.values()) {
            passedData.writeUtf(hasMultiModel.getTextureHolder(Layer.INNER, part).getTextureName());
        }
        passedData.writeEnum(hasMultiModel.getColorMM());
        passedData.writeBoolean(hasMultiModel.isContractMM());
        return passedData;
    }

    @Environment(EnvType.CLIENT)
    public static void receiveS2CPacket(RegistryFriendlyByteBuf buf, NetworkManager.PacketContext context) {
        int entityId = buf.readInt();
        String textureName = buf.readUtf();
        ArmorSets<String> armorTextureName = new ArmorSets<>();
        for (Part part : Part.values()) {
            armorTextureName.setArmor(buf.readUtf(), part);
        }
        TextureColors color = buf.readEnum(TextureColors.class);
        boolean isContract = buf.readBoolean();
        context.queue(() ->
                applyMultiModelClient(entityId, isContract, color, textureName, armorTextureName));
    }

    //context.getTaskQueue().execute()の中では@Environmentの効力が及ばないため別メソッドに分離
    @Environment(EnvType.CLIENT)
    public static void applyMultiModelClient(int entityId, boolean isContract, TextureColors color,
                                             String textureName, ArmorSets<String> armorTextureName) {
        Level level = Minecraft.getInstance().level;
        if (level == null) return;
        Entity entity = level.getEntity(entityId);
        if (!(entity instanceof IHasMultiModel multiModel)) return;
        multiModel.setContractMM(isContract);
        multiModel.setColorMM(color);
        LMTextureManager textureManager = LMTextureManager.INSTANCE;
        textureManager.getTexture(textureName).filter(textureHolder ->
                        multiModel.isAllowChangeTexture(entity, textureHolder, Layer.SKIN, Part.HEAD))
                .ifPresent(textureHolder -> multiModel.setTextureHolder(textureHolder, Layer.SKIN, Part.HEAD));
        for (Part part : Part.values()) {
            String armorName = armorTextureName.getArmor(part)
                    .orElseThrow(() -> new IllegalStateException("テクスチャが存在しません。"));
            textureManager.getTexture(armorName).filter(textureHolder ->
                            multiModel.isAllowChangeTexture(entity, textureHolder, Layer.INNER, part))
                    .ifPresent(textureHolder -> multiModel.setTextureHolder(textureHolder, Layer.INNER, part));
        }
    }

    public static void receiveC2SPacket(RegistryFriendlyByteBuf buf, NetworkManager.PacketContext context) {
        int entityId = buf.readInt();
        String textureName = buf.readUtf();
        ArmorSets<String> armorTextureName = new ArmorSets<>();
        for (Part part : Part.values()) {
            armorTextureName.setArmor(buf.readUtf(), part);
        }
        TextureColors color = buf.readEnum(TextureColors.class);
        boolean isContract = buf.readBoolean();
        context.queue(() ->
                applyMultiModelServer(context.getPlayer(), entityId, isContract, color, textureName, armorTextureName));
    }

    //クライアントに倣って分離
    public static void applyMultiModelServer(Player player, int entityId, boolean isContract, TextureColors color,
                                             String textureName, ArmorSets<String> armorTextureName) {
        Entity entity = player.level().getEntity(entityId);
        if (!(entity instanceof IHasMultiModel multiModel)) return;
        multiModel.setContractMM(isContract);
        multiModel.setColorMM(color);
        LMTextureManager textureManager = LMTextureManager.INSTANCE;
        textureManager.getTexture(textureName).filter(textureHolder ->
                        multiModel.isAllowChangeTexture(entity, textureHolder, Layer.SKIN, Part.HEAD))
                .ifPresent(textureHolder -> multiModel.setTextureHolder(textureHolder, Layer.SKIN, Part.HEAD));
        for (Part part : Part.values()) {
            String armorName = armorTextureName.getArmor(part)
                    .orElseThrow(() -> new IllegalStateException("テクスチャが存在しません。"));
            textureManager.getTexture(armorName).filter(textureHolder ->
                            multiModel.isAllowChangeTexture(entity, textureHolder, Layer.INNER, part))
                    .ifPresent(textureHolder -> multiModel.setTextureHolder(textureHolder, Layer.INNER, part));
        }
        sendS2CPacket(entity, multiModel);
    }

}
