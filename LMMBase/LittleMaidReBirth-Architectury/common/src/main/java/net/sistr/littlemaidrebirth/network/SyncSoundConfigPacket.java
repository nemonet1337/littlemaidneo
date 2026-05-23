package net.sistr.littlemaidrebirth.network;

import dev.architectury.networking.NetworkManager;
import io.netty.buffer.Unpooled;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.OwnableEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.sistr.littlemaidmodelloader.entity.compound.SoundPlayable;
import net.sistr.littlemaidmodelloader.resource.manager.LMConfigManager;
import net.sistr.littlemaidmodelloader.util.PlayerList;
import net.sistr.littlemaidrebirth.LMRBMod;
import net.sistr.littlemaidrebirth.entity.util.TameableUtil;

/**
 * サウンドコンフィグを同期するパケット
 */
public class SyncSoundConfigPacket {
    public static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath(LMRBMod.MODID, "sync_sound_config");

    @Environment(EnvType.CLIENT)
    public static void sendC2SPacket(Entity entity, String configName) {
        RegistryFriendlyByteBuf buf = createC2SPacket(entity, configName);
        NetworkManager.sendToServer(ID, buf);
    }

    public static RegistryFriendlyByteBuf createC2SPacket(Entity entity, String configName) {
        RegistryFriendlyByteBuf buf = new RegistryFriendlyByteBuf(Unpooled.buffer(),
                net.minecraft.core.RegistryAccess.EMPTY);
        buf.writeVarInt(entity.getId());
        buf.writeUtf(configName);
        return buf;
    }

    public static void sendS2CPacket(Entity entity, String configName) {
        RegistryFriendlyByteBuf buf = createS2CPacket(entity, configName);
        NetworkManager.sendToPlayers(PlayerList.tracking(entity), ID, buf);
    }

    public static RegistryFriendlyByteBuf createS2CPacket(Entity entity, String configName) {
        RegistryFriendlyByteBuf buf = new RegistryFriendlyByteBuf(Unpooled.buffer(),
                net.minecraft.core.RegistryAccess.EMPTY);
        buf.writeVarInt(entity.getId());
        buf.writeUtf(configName);
        return buf;
    }

    @Environment(EnvType.CLIENT)
    public static void receiveS2CPacket(RegistryFriendlyByteBuf buf, NetworkManager.PacketContext context) {
        int id = buf.readVarInt();
        String configName = buf.readUtf();
        context.queue(() -> applySoundConfigClient(id, configName));
    }

    @Environment(EnvType.CLIENT)
    private static void applySoundConfigClient(int id, String configName) {
        Player player = Minecraft.getInstance().player;
        if (player == null)
            return;
        Level world = player.level();
        Entity entity = world.getEntity(id);
        if (entity instanceof SoundPlayable) {
            LMConfigManager.INSTANCE.getConfig(configName)
                    .ifPresent(((SoundPlayable) entity)::setConfigHolder);
        }
    }

    public static void receiveC2SPacket(RegistryFriendlyByteBuf buf, NetworkManager.PacketContext context) {
        int id = buf.readVarInt();
        String configName = buf.readUtf(32767);
        context.queue(() -> applySoundConfigServer(context.getPlayer(), id, configName));
    }

    private static void applySoundConfigServer(Player player, int id, String configName) {
        Level world = player.level();
        Entity entity = world.getEntity(id);
        if (!(entity instanceof SoundPlayable)) {
            return;
        }
        if (entity instanceof OwnableEntity tameable
                && TameableUtil.getTameOwnerUuid(tameable)
                        .filter(ownerId -> ownerId.equals(player.getUUID()))
                        .isEmpty()) {
            return;
        }
        LMConfigManager.INSTANCE.getConfig(configName)
                .ifPresent(((SoundPlayable) entity)::setConfigHolder);
        sendS2CPacket(entity, configName);
    }

}
