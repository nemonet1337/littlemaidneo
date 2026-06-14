package work.nemonet.littlemaidneo.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import work.nemonet.littlemaidneo.entity.compound.IHasMultiModel;
import work.nemonet.littlemaidneo.multimodel.layer.MMRenderContext;
public class MultiModelSkinLayer<S extends MultiModelRenderState, M extends MultiModel<S>> extends RenderLayer<S, M> {

    public MultiModelSkinLayer(RenderLayerParent<S, M> context) {
        super(context);
    }

    @Override
    public void submit(PoseStack poseStack, SubmitNodeCollector submitNodeCollector, int light, S state, float headYaw, float headPitch) {
        if (state.skinTexture == null || state.skinModel == null) return;
        submitNodeCollector.submitCustomGeometry(poseStack, MultiModelRenderLayer.getDefault(state.skinTexture), (snapPose, consumer) -> {
            PoseStack localStack = new PoseStack();
            localStack.last().set(snapPose);
            state.skinModel.render(new MMRenderContext(localStack, consumer, light, OverlayTexture.NO_OVERLAY, 1f, 1f, 1f, 1f));
        });
    }
}
