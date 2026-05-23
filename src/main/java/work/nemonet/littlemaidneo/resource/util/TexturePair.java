package work.nemonet.littlemaidneo.resource.util;

import net.minecraft.resources.ResourceLocation;

public record TexturePair(ResourceLocation texture, ResourceLocation lightTexture) {

    public ResourceLocation getTexture(boolean isLight) {
        return isLight ? lightTexture : texture;
    }
}
