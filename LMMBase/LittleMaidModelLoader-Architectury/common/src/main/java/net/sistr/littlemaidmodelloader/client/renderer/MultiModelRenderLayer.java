package net.sistr.littlemaidmodelloader.client.renderer;

import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import net.sistr.littlemaidmodelloader.LMMLMod;

// 1.21.1移植: YarnマッピングからMojangマッピングへ変更
// - RenderLayer → RenderType
// - ResourceLocation → ResourceLocation
// - getEntityTranslucent → entityTranslucent
// - getEntityCutoutNoCull → entityCutoutNoCull
public class MultiModelRenderLayer {

    public static RenderType getDefault(ResourceLocation resourceLocation) {
        if (LMMLMod.getConfig().isEnableAlpha()) {
            return RenderType.entityTranslucent(resourceLocation);
        }
        return RenderType.entityCutoutNoCull(resourceLocation);
    }

}
