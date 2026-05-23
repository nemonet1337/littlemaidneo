package net.sistr.littlemaidrebirth.network;

import dev.architectury.networking.NetworkManager;
import io.netty.buffer.Unpooled;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.player.Player;
import net.sistr.littlemaidrebirth.LMRBMod;
import net.sistr.littlemaidrebirth.entity.targeting.TargetIdentifier;
import net.sistr.littlemaidrebirth.entity.targeting.TargetTagManager;
import net.sistr.littlemaidrebirth.entity.targeting.TargetTagManagerImpl;
import net.sistr.littlemaidrebirth.entity.targeting.TargetingSystem;

import java.util.Map;
import java.util.Set;

public class C2SSetTargetTagsPacket {
    public static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath(LMRBMod.MODID, "set_target_tags");

    @Environment(EnvType.CLIENT)
    public static <T extends Entity & TargetTagManager> void sendC2SPacket(T entity,
            Map<TargetIdentifier, Set<TargetingSystem.TargetTag>> targetTags) {
        CompoundTag tag = new CompoundTag();
        TargetTagManagerImpl.write(targetTags, tag);

        RegistryFriendlyByteBuf buf = new RegistryFriendlyByteBuf(Unpooled.buffer(),
                net.minecraft.core.RegistryAccess.EMPTY);
        buf.writeVarInt(entity.getId());
        buf.writeNbt(tag);

        NetworkManager.sendToServer(ID, buf);
    }

    public static void receiveC2SPacket(RegistryFriendlyByteBuf buf, NetworkManager.PacketContext context) {
        int id = buf.readVarInt();
        CompoundTag tag = buf.readNbt();
        context.queue(() -> applyServer(context.getPlayer(), id, tag));
    }

    private static void applyServer(Player player, int id, CompoundTag tag) {
        Entity entity = player.level().getEntity(id);
        if (!(entity instanceof TargetTagManager targetTagManager)) {
            return;
        }
        if (entity instanceof TamableAnimal && !player.getUUID().equals(((TamableAnimal) entity).getOwnerUUID())) {
            return;
        }
        targetTagManager.readTargetTags(tag);
    }
}
