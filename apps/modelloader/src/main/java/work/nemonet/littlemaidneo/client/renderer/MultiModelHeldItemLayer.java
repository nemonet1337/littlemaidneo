package work.nemonet.littlemaidneo.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import work.nemonet.littlemaidneo.entity.compound.IHasMultiModel;
import work.nemonet.littlemaidneo.multimodel.layer.MMMatrixStack;
public class MultiModelHeldItemLayer<S extends MultiModelRenderState, M extends MultiModel<S>> extends RenderLayer<S, M> {

    public MultiModelHeldItemLayer(RenderLayerParent<S, M> context) {
        super(context);
    }

    @Override
    public void submit(PoseStack poseStack, SubmitNodeCollector submitNodeCollector, int light, S state, float headYaw, float headPitch) {
        if (state.entity == null) return;
        boolean isMainRight = state.mainArm == HumanoidArm.RIGHT;
        ItemStack rightStack = isMainRight ? state.mainHandItem : state.offHandItem;
        ItemStack leftStack = isMainRight ? state.offHandItem : state.mainHandItem;
        if (!leftStack.isEmpty() || !rightStack.isEmpty()) {
            poseStack.pushPose();
            if (state.isBaby) {
                poseStack.translate(0.0D, 0.75D, 0.0D);
                poseStack.scale(0.5F, 0.5F, 0.5F);
            }
            this.handRender(state, rightStack, ItemDisplayContext.THIRD_PERSON_RIGHT_HAND, HumanoidArm.RIGHT, poseStack, submitNodeCollector, light);
            this.handRender(state, leftStack, ItemDisplayContext.THIRD_PERSON_LEFT_HAND, HumanoidArm.LEFT, poseStack, submitNodeCollector, light);
            poseStack.popPose();
        }
    }

    private void handRender(S state, ItemStack stack, ItemDisplayContext mode, HumanoidArm hand,
                             PoseStack poseStack, SubmitNodeCollector submitNodeCollector, int light) {
        if (!stack.isEmpty()) {
            poseStack.pushPose();
            boolean isLeft = hand == HumanoidArm.LEFT;
            state.multiModel.getModel(IHasMultiModel.Layer.SKIN, IHasMultiModel.Part.BODY)
                    .ifPresent(model -> model.adjustHandItem(new MMMatrixStack(poseStack), isLeft));
            poseStack.mulPose(Axis.XP.rotationDegrees(-90.0F));
            poseStack.mulPose(Axis.YP.rotationDegrees(180.0F));
            poseStack.translate(isLeft ? -0.0125F : 0.0125F, 0.05f, -0.15f);
            Minecraft.getInstance().getEntityRenderDispatcher().getItemInHandRenderer()
                    .renderItem(state.entity, stack, mode, poseStack, submitNodeCollector, light);
            poseStack.popPose();
        }
    }
}
