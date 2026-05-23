package net.sistr.littlemaidrebirth.network;

import dev.architectury.networking.NetworkManager;
import io.netty.buffer.Unpooled;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.sistr.littlemaidrebirth.LMRBMod;
import net.sistr.littlemaidrebirth.entity.LittleMaidEntity;

import java.util.UUID;

public class SpawnLittleMaidPacket {
    public static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath(LMRBMod.MODID, "spawn_littlemaid");

    @SuppressWarnings({ "unchecked", "UnstableApiUsage" })
    public static Packet<ClientGamePacketListener> create(LittleMaidEntity maid) {
        var buf = new RegistryFriendlyByteBuf(Unpooled.buffer(), net.minecraft.core.RegistryAccess.EMPTY);
        buf.writeVarInt(maid.getId());
        buf.writeUUID(maid.getUUID());
        buf.writeVarInt(BuiltInRegistries.ENTITY_TYPE.getId(maid.getType()));
        buf.writeDouble(maid.getX());
        buf.writeDouble(maid.getY());
        buf.writeDouble(maid.getZ());
        buf.writeFloat(maid.getXRot());
        buf.writeFloat(maid.getYRot());
        buf.writeFloat(maid.getYHeadRot());
        var velocity = maid.getDeltaMovement();
        buf.writeDouble(velocity.x());
        buf.writeDouble(velocity.y());
        buf.writeDouble(velocity.z());
        maid.saveAdditionalSpawnData(buf);
        return (Packet<ClientGamePacketListener>) NetworkManager.toPacket(NetworkManager.Side.S2C, ID, buf);
    }

    @Environment(EnvType.CLIENT)
    public static void receiveS2CPacket(RegistryFriendlyByteBuf buf, NetworkManager.PacketContext context) {
        var id = buf.readVarInt();
        var uuid = buf.readUUID();
        var entityType = BuiltInRegistries.ENTITY_TYPE.byId(buf.readVarInt());
        var x = buf.readDouble();
        var y = buf.readDouble();
        var z = buf.readDouble();
        var pitch = buf.readFloat();
        var yaw = buf.readFloat();
        var headYaw = buf.readFloat();
        var velocityX = buf.readDouble();
        var velocityY = buf.readDouble();
        var velocityZ = buf.readDouble();

        buf.retain();

        var client = Minecraft.getInstance();
        // こちらだと実行が遅れ、最初の同期パケットより後にスポーンしてしまう
        // context.queue(() -> acceptS2C(client, id, uuid, entityType, x, y, z, pitch,
        // yaw, headYaw, velocityX, velocityY, velocityZ));
        if (!client.isSameThread()) {
            client.executeIfPossible(() -> {
                acceptS2C(client, id, uuid, entityType, x, y, z, pitch, yaw, headYaw, velocityX, velocityY, velocityZ,
                        buf);
            });
        } else {
            acceptS2C(client, id, uuid, entityType, x, y, z, pitch, yaw, headYaw, velocityX, velocityY, velocityZ, buf);
        }
    }

    public static void acceptS2C(Minecraft client, int id, UUID uuid, EntityType<?> entityType,
            double x, double y, double z, float pitch, float yaw, float headYaw,
            double velocityX, double velocityY, double velocityZ, FriendlyByteBuf buf) {
        var player = client.player;
        if (player == null)
            return;
        var world = player.level();
        assert entityType != null;
        var entity = entityType.create(world);
        if (entity instanceof LittleMaidEntity maid) {
            maid.syncPacketPositionCodec(x, y, z);
            maid.yBodyRot = headYaw;
            maid.yHeadRot = headYaw;
            maid.yBodyRotO = maid.yBodyRot;
            maid.yHeadRotO = maid.yHeadRot;
            maid.setId(id);
            maid.setUUID(uuid);
            maid.absMoveTo(x, y, z, yaw, pitch);
            maid.setDeltaMovement(velocityX, velocityY, velocityZ);
            maid.loadAdditionalSpawnData(buf);
            buf.release();

            ((ClientLevel) world).addEntity(entity);
        }
    }

}
