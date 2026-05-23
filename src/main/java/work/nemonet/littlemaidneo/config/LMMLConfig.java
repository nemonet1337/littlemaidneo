package work.nemonet.littlemaidneo.config;

import net.neoforged.neoforge.common.ModConfigSpec;

public class LMMLConfig {
    private static ModConfigSpec.FloatValue VOICE_VOLUME;
    private static ModConfigSpec.BooleanValue ENABLE_ALPHA;
    private static ModConfigSpec.BooleanValue DEBUG_MODE;
    public static final ModConfigSpec SPEC;

    static {
        ModConfigSpec.Builder builder = new ModConfigSpec.Builder();
        builder.push("general");
        VOICE_VOLUME = builder.defineInRange("voiceVolume", 1.0f, 0.0f, 2.0f);
        builder.pop();
        builder.push("render");
        ENABLE_ALPHA = builder.define("enableAlpha", true);
        builder.pop();
        builder.push("misc");
        DEBUG_MODE = builder.define("debugMode", false);
        builder.pop();
        SPEC = builder.build();
    }

    public static float getVoiceVolume() {
        return VOICE_VOLUME.get();
    }

    public static boolean isEnableAlpha() {
        return ENABLE_ALPHA.get();
    }

    public static boolean isDebugMode() {
        return DEBUG_MODE.get();
    }
}
