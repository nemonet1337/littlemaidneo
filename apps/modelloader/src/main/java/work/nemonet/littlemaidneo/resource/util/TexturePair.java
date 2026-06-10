package work.nemonet.littlemaidneo.resource.util;

import net.minecraft.resources.Identifier;

public record TexturePair(Identifier texture, Identifier lightTexture) {

    public Identifier getTexture(boolean isLight) {
        return isLight ? lightTexture : texture;
    }
}
