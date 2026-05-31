package work.nemonet.littlemaidneo.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.resources.Identifier;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.item.Items;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import work.nemonet.littlemaidneo.LittleMaidNeo;
import work.nemonet.littlemaidneo.config.LMRBConfig;
import work.nemonet.littlemaidneo.entity.LittleMaidEntity;
import work.nemonet.littlemaidneo.entity.compound.IHasMultiModel;
import work.nemonet.littlemaidneo.entity.util.TameableUtil;
import work.nemonet.littlemaidneo.maidmodel.ModelMultiBase;
import work.nemonet.littlemaidneo.multimodel.layer.MMMatrixStack;

import static work.nemonet.littlemaidneo.maidmodel.IModelCaps.*;

@OnlyIn(Dist.CLIENT)
public class MaidModelRenderer extends MobRenderer<LittleMaidEntity, MaidRenderState, LMMultiModel<MaidRenderState>> {

    private static final Identifier NULL_TEXTURE = Identifier.fromNamespaceAndPath(LittleMaidNeo.MODID, "null");

    public MaidModelRenderer(EntityRendererProvider.Context ctx) {
        super(ctx, new LMMultiModel<>(), 0.5F);
        this.addLayer(new MultiModelSkinLayer<>(this));
        this.addLayer(new MultiModelArmorLayer<>(this));
        this.addLayer(new MultiModelHeldItemLayer<>(this));
        this.addLayer(new MultiModelLightLayer<>(this));
        this.addLayer(new LMHeadFeatureRenderer<>(this, ctx.getModelSet(), ctx.getPlayerSkinRenderCache()));
    }

    @Override
    public MaidRenderState createRenderState() {
        return new MaidRenderState();
    }

    @Override
    public void extractRenderState(LittleMaidEntity entity, MaidRenderState state, float partialTick) {
        super.extractRenderState(entity, state, partialTick);
        state.maidEntity = entity;
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
    protected void setupRotations(MaidRenderState state, PoseStack matrices, float bodyYaw, float scale) {
        super.setupRotations(state, matrices, bodyYaw, scale);
        if (state.maidEntity == null) return;
        state.maidEntity.getModel(IHasMultiModel.Layer.SKIN, IHasMultiModel.Part.HEAD)
                .ifPresent(model -> model.setupTransform(state.maidEntity.getCaps(),
                        new MMMatrixStack(matrices), state.ageInTicks, bodyYaw, state.partialTick));
    }

    @Override
    protected void scale(MaidRenderState state, PoseStack matrices) {
        if (state.maidEntity == null) return;
        state.maidEntity.getModel(IHasMultiModel.Layer.SKIN, IHasMultiModel.Part.HEAD)
                .filter(m -> m instanceof ModelMultiBase)
                .map(m -> (float) ((ModelMultiBase) m).getCapsValue(caps_ScaleFactor))
                .ifPresent(s -> matrices.scale(s, s, s));
    }

    @Override
    public void submit(MaidRenderState state, PoseStack poseStack, SubmitNodeCollector submitNodeCollector, CameraRenderState camera) {
        super.submit(state, poseStack, submitNodeCollector, camera);
    }

    @Override
    public Identifier getTextureLocation(MaidRenderState state) {
        if (state.maidEntity == null) return NULL_TEXTURE;
        return state.maidEntity.getTexture(IHasMultiModel.Layer.SKIN, IHasMultiModel.Part.HEAD, false)
                .orElse(NULL_TEXTURE);
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
                && (LMRBConfig.get().client.enableWaitPoseOnMoving
                        || entity.getDeltaMovement().lengthSqr() < 0.01));
        model.setCapsValue(caps_isContract, entity.isContract());
        model.setCapsValue(caps_isBloodsuck, entity.isBloodSuck());
        model.setCapsValue(caps_isClock, entity.getMainHandItem().getItem() == Items.CLOCK
                || entity.getOffhandItem().getItem() == Items.CLOCK);
    }
}
