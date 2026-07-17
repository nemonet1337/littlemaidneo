package work.nemonet.littlemaidneo.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;
import work.nemonet.littlemaidneo.maidmodel.LMModel;

public class LMSkinLayer<S extends MultiModelRenderState, M extends LMModel<S>> extends RenderLayer<S, M> {

    public LMSkinLayer(RenderLayerParent<S, M> context) {
        super(context);
    }

    @Override
    public void submit(PoseStack poseStack, SubmitNodeCollector submitNodeCollector, int light, S state, float headYaw, float headPitch) {
        if (state.skinTexture == null) return;
        M model = getParentModel();
        submitNodeCollector.submitCustomGeometry(poseStack, model.renderType(state.skinTexture), (snapPose, consumer) -> {
            PoseStack local = new PoseStack();
            local.last().set(snapPose);
            model.getSkinRoot().render(local, consumer, light, OverlayTexture.NO_OVERLAY);
        });
    }
}
