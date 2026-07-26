package work.nemonet.littlemaidneo.maidmodel;

import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.PartPose;
import work.nemonet.littlemaidneo.client.renderer.MultiModelRenderState;

/**
 * 旧 GLCompat 版 ModelLittleMaid_Beverly7（背の高い b16 系モデル）の忠実移植。
 * 二節腕・太もも付き二節脚・多段スカート裾・ポニーテールを含む。
 * ジオメトリの出典: git 4ce5eb5~1 の initModel / setLivingAnimations の静的ポーズ。
 */
public class ModelLittleMaid_Beverly7 extends LMModel<MultiModelRenderState> {

    public ModelLittleMaid_Beverly7() { this(0.0F); }
    public ModelLittleMaid_Beverly7(float psize) { this(psize, 0.0F); }
    public ModelLittleMaid_Beverly7(float psize, float pyoffset) { this(psize, pyoffset, 128, 64); }

    public ModelLittleMaid_Beverly7(float psize, float pyoffset, int texW, int texH) {
        super(
            buildAndBake(pyoffset, texW, texH, CubeDeformation.NONE.extend(psize)),
            buildAndBake(pyoffset, texW, texH, CubeDeformation.NONE.extend(0.1F + psize)),
            buildAndBake(pyoffset, texW, texH, CubeDeformation.NONE.extend(0.5F + psize))
        );
        // まばたきオーバーレイは旧実装では通常非表示（まばたきの瞬間のみ表示）
        setHeadPartVisible("eye_right", false);
        setHeadPartVisible("eye_left", false);
    }

    private static ModelPart buildAndBake(float pyoffset, int texW, int texH, CubeDeformation deform) {
        MeshDefinition mesh = new MeshDefinition();
        buildMesh(mesh, deform, pyoffset);
        return LMModel.bake(mesh, texW, texH);
    }

    @Override
    protected void buildMesh(MeshDefinition mesh, CubeDeformation deform) {
        buildMesh(mesh, deform, 0.0F);
    }

    private static void buildMesh(MeshDefinition mesh, CubeDeformation deform, float pyoffset) {
        var root = mesh.getRoot();
        // 旧 offsetY = pyoffset + 5（背の高いモデルのため標準の 8 ではない）
        var mainFrame = root.addOrReplaceChild("main_frame", CubeListBuilder.create(), PartPose.offset(0, pyoffset + 5, 0));
        var bipedTorso = mainFrame.addOrReplaceChild("biped_torso", CubeListBuilder.create(), PartPose.offset(0, 0, 0));
        var bipedNeck = bipedTorso.addOrReplaceChild("biped_neck", CubeListBuilder.create(), PartPose.offset(0, 0, 0));

        var bipedBody = bipedTorso.addOrReplaceChild("biped_body", CubeListBuilder.create()
                .texOffs(20, 26).addBox(-3, -8.5F, -2.1F, 6, 9, 4, deform)
                .texOffs(24, 16).addBox(-1, -9.8F, -1, 2, 2, 2, deform.extend(0.5F)),
                PartPose.offsetAndRotation(0, 0, 0, -0.1F, 0, 0));
        bipedBody.addOrReplaceChild("breast_right", CubeListBuilder.create()
                .texOffs(20, 20).addBox(-3, 0, -3, 3, 3, 3, deform.extend(0.1F)),
                PartPose.offsetAndRotation(-0.5F, -7.2F, -2.1F, 0.785F, 0, -0.15F));
        bipedBody.addOrReplaceChild("breast_left", CubeListBuilder.create()
                .texOffs(32, 20).mirror().addBox(0, 0, -3, 3, 3, 3, deform.extend(0.1F)),
                PartPose.offsetAndRotation(0.5F, -7.2F, -2.1F, 0.785F, 0, 0.15F));
        bipedBody.addOrReplaceChild("hip_body", CubeListBuilder.create()
                .texOffs(18, 39).addBox(-4, 0, -2.4F, 8, 4, 5, deform.extend(-0.2F)),
                PartPose.offsetAndRotation(0, 0, 0, 0.2F, 0, 0));

        // 顔本体のみ。髪は眼より後に描画する子パーツへ分離（眼が前髪を貫通するのを防ぐ）
        var bipedHead = bipedNeck.addOrReplaceChild("biped_head", CubeListBuilder.create()
                .texOffs(0, 0).addBox(-4, -8, -4, 8, 8, 8, deform),
                PartPose.offset(0, -9, 0));
        // まばたきオーバーレイ（通常非表示）。片面プレートで隣接 UV の裏抜けを防ぐ
        bipedHead.addOrReplaceChild("eye_right", CubeListBuilder.create()
                .texOffs(0, 0).addBox(-4, -8, -4.01F, 4, 8, 0, PLATE_FACE), PartPose.offset(0, 0, 0));
        bipedHead.addOrReplaceChild("eye_left", CubeListBuilder.create()
                .texOffs(4, 0).addBox(0, -8, -4.01F, 4, 8, 0, PLATE_FACE), PartPose.offset(0, 0, 0));
        // 髪・横髪は眼の後に描画（カットアウト半透明の前髪が眼より手前に来る）
        bipedHead.addOrReplaceChild("head_hair", CubeListBuilder.create()
                .texOffs(32, 0).addBox(-4, -8, -4, 8, 12, 8, deform.extend(0.3F))
                .texOffs(72, 0).addBox(-2, -7.2F, 4, 4, 4, 2, deform)
                .texOffs(56, 0).addBox(-5, -7, 0.2F, 1, 3, 3, deform)
                .mirror()
                .texOffs(64, 0).addBox(4, -7, 0.2F, 1, 3, 3, deform),
                PartPose.offset(0, 0, 0));
        bipedHead.addOrReplaceChild("ponytail", CubeListBuilder.create()
                .texOffs(76, 6).addBox(-1.5F, -1.5F, -1, 3, 9, 3, deform),
                PartPose.offsetAndRotation(0, -5.2F, 5, 0.05F, 0, 0));
        bipedHead.addOrReplaceChild("bunch_right", CubeListBuilder.create()
                .texOffs(64, 6).addBox(-1, -1.3F, -0.8F, 1, 9, 2, deform),
                PartPose.offsetAndRotation(-4.5F, -5.5F, 1.7F, 0, 0, 0.05F));
        bipedHead.addOrReplaceChild("bunch_left", CubeListBuilder.create()
                .texOffs(70, 6).mirror().addBox(0, -1.3F, -0.8F, 1, 9, 2, deform),
                PartPose.offsetAndRotation(4.5F, -5.5F, 1.7F, 0, 0, -0.05F));
        bipedHead.addOrReplaceChild("head_mount", CubeListBuilder.create(), PartPose.offset(0, -4, 0));

        // 二節腕: 肩（上腕）で振り、下腕は子として追従（旧 upperArm→bipedArm 構造）
        var bipedRightArm = bipedNeck.addOrReplaceChild("biped_right_arm", CubeListBuilder.create()
                .texOffs(0, 16).addBox(-1, -1, -1, 2, 6, 3, deform),
                PartPose.offset(-4, -7.5F, 0));
        var rightArmLower = bipedRightArm.addOrReplaceChild("arm_lower", CubeListBuilder.create()
                .texOffs(0, 25).addBox(-1, 0, -1.5F, 2, 8, 3, deform),
                PartPose.offset(0, 5, 0.5F));
        rightArmLower.addOrReplaceChild("arm_right", CubeListBuilder.create(), PartPose.offset(-0.5F, 7, 0));

        var bipedLeftArm = bipedNeck.addOrReplaceChild("biped_left_arm", CubeListBuilder.create()
                .texOffs(10, 16).mirror().addBox(-1, -1, -1, 2, 6, 3, deform),
                PartPose.offset(4, -7.5F, 0));
        var leftArmLower = bipedLeftArm.addOrReplaceChild("arm_lower", CubeListBuilder.create()
                .texOffs(10, 25).mirror().addBox(-1, 0, -1.5F, 2, 8, 3, deform),
                PartPose.offset(0, 5, 0.5F));
        leftArmLower.addOrReplaceChild("arm_left", CubeListBuilder.create(), PartPose.offset(0.5F, 7, 0));

        var bipedPelvic = bipedTorso.addOrReplaceChild("biped_pelvic", CubeListBuilder.create(), PartPose.offset(0, 4, 0));

        // 二節脚: 太もも（0,36）＋下脚（0,47）。静的スタンス角は旧 setLivingAnimations の値
        var bipedRightLeg = bipedPelvic.addOrReplaceChild("biped_right_leg", CubeListBuilder.create()
                .texOffs(0, 36).addBox(-1.5F, -1, -1.7F, 3, 7, 4, deform.extend(0.2F)),
                PartPose.offsetAndRotation(-2, 0, 0, -0.05F, 0.05F, -0.05F));
        bipedRightLeg.addOrReplaceChild("leg_lower", CubeListBuilder.create()
                .texOffs(0, 47).addBox(-1.6F, -1, -2, 3, 10, 4, deform),
                PartPose.offsetAndRotation(0, 6, 0, 0.05F, -0.1F, 0.02F));

        var bipedLeftLeg = bipedPelvic.addOrReplaceChild("biped_left_leg", CubeListBuilder.create()
                .texOffs(0, 36).mirror().addBox(-1.5F, -1, -1.7F, 3, 7, 4, deform.extend(0.2F)),
                PartPose.offsetAndRotation(2, 0, 0, -0.05F, -0.05F, 0.05F));
        bipedLeftLeg.addOrReplaceChild("leg_lower", CubeListBuilder.create()
                .texOffs(0, 47).mirror().addBox(-1.4F, -1, -2, 3, 10, 4, deform),
                PartPose.offsetAndRotation(0, 6, 0, 0.05F, 0.1F, -0.02F));

        // 多段スカート: ウエストバンド＋左右の裾2段（旧 Skirt/hemSkirtR1/R2/L1/L2）
        var skirt = bipedPelvic.addOrReplaceChild("skirt", CubeListBuilder.create()
                .texOffs(18, 48).addBox(-4, 0, -2, 8, 3, 5, deform.extend(0.6F)),
                PartPose.offset(0, -3, 0));
        var hemRight = skirt.addOrReplaceChild("hem_right", CubeListBuilder.create()
                .texOffs(69, 34).addBox(-3, -1, -5, 6, 7, 7, deform),
                PartPose.offsetAndRotation(-2, 3, 2, 0, 0, 0.05F));
        hemRight.addOrReplaceChild("hem_right2", CubeListBuilder.create()
                .texOffs(68, 48).addBox(-3.5F, -2, -4.5F, 7, 8, 8, deform.extend(0.2F)),
                PartPose.offsetAndRotation(0, 6, -1, 0, 0, -0.03F));
        var hemLeft = skirt.addOrReplaceChild("hem_left", CubeListBuilder.create()
                .texOffs(99, 34).mirror().addBox(-3, -1, -5, 6, 7, 7, deform),
                PartPose.offsetAndRotation(2, 3, 2, 0, 0, -0.05F));
        hemLeft.addOrReplaceChild("hem_left2", CubeListBuilder.create()
                .texOffs(98, 48).mirror().addBox(-3.5F, -2, -4.5F, 7, 8, 8, deform.extend(0.2F)),
                PartPose.offsetAndRotation(0, 6, -1, 0, 0, 0.03F));
    }

    @Override
    protected void applyExtraPose(MultiModelRenderState state, ModelPart mainFrame, BipedParts parts) {
        applyTwoSegmentWalkPose(state, parts);
        applyBlink(state, parts);
    }

    @Override
    protected float walkArmSwing(float pos, float speed, boolean left) {
        // 旧 setRotationAngles: upperArm.xRot -=/+= sin(f * 0.4444) * 0.7 * f1
        return (left ? 1F : -1F) * mh_sin(pos * 0.4444F) * 0.7F * speed;
    }

    @Override
    protected float walkLegSwing(float pos, float speed, boolean left) {
        // 旧 setRotationAngles: upperLeg.xRot +=/-= sin(f * 0.4444) * 0.9 * f1
        return (left ? -1F : 1F) * mh_sin(pos * 0.4444F) * 0.9F * speed;
    }
}
