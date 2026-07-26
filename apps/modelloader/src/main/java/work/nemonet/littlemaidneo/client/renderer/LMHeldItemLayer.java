package work.nemonet.littlemaidneo.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import work.nemonet.littlemaidneo.maidmodel.LMModel;

/**
 * メイドさんの手持ちアイテム描画。
 * 旧 {@code Arms[n].postRender} 相当: main_frame→torso→neck→arm→(arm_lower)→arm_right/left
 * まで親チェーンを辿り、ハンドアンカー位置でアイテムを描画する。
 */
public class LMHeldItemLayer<S extends MultiModelRenderState, M extends LMModel<S>> extends RenderLayer<S, M> {

    public LMHeldItemLayer(RenderLayerParent<S, M> context) {
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
            handRender(state, rightStack, ItemDisplayContext.THIRD_PERSON_RIGHT_HAND, HumanoidArm.RIGHT, poseStack, submitNodeCollector, light);
            handRender(state, leftStack, ItemDisplayContext.THIRD_PERSON_LEFT_HAND, HumanoidArm.LEFT, poseStack, submitNodeCollector, light);
            poseStack.popPose();
        }
    }

    private void handRender(S state, ItemStack stack, ItemDisplayContext mode, HumanoidArm hand,
                             PoseStack poseStack, SubmitNodeCollector submitNodeCollector, int light) {
        if (stack.isEmpty()) return;

        poseStack.pushPose();
        boolean isLeft = hand == HumanoidArm.LEFT;
        M model = getParentModel();
        if (model != null) {
            translateToHand(model, isLeft, poseStack);
        }
        // ItemInHandRenderer が想定する第三者視点ハンド向きへ合わせる
        poseStack.mulPose(Axis.XP.rotationDegrees(-90.0F));
        poseStack.mulPose(Axis.YP.rotationDegrees(180.0F));
        // 旧 renderItems: glTranslatef(0, 0.05, -0.05) 相当 + わずかな左右補正
        poseStack.translate(isLeft ? -0.0125F : 0.0125F, 0.05F, -0.05F);
        Minecraft.getInstance().getEntityRenderDispatcher().getItemInHandRenderer()
                .renderItem(state.entity, stack, mode, poseStack, submitNodeCollector, light);
        poseStack.popPose();
    }

    /**
     * 旧 Arms.postRender 相当の変換を PoseStack に適用する。
     * <pre>
     * main_frame → biped_torso → biped_neck → biped_*_arm
     *   → [arm_lower] → arm_right / arm_left   （ハンドアンカー）
     * </pre>
     * アンカーが無いモデルは旧 Orign 系の固定オフセット (∓1, 5, -1)/16 にフォールバックする。
     */
    private void translateToHand(M model, boolean isLeft, PoseStack poseStack) {
        ModelPart mainFrame = LMModel.getChildSafe(model.getSkinRoot(), "main_frame");
        ModelPart torso = LMModel.getChildSafe(mainFrame, "biped_torso");
        ModelPart neck = LMModel.getChildSafe(torso, "biped_neck");
        ModelPart arm = LMModel.getChildSafe(neck, isLeft ? "biped_left_arm" : "biped_right_arm");
        if (mainFrame == null || torso == null || neck == null || arm == null) {
            return;
        }
        mainFrame.translateAndRotate(poseStack);
        torso.translateAndRotate(poseStack);
        neck.translateAndRotate(poseStack);
        arm.translateAndRotate(poseStack);

        // 二節腕: 下腕を経由
        ModelPart lower = LMModel.getChildSafe(arm, "arm_lower");
        if (lower != null) {
            lower.translateAndRotate(poseStack);
        }

        // ハンドアンカー（旧 Arms[0]/[1]）
        ModelPart anchor = LMModel.getChildSafe(lower != null ? lower : arm, isLeft ? "arm_left" : "arm_right");
        if (anchor != null) {
            anchor.translateAndRotate(poseStack);
        } else {
            // アンカー未定義モデル用フォールバック（旧 ModelLittleMaidBase Arms 位置）
            poseStack.translate(isLeft ? 0.0625F : -0.0625F, 0.3125F, -0.0625F);
        }
    }
}
