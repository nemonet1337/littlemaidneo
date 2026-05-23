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
public class MultiModelLightLayer<T extends LivingEntity & IHasMultiModel, M extends MultiModel<T>> extends RenderLayer<T, M> {

    public MultiModelLightLayer(RenderLayerParent<T, M> context) {
        super(context);
    }

    @Override
    public void render(PoseStack poseStack, MultiBufferSource bufferSource, int light, T entity,
                       float limbAngle, float limbDistance, float tickDelta, float animationProgress,
                       float headYaw, float headPitch) {
        ProfilerFiller profiler = Minecraft.getInstance().getProfiler();
        profiler.push("littlemaidneo:mm_eye_layer");
        renderLightLayer(poseStack, bufferSource, entity, limbAngle, limbDistance, tickDelta, animationProgress,
                headYaw, headPitch, entity.getCaps());
        profiler.pop();
    }

    private void renderLightLayer(PoseStack poseStack, MultiBufferSource bufferSource, T entity,
                                  float limbAngle, float limbDistance, float tickDelta, float animationProgress,
                                  float headYaw, float headPitch, IModelCaps caps) {
        entity.getTexture(IHasMultiModel.Layer.SKIN, IHasMultiModel.Part.HEAD, true).ifPresent(resourceLocation ->
                entity.getModel(IHasMultiModel.Layer.SKIN, IHasMultiModel.Part.HEAD).ifPresent(model -> {
                    VertexConsumer builder = bufferSource.getBuffer(MultiModelRenderLayer.getDefault(resourceLocation));
                    model.animateModel(caps, limbAngle, limbDistance, tickDelta);
                    model.setAngles(caps, limbAngle, limbDistance, animationProgress, headYaw, headPitch);
                    model.render(new MMRenderContext(poseStack, builder, 0xF00000, OverlayTexture.NO_OVERLAY,
                            1F, 1F, 1F, 1F));
                }));
    }
}
