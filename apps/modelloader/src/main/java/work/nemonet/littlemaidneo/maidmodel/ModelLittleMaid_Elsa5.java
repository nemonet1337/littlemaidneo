package work.nemonet.littlemaidneo.maidmodel;

import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.PartPose;
import work.nemonet.littlemaidneo.client.renderer.MultiModelRenderState;

public class ModelLittleMaid_Elsa5 extends LMModel<MultiModelRenderState> {

    public ModelLittleMaid_Elsa5() { this(0.0F); }
    public ModelLittleMaid_Elsa5(float psize) { this(psize, 0.0F); }
    public ModelLittleMaid_Elsa5(float psize, float pyoffset) { this(psize, pyoffset, 64, 64); }

    public ModelLittleMaid_Elsa5(float psize, float pyoffset, int texW, int texH) {
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
        var mainFrame = root.addOrReplaceChild("main_frame", CubeListBuilder.create(), PartPose.offset(0, pyoffset + 10, 0));
        var bipedTorso = mainFrame.addOrReplaceChild("biped_torso", CubeListBuilder.create(), PartPose.offset(0, 0, 0));
        var bipedNeck = bipedTorso.addOrReplaceChild("biped_neck", CubeListBuilder.create(), PartPose.offset(0, 0, 0));
        var bipedBody = bipedTorso.addOrReplaceChild("biped_body", CubeListBuilder.create(), PartPose.offset(0, 0, 0));
        bipedBody.addOrReplaceChild("body_main", CubeListBuilder.create()
                .texOffs(0, 16).addBox(-3, -6, -2, 6, 9, 4, deform), PartPose.offset(0, 0, 0));
        bipedBody.addOrReplaceChild("body_extra", CubeListBuilder.create()
                .texOffs(20, 20).addBox(-3, -4.5F, -2.21F, 6, 2, 2, CubeDeformation.NONE.extend(0.2F)), PartPose.offset(0, 0, 0));

        var bipedPelvic = bipedTorso.addOrReplaceChild("biped_pelvic", CubeListBuilder.create(), PartPose.offset(0, 3, 0));

        var bipedHead = bipedNeck.addOrReplaceChild("biped_head", CubeListBuilder.create(), PartPose.offset(0, -6, 0));
        bipedHead.addOrReplaceChild("head_main", CubeListBuilder.create()
                .texOffs(0, 0).addBox(-4, -8, -4, 8, 8, 8, deform), PartPose.offset(0, 0, 0));
        bipedHead.addOrReplaceChild("head_front", CubeListBuilder.create()
                .texOffs(32, 0).addBox(-4, -8, -4, 8, 12, 8, CubeDeformation.NONE.extend(0.3F)), PartPose.offset(0, 0, 0));
        bipedHead.addOrReplaceChild("head_hair_back", CubeListBuilder.create()
                .texOffs(52, 20).addBox(-2, -7.2F, 4, 4, 4, 2, deform), PartPose.offset(0, 0, 0));
        bipedHead.addOrReplaceChild("head_hair_side_left", CubeListBuilder.create()
                .texOffs(36, 20).addBox(-5, -7, 0.2F, 1, 3, 3, deform), PartPose.offset(0, 0, 0));
        bipedHead.addOrReplaceChild("head_hair_side_right", CubeListBuilder.create()
                .texOffs(44, 20).mirror().addBox(4, -7, 0.2F, 1, 3, 3, deform), PartPose.offset(0, 0, 0));
        bipedHead.addOrReplaceChild("eye_right", CubeListBuilder.create()
                .texOffs(0, 0).addBox(-4, -8, -4.001F, 4, 8, 0, deform), PartPose.offset(0, 0, 0));
        bipedHead.addOrReplaceChild("eye_left", CubeListBuilder.create()
                .texOffs(4, 0).addBox(0, -8, -4.001F, 4, 8, 0, deform), PartPose.offset(0, 0, 0));
        bipedHead.addOrReplaceChild("head_mount", CubeListBuilder.create(), PartPose.offset(0, -4, 0));

        var bipedRightArm = bipedNeck.addOrReplaceChild("biped_right_arm", CubeListBuilder.create(), PartPose.offset(-3.5F, -5F, 0));
        bipedRightArm.addOrReplaceChild("arm_main", CubeListBuilder.create()
                .texOffs(20, 24).addBox(-1.5F, -0.5F, -0.5F, 2, 10, 2, deform), PartPose.offset(0, 0, 0));

        var bipedLeftArm = bipedNeck.addOrReplaceChild("biped_left_arm", CubeListBuilder.create(), PartPose.offset(3.5F, -5F, 0));
        bipedLeftArm.addOrReplaceChild("arm_main", CubeListBuilder.create()
                .texOffs(28, 24).mirror().addBox(-0.5F, -0.5F, -0.5F, 2, 10, 2, deform), PartPose.offset(0, 0, 0));

        var bipedRightLeg = bipedPelvic.addOrReplaceChild("biped_right_leg", CubeListBuilder.create(), PartPose.offset(-1.5F, 3, 0));
        bipedRightLeg.addOrReplaceChild("leg_main", CubeListBuilder.create()
                .texOffs(0, 29).addBox(-1.8F, 0, -2, 3, 11, 4, deform), PartPose.offset(0, 0, 0));

        var bipedLeftLeg = bipedPelvic.addOrReplaceChild("biped_left_leg", CubeListBuilder.create(), PartPose.offset(1.5F, 3, 0));
        bipedLeftLeg.addOrReplaceChild("leg_main", CubeListBuilder.create()
                .texOffs(0, 29).mirror().addBox(-1.2F, 0, -2, 3, 11, 4, deform), PartPose.offset(0, 0, 0));

        var skirt = bipedPelvic.addOrReplaceChild("skirt", CubeListBuilder.create(), PartPose.offset(0, -1, 0));
        skirt.addOrReplaceChild("skirt_main", CubeListBuilder.create()
                .texOffs(36, 40).addBox(-4, -2, -3, 8, 4, 6, deform), PartPose.offset(0, 0, 0));
        skirt.addOrReplaceChild("hem_skirt", CubeListBuilder.create()
                .texOffs(34, 50).addBox(-4, -1, -3.5F, 8, 7, 7, CubeDeformation.NONE.extend(0.3F)), PartPose.offset(0, 2, 0));
    }

    @Override
    protected void applyExtraPose(MultiModelRenderState state, ModelPart mainFrame, BipedParts parts) {
        // 旧 Elsa5 は基準頻度 0.16F（体力連動）
        applyBlink(state, parts, 0.16F);
    }
}
