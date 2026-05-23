package work.nemonet.littlemaidneo.resource.util;

import com.google.common.collect.Lists;
import net.minecraft.resources.ResourceLocation;
import work.nemonet.littlemaidneo.entity.compound.IHasMultiModel;
import work.nemonet.littlemaidneo.multimodel.IMultiModel;
import work.nemonet.littlemaidneo.resource.holder.TextureHolder;

import java.lang.ref.WeakReference;
import java.util.*;

public class ArmorPart {
    private final TexturePair innerTex;
    private final TexturePair outerTex;
    private final IMultiModel innerModel;
    private final IMultiModel outerModel;

    public ArmorPart(ResourceLocation innerTex, ResourceLocation innerTexLight,
                     ResourceLocation outerTex, ResourceLocation outerTexLight,
                     IMultiModel innerModel, IMultiModel outerModel) {
        this.innerTex = new TexturePair(innerTex, innerTexLight);
        this.outerTex = new TexturePair(outerTex, outerTexLight);
        this.innerModel = innerModel;
        this.outerModel = outerModel;
    }

    public ResourceLocation getTexture(IHasMultiModel.Layer layer, boolean isLight) {
        if (!layer.isArmor()) throw new IllegalArgumentException("取得できません。");
        return layer == IHasMultiModel.Layer.INNER ? innerTex.getTexture(isLight) : outerTex.getTexture(isLight);
    }

    public IMultiModel getModel(IHasMultiModel.Layer layer) {
        if (!layer.isArmor()) throw new IllegalArgumentException("取得できません。");
        return layer == IHasMultiModel.Layer.INNER ? innerModel : outerModel;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ArmorPart that = (ArmorPart) o;
        return Objects.equals(innerTex, that.innerTex) && Objects.equals(outerTex, that.outerTex)
                && Objects.equals(innerModel, that.innerModel) && Objects.equals(outerModel, that.outerModel);
    }

    @Override
    public int hashCode() {
        return Objects.hash(innerTex, outerTex, innerModel, outerModel);
    }

    public static final class Builder {
        private static final Map<TextureHolder, List<WeakReference<ArmorPart>>> REFERENCES = new HashMap<>();
        private ResourceLocation innerTex;
        private ResourceLocation innerTexLight;
        private ResourceLocation outerTex;
        private ResourceLocation outerTexLight;
        private IMultiModel innerModel;
        private IMultiModel outerModel;

        private Builder() {}

        public static Builder newInstance() {
            return new Builder();
        }

        public Builder innerTex(ResourceLocation innerTex) { this.innerTex = innerTex; return this; }
        public Builder innerTexLight(ResourceLocation innerTexLight) { this.innerTexLight = innerTexLight; return this; }
        public Builder outerTex(ResourceLocation outerTex) { this.outerTex = outerTex; return this; }
        public Builder outerTexLight(ResourceLocation outerTexLight) { this.outerTexLight = outerTexLight; return this; }
        public Builder innerModel(IMultiModel innerModel) { this.innerModel = innerModel; return this; }
        public Builder outerModel(IMultiModel outerModel) { this.outerModel = outerModel; return this; }

        public ArmorPart build() {
            return new ArmorPart(innerTex, innerTexLight, outerTex, outerTexLight, innerModel, outerModel);
        }
    }
}
