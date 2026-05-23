package work.nemonet.littlemaidneo.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import work.nemonet.littlemaidneo.entity.compound.IHasMultiModel;
import work.nemonet.littlemaidneo.maidmodel.IModelCaps;
import work.nemonet.littlemaidneo.multimodel.layer.MMRenderContext;

@OnlyIn(Dist.CLIENT)
public class MultiModelArmorLayer<T extends LivingEntity & IHasMultiModel, M extends MultiModel<T>> extends RenderLayer<T, M> {

    public MultiModelArmorLayer(RenderLayerParent<T, M> context) {
        super(context);
    }

    @Override
    public void render(PoseStack poseStack, MultiBufferSource bufferSource, int light, T entity,
                       float limbAngle, float limbDistance, float tickDelta, float animationProgress,
                       float headYaw, float headPitch) {
        ProfilerFiller profiler = Minecraft.getInstance().getProfiler();
        profiler.push("littlemaidneo:mm_armor_layer");
        this.renderArmorPart(poseStack, bufferSource, light, entity, limbAngle, limbDistance, tickDelta, animationProgress,
                headYaw, headPitch, IHasMultiModel.Part.HEAD);
        this.renderArmorPart(poseStack, bufferSource, light, entity, limbAngle, limbDistance, tickDelta, animationProgress,
                headYaw, headPitch, IHasMultiModel.Part.BODY);
        this.renderArmorPart(poseStack, bufferSource, light, entity, limbAngle, limbDistance, tickDelta, animationProgress,
                headYaw, headPitch, IHasMultiModel.Part.LEGS);
        this.renderArmorPart(poseStack, bufferSource, light, entity, limbAngle, limbDistance, tickDelta, animationProgress,
                headYaw, headPitch, IHasMultiModel.Part.FEET);
        profiler.pop();
    }

    private void renderArmorPart(PoseStack poseStack, MultiBufferSource bufferSource, int light, T entity,
                                 float limbAngle, float limbDistance, float tickDelta, float animationProgress,
                                 float headYaw, float headPitch, IHasMultiModel.Part part) {
        if (!entity.isArmorVisible(part)) {
            return;
        }

        boolean glint = entity.isArmorGlint(part);
        IModelCaps caps = entity.getCaps();

        renderArmorLayer(poseStack, bufferSource, light, entity,
                limbAngle, limbDistance, tickDelta, animationProgress, headYaw, headPitch,
                part, IHasMultiModel.Layer.INNER, false, caps, glint);
        renderArmorLayer(poseStack, bufferSource, light, entity,
                limbAngle, limbDistance, tickDelta, animationProgress, headYaw, headPitch,
                part, IHasMultiModel.Layer.INNER, true, caps, glint);
        renderArmorLayer(poseStack, bufferSource, light, entity,
                limbAngle, limbDistance, tickDelta, animationProgress, headYaw, headPitch,
                part, IHasMultiModel.Layer.OUTER, false, caps, glint);
        renderArmorLayer(poseStack, bufferSource, light, entity,
                limbAngle, limbDistance, tickDelta, animationProgress, headYaw, headPitch,
                part, IHasMultiModel.Layer.OUTER, true, caps, glint);
    }

    private void renderArmorLayer(PoseStack poseStack, MultiBufferSource bufferSource, int light, T entity,
                                  float limbAngle, float limbDistance, float tickDelta, float animationProgress,
                                  float headYaw, float headPitch,
                                  IHasMultiModel.Part part, IHasMultiModel.Layer layer, boolean isLight, IModelCaps caps,
                                  boolean glint) {
        entity.getTexture(layer, part, isLight).ifPresent(resourceLocation ->
                entity.getModel(layer, part).ifPresent(model -> {
                    model.showArmorParts(part.getIndex(), layer.getPartIndex());
                    VertexConsumer builder = bufferSource.getBuffer(MultiModelRenderLayer.getDefault(resourceLocation));
                    int light0 = isLight ? 0xF00000 : light;
                    model.animateModel(caps, limbAngle, limbDistance, tickDelta);
                    model.setAngles(caps, limbAngle, limbDistance, animationProgress, headYaw, headPitch);
                    model.render(new MMRenderContext(poseStack, builder, light0, OverlayTexture.NO_OVERLAY,
                            1F, 1F, 1F, 1F));
                })
        );
    }
}
