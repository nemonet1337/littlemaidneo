package work.nemonet.littlemaidneo.client.renderer;

import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.resources.Identifier;
import work.nemonet.littlemaidneo.config.LMMLConfig;

public class MultiModelRenderLayer {

    public static RenderType getDefault(Identifier Identifier) {
        if (LMMLConfig.isEnableAlpha()) {
            return RenderTypes.entityTranslucent(Identifier);
        }
        return RenderTypes.entityCutout(Identifier);
    }
}
