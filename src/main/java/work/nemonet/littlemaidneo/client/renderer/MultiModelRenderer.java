package work.nemonet.littlemaidneo.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import work.nemonet.littlemaidneo.LittleMaidNeo;
import work.nemonet.littlemaidneo.entity.compound.IHasMultiModel;
import work.nemonet.littlemaidneo.maidmodel.ModelMultiBase;
import work.nemonet.littlemaidneo.multimodel.layer.MMMatrixStack;

import static work.nemonet.littlemaidneo.maidmodel.IModelCaps.*;

@OnlyIn(Dist.CLIENT)
public class MultiModelRenderer<T extends LivingEntity & IHasMultiModel> extends LivingEntityRenderer<T, MultiModel<T>> {
    private static final ResourceLocation NULL_TEXTURE = ResourceLocation.fromNamespaceAndPath(LittleMaidNeo.MODID, "null");

    public MultiModelRenderer(EntityRendererProvider.Context ctx) {
        super(ctx, new MultiModel<>(), 0.5F);
        this.addLayer(new MultiModelArmorLayer<>(this));
        this.addLayer(new MultiModelHeldItemLayer<>(this));
        this.addLayer(new MultiModelLightLayer<>(this));
    }

    @Override
    protected boolean shouldShowName(T entity) {
        return super.shouldShowName(entity) && (entity.hasCustomName()
                && entity == Minecraft.getInstance().crosshairPickEntity);
    }

    @Override
    protected void setupRotations(T entity, PoseStack poseStack, float ageInTicks, float bodyYaw, float partialTick, float scale) {
        super.setupRotations(entity, poseStack, ageInTicks, bodyYaw, partialTick, scale);
        entity.getModel(IHasMultiModel.Layer.SKIN, IHasMultiModel.Part.HEAD)
                .ifPresent(model -> model.setupTransform(entity.getCaps(),
                        new MMMatrixStack(poseStack), ageInTicks, bodyYaw, partialTick));
    }

    @Override
    protected void scale(T entity, PoseStack poseStack, float partialTick) {
        entity.getModel(IHasMultiModel.Layer.SKIN, IHasMultiModel.Part.HEAD)
                .filter(model -> model instanceof ModelMultiBase)
                .map(model -> (float) ((ModelMultiBase) model).getCapsValue(caps_ScaleFactor))
                .ifPresent(scale -> poseStack.scale(scale, scale, scale));
    }

    @Override
    public void render(T livingEntity, float entityYaw, float partialTicks, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight) {
        ProfilerFiller profiler = Minecraft.getInstance().getProfiler();
        profiler.push("littlemaidneo:mm");
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
        super.render(livingEntity, entityYaw, partialTicks, poseStack, bufferSource, packedLight);
        profiler.pop();
    }

    public void syncCaps(T entity, ModelMultiBase model, float partialTicks) {
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
        model.setCapsValue(caps_isSneak, entity.isCrouching());
        model.setCapsValue(caps_isChild, entity.isBaby());
        model.setCapsValue(caps_heldItemLeft, 0F);
        model.setCapsValue(caps_heldItemRight, 0F);
        model.setCapsValue(caps_aimedBow, false);
        model.setCapsValue(caps_entityIdFactor, 0F);
        model.setCapsValue(caps_ticksExisted, entity.tickCount);
    }

    @Override
    public ResourceLocation getTextureLocation(T entity) {
        return entity.getTexture(IHasMultiModel.Layer.SKIN, IHasMultiModel.Part.HEAD, false)
                .orElse(NULL_TEXTURE);
    }
}
