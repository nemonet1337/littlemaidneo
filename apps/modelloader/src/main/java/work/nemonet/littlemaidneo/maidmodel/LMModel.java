package work.nemonet.littlemaidneo.maidmodel;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.resources.Identifier;
import work.nemonet.littlemaidneo.client.renderer.MultiModelRenderState;

import net.minecraft.util.Mth;
import java.util.NoSuchElementException;
import java.util.Random;

public abstract class LMModel<S extends MultiModelRenderState> extends EntityModel<S> {

    /**
     * 旧 ModelPlate 相当の片面プレート用 visibleFaces。
     * 厚み0の addBox は裏面が隣接 UV 領域をサンプリングするため、NORTH 面のみ生成し
     * 向きは親/子パーツの yRot で合わせる（planeXY=0°, XYInv=180°, ZY=-90°, ZYInv=+90°）。
     */
    protected static final java.util.Set<net.minecraft.core.Direction> PLATE_FACE =
            java.util.Set.of(net.minecraft.core.Direction.NORTH);

    protected final ModelPart skinRoot;
    protected final ModelPart innerRoot;
    protected final ModelPart outerRoot;

    protected LMModel(ModelPart skinRoot, ModelPart innerRoot, ModelPart outerRoot) {
        super(skinRoot, RenderTypes::entityCutout);
        this.skinRoot = skinRoot;
        this.innerRoot = innerRoot;
        this.outerRoot = outerRoot;
    }

    protected abstract void buildMesh(MeshDefinition mesh, CubeDeformation deform);

    protected static ModelPart bake(MeshDefinition mesh, int texW, int texH) {
        return LayerDefinition.create(mesh, texW, texH).bakeRoot();
    }

    protected CubeDeformation innerDeform() { return CubeDeformation.NONE.extend(0.1F); }
    protected CubeDeformation outerDeform() { return CubeDeformation.NONE.extend(0.5F); }

    public ModelPart getSkinRoot() { return skinRoot; }
    public ModelPart getInnerRoot() { return innerRoot; }
    public ModelPart getOuterRoot() { return outerRoot; }

    public void setPartVisible(ModelPart part, boolean visible) {
        part.visible = visible;
    }

    /**
     * skin/inner/outer 全ルートへ共通ポーズを適用する。
     * アーマーレイヤーは inner/outer ルートを描画するため、skin だけでなく全ルートをポーズする必要がある。
     */
    @Override
    public void setupAnim(S state) {
        poseRoot(skinRoot, state);
        poseRoot(innerRoot, state);
        poseRoot(outerRoot, state);
    }

    private void poseRoot(ModelPart root, S state) {
        ModelPart mainFrame = getChildSafe(root, "main_frame");
        if (mainFrame == null) return;
        BipedParts p = new BipedParts(mainFrame);
        applyMaidPose(state, p.torso, p.neck, p.head, p.rightArm, p.leftArm,
                p.pelvic, p.rightLeg, p.leftLeg, p.skirt);
        applyExtraPose(state, mainFrame, p);
    }

    /** モデル固有の追加ポーズ用フック（二節肢の肘・膝スイング等）。標準ポーズ適用後に呼ばれる。 */
    protected void applyExtraPose(MultiModelRenderState state, ModelPart mainFrame, BipedParts parts) {
    }

    /**
     * 二節肢モデル用: 肘・膝の追従スイングとスカートの揺れ
     * （旧 Beverly7/Chloe2 setRotationAngles の歩行式）。
     * 肘・膝は max/min(sin,cos) により片側にのみ曲がる。walkArmSwing/walkLegSwing の
     * 旧固有波形（周波数 0.4444）オーバーライドとセットで使用し、位相を揃えること。
     */
    protected static void applyTwoSegmentWalkPose(MultiModelRenderState state, BipedParts p) {
        float f15 = mh_sin(state.walkAnimationPos * 0.4444F);
        float f16 = mh_cos(state.walkAnimationPos * 0.4444F);
        float fMax = Math.max(f15, f16);
        float fMin = Math.min(f15, f16);
        float amount = state.walkAnimationSpeed;
        ModelPart rightElbow = getChildSafe(p.rightArm, "arm_lower");
        ModelPart leftElbow = getChildSafe(p.leftArm, "arm_lower");
        if (rightElbow != null) { resetPart(rightElbow); rightElbow.xRot -= fMax * 0.7F * amount; }
        if (leftElbow != null) { resetPart(leftElbow); leftElbow.xRot += fMin * 0.7F * amount; }
        ModelPart rightKnee = getChildSafe(p.rightLeg, "leg_lower");
        ModelPart leftKnee = getChildSafe(p.leftLeg, "leg_lower");
        if (rightKnee != null) { resetPart(rightKnee); rightKnee.xRot += fMax * 0.9F * amount; }
        if (leftKnee != null) { resetPart(leftKnee); leftKnee.xRot -= fMin * 0.9F * amount; }
        // スカートの揺れ: 脚のスイングに前後追従し、裾は歩行量に応じて外側にフレア
        if (p.skirt != null) {
            p.skirt.xRot += f15 * 0.35F * amount;
            float flare = amount * 0.25F;
            ModelPart hemRight = getChildSafe(p.skirt, "hem_right");
            ModelPart hemLeft = getChildSafe(p.skirt, "hem_left");
            if (hemRight != null) { resetPart(hemRight); hemRight.zRot += flare; hemRight.xRot += f15 * 0.15F * amount; }
            if (hemLeft != null) { resetPart(hemLeft); hemLeft.zRot -= flare; hemLeft.xRot -= f15 * 0.15F * amount; }
        }
    }

    /**
     * 旧 setLivingAnimations のまばたきロジック（Beverly7/Chloe2/Elsa5 系）。
     * まばたきオーバーレイ（eye_right/eye_left）の可視を周期的に切り替える。
     * 体力が減るほどまばたき頻度が上がる（旧仕様）。baseFreq は Beverly7/Chloe2=0.20F, Elsa5=0.16F。
     */
    protected static void applyBlink(MultiModelRenderState state, BipedParts p) {
        applyBlink(state, p, 0.20F);
    }

    protected static void applyBlink(MultiModelRenderState state, BipedParts p, float baseFreq) {
        float blinkFreq = baseFreq;
        if (state.entity != null) blinkFreq += 1F - state.entity.getHealth() / 20F;
        float f3 = blinkTime(state) * 0.01F;
        boolean closed = mh_sin(f3 * 3F) + mh_sin(f3 * 17F) + mh_sin(f3 * 37F) + blinkFreq - 2.23309F < 0;
        setEyeOverlayVisible(p, closed);
    }

    /** SR2/Aug 系の低頻度まばたき（旧 SR2 setLivingAnimations 式・体力非依存）。 */
    protected static void applyBlinkSlow(MultiModelRenderState state, BipedParts p) {
        float t = blinkTime(state);
        boolean closed = mh_sin(t * 0.05F) + mh_sin(t * 0.13F) + mh_sin(t * 0.7F) + 2.55F < 0;
        setEyeOverlayVisible(p, closed);
    }

    /** 個体ごとにまばたきの位相をずらす（旧 entityIdFactor 相当）。 */
    private static float blinkTime(MultiModelRenderState state) {
        float idFactor = state.entity != null ? (state.entity.getId() % 89) * 3.1F : 0F;
        return state.ageInTicks + idFactor;
    }

    private static void setEyeOverlayVisible(BipedParts p, boolean visible) {
        ModelPart eyeRight = getChildSafe(p.head, "eye_right");
        ModelPart eyeLeft = getChildSafe(p.head, "eye_left");
        if (eyeRight != null) eyeRight.visible = visible;
        if (eyeLeft != null) eyeLeft.visible = visible;
    }

    public int getTextureWidth() { return 64; }

    public int getTextureHeight() { return 32; }

    public float getInnerArmorSize() { return 0.1F; }

    public float getOuterArmorSize() { return 0.5F; }

    public static float mh_sin(float f) { return Mth.sin(f); }
    public static float mh_cos(float f) { return Mth.cos(f); }
    public static float mh_sqrt(float f) { return Mth.sqrt(f); }
    public static float mh_sqrt(double d) { return Mth.sqrt((float) d); }
    public static int mh_floor(float f) { return Mth.floor(f); }
    public static int mh_floor(double d) { return Mth.floor(d); }
    public static long mh_floor_long(double d) { return Mth.floor(d); }
    public static float mh_abs(float f) { return Mth.abs(f); }
    public static double mh_abs_max(double d, double d1) { return Mth.absMax(d, d1); }
    public static int mh_bucketInt(int i, int j) { return Mth.floorDiv(i, j); }
    public static boolean mh_stringNullOrLengthZero(String s) { return s == null || s.isEmpty(); }
    public static int mh_getRandomIntegerInRange(Random random, int minimum, int maximum) {
        return minimum >= maximum ? minimum : random.nextInt(maximum - minimum + 1) + minimum;
    }

    /**
     * メイドさんモデル共通のポーズ適用ロジック。各モデルの setupAnim から呼び出す。
     * biped_* パーツは各モデルのパーツ階層から取得して渡す。存在しないパーツは null を渡してよい。
     */
    protected void applyMaidPose(MultiModelRenderState state,
            ModelPart torso, ModelPart neck, ModelPart head,
            ModelPart rightArm, ModelPart leftArm,
            ModelPart pelvic, ModelPart rightLeg, ModelPart leftLeg,
            ModelPart skirt) {
        float par1 = state.walkAnimationPos;
        float par2 = state.walkAnimationSpeed;
        float pTicksExisted = state.ageInTicks;
        float pHeadYaw = state.yRot;
        float pHeadPitch = state.xRot;

        float roll = Mth.clamp((state.roll + 0.0F) * (state.roll + 0.0F) / 100.0F, 0.0F, 1.0F);
        float leaningPitch = state.leaningPitch;

        if (state.isFallFlying) {
            par2 *= (1 - roll);
            pHeadPitch = -15f * roll + pHeadPitch * (1 - roll);
        } else if (leaningPitch > 0) {
            pHeadPitch = -15f * leaningPitch + pHeadPitch * (1 - leaningPitch);
        }

        setDefaultPose(torso, neck, head, rightArm, leftArm, pelvic, rightLeg, leftLeg, skirt, par1, par2);

        if (head != null) head.setRotation(
                (float) Math.toRadians(pHeadPitch), (float) Math.toRadians(pHeadYaw), head.zRot);

        if (head != null) head.zRot = state.interestedAngle;

        boolean isRiding = state.entity != null && state.entity.isPassenger();
        if (isRiding) {
            if (rightArm != null) { rightArm.xRot -= 0.6283185F; }
            if (leftArm != null) { leftArm.xRot -= 0.6283185F; }
            if (rightLeg != null) { rightLeg.xRot = -1.256637F; }
            if (leftLeg != null) { leftLeg.xRot = -1.256637F; }
            if (rightLeg != null) { rightLeg.yRot = 0.3141593F; }
            if (leftLeg != null) { leftLeg.yRot = -0.3141593F; }
        }

        float heldRight = state.mainArm == net.minecraft.world.entity.HumanoidArm.RIGHT
                && state.mainHandItem != null && !state.mainHandItem.isEmpty() ? 1F : 0F;
        float heldLeft = state.mainArm != net.minecraft.world.entity.HumanoidArm.RIGHT
                && state.mainHandItem != null && !state.mainHandItem.isEmpty() ? 1F : 0F;
        if (state.mainArm != net.minecraft.world.entity.HumanoidArm.RIGHT) {
            heldRight = state.offHandItem != null && !state.offHandItem.isEmpty() ? 1F : 0F;
            heldLeft = state.mainHandItem != null && !state.mainHandItem.isEmpty() ? 1F : 0F;
        }
        if (heldLeft != 0 && leftArm != null) {
            leftArm.xRot = leftArm.xRot * 0.5F;
            leftArm.xRot += (float) Math.toRadians(-18F * heldLeft);
        }
        if (heldRight != 0 && rightArm != null) {
            rightArm.xRot = rightArm.xRot * 0.5F;
            rightArm.xRot += (float) Math.toRadians(-18F * heldRight);
        }

        float onGround0 = state.swingProgressRight;
        float onGround1 = state.swingProgressLeft;
        if ((onGround0 > -9990F || onGround1 > -9990F) && !state.isAimingBow) {
            float f6, f7, f8;
            f6 = mh_sin(mh_sqrt(onGround0) * (float) Math.PI * 2.0F);
            f7 = mh_sin(mh_sqrt(onGround1) * (float) Math.PI * 2.0F);
            if (torso != null) { torso.yRot = (f6 - f7) * 0.2F; }
            if (skirt != null) { skirt.yRot = torso != null ? torso.yRot : 0; }
            if (rightArm != null) { rightArm.yRot = torso != null ? torso.yRot : 0; }
            if (leftArm != null) { leftArm.yRot = torso != null ? torso.yRot : 0; }
            if (pelvic != null) { pelvic.yRot = -(torso != null ? torso.yRot : 0); }
            if (head != null) { head.yRot = -(torso != null ? torso.yRot : 0); }
            if (onGround0 > 0F && rightArm != null) {
                f6 = 1.0F - onGround0; f6 *= f6; f6 *= f6; f6 = 1.0F - f6;
                f7 = mh_sin(f6 * (float) Math.PI);
                f8 = mh_sin(onGround0 * (float) Math.PI) * -(head != null ? head.xRot : 0.7F - 0.7F) * 0.75F;
                rightArm.xRot -= f7 * 1.2F + f8;
                rightArm.yRot += (torso != null ? torso.yRot : 0) * 2.0F;
                rightArm.zRot = mh_sin(onGround0 * 3.141593F) * -0.4F;
            } else if (rightArm != null) {
                // 旧 addRotateAngleX: 加算（代入にすると歩行スイングが消える）
                rightArm.xRot += torso != null ? torso.yRot : 0;
            }
            if (onGround1 > 0F && leftArm != null) {
                f6 = 1.0F - onGround1; f6 *= f6; f6 *= f6; f6 = 1.0F - f6;
                f7 = mh_sin(f6 * (float) Math.PI);
                f8 = mh_sin(onGround1 * (float) Math.PI) * -(head != null ? head.xRot : 0.7F - 0.7F) * 0.75F;
                leftArm.xRot -= (float) ((double) f7 * 1.2D + (double) f8);
                leftArm.yRot += (torso != null ? torso.yRot : 0) * 2.0F;
                leftArm.zRot = mh_sin(onGround1 * 3.141593F) * 0.4F;
            } else if (leftArm != null) {
                leftArm.xRot += torso != null ? torso.yRot : 0;
            }
        }

        boolean isCrouching = state.pose == net.minecraft.world.entity.Pose.CROUCHING;
        if (isCrouching) {
            if (torso != null) { torso.xRot += 0.5F; torso.y += 1.00F; }
            if (neck != null) { neck.xRot -= 0.5F; }
            if (rightArm != null) { rightArm.xRot += 0.2F; }
            if (leftArm != null) { leftArm.xRot += 0.2F; }
            if (pelvic != null) { pelvic.y += -0.5F; pelvic.z += -0.6F; pelvic.xRot += -0.5F; }
            if (head != null) { head.y += 1.0F; }
            if (skirt != null) { skirt.y += -0.25F; skirt.xRot += 0.2F; }
        }

        if (state.isWait) {
            float lx = mh_sin(pTicksExisted * 0.067F) * 0.05F - 0.7F;
            if (rightArm != null) rightArm.setRotation(lx, 0.0F, -0.4F);
            if (leftArm != null) leftArm.setRotation(lx, 0.0F, 0.4F);
        } else {
            if (state.isAimingBow) {
                float lonGround = state.mainArm == net.minecraft.world.entity.HumanoidArm.RIGHT ? onGround0 : onGround1;
                float f6 = mh_sin(lonGround * 3.141593F);
                float f7 = mh_sin((1.0F - (1.0F - lonGround) * (1.0F - lonGround)) * 3.141593F);
                float la = 0.1F - f6 * 0.6F;
                if (rightArm != null) rightArm.setRotation(-1.470796F, -la, 0.0F);
                if (leftArm != null) leftArm.setRotation(-1.470796F, la, 0.0F);
                la = head != null ? head.xRot : 0F;
                float lb = mh_sin(pTicksExisted * 0.067F) * 0.05F;
                float lc = f6 * 1.2F - f7 * 0.4F;
                if (rightArm != null) { rightArm.xRot += la + lb - lc; rightArm.yRot += (head != null ? head.yRot : 0); rightArm.zRot += mh_cos(pTicksExisted * 0.09F) * 0.05F + 0.05F; }
                if (leftArm != null) { leftArm.xRot += la - lb - lc; leftArm.yRot += (head != null ? head.yRot : 0); leftArm.zRot += -(mh_cos(pTicksExisted * 0.09F) * 0.05F + 0.05F); }
            } else {
                float la = mh_sin(pTicksExisted * 0.067F) * 0.05F;
                float lc = 0.5F + mh_cos(pTicksExisted * 0.09F) * 0.05F + 0.05F;
                if (rightArm != null) { rightArm.xRot += la; rightArm.zRot += lc; }
                if (leftArm != null) { leftArm.xRot += -la; leftArm.zRot += -lc; }
            }
        }
    }

    /**
     * 各パーツをベイク時の初期ポーズ（モデル固有の回転点）に戻してから歩行スイングのみ適用する。
     * 旧実装のように座標をハードコードすると、回転点の異なるモデル（Beverly7 等）が崩壊する。
     */
    private void setDefaultPose(ModelPart torso, ModelPart neck, ModelPart head,
            ModelPart rightArm, ModelPart leftArm, ModelPart pelvic,
            ModelPart rightLeg, ModelPart leftLeg, ModelPart skirt,
            float par1, float par2) {
        resetPart(torso);
        resetPart(neck);
        resetPart(pelvic);
        resetPart(head);
        resetPart(rightArm);
        resetPart(leftArm);
        resetPart(rightLeg);
        resetPart(leftLeg);
        resetPart(skirt);
        if (rightArm != null) rightArm.xRot = walkArmSwing(par1, par2, false);
        if (leftArm != null) leftArm.xRot = walkArmSwing(par1, par2, true);
        if (rightLeg != null) rightLeg.xRot = walkLegSwing(par1, par2, false);
        if (leftLeg != null) leftLeg.xRot = walkLegSwing(par1, par2, true);
    }

    /** 歩行時の腕の基本スイング角。二節肢モデルは旧固有波形にオーバーライドする。 */
    protected float walkArmSwing(float pos, float speed, boolean left) {
        return mh_cos(pos * 0.6662F + (left ? 0F : 3.141593F)) * 2.0F * speed * 0.5F;
    }

    /** 歩行時の脚の基本スイング角。二節肢モデルは旧固有波形にオーバーライドする。 */
    protected float walkLegSwing(float pos, float speed, boolean left) {
        return mh_cos(pos * 0.6662F + (left ? 3.141593F : 0F)) * 1.4F * speed;
    }

    protected static void resetPart(ModelPart part) {
        if (part != null) part.loadPose(part.getInitialPose());
    }

    /**
     * main_frame ツリーから標準メイドパーツを取得する。モデルごとの setupAnim 補助用。
     */
    protected static final class BipedParts {
        public final ModelPart torso, neck, head, rightArm, leftArm, pelvic, rightLeg, leftLeg, skirt;
        public BipedParts(ModelPart mainFrame) {
            this.torso = getChildSafe(mainFrame, "biped_torso");
            this.neck = getChildSafe(torso, "biped_neck");
            this.head = getChildSafe(neck, "biped_head");
            this.rightArm = getChildSafe(neck, "biped_right_arm");
            this.leftArm = getChildSafe(neck, "biped_left_arm");
            ModelPart pv = getChildSafe(torso, "biped_pelvic");
            if (pv == null) pv = getChildSafe(getChildSafe(torso, "biped_trunk"), "biped_pelvic");
            this.pelvic = pv;
            this.rightLeg = getChildSafe(pelvic, "biped_right_leg");
            this.leftLeg = getChildSafe(pelvic, "biped_left_leg");
            this.skirt = getChildSafe(pelvic, "skirt");
        }
    }

    /**
     * 遅延描画ラムダ用のポーズスナップショット。
     * モデルインスタンスは全メイドさんで共有されるため、submitCustomGeometry のラムダが
     * 実行される時点ではツリーのポーズが別エンティティのものに書き換わっている。
     * submit 時点の状態を保存し、描画直前に apply() で復元する。
     */
    public static final class PoseSnapshot {
        private final ModelPart[] parts;
        private final float[] data;
        private final boolean[] visible;

        private PoseSnapshot(java.util.List<ModelPart> list) {
            parts = list.toArray(new ModelPart[0]);
            data = new float[parts.length * 9];
            visible = new boolean[parts.length];
            for (int i = 0; i < parts.length; i++) {
                ModelPart p = parts[i];
                int o = i * 9;
                data[o] = p.x; data[o + 1] = p.y; data[o + 2] = p.z;
                data[o + 3] = p.xRot; data[o + 4] = p.yRot; data[o + 5] = p.zRot;
                data[o + 6] = p.xScale; data[o + 7] = p.yScale; data[o + 8] = p.zScale;
                visible[i] = p.visible;
            }
        }

        public void apply() {
            for (int i = 0; i < parts.length; i++) {
                ModelPart p = parts[i];
                int o = i * 9;
                p.x = data[o]; p.y = data[o + 1]; p.z = data[o + 2];
                p.xRot = data[o + 3]; p.yRot = data[o + 4]; p.zRot = data[o + 5];
                p.xScale = data[o + 6]; p.yScale = data[o + 7]; p.zScale = data[o + 8];
                p.visible = visible[i];
            }
        }
    }

    public static PoseSnapshot capturePose(ModelPart root) {
        return new PoseSnapshot(root.getAllParts());
    }

    /** 頭部配下の指定パーツの可視状態を skin/inner/outer 全ルートに対して設定する（まばたきオーバーレイ等）。 */
    protected void setHeadPartVisible(String name, boolean visible) {
        for (ModelPart root : new ModelPart[]{skinRoot, innerRoot, outerRoot}) {
            ModelPart head = getChildSafe(getChildSafe(getChildSafe(
                    getChildSafe(root, "main_frame"), "biped_torso"), "biped_neck"), "biped_head");
            ModelPart p = getChildSafe(head, name);
            if (p != null) p.visible = visible;
        }
    }

    /** ModelPart.getChild の NoSuchElementException を安全にスキップするヘルパー。 */
    public static ModelPart getChildSafe(ModelPart parent, String name) {
        if (parent == null) return null;
        try {
            return parent.getChild(name);
        } catch (NoSuchElementException e) {
            return null;
        }
    }
}
