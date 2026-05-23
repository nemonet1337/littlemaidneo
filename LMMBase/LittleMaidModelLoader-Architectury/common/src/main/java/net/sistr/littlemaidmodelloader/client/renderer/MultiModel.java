package net.sistr.littlemaidmodelloader.client.renderer;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.model.EntityModel;
import net.minecraft.world.entity.LivingEntity;
import net.sistr.littlemaidmodelloader.entity.compound.IHasMultiModel;
import net.sistr.littlemaidmodelloader.multimodel.layer.MMRenderContext;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;

// 1.21.1移植: YarnマッピングからMojangマッピングへ変更
// - RenderLayer → RenderType
// - VertexConsumer → VertexConsumer (同じ)
// - EntityModel → EntityModel (同じ)
// - MatrixStack → PoseStack
// - LivingEntity → LivingEntity (同じ)
// - animateModel → prepareMobModel
// - setAngles → setupAnim
// - render → renderToBuffer (シグネチャ変更: int packedOverlayが追加)
@Environment(EnvType.CLIENT)
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

    // 1.21.1: renderToBufferのシグネチャ変更 (packedOverlayがintからintに変更、引数追加なし)
    @Override
    public void renderToBuffer(PoseStack poseStack, VertexConsumer buffer, int packedLight, int packedOverlay, int color) {
        if (this.entity == null) {
            return;
        }
        // ARGBからRGBAに変換
        float alpha = ((color >> 24) & 0xFF) / 255.0f;
        float red = ((color >> 16) & 0xFF) / 255.0f;
        float green = ((color >> 8) & 0xFF) / 255.0f;
        float blue = (color & 0xFF) / 255.0f;
        
        this.entity.getModel(IHasMultiModel.Layer.SKIN, IHasMultiModel.Part.HEAD)
                .ifPresent(model -> model.render(new MMRenderContext(poseStack, buffer, packedLight, packedOverlay, red, green, blue, alpha)));
        this.entity = null;
    }

}
