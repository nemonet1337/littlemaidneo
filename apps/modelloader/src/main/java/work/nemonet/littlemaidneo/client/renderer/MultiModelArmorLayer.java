package work.nemonet.littlemaidneo.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.neoforged.api.distmarker.Dist;
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
        IHasMultiModel mm = state.multiModel;
        if (!mm.isArmorVisible(part)) return;

        boolean glint = mm.isArmorGlint(part);
        IModelCaps caps = mm.getCaps();

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
        IHasMultiModel mm = state.multiModel;
        mm.getTexture(layer, part, isLight).ifPresent(texId ->
                mm.getModel(layer, part).ifPresent(model -> {
                    model.showArmorParts(part.getIndex(), layer.getPartIndex());
                    int light0 = isLight ? 0xF00000 : light;
                    model.animateModel(caps, state.walkAnimationPos, state.walkAnimationSpeed, state.partialTick);
                    model.setAngles(caps, state.walkAnimationPos, state.walkAnimationSpeed,
                            state.ageInTicks, headYaw, headPitch);
                    submitNodeCollector.submitCustomGeometry(poseStack, MultiModelRenderLayer.getDefault(texId), (snapPose, consumer) -> {
                        PoseStack localStack = new PoseStack();
                        localStack.last().set(snapPose);
                        model.render(new MMRenderContext(localStack, consumer, light0, OverlayTexture.NO_OVERLAY, 1F, 1F, 1F, 1F));
                    });
                })
        );
    }
}
