package net.sistr.littlemaidrebirth.client.key;

import dev.architectury.event.events.client.ClientTickEvent;
import dev.architectury.registry.client.keymappings.KeyMappingRegistry;
import net.minecraft.client.KeyMapping;
import net.sistr.littlemaidrebirth.network.OpenMaidManagerScreenPacket;
import org.lwjgl.glfw.GLFW;

public class LMKeys {
    public static final KeyMapping OPEN_MAID_MANAGER_SCREEN
            = register(new KeyMapping(
            "key.littlemaidrebirth.open_maid_manager_screen",
            GLFW.GLFW_KEY_M,
            "key.categories.littlemaidrebirth"
    ));

    public static void init() {
        ClientTickEvent.CLIENT_PRE.register(client -> {
            boolean flag = false;
            while (OPEN_MAID_MANAGER_SCREEN.consumeClick()) {
                flag = true;
            }
            if (flag) {
                if (client.player == null || client.screen != null) {
                    return;
                }
                OpenMaidManagerScreenPacket.sendC2SPacket();
            }
        });
    }

    private static KeyMapping register(KeyMapping keyBinding) {
        KeyMappingRegistry.register(keyBinding);
        return keyBinding;
    }
}
