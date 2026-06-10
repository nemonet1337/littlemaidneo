package work.nemonet.littlemaidneo.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.resources.Identifier;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.api.distmarker.Dist;
import work.nemonet.littlemaidneo.common.LMNLib;
import work.nemonet.littlemaidneo.entity.compound.IHasMultiModel;
import work.nemonet.littlemaidneo.maidmodel.ModelMultiBase;
import work.nemonet.littlemaidneo.multimodel.layer.MMMatrixStack;

import static work.nemonet.littlemaidneo.maidmodel.IModelCaps.*;
public class MultiModelRenderer<T extends LivingEntity & IHasMultiModel>
        extends LivingEntityRenderer<T, MultiModelRenderState, MultiModel<MultiModelRenderState>> {

    private static final Identifier NULL_TEXTURE = Identifier.fromNamespaceAndPath(LMNLib.MODID, "null");

    public MultiModelRenderer(EntityRendererProvider.Context ctx) {
        super(ctx, new MultiModel<>(), 0.5F);
        this.addLayer(new MultiModelSkinLayer<>(this));
        this.addLayer(new MultiModelArmorLayer<>(this));
        this.addLayer(new MultiModelHeldItemLayer<>(this));
        this.addLayer(new MultiModelLightLayer<>(this));
    }

    @Override
    public MultiModelRenderState createRenderState() {
        return new MultiModelRenderState();
    }

    @Override
    public void extractRenderState(T entity, MultiModelRenderState state, float partialTick) {
        super.extractRenderState(entity, state, partialTick);
        state.multiModel = entity;
        state.entity = entity;
        state.mainArm = entity.getMainArm();
        state.mainHandItem = entity.getMainHandItem();
        state.offHandItem = entity.getOffhandItem();
        entity.getModel(IHasMultiModel.Layer.SKIN, IHasMultiModel.Part.HEAD)
                .filter(m -> m instanceof ModelMultiBase)
                .ifPresent(m -> syncCaps(entity, (ModelMultiBase) m, partialTick));
        for (IHasMultiModel.Part part : IHasMultiModel.Part.values()) {
            entity.getModel(IHasMultiModel.Layer.INNER, part)
                    .filter(m -> m instanceof ModelMultiBase)
                    .ifPresent(m -> syncCaps(entity, (ModelMultiBase) m, partialTick));
            entity.getModel(IHasMultiModel.Layer.OUTER, part)
                    .filter(m -> m instanceof ModelMultiBase)
                    .ifPresent(m -> syncCaps(entity, (ModelMultiBase) m, partialTick));
        }
    }

    @Override
    protected boolean shouldShowName(T entity, double distance) {
        return super.shouldShowName(entity, distance)
                && entity.hasCustomName()
                && entity == Minecraft.getInstance().crosshairPickEntity;
    }

    @Override
    protected void setupRotations(MultiModelRenderState state, PoseStack poseStack, float bodyYaw, float scale) {
        super.setupRotations(state, poseStack, bodyYaw, scale);
        if (state.multiModel == null) return;
        state.multiModel.getModel(IHasMultiModel.Layer.SKIN, IHasMultiModel.Part.HEAD)
                .ifPresent(model -> model.setupTransform(state.multiModel.getCaps(),
                        new MMMatrixStack(poseStack), state.ageInTicks, bodyYaw, state.partialTick));
    }

    @Override
    protected void scale(MultiModelRenderState state, PoseStack poseStack) {
        if (state.multiModel == null) return;
        state.multiModel.getModel(IHasMultiModel.Layer.SKIN, IHasMultiModel.Part.HEAD)
                .filter(m -> m instanceof ModelMultiBase)
                .map(m -> (float) ((ModelMultiBase) m).getCapsValue(caps_ScaleFactor))
                .ifPresent(s -> poseStack.scale(s, s, s));
    }

    @Override
    public void submit(MultiModelRenderState state, PoseStack poseStack, SubmitNodeCollector submitNodeCollector, CameraRenderState camera) {
        super.submit(state, poseStack, submitNodeCollector, camera);
    }

    @Override
    public Identifier getTextureLocation(MultiModelRenderState state) {
        if (state.multiModel == null) return NULL_TEXTURE;
        return state.multiModel.getTexture(IHasMultiModel.Layer.SKIN, IHasMultiModel.Part.HEAD, false)
                .orElse(NULL_TEXTURE);
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
}
