package work.nemonet.littlemaidneo.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.resources.Identifier;
import net.minecraft.util.LightCoordsUtil;
import work.nemonet.littlemaidneo.entity.compound.IHasMultiModel;
import work.nemonet.littlemaidneo.maidmodel.LMModel;

public class LMArmorLayer<S extends MultiModelRenderState, M extends LMModel<S>> extends RenderLayer<S, M> {

    public LMArmorLayer(RenderLayerParent<S, M> context) {
        super(context);
    }

    @Override
    public void submit(PoseStack poseStack, SubmitNodeCollector submitNodeCollector, int light, S state, float headYaw, float headPitch) {
        if (state.armorStates == null) return;

        // 装備スロットごとに、そのスロットに対応するボディパーツのみを描画する（旧 showArmorParts 相当）
        for (IHasMultiModel.Part part : IHasMultiModel.Part.values()) {
            MultiModelRenderState.ArmorRenderState ars = state.armorStates[part.getIndex()];
            if (ars == null || !ars.visible()) continue;

            renderSingle(poseStack, submitNodeCollector, light, ars.innerModel(), ars.innerTexture(), false, IHasMultiModel.Layer.INNER, part);
            renderSingle(poseStack, submitNodeCollector, light, ars.innerModel(), ars.innerLightTexture(), true, IHasMultiModel.Layer.INNER, part);
            renderSingle(poseStack, submitNodeCollector, light, ars.outerModel(), ars.outerTexture(), false, IHasMultiModel.Layer.OUTER, part);
            renderSingle(poseStack, submitNodeCollector, light, ars.outerModel(), ars.outerLightTexture(), true, IHasMultiModel.Layer.OUTER, part);
        }
    }

    private void renderSingle(PoseStack poseStack, SubmitNodeCollector submitNodeCollector, int light,
                               LMModel<?> model, Identifier tex, boolean isLight, IHasMultiModel.Layer layer,
                               IHasMultiModel.Part part) {
        if (model == null || tex == null) return;
        int lightVal = isLight ? LightCoordsUtil.FULL_BRIGHT : light;
        // ルート解決とポーズ保存は submit 時点で行う（ラムダ実行時は別メイドさんの状態に書き換わっている）
        ModelPart root = layer == IHasMultiModel.Layer.OUTER ? model.getOuterRoot() : model.getInnerRoot();
        if (root == null) root = model.getSkinRoot();
        ModelPart mainFrame = LMModel.getChildSafe(root, "main_frame");
        ModelPart renderRoot = mainFrame != null ? mainFrame : root;
        LMModel.PoseSnapshot pose = LMModel.capturePose(renderRoot);
        submitNodeCollector.submitCustomGeometry(poseStack, model.renderType(tex), (snapPose, consumer) -> {
            PoseStack local = new PoseStack();
            local.last().set(snapPose);
            pose.apply();
            setPartVisibility(renderRoot, part);
            renderRoot.render(local, consumer, lightVal, OverlayTexture.NO_OVERLAY);
            setPartVisibility(renderRoot, null);
        });
    }

    /** part に対応するパーツのみ可視にする。null なら全パーツ可視に戻す。 */
    private static void setPartVisibility(ModelPart mainFrame, IHasMultiModel.Part part) {
        ModelPart torso = LMModel.getChildSafe(mainFrame, "biped_torso");
        ModelPart neck = LMModel.getChildSafe(torso, "biped_neck");
        ModelPart pelvic = LMModel.getChildSafe(torso, "biped_pelvic");
        if (pelvic == null) pelvic = LMModel.getChildSafe(LMModel.getChildSafe(torso, "biped_trunk"), "biped_pelvic");
        boolean all = part == null;
        setVisible(LMModel.getChildSafe(neck, "biped_head"), all || part == IHasMultiModel.Part.HEAD);
        boolean body = all || part == IHasMultiModel.Part.BODY;
        setVisible(LMModel.getChildSafe(torso, "biped_body"), body);
        setVisible(LMModel.getChildSafe(neck, "biped_right_arm"), body);
        setVisible(LMModel.getChildSafe(neck, "biped_left_arm"), body);
        setVisible(LMModel.getChildSafe(pelvic, "skirt"), all || part == IHasMultiModel.Part.LEGS);
        boolean feet = all || part == IHasMultiModel.Part.FEET;
        setVisible(LMModel.getChildSafe(pelvic, "biped_right_leg"), feet);
        setVisible(LMModel.getChildSafe(pelvic, "biped_left_leg"), feet);
    }

    private static void setVisible(ModelPart p, boolean visible) {
        if (p != null) p.visible = visible;
    }
}
