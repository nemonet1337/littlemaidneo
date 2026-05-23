package net.sistr.littlemaidmodelloader.setup;

import net.sistr.littlemaidmodelloader.resource.loader.LMFileLoader;

public class ModSetup {

    public static void init() {
        // ネットワーク登録は各プラットフォームのイベントハンドラで行う
        // Fabric: ModInitializer.onInitialize()
        // NeoForge: RegisterPayloadHandlersEvent
    }

}
