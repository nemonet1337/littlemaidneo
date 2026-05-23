package work.nemonet.littlemaidneo.resource.util;

import net.minecraft.resources.ResourceLocation;

import java.util.Objects;

public class TexturePair {
    private final ResourceLocation texture;
    private final ResourceLocation lightTexture;

    public TexturePair(ResourceLocation texture, ResourceLocation lightTexture) {
        this.texture = texture;
        this.lightTexture = lightTexture;
    }

    public ResourceLocation getTexture(boolean isLight) {
        return isLight ? lightTexture : texture;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        TexturePair that = (TexturePair) o;
        return Objects.equals(texture, that.texture) && Objects.equals(lightTexture, that.lightTexture);
    }

    @Override
    public int hashCode() {
        return Objects.hash(texture, lightTexture);
    }
}
