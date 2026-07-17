package work.nemonet.littlemaidneo.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.resources.Identifier;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import work.nemonet.littlemaidneo.LittleMaidNeo;
import work.nemonet.littlemaidneo.config.LMNConfig;
import work.nemonet.littlemaidneo.entity.LittleMaidEntity;
import work.nemonet.littlemaidneo.entity.compound.IHasMultiModel;
import work.nemonet.littlemaidneo.entity.util.TameableUtil;
import work.nemonet.littlemaidneo.maidmodel.LMModel;
public class MaidModelRenderer extends MobRenderer<LittleMaidEntity, MaidRenderState, LMMultiModel<MaidRenderState>> {

    private static final Identifier NULL_TEXTURE = Identifier.fromNamespaceAndPath(LittleMaidNeo.MODID, "null");

    public MaidModelRenderer(EntityRendererProvider.Context ctx) {
        super(ctx, new LMMultiModel<>(), 0.5F);
        this.addLayer(new LMSkinLayer<>(this));
        this.addLayer(new LMArmorLayer<>(this));
        this.addLayer(new LMHeldItemLayer<>(this));
        this.addLayer(new LMLightLayer<>(this));
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
        state.walkAnimationPos = entity.walkAnimation.position(partialTick);
        state.walkAnimationSpeed = entity.walkAnimation.speed(partialTick);

        state.caps = entity.getCaps();
        state.skinModel = entity.getModel(IHasMultiModel.Layer.SKIN, IHasMultiModel.Part.HEAD).orElse(null);
        state.skinTexture = entity.getTexture(IHasMultiModel.Layer.SKIN, IHasMultiModel.Part.HEAD, false).orElse(null);
        state.skinTextureLight = entity.getTexture(IHasMultiModel.Layer.SKIN, IHasMultiModel.Part.HEAD, true).orElse(null);

        state.skinModelNew = entity.getModel(IHasMultiModel.Layer.SKIN, IHasMultiModel.Part.HEAD)
                .filter(m -> m instanceof LMModel<?>)
                .map(m -> (LMModel<?>) m)
                .orElse(null);
        state.skinTextureNew = entity.getTexture(IHasMultiModel.Layer.SKIN, IHasMultiModel.Part.HEAD, false).orElse(null);
        state.skinTextureLightNew = entity.getTexture(IHasMultiModel.Layer.SKIN, IHasMultiModel.Part.HEAD, true).orElse(null);

        state.armorsVisible.clear();
        state.armorsGlint.clear();
        state.innerModels.clear();
        state.outerModels.clear();
        state.innerTextures.clear();
        state.innerTexturesLight.clear();
        state.outerTextures.clear();
        state.outerTexturesLight.clear();

        for (IHasMultiModel.Part part : IHasMultiModel.Part.values()) {
            state.armorsVisible.setArmor(entity.isArmorVisible(part), part);
            state.armorsGlint.setArmor(entity.isArmorGlint(part), part);

            state.innerModels.setArmor(entity.getModel(IHasMultiModel.Layer.INNER, part).orElse(null), part);
            state.outerModels.setArmor(entity.getModel(IHasMultiModel.Layer.OUTER, part).orElse(null), part);

            state.innerTextures.setArmor(entity.getTexture(IHasMultiModel.Layer.INNER, part, false).orElse(null), part);
            state.innerTexturesLight.setArmor(entity.getTexture(IHasMultiModel.Layer.INNER, part, true).orElse(null), part);

            state.outerTextures.setArmor(entity.getTexture(IHasMultiModel.Layer.OUTER, part, false).orElse(null), part);
            state.outerTexturesLight.setArmor(entity.getTexture(IHasMultiModel.Layer.OUTER, part, true).orElse(null), part);

            LMModel<?> innerLMModel = entity.getModel(IHasMultiModel.Layer.INNER, part)
                    .filter(m -> m instanceof LMModel)
                    .map(m -> (LMModel<?>) m)
                    .orElse(null);
            LMModel<?> outerLMModel = entity.getModel(IHasMultiModel.Layer.OUTER, part)
                    .filter(m -> m instanceof LMModel)
                    .map(m -> (LMModel<?>) m)
                    .orElse(null);
            state.armorStates[part.getIndex()] = new MultiModelRenderState.ArmorRenderState(
                    innerLMModel, outerLMModel,
                    state.innerTextures.getArmor(part).orElse(null),
                    state.innerTexturesLight.getArmor(part).orElse(null),
                    state.outerTextures.getArmor(part).orElse(null),
                    state.outerTexturesLight.getArmor(part).orElse(null),
                    entity.isArmorVisible(part),
                    entity.isArmorGlint(part)
            );

            if (false) {
                LittleMaidNeo.LOGGER.info("[ArmorDebug] part={} visible={} innerTex={} outerTex={} innerModel={} outerModel={}",
                        part, entity.isArmorVisible(part),
                        state.innerTextures.getArmor(part).orElse(null),
                        state.outerTextures.getArmor(part).orElse(null),
                        state.innerModels.getArmor(part).isPresent(),
                        state.outerModels.getArmor(part).isPresent());
            }

            entity.getModel(IHasMultiModel.Layer.INNER, part)
                    .filter(m -> m instanceof ModelMultiBase)
                    .ifPresent(m -> syncCaps(entity, (ModelMultiBase) m, partialTick));
            entity.getModel(IHasMultiModel.Layer.OUTER, part)
                    .filter(m -> m instanceof ModelMultiBase)
                    .ifPresent(m -> syncCaps(entity, (ModelMultiBase) m, partialTick));
        }

        entity.getModel(IHasMultiModel.Layer.SKIN, IHasMultiModel.Part.HEAD)
                .filter(m -> m instanceof ModelMultiBase)
                .ifPresent(m -> syncCaps(entity, (ModelMultiBase) m, partialTick));
    }

    @Override
    protected void setupRotations(MaidRenderState state, PoseStack matrices, float bodyYaw, float scale) {
        super.setupRotations(state, matrices, bodyYaw, scale);
        if (state.maidEntity == null) return;
        LittleMaidEntity entity = state.maidEntity;
        IMultiModel skinModel = entity.getModel(IHasMultiModel.Layer.SKIN, IHasMultiModel.Part.HEAD).orElse(null);
        if (skinModel instanceof LMModel<?> lmModel) {
            ModelPart mainFrame = lmModel.getSkinRoot().getChild("main_frame");
            if (mainFrame != null) {
                matrices.pushPose();
                mainFrame.translateAndRotate(matrices);
                matrices.popPose();
            }
        } else if (skinModel != null) {
            skinModel.setupTransform(entity.getCaps(),
                    new MMMatrixStack(matrices), state.ageInTicks, bodyYaw, state.partialTick);
        }
    }

    @Override
    protected void scale(MaidRenderState state, PoseStack matrices) {
        if (state.maidEntity == null) return;
        LittleMaidEntity entity = state.maidEntity;
        IMultiModel model = entity.getModel(IHasMultiModel.Layer.SKIN, IHasMultiModel.Part.HEAD).orElse(null);
        if (model instanceof LMModel<?> lmModel) {
            float s = 0.9375F;
            matrices.scale(s, s, s);
        } else if (model instanceof ModelMultiBase mmb) {
            mmb.setCapsValue(caps_ScaleFactor, 0.9375F);
            Object scaleObj = entity.getCaps().getCapsValue(caps_ScaleFactor);
            if (scaleObj instanceof Float scale) matrices.scale(scale, scale, scale);
        }
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

        ItemStack mainHand = entity.getMainHandItem();
        ItemStack offHand = entity.getOffhandItem();
        float mainHandVal = mainHand.isEmpty() ? 0F : 1.0F;
        float offHandVal = offHand.isEmpty() ? 0F : 1.0F;
        if (entity.getMainArm() == HumanoidArm.RIGHT) {
            model.setCapsValue(caps_heldItemRight, mainHandVal);
            model.setCapsValue(caps_heldItemLeft, offHandVal);
        } else {
            model.setCapsValue(caps_heldItemRight, offHandVal);
            model.setCapsValue(caps_heldItemLeft, mainHandVal);
        }

        model.setCapsValue(caps_aimedBow, false);
        model.setCapsValue(caps_entityIdFactor, 0F);
        model.setCapsValue(caps_ticksExisted, entity.tickCount);

        model.setCapsValue(caps_aimedBow, entity.isAimingBow());
        model.setCapsValue(caps_isWait, TameableUtil.isWait(entity)
                && (LMNConfig.get().client.enableWaitPoseOnMoving
                        || entity.getDeltaMovement().lengthSqr() < 0.01));
        model.setCapsValue(caps_isContract, entity.isContract());
        model.setCapsValue(caps_isBloodsuck, entity.isBloodSuck());
        model.setCapsValue(caps_isClock, entity.getMainHandItem().getItem() == Items.CLOCK
                || entity.getOffhandItem().getItem() == Items.CLOCK);
    }
}
