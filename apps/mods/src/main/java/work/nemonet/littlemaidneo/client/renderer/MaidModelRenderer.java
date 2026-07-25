package work.nemonet.littlemaidneo.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
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

        state.skinModel = entity.getModel(IHasMultiModel.Layer.SKIN, IHasMultiModel.Part.HEAD).orElse(null);
        state.skinTexture = entity.getTexture(IHasMultiModel.Layer.SKIN, IHasMultiModel.Part.HEAD, false).orElse(null);
        state.skinTextureLight = entity.getTexture(IHasMultiModel.Layer.SKIN, IHasMultiModel.Part.HEAD, true).orElse(null);

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

            LMModel<?> innerLMModel = entity.getModel(IHasMultiModel.Layer.INNER, part).orElse(null);
            LMModel<?> outerLMModel = entity.getModel(IHasMultiModel.Layer.OUTER, part).orElse(null);
            state.armorStates[part.getIndex()] = new MultiModelRenderState.ArmorRenderState(
                    innerLMModel, outerLMModel,
                    state.innerTextures.getArmor(part).orElse(null),
                    state.innerTexturesLight.getArmor(part).orElse(null),
                    state.outerTextures.getArmor(part).orElse(null),
                    state.outerTexturesLight.getArmor(part).orElse(null),
                    entity.isArmorVisible(part),
                    entity.isArmorGlint(part)
            );
        }

        float swingProgress = entity.getAttackAnim(partialTick);
        if (entity.swingingArm == net.minecraft.world.InteractionHand.MAIN_HAND) {
            if (entity.getMainArm() == net.minecraft.world.entity.HumanoidArm.RIGHT) {
                state.swingProgressRight = swingProgress;
            } else {
                state.swingProgressLeft = swingProgress;
            }
        } else {
            if (entity.getMainArm() != net.minecraft.world.entity.HumanoidArm.RIGHT) {
                state.swingProgressRight = swingProgress;
            } else {
                state.swingProgressLeft = swingProgress;
            }
        }

        state.interestedAngle = entity.getInterestedAngle(partialTick);
        state.isBegging = entity.isBegging();
        state.isFreedomMode = entity.getMaidMode() == work.nemonet.littlemaidneo.entity.util.MaidMode.FREEDOM;
        state.isTracerMode = entity.getMaidMode() == work.nemonet.littlemaidneo.entity.util.MaidMode.TRACER;
        state.isPlayingSnow = entity.isPlayingSnow();
        state.isWorking = !entity.getActiveJobName().equals("none");
        state.isPlanter = false;
        state.isOverdrive = entity.getAccelerationTicks() > 0;
        state.activeJobName = entity.getActiveJobName();

        state.isWait = TameableUtil.isWait(entity)
                && (LMNConfig.get().client.enableWaitPoseOnMoving
                        || entity.getDeltaMovement().lengthSqr() < 0.01);
        state.isContract = entity.isContract();
        state.isBloodSuck = entity.isBloodSuck();
        state.isHoldingClock = entity.getMainHandItem().getItem() == net.minecraft.world.item.Items.CLOCK
                || entity.getOffhandItem().getItem() == net.minecraft.world.item.Items.CLOCK;
        state.isAimingBow = entity.isAimingBow();
        state.mainArm = entity.getMainArm();
        state.roll = entity.getFallFlyingTicks();
        state.isFallFlying = entity.isFallFlying();
        state.isSwimming = entity.isSwimming();
        state.isBlocking = entity.isBlocking();
        state.isLeashed = entity.isLeashed();

        float swimAmount = Mth.lerp(partialTick, entity.getSwimAmount(0F), entity.getSwimAmount(1F));
        state.leaningPitch = swimAmount;
    }

    @Override
    protected void setupRotations(MaidRenderState state, PoseStack matrices, float bodyYaw, float scale) {
        super.setupRotations(state, matrices, bodyYaw, scale);
    }

    @Override
    protected void scale(MaidRenderState state, PoseStack matrices) {
        matrices.scale(0.9375F, 0.9375F, 0.9375F);
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
}
