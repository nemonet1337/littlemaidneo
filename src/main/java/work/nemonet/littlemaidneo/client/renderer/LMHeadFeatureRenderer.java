package work.nemonet.littlemaidneo.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.model.geom.EntityModelSet;
import net.minecraft.client.model.object.skull.SkullModelBase;
import net.minecraft.client.renderer.PlayerSkinRenderCache;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.SkullBlockRenderer;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.util.Util;
import net.minecraft.world.item.component.ResolvableProfile;
import net.minecraft.world.level.block.SkullBlock;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

import java.util.function.Function;

/**
 * メイドさんの頭飾りレンダラ
 */
@OnlyIn(Dist.CLIENT)
public class LMHeadFeatureRenderer<S extends MaidRenderState, M extends LMMultiModel<S>> extends RenderLayer<S, M> {

    private final Function<SkullBlock.Type, SkullModelBase> headModels;
    private final PlayerSkinRenderCache playerSkinRenderCache;

    public LMHeadFeatureRenderer(RenderLayerParent<S, M> context, EntityModelSet loader, PlayerSkinRenderCache playerSkinRenderCache) {
        super(context);
        this.headModels = Util.memoize(type -> SkullBlockRenderer.createModel(loader, type));
        this.playerSkinRenderCache = playerSkinRenderCache;
    }

    @Override
    public void submit(PoseStack matrixStack, SubmitNodeCollector submitNodeCollector, int light, S state, float headYaw, float headPitch) {
        if (state.maidEntity == null) return;

        boolean showHeadItem = !state.headItem.isEmpty() || state.wornHeadType != null;
        if (!showHeadItem) return;

        matrixStack.pushPose();
        this.getParentModel().getHead().translateAndRotate(matrixStack);

        if (state.wornHeadType != null) {
            matrixStack.scale(1.1875f, -1.1875f, -1.1875f);
            matrixStack.translate(-0.5, 0.0, -0.5);
            SkullModelBase skullModel = headModels.apply(state.wornHeadType);
            RenderType renderType = resolveSkullRenderType(state);
            SkullBlockRenderer.submitSkull(state.wornHeadAnimationPos, matrixStack, submitNodeCollector, light, skullModel, renderType, state.outlineColor, null);
        } else {
            translate(matrixStack, false);
            state.headItem.submit(matrixStack, submitNodeCollector, light, OverlayTexture.NO_OVERLAY, state.outlineColor);
        }

        matrixStack.popPose();
    }

    private RenderType resolveSkullRenderType(S state) {
        if (state.wornHeadType == SkullBlock.Types.PLAYER) {
            ResolvableProfile profile = state.wornHeadProfile;
            if (profile != null) {
                return this.playerSkinRenderCache.getOrDefault(profile).renderType();
            }
        }
        return SkullBlockRenderer.getSkullRenderType(state.wornHeadType, null);
    }

    public static void translate(PoseStack matrices, boolean villager) {
        matrices.translate(0.0, -0.25, 0.0);
        matrices.mulPose(Axis.YP.rotationDegrees(180.0f));
        matrices.scale(0.625f, -0.625f, -0.625f);
        if (villager) {
            matrices.translate(0.0, 0.1875, 0.0);
        }
    }
}
