package work.nemonet.littlemaidneo.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.resources.Identifier;
import work.nemonet.littlemaidneo.entity.compound.IHasMultiModel;
import work.nemonet.littlemaidneo.maidmodel.LMModel;

public class LMArmorLayer<S extends MultiModelRenderState, M extends LMModel<S>> extends RenderLayer<S, M> {

    public LMArmorLayer(RenderLayerParent<S, M> context) {
        super(context);
    }

    @Override
    public void submit(PoseStack poseStack, SubmitNodeCollector submitNodeCollector, int light, S state, float headYaw, float headPitch) {
        if (state.armorStates == null) return;

        for (int i = 0; i < 4; i++) {
            MultiModelRenderState.ArmorRenderState ars = state.armorStates[i];
            if (ars == null || !ars.visible()) continue;

            renderArmorPart(poseStack, submitNodeCollector, light, ars, IHasMultiModel.Part.HEAD, i);
            renderArmorPart(poseStack, submitNodeCollector, light, ars, IHasMultiModel.Part.BODY, i);
            renderArmorPart(poseStack, submitNodeCollector, light, ars, IHasMultiModel.Part.LEGS, i);
            renderArmorPart(poseStack, submitNodeCollector, light, ars, IHasMultiModel.Part.FEET, i);
        }
    }

    private void renderArmorPart(PoseStack poseStack, SubmitNodeCollector submitNodeCollector, int light,
                                 MultiModelRenderState.ArmorRenderState ars, IHasMultiModel.Part part, int layerPartIndex) {
        renderSingle(poseStack, submitNodeCollector, light, ars.innerModel(), ars.innerTexture(), false, part);
        renderSingle(poseStack, submitNodeCollector, light, ars.innerModel(), ars.innerLightTexture(), true, part);
        renderSingle(poseStack, submitNodeCollector, light, ars.outerModel(), ars.outerTexture(), false, part);
        renderSingle(poseStack, submitNodeCollector, light, ars.outerModel(), ars.outerLightTexture(), true, part);
    }

    private void renderSingle(PoseStack poseStack, SubmitNodeCollector submitNodeCollector, int light,
                              LMModel<?> model, Identifier tex, boolean isLight, IHasMultiModel.Part part) {
        if (model == null || tex == null) return;
        int lightVal = isLight ? 0xF00000 : light;
        submitNodeCollector.submitCustomGeometry(poseStack, model.renderType(tex), (snapPose, consumer) -> {
            PoseStack local = new PoseStack();
            local.last().set(snapPose);
            ModelPart partRoot = switch (part) {
                case HEAD -> model.getSkinRoot().getChild("head");
                case BODY -> model.getSkinRoot().getChild("body");
                case LEGS -> model.getSkinRoot().getChild("legs");
                case FEET -> model.getSkinRoot().getChild("feet");
            };
            if (partRoot == null) return;
            boolean prev = partRoot.visible;
            partRoot.visible = true;
            partRoot.render(local, consumer, lightVal, OverlayTexture.NO_OVERLAY);
            partRoot.visible = prev;
        });
    }
}
