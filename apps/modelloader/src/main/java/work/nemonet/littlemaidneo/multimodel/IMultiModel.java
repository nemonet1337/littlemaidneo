package work.nemonet.littlemaidneo.multimodel;

import net.minecraft.util.Mth;
import work.nemonet.littlemaidneo.maidmodel.IModelCaps;
import work.nemonet.littlemaidneo.multimodel.layer.MMMatrixStack;
import work.nemonet.littlemaidneo.multimodel.layer.MMPose;
import work.nemonet.littlemaidneo.multimodel.layer.MMRenderContext;

public interface IMultiModel {

    void setupTransform(IModelCaps caps, MMMatrixStack matrices, float animationProgress, float bodyYaw, float tickDelta);

    void animateModel(IModelCaps caps, float limbAngle, float limbDistance, float tickDelta);

    void setAngles(IModelCaps caps, float limbAngle, float limbDistance, float animationProgress, float headYaw, float headPitch);

    void render(MMRenderContext context);

    void adjustHandItem(MMMatrixStack matrices, boolean isLeft);

    int getTextureWidth();

    int getTextureHeight();

    float getInnerArmorSize();

    float getOuterArmorSize();

    float getWidth(IModelCaps caps, MMPose pose);

    float getHeight(IModelCaps caps, MMPose pose);

    float getEyeHeight(IModelCaps caps, MMPose pose);

    float getyOffset(IModelCaps caps);

    float getMountedYOffset(IModelCaps caps);

    float getLeashOffset(IModelCaps caps);

    void showAllParts(IModelCaps caps);

    int showArmorParts(int parts, int index);

    void renderItems(IModelCaps pEntityCaps);

    void renderFirstPersonHand(IModelCaps pEntityCaps);

    static float sin(float value) { return Mth.sin(value); }
    static float cos(float value) { return Mth.cos(value); }
    static float sqrt(float value) { return Mth.sqrt(value); }
    static float floor(float value) { return Mth.floor(value); }
    static float ceil(float value) { return Mth.ceil(value); }
    static float abs(float value) { return Mth.abs(value); }

    static float clamp(float value, float min, float max) {
        if (value < min) return min;
        if (max < value) return max;
        return value;
    }

    static float lerp(float delta, float start, float end) {
        return start + delta * (end - start);
    }
}
