package work.nemonet.littlemaidneo.maidmodel;

import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.api.distmarker.Dist;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import work.nemonet.littlemaidneo.multimodel.IMultiModel;
import work.nemonet.littlemaidneo.multimodel.layer.MMMatrixStack;
import work.nemonet.littlemaidneo.multimodel.layer.MMPose;
import work.nemonet.littlemaidneo.multimodel.layer.MMRenderContext;

import java.util.HashMap;
import java.util.Map;

public abstract class ModelMultiBase extends ModelBase implements IModelCaps, IMultiModel {

    public final float[] heldItem = new float[]{0.0F, 0.0F};
    public boolean aimedBow;
    public boolean isSneak;
    public boolean isWait;

    public ModelRenderer mainFrame;
    public ModelRenderer HeadMount;
    public ModelRenderer HeadTop;
    public ModelRenderer[] Arms;
    public ModelRenderer[] HardPoint;

    public float entityIdFactor;
    public int entityTicksExisted;
    public float scaleFactor = 0.9375F;

    private final Map<String, Integer> fcapsmap = new HashMap<>() {{
        put("onGround", caps_onGround);
        put("isRiding", caps_isRiding);
        put("isSneak", caps_isSneak);
        put("isWait", caps_isWait);
        put("isChild", caps_isChild);
        put("heldItemLeft", caps_heldItemLeft);
        put("heldItemRight", caps_heldItemRight);
        put("aimedBow", caps_aimedBow);
        put("ScaleFactor", caps_ScaleFactor);
        put("entityIdFactor", caps_entityIdFactor);
        put("dominantArm", caps_dominantArm);
    }};

    private IModelCaps caps;
    private float limbSwing;
    private float limbSwingAmount;
    private float ageInTicks;
    private float netHeadYaw;
    private float headPitch;

    // 直前の setAngles 入力（同一インスタンスへの重複呼び出しで setRotationAngles を省略するため）。
    private IModelCaps lastAnglesCaps;
    private float lastLimbSwing;
    private float lastLimbSwingAmount;
    private float lastAgeInTicks;
    private float lastNetHeadYaw;
    private float lastHeadPitch;
    private boolean hasLastAngles;

    public ModelMultiBase() { this(0.0F); }

    public ModelMultiBase(float pSizeAdjust) { this(pSizeAdjust, 0.0F, 64, 32); }

    public ModelMultiBase(float pSizeAdjust, float pYOffset, int pTextureWidth, int pTextureHeight) {
        isSneak = false;
        aimedBow = false;
        textureWidth = pTextureWidth;
        textureHeight = pTextureHeight;

        if (FMLEnvironment.getDist() == Dist.CLIENT) {
            Arms = new ModelRenderer[2];
            HeadMount = new ModelRenderer(this, "HeadMount");
            HeadTop = new ModelRenderer(this, "HeadTop");
            initModel(pSizeAdjust, pYOffset);
        }
    }

    @Override
    public void setupTransform(IModelCaps caps, MMMatrixStack matrices, float animationProgress, float bodyYaw, float tickDelta) {
        float leaningPitch = ModelCapsHelper.getCapsValueFloat(caps, IModelCaps.caps_leaningPitch);
        float roll;
        float k;
        if (ModelCapsHelper.getCapsValueBoolean(caps, IModelCaps.caps_isFallFlying)) {
            roll = ModelCapsHelper.getCapsValueInt(caps, IModelCaps.caps_roll) + tickDelta;
            k = Mth.clamp(roll * roll / 100.0F, 0.0F, 1.0F);
            if (!ModelCapsHelper.getCapsValueBoolean(caps, IModelCaps.caps_isUsingRiptide)) {
                matrices.rotateXDeg(k * (-90.0F - ModelCapsHelper.getCapsValueFloat(caps, IModelCaps.caps_rotationPitch)));
            }
            Vec3 lookFor = getRotationVec(caps, tickDelta);
            Vec3 velocity = new Vec3(
                    ModelCapsHelper.getCapsValueDouble(caps, IModelCaps.caps_motionX),
                    ModelCapsHelper.getCapsValueDouble(caps, IModelCaps.caps_motionY),
                    ModelCapsHelper.getCapsValueDouble(caps, IModelCaps.caps_motionZ));
            double d = velocity.horizontalDistanceSqr();
            double e = lookFor.horizontalDistanceSqr();
            if (d > 0.0D && e > 0.0D) {
                double l = (velocity.x * lookFor.x + velocity.z * lookFor.z) / Math.sqrt(d * e);
                double m = velocity.x * lookFor.z - velocity.z * lookFor.x;
                matrices.rotateYRad((float) (Math.signum(m) * Math.acos(l)));
            }
        } else if (leaningPitch > 0.0F) {
            roll = ModelCapsHelper.getCapsValueBoolean(caps, IModelCaps.caps_isInWater)
                    ? -90.0F - ModelCapsHelper.getCapsValueFloat(caps, IModelCaps.caps_rotationPitch)
                    : -90.0F;
            k = Mth.lerp(leaningPitch, 0.0F, roll);
            matrices.rotateXDeg(k);
            if (ModelCapsHelper.getCapsValueBoolean(caps, IModelCaps.caps_isSwimming)) {
                matrices.translate(0.0D, -1.0D, 0.3D);
            }
        }
    }

    private Vec3 getRotationVec(IModelCaps caps, float tickDelta) {
        float yaw = ModelCapsHelper.getCapsValueFloat(caps, IModelCaps.caps_rotationYaw);
        float prevYaw = ModelCapsHelper.getCapsValueFloat(caps, IModelCaps.caps_prevRotationYaw);
        float pitch = ModelCapsHelper.getCapsValueFloat(caps, IModelCaps.caps_rotationPitch);
        float prevPitch = ModelCapsHelper.getCapsValueFloat(caps, IModelCaps.caps_prevRotationPitch);
        return getRotationVector(Mth.lerp(tickDelta, prevPitch, pitch), Mth.lerp(tickDelta, prevYaw, yaw));
    }

    private Vec3 getRotationVector(float pitch, float yaw) {
        float f = pitch * 0.017453292F;
        float g = -yaw * 0.017453292F;
        float h = Mth.cos(g);
        float i = Mth.sin(g);
        float j = Mth.cos(f);
        float k = Mth.sin(f);
        return new Vec3(i * j, -k, h * j);
    }

    @Override
    public void animateModel(IModelCaps caps, float limbAngle, float limbDistance, float tickDelta) {
        setLivingAnimations(caps, limbAngle, limbDistance, tickDelta);
    }

    @Override
    public void setAngles(IModelCaps caps, float limbAngle, float limbDistance, float animationProgress, float headYaw, float headPitch) {
        this.caps = caps;
        this.limbSwing = limbAngle;
        this.limbSwingAmount = limbDistance;
        this.ageInTicks = animationProgress;
        this.netHeadYaw = headYaw;
        this.headPitch = headPitch;
        // base body と発光レイヤーは同一 SKIN モデルへ同一入力で setAngles を二重に呼ぶ。
        // 遅延描画は最終状態のみ読むため、直前入力と一致するなら重い setRotationAngles を省略する
        // （setRotationAngles は入力からの角度設定＝冪等という LMM 規約が前提）。
        if (hasLastAngles
                && lastAnglesCaps == caps
                && lastLimbSwing == limbAngle
                && lastLimbSwingAmount == limbDistance
                && lastAgeInTicks == animationProgress
                && lastNetHeadYaw == headYaw
                && lastHeadPitch == headPitch) {
            return;
        }
        lastAnglesCaps = caps;
        lastLimbSwing = limbAngle;
        lastLimbSwingAmount = limbDistance;
        lastAgeInTicks = animationProgress;
        lastNetHeadYaw = headYaw;
        lastHeadPitch = headPitch;
        hasLastAngles = true;
        setRotationAngles(limbAngle, limbDistance, animationProgress, headYaw, headPitch, 0.0625F, caps);
    }

    @Override
    public void render(MMRenderContext context) {
        context.render(ModelRenderer::setParam);
        render(caps, limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch, 0.0625F, true);
    }

    @Override
    public void adjustHandItem(MMMatrixStack matrices, boolean isLeft) {
        ModelRenderer.poseStack = matrices.getVanillaMatrixStack();
        Arms[isLeft ? 1 : 0].postRender(0.0625F);
    }

    @Override
    public int getTextureWidth() { return this.textureWidth; }

    @Override
    public int getTextureHeight() { return this.textureHeight; }

    @Override
    public float getInnerArmorSize() { return getArmorModelsSize()[0]; }

    @Override
    public float getOuterArmorSize() { return getArmorModelsSize()[1]; }

    public float getEyeHeight(IModelCaps caps) { return this.getEyeHeight(caps, MMPose.STANDING); }

    @Override
    public float getEyeHeight(IModelCaps caps, MMPose pose) { return getHeight(caps, pose) * 0.85F; }

    public abstract void initModel(float psize, float pyoffset);

    public String getUsingTexture() { return null; }

    @Deprecated
    public abstract float getHeight();

    public float getHeight(IModelCaps pEntityCaps) { return getHeight(); }

    @Override
    public float getHeight(IModelCaps pEntityCaps, MMPose pose) {
        if (pose == MMPose.FALL_FLYING || pose == MMPose.SWIMMING || pose == MMPose.SPIN_ATTACK) {
            return Math.min(getHeight(pEntityCaps), getWidth(pEntityCaps, pose));
        } else if (pose == MMPose.SLEEPING || pose == MMPose.DYING) {
            return 0.2f;
        } else if (pose == MMPose.CROUCHING) {
            return Math.max(0.2f, getHeight(pEntityCaps) - 0.3f);
        }
        return getHeight(pEntityCaps);
    }

    @Deprecated
    public abstract float getWidth();

    public float getWidth(IModelCaps pEntityCaps) { return getWidth(); }

    @Override
    public float getWidth(IModelCaps pEntityCaps, MMPose pose) {
        if (pose == MMPose.SLEEPING || pose == MMPose.DYING) return 0.2f;
        return getWidth();
    }

    @Deprecated
    public abstract float getyOffset();

    @Override
    public float getyOffset(IModelCaps pEntityCaps) { return getyOffset(); }

    @Deprecated
    public abstract float getMountedYOffset();

    @Override
    public float getMountedYOffset(IModelCaps pEntityCaps) { return getMountedYOffset(); }

    @Override
    public float getLeashOffset(IModelCaps pEntityCaps) { return 0.4F; }

    @Deprecated
    public boolean isItemHolder() { return false; }

    public boolean isItemHolder(IModelCaps pEntityCaps) { return isItemHolder(); }

    public void showAllParts() {}

    @Override
    public void showAllParts(IModelCaps pEntityCaps) { showAllParts(); }

    @Override
    public int showArmorParts(int parts, int index) { return -1; }

    @Override
    public abstract void renderItems(IModelCaps pEntityCaps);

    @Override
    public abstract void renderFirstPersonHand(IModelCaps pEntityCaps);

    @Override
    public Map<String, Integer> getModelCaps() { return fcapsmap; }

    @Override
    public Object getCapsValue(int pIndex, Object... pArg) {
        return switch (pIndex) {
            case caps_onGround -> onGrounds;
            case caps_isRiding -> isRiding;
            case caps_isSneak -> isSneak;
            case caps_isWait -> isWait;
            case caps_isChild -> isChild;
            case caps_heldItemLeft -> heldItem[1];
            case caps_heldItemRight -> heldItem[0];
            case caps_aimedBow -> aimedBow;
            case caps_entityIdFactor -> entityIdFactor;
            case caps_ticksExisted -> entityTicksExisted;
            case caps_ScaleFactor -> scaleFactor;
            case caps_dominantArm -> dominantArm;
            case caps_motionSitting -> motionSitting;
            default -> null;
        };
    }

    @Override
    public boolean setCapsValue(int pIndex, Object... pArg) {
        switch (pIndex) {
            case caps_onGround -> {
                for (int li = 0; li < onGrounds.length && li < pArg.length; li++) {
                    onGrounds[li] = (Float) pArg[li];
                }
                return true;
            }
            case caps_isRiding -> { isRiding = (Boolean) pArg[0]; return true; }
            case caps_isSneak -> { isSneak = (Boolean) pArg[0]; return true; }
            case caps_isWait -> { isWait = (Boolean) pArg[0]; return true; }
            case caps_isChild -> { isChild = (Boolean) pArg[0]; return true; }
            case caps_heldItemLeft -> {
                heldItem[1] = pArg[0] instanceof Float ? (Float) pArg[0] : 0.0F; return true;
            }
            case caps_heldItemRight -> {
                heldItem[0] = pArg[0] instanceof Float ? (Float) pArg[0] : 0.0F; return true;
            }
            case caps_aimedBow -> { aimedBow = (Boolean) pArg[0]; return true; }
            case caps_entityIdFactor -> { entityIdFactor = (Float) pArg[0]; return true; }
            case caps_ticksExisted -> { entityTicksExisted = (Integer) pArg[0]; return true; }
            case caps_ScaleFactor -> { scaleFactor = (Float) pArg[0]; return true; }
            case caps_dominantArm -> { dominantArm = (Integer) pArg[0]; return true; }
            case caps_motionSitting -> { motionSitting = (Boolean) pArg[0]; return true; }
        }
        return false;
    }

    public static float mh_sqrt_float(float f) { return Mth.sqrt(f); }
    public static float mh_sqrt_double(double d) { return Mth.sqrt((float) d); }
    public static int mh_floor_float(float f) { return Mth.floor(f); }
    public static int mh_floor_double(double d) { return Mth.floor(d); }
    public static long mh_floor_double_long(double d) { return Mth.floor(d); }
}
