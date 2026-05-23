package net.sistr.littlemaidmodelloader.client.renderer;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.util.profiling.ProfilerFiller;
import net.sistr.littlemaidmodelloader.entity.compound.IHasMultiModel;
import net.sistr.littlemaidmodelloader.maidmodel.IModelCaps;
import net.sistr.littlemaidmodelloader.multimodel.layer.MMRenderContext;

// 1.21.1移植: YarnマッピングからMojangマッピングへ変更
// - Minecraft → Minecraft
// - VertexConsumerProvider → MultiBufferSource
// - FeatureRenderer → RenderLayer
// - FeatureRendererContext → RenderLayerParent
// - MatrixStack → PoseStack (MMRenderContext内で処理)
// - RenderLayer → RenderType
// - Profiler → ProfilerFiller
// - ItemRenderer.getArmorGlintConsumer → 新しいAPIパターンに変更
//TODO 重すぎる
@Environment(EnvType.CLIENT)
public class MultiModelArmorLayer<T extends LivingEntity & IHasMultiModel, M extends MultiModel<T>> extends RenderLayer<T, M> {

    public MultiModelArmorLayer(RenderLayerParent<T, M> context) {
        super(context);
    }

    @Override
    public void render(com.mojang.blaze3d.vertex.PoseStack poseStack, MultiBufferSource bufferSource, int light, T entity,
                       float limbAngle, float limbDistance, float tickDelta, float animationProgress,
                       float headYaw, float headPitch) {
        ProfilerFiller profiler = Minecraft.getInstance().getProfiler();
        profiler.push("littlemaidmodelloader:mm_armor_layer");
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

    private void renderArmorPart(com.mojang.blaze3d.vertex.PoseStack poseStack, MultiBufferSource bufferSource, int light, T entity,
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

    private void renderArmorLayer(com.mojang.blaze3d.vertex.PoseStack poseStack, MultiBufferSource bufferSource, int light, T entity,
                                  float limbAngle, float limbDistance, float tickDelta, float animationProgress,
                                  float headYaw, float headPitch,
                                  IHasMultiModel.Part part, IHasMultiModel.Layer layer, boolean isLight, IModelCaps caps,
                                  boolean glint) {
        entity.getTexture(layer, part, isLight).ifPresent(resourceLocation ->
                entity.getModel(layer, part).ifPresent(model -> {
                    model.showArmorParts(part.getIndex(), layer.getPartIndex());
                    RenderType type = MultiModelRenderLayer.getDefault(resourceLocation);
                    // 1.21.1: ItemRenderer.getArmorGlintConsumer が変更されたため、新しいAPIを使用
                    VertexConsumer builder = bufferSource.getBuffer(type);
                    int light0 = isLight ? 0xF00000 : light;
                    model.animateModel(caps, limbAngle, limbDistance, tickDelta);
                    model.setAngles(caps, limbAngle, limbDistance, animationProgress, headYaw, headPitch);
                    model.render(new MMRenderContext(poseStack, builder, light0, OverlayTexture.NO_OVERLAY,
                            1F, 1F, 1F, 1F));
                })
        );
    }

}
