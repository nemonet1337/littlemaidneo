package work.nemonet.littlemaidneo.maidmodel;

import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.PartPose;
import work.nemonet.littlemaidneo.client.renderer.MultiModelRenderState;

public class ModelLittleMaid_Aug extends LMModel<MultiModelRenderState> {

    public ModelLittleMaid_Aug() { this(0.0F); }
    public ModelLittleMaid_Aug(float psize) { this(psize, 0.0F); }
    public ModelLittleMaid_Aug(float psize, float pyoffset) { this(psize, pyoffset, 64, 32); }

    public ModelLittleMaid_Aug(float psize, float pyoffset, int texW, int texH) {
        super(
            buildAndBake(pyoffset, texW, texH, CubeDeformation.NONE.extend(psize)),
            buildAndBake(pyoffset, texW, texH, CubeDeformation.NONE.extend(0.1F + psize)),
            buildAndBake(pyoffset, texW, texH, CubeDeformation.NONE.extend(0.5F + psize))
        );
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
        var mainFrame = root.addOrReplaceChild("main_frame", CubeListBuilder.create(), PartPose.offset(0, pyoffset + 8, 0));
        var bipedTorso = mainFrame.addOrReplaceChild("biped_torso", CubeListBuilder.create(), PartPose.offset(0, 0, 0));
        var bipedNeck = bipedTorso.addOrReplaceChild("biped_neck", CubeListBuilder.create(), PartPose.offset(0, 0, 0));
        var bipedBody = bipedTorso.addOrReplaceChild("biped_body", CubeListBuilder.create(), PartPose.offset(0, 0, 0));
        bipedBody.addOrReplaceChild("body_main", CubeListBuilder.create()
                .texOffs(32, 8).addBox(-3, 0, -2, 6, 7, 4, deform), PartPose.offset(0, 0, 0));

        var bipedPelvic = bipedTorso.addOrReplaceChild("biped_pelvic", CubeListBuilder.create(), PartPose.offset(0, 7, 0));

        var bipedHead = bipedNeck.addOrReplaceChild("biped_head", CubeListBuilder.create(), PartPose.offset(0, 0, 0));
        bipedHead.addOrReplaceChild("head_main", CubeListBuilder.create()
                .texOffs(0, 0).addBox(-4, -8, -4, 8, 8, 8, deform), PartPose.offset(0, 0, 0));
        bipedHead.addOrReplaceChild("head_front", CubeListBuilder.create()
                .texOffs(24, 0).addBox(-4, 0, 1, 8, 4, 3, deform), PartPose.offset(0, 0, 0));
        bipedHead.addOrReplaceChild("head_cheek_left", CubeListBuilder.create()
                .texOffs(24, 18).addBox(-5, -7, 0.2F, 1, 3, 3, deform), PartPose.offset(0, 0, 0));
        bipedHead.addOrReplaceChild("head_cheek_right", CubeListBuilder.create()
                .texOffs(24, 18).mirror().addBox(4, -7, 0.2F, 1, 3, 3, deform), PartPose.offset(0, 0, 0));
        bipedHead.addOrReplaceChild("head_hair_back", CubeListBuilder.create()
                .texOffs(52, 10).addBox(-2, -7.2F, 4, 4, 4, 2, deform), PartPose.offset(0, 0, 0));
        bipedHead.addOrReplaceChild("head_hair_side", CubeListBuilder.create()
                .texOffs(46, 20).addBox(-1.5F, -6.8F, 4, 3, 9, 3, deform), PartPose.offset(0, 0, 0));
        bipedHead.addOrReplaceChild("head_hair_back_left", CubeListBuilder.create()
                .texOffs(58, 21).addBox(-5.5F, -6.8F, 0.9F, 1, 8, 2, deform), PartPose.offset(0, 0, 0));
        bipedHead.addOrReplaceChild("head_hair_back_right", CubeListBuilder.create()
                .texOffs(58, 21).mirror().addBox(4.5F, -6.8F, 0.9F, 1, 8, 2, deform), PartPose.offset(0, 0, 0));
        bipedHead.addOrReplaceChild("head_hair_top", CubeListBuilder.create()
                .texOffs(52, 15).addBox(-3.5F, -9.5F, 0.9F, 7, 3, 2, deform), PartPose.offset(0, 0, 0));
        bipedHead.addOrReplaceChild("side_tail_right", CubeListBuilder.create()
                .texOffs(46, 20).addBox(-1.5F, -0.5F, -1.0F, 2, 10, 2, deform), PartPose.offset(-5F, -7.8F, 1.9F));
        bipedHead.addOrReplaceChild("side_tail_left", CubeListBuilder.create()
                .texOffs(54, 20).addBox(0.5F, -0.5F, -1.0F, 2, 10, 2, deform), PartPose.offset(4F, -7.8F, 1.9F));
        bipedHead.addOrReplaceChild("shaggy_back", CubeListBuilder.create()
                .texOffs(24, 0).addBox(-5.0F, 0.0F, 0.0F, 10, 4, 4, deform), PartPose.offset(0.0F, -1.0F, 4.0F));
        bipedHead.addOrReplaceChild("shaggy_right", CubeListBuilder.create()
                .texOffs(34, 4).addBox(0.0F, 0.0F, -5.0F, 10, 4, 1, deform), PartPose.offset(4.0F, -1.0F, 0.0F));
        bipedHead.addOrReplaceChild("shaggy_left", CubeListBuilder.create()
                .texOffs(24, 4).addBox(0.0F, 0.0F, -5.0F, 10, 4, 5, deform), PartPose.offset(-4.0F, -1.0F, 0.0F));
        bipedHead.addOrReplaceChild("sensor1", CubeListBuilder.create()
                .texOffs(0, 0).addBox(-8.0F, -4.0F, 0.0F, 8, 4, 0, deform), PartPose.offset(0.0F, -8.0F, 0.0F));
        bipedHead.addOrReplaceChild("sensor2", CubeListBuilder.create()
                .texOffs(0, 4).addBox(0.0F, -4.0F, 0.0F, 8, 4, 0, deform), PartPose.offset(0.0F, -8.0F, 0.0F));
        bipedHead.addOrReplaceChild("sensor3", CubeListBuilder.create()
                .texOffs(44, 0).addBox(0.0F, -7.0F, -4.0F, 4, 8, 1, deform), PartPose.offset(0.0F, -8.0F, 0.0F));
        bipedHead.addOrReplaceChild("sensor4", CubeListBuilder.create()
                .texOffs(34, 0).addBox(0.0F, -4.0F, -10.0F, 10, 4, 1, deform), PartPose.offset(0.0F, -8.0F, 0.0F));
        bipedHead.addOrReplaceChild("eye_right", CubeListBuilder.create()
                .texOffs(32, 19).addBox(-4, -5, -4.001F, 4, 4, 0, deform), PartPose.offset(0, 0, 0));
        bipedHead.addOrReplaceChild("eye_left", CubeListBuilder.create()
                .texOffs(42, 19).addBox(0, -5, -4.001F, 4, 4, 0, deform), PartPose.offset(0, 0, 0));
        bipedHead.addOrReplaceChild("head_mount", CubeListBuilder.create(), PartPose.offset(0, -4, 0));
        bipedHead.addOrReplaceChild("head_top", CubeListBuilder.create(), PartPose.offset(0, -13, 0));

        var bipedRightArm = bipedNeck.addOrReplaceChild("biped_right_arm", CubeListBuilder.create(), PartPose.offset(-3, 1.5F, 0));
        bipedRightArm.addOrReplaceChild("arm_main", CubeListBuilder.create()
                .texOffs(48, 0).addBox(-2, -1, -1, 2, 8, 2, deform), PartPose.offset(0, 0, 0));
        bipedRightArm.addOrReplaceChild("arm_right", CubeListBuilder.create(), PartPose.offset(-1, 5, -1));

        var bipedLeftArm = bipedNeck.addOrReplaceChild("biped_left_arm", CubeListBuilder.create(), PartPose.offset(3, 1.5F, 0));
        bipedLeftArm.addOrReplaceChild("arm_main", CubeListBuilder.create()
                .texOffs(56, 0).mirror().addBox(0, -1, -1, 2, 8, 2, deform), PartPose.offset(0, 0, 0));
        bipedLeftArm.addOrReplaceChild("arm_left", CubeListBuilder.create(), PartPose.offset(1, 5, -1));

        var bipedRightLeg = bipedPelvic.addOrReplaceChild("biped_right_leg", CubeListBuilder.create(), PartPose.offset(-1, 0, 0));
        bipedRightLeg.addOrReplaceChild("leg_main", CubeListBuilder.create()
                .texOffs(32, 19).addBox(-2, 0, -2, 3, 9, 4, deform), PartPose.offset(0, 0, 0));

        var bipedLeftLeg = bipedPelvic.addOrReplaceChild("biped_left_leg", CubeListBuilder.create(), PartPose.offset(1, 0, 0));
        bipedLeftLeg.addOrReplaceChild("leg_main", CubeListBuilder.create()
                .texOffs(32, 19).mirror().addBox(-1, 0, -2, 3, 9, 4, deform), PartPose.offset(0, 0, 0));

        var skirt = bipedPelvic.addOrReplaceChild("skirt", CubeListBuilder.create(), PartPose.offset(0, 0, 0));
        skirt.addOrReplaceChild("skirt_main", CubeListBuilder.create()
                .texOffs(0, 16).addBox(-4, -2, -4, 8, 8, 8, deform), PartPose.offset(0, 0, 0));
    }
}
