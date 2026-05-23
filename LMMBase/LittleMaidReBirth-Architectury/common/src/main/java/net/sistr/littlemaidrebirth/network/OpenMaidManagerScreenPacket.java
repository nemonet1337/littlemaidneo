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
import net.minecraft.world.entity.player.Player;
import net.sistr.littlemaidrebirth.LMRBMod;
import net.sistr.littlemaidrebirth.client.screen.MaidManagerScreen;
import net.sistr.littlemaidrebirth.entity.targeting.TargetTagManager;
import net.sistr.littlemaidrebirth.entity.util.MaidManager;
import net.sistr.littlemaidrebirth.entity.util.MaidManagerImpl;

import java.util.ArrayList;
import java.util.List;

public class OpenMaidManagerScreenPacket {
    public static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath(LMRBMod.MODID,
            "open_maid_manager_screen");

    public static void sendS2CPacket(Player player) {
        RegistryFriendlyByteBuf buf = createS2CPacket(player);
        NetworkManager.sendToPlayer((ServerPlayer) player, ID, buf);
    }

    public static RegistryFriendlyByteBuf createS2CPacket(Player player) {
        RegistryFriendlyByteBuf buf = new RegistryFriendlyByteBuf(Unpooled.buffer(),
                net.minecraft.core.RegistryAccess.EMPTY);
        var nbt = new CompoundTag();
        var lmInfos = ((MaidManager) player).getMaidList();
        MaidManagerImpl.write(nbt, lmInfos);
        buf.writeNbt(nbt);
        return buf;
    }

    @Environment(EnvType.CLIENT)
    public static void sendC2SPacket() {
        RegistryFriendlyByteBuf buf = createC2SPacket();
        NetworkManager.sendToServer(ID, buf);
    }

    public static RegistryFriendlyByteBuf createC2SPacket() {
        RegistryFriendlyByteBuf buf = new RegistryFriendlyByteBuf(Unpooled.buffer(),
                net.minecraft.core.RegistryAccess.EMPTY);
        return buf;
    }

    @Environment(EnvType.CLIENT)
    public static void receiveS2CPacket(RegistryFriendlyByteBuf buf, NetworkManager.PacketContext context) {
        Player player = context.getPlayer();
        if (player == null)
            return;
        var nbt = buf.readNbt();
        var lmInfos = new ArrayList<MaidManager.LMInfo>();
        MaidManagerImpl.read(nbt, lmInfos);
        context.queue(() -> openScreen(player, lmInfos));
    }

    @Environment(EnvType.CLIENT)
    private static void openScreen(Player player, List<MaidManager.LMInfo> lmInfos) {
        Minecraft.getInstance().setScreen(new MaidManagerScreen(lmInfos));
    }

    public static void receiveC2SPacket(RegistryFriendlyByteBuf buf, NetworkManager.PacketContext context) {
        context.queue(() -> openScreen(context.getPlayer()));
    }

    private static <T extends Entity & TargetTagManager> void openScreen(Player player) {
        sendS2CPacket(player);
    }
}
