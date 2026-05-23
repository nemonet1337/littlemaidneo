package net.sistr.littlemaidmodelloader.client.renderer;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.item.ItemDisplayContext;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.entity.HumanoidArm;
import com.mojang.math.Axis;
import net.sistr.littlemaidmodelloader.entity.compound.IHasMultiModel;
import net.sistr.littlemaidmodelloader.multimodel.layer.MMMatrixStack;

// 1.21.1移植: YarnマッピングからMojangマッピングへ変更
// - Minecraft → Minecraft
// - VertexConsumerProvider → MultiBufferSource
// - FeatureRenderer → RenderLayer
// - FeatureRendererContext → RenderLayerParent
// - MatrixStack → PoseStack
// - Arm → HumanoidArm
// - RotationAxis → Axis
// - ModelTransformationMode → ItemDisplayContext
// - getMainHandStack → getMainHandItem
// - getOffHandStack → getOffhandItem
// - child → young (フィールド名)
// - push → pushPose
// - pop → popPose
// - multiply → mulPose
// - getWorld → level
@Environment(EnvType.CLIENT)
public class MultiModelHeldItemLayer<T extends LivingEntity & IHasMultiModel, M extends MultiModel<T>> extends RenderLayer<T, M> {

    public MultiModelHeldItemLayer(RenderLayerParent<T, M> context) {
        super(context);
    }

    @Override
    public void render(PoseStack poseStack, MultiBufferSource bufferSource, int light, T entity,
                       float limbAngle, float limbDistance, float tickDelta, float animationProgress,
                       float headYaw, float headPitch) {
        boolean isMainRight = entity.getMainArm() == HumanoidArm.RIGHT;
        ItemStack rightStack = isMainRight ? entity.getMainHandItem() : entity.getOffhandItem();
        ItemStack leftStack = isMainRight ? entity.getOffhandItem() : entity.getMainHandItem();
        if (!leftStack.isEmpty() || !rightStack.isEmpty()) {
            poseStack.pushPose();
            if (this.getParentModel().young) {
                poseStack.translate(0.0D, 0.75D, 0.0D);
                poseStack.scale(0.5F, 0.5F, 0.5F);
            }

            this.handRender(entity, rightStack, ItemDisplayContext.THIRD_PERSON_RIGHT_HAND, HumanoidArm.RIGHT, poseStack, bufferSource, light);
            this.handRender(entity, leftStack, ItemDisplayContext.THIRD_PERSON_LEFT_HAND, HumanoidArm.LEFT, poseStack, bufferSource, light);
            poseStack.popPose();
        }
    }

    //TODO 位置調整
    private void handRender(T entity, ItemStack stack, ItemDisplayContext mode, HumanoidArm hand, PoseStack poseStack, MultiBufferSource buffer, int light) {
        if (!stack.isEmpty()) {
            poseStack.pushPose();
            boolean isLeft = hand == HumanoidArm.LEFT;
            entity.getModel(IHasMultiModel.Layer.SKIN, IHasMultiModel.Part.BODY)
                    .ifPresent(model -> model.adjustHandItem(new MMMatrixStack(poseStack), isLeft));

            poseStack.mulPose(Axis.XP.rotationDegrees(-90.0F));
            poseStack.mulPose(Axis.YP.rotationDegrees(180.0F));
            /* 初期モデル構成で
             * x: 手の甲に垂直な方向(-で向かって右に移動)
             * y: 体の面に垂直な方向(-で向かって背面方向に移動)
             * z: 腕に平行な方向(-で向かって手の先方向に移動)
             */
            poseStack.translate(isLeft ? -0.0125F : 0.0125F, 0.05f, -0.15f);
            // 1.21.1: renderItemのシグネチャが変更されたため、新しいAPIを使用
            Minecraft.getInstance().getItemRenderer().renderStatic(entity, stack, mode, isLeft, poseStack, buffer, entity.level(), light, OverlayTexture.NO_OVERLAY, entity.getId() + mode.ordinal());
            poseStack.popPose();
        }
    }

}
