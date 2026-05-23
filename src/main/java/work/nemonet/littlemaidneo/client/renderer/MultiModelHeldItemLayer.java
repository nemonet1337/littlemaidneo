package work.nemonet.littlemaidneo.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import work.nemonet.littlemaidneo.entity.compound.IHasMultiModel;
import work.nemonet.littlemaidneo.multimodel.layer.MMMatrixStack;

@OnlyIn(Dist.CLIENT)
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

    private void handRender(T entity, ItemStack stack, ItemDisplayContext mode, HumanoidArm hand, PoseStack poseStack, MultiBufferSource buffer, int light) {
        if (!stack.isEmpty()) {
            poseStack.pushPose();
            boolean isLeft = hand == HumanoidArm.LEFT;
            entity.getModel(IHasMultiModel.Layer.SKIN, IHasMultiModel.Part.BODY)
                    .ifPresent(model -> model.adjustHandItem(new MMMatrixStack(poseStack), isLeft));
            poseStack.mulPose(Axis.XP.rotationDegrees(-90.0F));
            poseStack.mulPose(Axis.YP.rotationDegrees(180.0F));
            poseStack.translate(isLeft ? -0.0125F : 0.0125F, 0.05f, -0.15f);
            Minecraft.getInstance().getItemRenderer().renderStatic(entity, stack, mode, isLeft, poseStack, buffer, entity.level(), light, OverlayTexture.NO_OVERLAY, entity.getId() + mode.ordinal());
            poseStack.popPose();
        }
    }
}
