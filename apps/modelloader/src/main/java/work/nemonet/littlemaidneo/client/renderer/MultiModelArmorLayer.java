package work.nemonet.littlemaidneo.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import work.nemonet.littlemaidneo.entity.compound.IHasMultiModel;
import work.nemonet.littlemaidneo.maidmodel.IModelCaps;
import work.nemonet.littlemaidneo.multimodel.layer.MMRenderContext;
public class MultiModelArmorLayer<S extends MultiModelRenderState, M extends MultiModel<S>> extends RenderLayer<S, M> {

    public MultiModelArmorLayer(RenderLayerParent<S, M> context) {
        super(context);
    }

    @Override
    public void submit(PoseStack poseStack, SubmitNodeCollector submitNodeCollector, int light, S state, float headYaw, float headPitch) {
        if (state.multiModel == null) return;
        this.renderArmorPart(poseStack, submitNodeCollector, light, state, headYaw, headPitch, IHasMultiModel.Part.HEAD);
        this.renderArmorPart(poseStack, submitNodeCollector, light, state, headYaw, headPitch, IHasMultiModel.Part.BODY);
        this.renderArmorPart(poseStack, submitNodeCollector, light, state, headYaw, headPitch, IHasMultiModel.Part.LEGS);
        this.renderArmorPart(poseStack, submitNodeCollector, light, state, headYaw, headPitch, IHasMultiModel.Part.FEET);
    }

    private void renderArmorPart(PoseStack poseStack, SubmitNodeCollector submitNodeCollector, int light, S state,
                                 float headYaw, float headPitch, IHasMultiModel.Part part) {
        if (state.armorsVisible == null || !state.armorsVisible.getArmor(part).orElse(false)) return;

        IModelCaps caps = state.caps;

        renderArmorLayer(poseStack, submitNodeCollector, light, state, headYaw, headPitch,
                part, IHasMultiModel.Layer.INNER, false, caps);
        renderArmorLayer(poseStack, submitNodeCollector, light, state, headYaw, headPitch,
                part, IHasMultiModel.Layer.INNER, true, caps);
        renderArmorLayer(poseStack, submitNodeCollector, light, state, headYaw, headPitch,
                part, IHasMultiModel.Layer.OUTER, false, caps);
        renderArmorLayer(poseStack, submitNodeCollector, light, state, headYaw, headPitch,
                part, IHasMultiModel.Layer.OUTER, true, caps);
    }

    private void renderArmorLayer(PoseStack poseStack, SubmitNodeCollector submitNodeCollector, int light, S state,
                                  float headYaw, float headPitch,
                                  IHasMultiModel.Part part, IHasMultiModel.Layer layer, boolean isLight, IModelCaps caps) {
        net.minecraft.resources.Identifier texId = null;
        work.nemonet.littlemaidneo.multimodel.IMultiModel model = null;
        if (layer == IHasMultiModel.Layer.INNER) {
            texId = isLight ? state.innerTexturesLight.getArmor(part).orElse(null) : state.innerTextures.getArmor(part).orElse(null);
            model = state.innerModels.getArmor(part).orElse(null);
        } else {
            texId = isLight ? state.outerTexturesLight.getArmor(part).orElse(null) : state.outerTextures.getArmor(part).orElse(null);
            model = state.outerModels.getArmor(part).orElse(null);
        }

        if (texId != null && model != null) {
            final int light0 = isLight ? 0xF00000 : light;
            final work.nemonet.littlemaidneo.multimodel.IMultiModel finalModel = model;
            model.animateModel(caps, state.walkAnimationPos, state.walkAnimationSpeed, state.partialTick);
            model.setAngles(caps, state.walkAnimationPos, state.walkAnimationSpeed,
                    state.ageInTicks, headYaw, headPitch);
            final int partIndex = part.getIndex();
            final int layerPartIndex = layer.getPartIndex();
            submitNodeCollector.submitCustomGeometry(poseStack, MultiModelRenderLayer.getArmor(texId), (snapPose, consumer) -> {
                PoseStack localStack = new PoseStack();
                localStack.last().set(snapPose);
                finalModel.showArmorParts(partIndex, layerPartIndex);
                finalModel.render(new MMRenderContext(localStack, consumer, light0, OverlayTexture.NO_OVERLAY, 1F, 1F, 1F, 1F));
            });
        }
    }
}
