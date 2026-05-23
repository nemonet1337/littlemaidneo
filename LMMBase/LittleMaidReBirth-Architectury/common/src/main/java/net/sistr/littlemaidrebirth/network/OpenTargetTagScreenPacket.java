package net.sistr.littlemaidrebirth.network;

import dev.architectury.networking.NetworkManager;
import io.netty.buffer.Unpooled;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.player.Player;
import net.sistr.littlemaidrebirth.LMRBMod;
import net.sistr.littlemaidrebirth.client.screen.TargetTagScreen;
import net.sistr.littlemaidrebirth.entity.targeting.TargetIdentifier;
import net.sistr.littlemaidrebirth.entity.targeting.TargetTagManager;
import net.sistr.littlemaidrebirth.entity.targeting.TargetTagManagerImpl;
import net.sistr.littlemaidrebirth.entity.targeting.TargetingSystem;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class OpenTargetTagScreenPacket {
    public static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath(LMRBMod.MODID,
            "open_target_tag_screen");

    public static <T extends Entity & TargetTagManager> void sendS2CPacket(T entity, Player player) {
        RegistryFriendlyByteBuf buf = createS2CPacket(entity, player);
        NetworkManager.sendToPlayer((ServerPlayer) player, ID, buf);
    }

    public static <T extends Entity & TargetTagManager> RegistryFriendlyByteBuf createS2CPacket(T entity,
            Player player) {
        CompoundTag nbt = new CompoundTag();
        entity.writeTargetTags(nbt);

        RegistryFriendlyByteBuf buf = new RegistryFriendlyByteBuf(Unpooled.buffer(),
                net.minecraft.core.RegistryAccess.EMPTY);
        buf.writeVarInt(entity.getId());
        buf.writeNbt(nbt);
        return buf;
    }

    @Environment(EnvType.CLIENT)
    public static void sendC2SPacket(Entity entity) {
        RegistryFriendlyByteBuf buf = createC2SPacket(entity);
        NetworkManager.sendToServer(ID, buf);
    }

    public static RegistryFriendlyByteBuf createC2SPacket(Entity entity) {
        RegistryFriendlyByteBuf buf = new RegistryFriendlyByteBuf(Unpooled.buffer(),
                net.minecraft.core.RegistryAccess.EMPTY);
        buf.writeVarInt(entity.getId());
        return buf;
    }

    @Environment(EnvType.CLIENT)
    public static void receiveS2CPacket(RegistryFriendlyByteBuf buf, NetworkManager.PacketContext context) {
        Player player = context.getPlayer();
        if (player == null)
            return;
        int id = buf.readVarInt();
        CompoundTag nbt = buf.readNbt();
        context.queue(() -> openScreen(id, nbt, player));
    }

    @Environment(EnvType.CLIENT)
    private static void openScreen(int id, CompoundTag nbt, Player player) {
        Entity entity = player.level().getEntity(id);
        if (!(entity instanceof TargetTagManager targetTagManager)) {
            return;
        }
        Map<TargetIdentifier, Set<TargetingSystem.TargetTag>> targetTagMap = new HashMap<>();
        TargetTagManagerImpl.read(targetTagMap, nbt);

        Minecraft.getInstance().setScreen(new TargetTagScreen(entity, targetTagMap));
    }

    public static void receiveC2SPacket(RegistryFriendlyByteBuf buf, NetworkManager.PacketContext context) {
        int id = buf.readVarInt();
        context.queue(() -> openScreen(id, context.getPlayer()));
    }

    private static <T extends Entity & TargetTagManager> void openScreen(int id, Player player) {
        Entity entity = player.level().getEntity(id);
        if (!(entity instanceof TargetTagManager)
                || (entity instanceof TamableAnimal
                        && !player.getUUID().equals(((TamableAnimal) entity).getOwnerUUID()))) {
            return;
        }
        // noinspection unchecked
        sendS2CPacket((T) entity, player);
    }
}
