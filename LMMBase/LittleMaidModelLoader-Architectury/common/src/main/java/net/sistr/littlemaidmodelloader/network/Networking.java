package net.sistr.littlemaidmodelloader.network;

import dev.architectury.networking.NetworkManager;
import net.minecraft.network.RegistryFriendlyByteBuf;
import dev.architectury.platform.Platform;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

public class Networking {
    public static final Networking INSTANCE = new Networking();

    public void init() {
        if (Platform.getEnv() == EnvType.CLIENT) clientInit();
        serverInit();
    }

    @Environment(EnvType.CLIENT)
    private void clientInit() {
        NetworkManager.registerReceiver(NetworkManager.Side.S2C, SyncMultiModelPacket.ID, 
                (RegistryFriendlyByteBuf buf, NetworkManager.PacketContext context) -> 
                        SyncMultiModelPacket.receiveS2CPacket(buf, context));
        NetworkManager.registerReceiver(NetworkManager.Side.S2C, SyncSoundPackPacket.ID, 
                (RegistryFriendlyByteBuf buf, NetworkManager.PacketContext context) -> 
                        SyncSoundPackPacket.receiveS2CPacket(buf, context));
        NetworkManager.registerReceiver(NetworkManager.Side.S2C, LMSoundPacket.ID, 
                (RegistryFriendlyByteBuf buf, NetworkManager.PacketContext context) -> 
                        LMSoundPacket.receiveS2CPacket(buf, context));
    }

    private void serverInit() {
        NetworkManager.registerReceiver(NetworkManager.Side.C2S, SyncMultiModelPacket.ID, 
                (RegistryFriendlyByteBuf buf, NetworkManager.PacketContext context) -> 
                        SyncMultiModelPacket.receiveC2SPacket(buf, context));
        NetworkManager.registerReceiver(NetworkManager.Side.C2S, SyncSoundPackPacket.ID, 
                (RegistryFriendlyByteBuf buf, NetworkManager.PacketContext context) -> 
                        SyncSoundPackPacket.receiveC2SPacket(buf, context));
    }

}
