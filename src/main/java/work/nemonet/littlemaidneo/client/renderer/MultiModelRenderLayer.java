package work.nemonet.littlemaidneo.client.renderer;

import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import work.nemonet.littlemaidneo.config.LMMLConfig;

public class MultiModelRenderLayer {

    public static RenderType getDefault(ResourceLocation resourceLocation) {
        if (LMMLConfig.isEnableAlpha()) {
            return RenderType.entityTranslucent(resourceLocation);
        }
        return RenderType.entityCutoutNoCull(resourceLocation);
    }
}
