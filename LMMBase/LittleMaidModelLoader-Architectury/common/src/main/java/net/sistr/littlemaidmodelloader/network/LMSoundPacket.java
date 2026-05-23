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
import net.sistr.littlemaidmodelloader.LMMLMod;
import net.sistr.littlemaidmodelloader.entity.compound.SoundPlayable;
import net.sistr.littlemaidmodelloader.util.PlayerList;

public class LMSoundPacket {
    // 1.21.1変更: ResourceLocationコンストラクタが非公開になり、fromNamespaceAndPathを使用
    public static final ResourceLocation ID =
            ResourceLocation.fromNamespaceAndPath(LMMLMod.MODID, "lm_sound");

    public static void sendS2CPacket(Entity entity, String soundName) {
        RegistryFriendlyByteBuf passedData = createS2CPacket(entity, soundName);
        NetworkManager.sendToPlayers(PlayerList.tracking(entity)
                .stream()
                .filter(p -> p.distanceToSqr(entity) < 16 * 16)
                .toList(), ID, passedData);
    }

    public static RegistryFriendlyByteBuf createS2CPacket(Entity entity, String soundName) {
        RegistryFriendlyByteBuf passedData = new RegistryFriendlyByteBuf(Unpooled.buffer(), null);
        passedData.writeVarInt(entity.getId());
        passedData.writeUtf(soundName);
        return passedData;
    }

    @Environment(EnvType.CLIENT)
    public static void receiveS2CPacket(RegistryFriendlyByteBuf buf, NetworkManager.PacketContext context) {
        int entityId = buf.readVarInt();
        String soundName = buf.readUtf();
        context.queue(() -> playSoundClient(entityId, soundName));
    }

    @Environment(EnvType.CLIENT)
    public static void playSoundClient(int entityId, String soundName) {
        Player player = Minecraft.getInstance().player;
        if (player == null) return;
        Entity entity = player.level().getEntity(entityId);
        if (entity instanceof SoundPlayable) {
            ((SoundPlayable) entity).play(soundName);
        }
    }

}
