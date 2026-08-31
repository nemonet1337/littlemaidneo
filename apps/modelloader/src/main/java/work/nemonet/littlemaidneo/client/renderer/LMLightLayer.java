package work.nemonet.littlemaidneo.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.util.LightCoordsUtil;
import work.nemonet.littlemaidneo.maidmodel.LMModel;

public class LMLightLayer<S extends MultiModelRenderState, M extends LMModel<S>> extends RenderLayer<S, M> {

    public LMLightLayer(RenderLayerParent<S, M> context) {
        super(context);
    }

    @Override
    public void submit(PoseStack poseStack, SubmitNodeCollector submitNodeCollector, int light, S state, float headYaw, float headPitch) {
        if (state.skinTextureLight == null) return;
        M model = getParentModel();
        // ルート解決とポーズ保存は submit 時点で行う（ラムダ実行時は別メイドさんの状態に書き換わっている）
        var skinRoot = model.getSkinRoot();
        LMModel.PoseSnapshot pose = LMModel.capturePose(skinRoot);
        submitNodeCollector.submitCustomGeometry(poseStack, model.renderType(state.skinTextureLight), (snapPose, consumer) -> {
            PoseStack local = new PoseStack();
            local.last().set(snapPose);
            pose.apply();
            skinRoot.render(local, consumer, LightCoordsUtil.FULL_BRIGHT, OverlayTexture.NO_OVERLAY);
        });
    }
}
