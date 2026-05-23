package work.nemonet.littlemaidneo.client.key;

import net.minecraft.client.KeyMapping;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.network.PacketDistributor;
import org.lwjgl.glfw.GLFW;
import work.nemonet.littlemaidneo.network.OpenMaidManagerScreenC2SPayload;

public class LMKeys {
    public static final KeyMapping OPEN_MAID_MANAGER_SCREEN = new KeyMapping(
            "key.littlemaidneo.open_maid_manager_screen",
            GLFW.GLFW_KEY_M,
            "key.categories.littlemaidneo"
    );

    public static void onRegisterKeyMappings(RegisterKeyMappingsEvent event) {
        event.register(OPEN_MAID_MANAGER_SCREEN);
    }

    public static void init() {
        NeoForge.EVENT_BUS.addListener(LMKeys::onClientTick);
    }

    private static void onClientTick(ClientTickEvent.Pre event) {
        boolean flag = false;
        while (OPEN_MAID_MANAGER_SCREEN.consumeClick()) {
            flag = true;
        }
        if (flag) {
            var mc = net.minecraft.client.Minecraft.getInstance();
            if (mc.player == null || mc.screen != null) {
                return;
            }
            PacketDistributor.sendToServer(OpenMaidManagerScreenC2SPayload.INSTANCE);
        }
    }
}
