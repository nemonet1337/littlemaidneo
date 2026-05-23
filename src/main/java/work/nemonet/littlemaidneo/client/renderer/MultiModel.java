package work.nemonet.littlemaidneo.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import work.nemonet.littlemaidneo.entity.compound.IHasMultiModel;
import work.nemonet.littlemaidneo.multimodel.layer.MMRenderContext;

@OnlyIn(Dist.CLIENT)
public class MultiModel<T extends LivingEntity & IHasMultiModel> extends EntityModel<T> {
    private T entity;

    public MultiModel() {
        super(MultiModelRenderLayer::getDefault);
    }

    @Override
    public void prepareMobModel(T entity, float limbAngle, float limbDistance, float tickDelta) {
        entity.getModel(IHasMultiModel.Layer.SKIN, IHasMultiModel.Part.HEAD)
                .ifPresent(model -> model.animateModel(entity.getCaps(), limbAngle, limbDistance, tickDelta));
    }

    @Override
    public void setupAnim(T entity, float limbAngle, float limbDistance, float animationProgress, float headYaw, float headPitch) {
        entity.getModel(IHasMultiModel.Layer.SKIN, IHasMultiModel.Part.HEAD)
                .ifPresent(model -> model.setAngles(entity.getCaps(), limbAngle, limbDistance, animationProgress, headYaw, headPitch));
        this.entity = entity;
    }

    @Override
    public void renderToBuffer(PoseStack poseStack, VertexConsumer buffer, int packedLight, int packedOverlay, int color) {
        if (this.entity == null) {
            return;
        }
        float alpha = ((color >> 24) & 0xFF) / 255.0f;
        float red = ((color >> 16) & 0xFF) / 255.0f;
        float green = ((color >> 8) & 0xFF) / 255.0f;
        float blue = (color & 0xFF) / 255.0f;

        this.entity.getModel(IHasMultiModel.Layer.SKIN, IHasMultiModel.Part.HEAD)
                .ifPresent(model -> model.render(new MMRenderContext(poseStack, buffer, packedLight, packedOverlay, red, green, blue, alpha)));
        this.entity = null;
    }
}
