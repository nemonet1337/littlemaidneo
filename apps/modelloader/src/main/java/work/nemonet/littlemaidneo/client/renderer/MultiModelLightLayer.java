package work.nemonet.littlemaidneo.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import work.nemonet.littlemaidneo.entity.compound.IHasMultiModel;
import work.nemonet.littlemaidneo.maidmodel.IModelCaps;
import work.nemonet.littlemaidneo.multimodel.layer.MMRenderContext;
public class MultiModelLightLayer<S extends MultiModelRenderState, M extends MultiModel<S>> extends RenderLayer<S, M> {

    public MultiModelLightLayer(RenderLayerParent<S, M> context) {
        super(context);
    }

    @Override
    public void submit(PoseStack poseStack, SubmitNodeCollector submitNodeCollector, int light, S state, float headYaw, float headPitch) {
        if (state.multiModel == null) return;
        IHasMultiModel mm = state.multiModel;
        IModelCaps caps = mm.getCaps();
        mm.getTexture(IHasMultiModel.Layer.SKIN, IHasMultiModel.Part.HEAD, true).ifPresent(texId ->
                mm.getModel(IHasMultiModel.Layer.SKIN, IHasMultiModel.Part.HEAD).ifPresent(model -> {
                    model.animateModel(caps, state.walkAnimationPos, state.walkAnimationSpeed, state.partialTick);
                    model.setAngles(caps, state.walkAnimationPos, state.walkAnimationSpeed,
                            state.ageInTicks, headYaw, headPitch);
                    submitNodeCollector.submitCustomGeometry(poseStack, MultiModelRenderLayer.getDefault(texId), (snapPose, consumer) -> {
                        PoseStack localStack = new PoseStack();
                        localStack.last().set(snapPose);
                        model.render(new MMRenderContext(localStack, consumer, 0xF00000, OverlayTexture.NO_OVERLAY, 1F, 1F, 1F, 1F));
                    });
                })
        );
    }
}
