package net.sistr.littlemaidrebirth.client.renderer;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.item.Items;
import net.sistr.littlemaidmodelloader.client.renderer.MultiModelArmorLayer;
import net.sistr.littlemaidmodelloader.client.renderer.MultiModelHeldItemLayer;
import net.sistr.littlemaidmodelloader.client.renderer.MultiModelLightLayer;
import net.sistr.littlemaidmodelloader.entity.compound.IHasMultiModel;
import net.sistr.littlemaidmodelloader.maidmodel.ModelMultiBase;
import net.sistr.littlemaidmodelloader.multimodel.layer.MMMatrixStack;
import net.sistr.littlemaidrebirth.LMRBMod;
import net.sistr.littlemaidrebirth.entity.LittleMaidEntity;
import net.sistr.littlemaidrebirth.entity.util.TameableUtil;

import static net.sistr.littlemaidmodelloader.maidmodel.IModelCaps.*;

import com.mojang.blaze3d.vertex.PoseStack;

/**
 * メイド用レンダラ
 */
@Environment(EnvType.CLIENT)
public class MaidModelRenderer extends MobRenderer<LittleMaidEntity, LMMultiModel<LittleMaidEntity>> {
    private static final ResourceLocation NULL_TEXTURE = ResourceLocation.fromNamespaceAndPath(LMRBMod.MODID, "null");

    public MaidModelRenderer(EntityRendererProvider.Context ctx) {
        super(ctx, new LMMultiModel<>(), 0.5F);
        // エラー吐くので<>消した(ゴリ押し)
        this.addLayer(new MultiModelArmorLayer(this));
        this.addLayer(new MultiModelHeldItemLayer(this));
        this.addLayer(new MultiModelLightLayer(this));
        this.addLayer(new LMHeadFeatureRenderer<>(this, ctx.getModelSet()));
    }

    @Override
    protected void setupRotations(LittleMaidEntity entity, PoseStack matrices, float bodyYaw,
            float animationProgress, float tickDelta, float scale) {
        super.setupRotations(entity, matrices, bodyYaw, animationProgress, tickDelta, scale);
        entity.getModel(IHasMultiModel.Layer.SKIN, IHasMultiModel.Part.HEAD)
                .ifPresent(model -> model.setupTransform(entity.getCaps(),
                        new MMMatrixStack(matrices), animationProgress, bodyYaw, tickDelta));
    }

    @Override
    protected void scale(LittleMaidEntity entity, PoseStack matrices, float amount) {
        entity.getModel(IHasMultiModel.Layer.SKIN, IHasMultiModel.Part.HEAD)
                .filter(model -> model instanceof ModelMultiBase)
                .map(model -> (float) ((ModelMultiBase) model).getCapsValue(caps_ScaleFactor))
                .ifPresent(scale -> matrices.scale(scale, scale, scale));
    }

    @Override
    public void render(LittleMaidEntity livingEntity, float entityYaw, float partialTicks, PoseStack matrixStack,
            MultiBufferSource vertexConsumerProvider, int light) {
        ProfilerFiller profiler = Minecraft.getInstance().getProfiler();
        profiler.push("littlemaidmodelloader:mm");
        livingEntity.getModel(IHasMultiModel.Layer.SKIN, IHasMultiModel.Part.HEAD)
                .filter(model -> model instanceof ModelMultiBase)
                .ifPresent(model -> syncCaps(livingEntity, (ModelMultiBase) model, partialTicks));
        for (IHasMultiModel.Part part : IHasMultiModel.Part.values()) {
            livingEntity.getModel(IHasMultiModel.Layer.INNER, part)
                    .filter(model -> model instanceof ModelMultiBase)
                    .ifPresent(model -> syncCaps(livingEntity, (ModelMultiBase) model, partialTicks));
            livingEntity.getModel(IHasMultiModel.Layer.OUTER, part)
                    .filter(model -> model instanceof ModelMultiBase)
                    .ifPresent(model -> syncCaps(livingEntity, (ModelMultiBase) model, partialTicks));
        }
        super.render(livingEntity, entityYaw, partialTicks, matrixStack, vertexConsumerProvider, light);
        profiler.pop();
    }

    public void syncCaps(LittleMaidEntity entity, ModelMultiBase model, float partialTicks) {
        float swingProgress = entity.getAttackAnim(partialTicks);
        float right = 0;
        float left = 0;
        if (entity.swingingArm == InteractionHand.MAIN_HAND) {
            if (entity.getMainArm() == HumanoidArm.RIGHT) {
                right = swingProgress;
            } else {
                left = swingProgress;
            }
        } else {
            if (entity.getMainArm() != HumanoidArm.RIGHT) {
                right = swingProgress;
            } else {
                left = swingProgress;
            }
        }
        model.setCapsValue(caps_onGround, right, left);
        model.setCapsValue(caps_isRiding, entity.isPassenger());
        model.setCapsValue(caps_isSneak, entity.isShiftKeyDown());
        model.setCapsValue(caps_isChild, entity.isBaby());
        model.setCapsValue(caps_heldItemLeft, 0F);
        model.setCapsValue(caps_heldItemRight, 0F);
        model.setCapsValue(caps_aimedBow, false);
        model.setCapsValue(caps_entityIdFactor, 0F);
        model.setCapsValue(caps_ticksExisted, entity.tickCount);

        model.setCapsValue(caps_aimedBow, entity.isAimingBow());
        model.setCapsValue(caps_isWait, TameableUtil.isWait(entity)
                && (LMRBMod.getConfig().client.enableWaitPoseOnMoving
                        || entity.getDeltaMovement().lengthSqr() < 0.01));
        model.setCapsValue(caps_isContract, entity.isContract());
        model.setCapsValue(caps_isBloodsuck, entity.isBloodSuck());
        model.setCapsValue(caps_isClock, entity.getMainHandItem().getItem() == Items.CLOCK
                || entity.getOffhandItem().getItem() == Items.CLOCK);
    }

    @Override
    public ResourceLocation getTextureLocation(LittleMaidEntity entity) {
        return entity.getTexture(IHasMultiModel.Layer.SKIN, IHasMultiModel.Part.HEAD, false)
                .orElse(NULL_TEXTURE);
    }

}
