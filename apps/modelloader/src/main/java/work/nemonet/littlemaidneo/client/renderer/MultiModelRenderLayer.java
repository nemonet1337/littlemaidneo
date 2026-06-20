package work.nemonet.littlemaidneo.client.renderer;

import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.resources.Identifier;
import work.nemonet.littlemaidneo.config.LMNModelConfig;

public class MultiModelRenderLayer {

    public static RenderType getDefault(Identifier Identifier) {
        if (LMNModelConfig.isEnableAlpha()) {
            return RenderTypes.entityTranslucent(Identifier);
        }
        return RenderTypes.entityCutout(Identifier);
    }

    public static RenderType getArmor(Identifier Identifier) {
        return RenderTypes.armorCutoutNoCull(Identifier);
    }
}
