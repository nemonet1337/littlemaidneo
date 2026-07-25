package work.nemonet.littlemaidneo.maidmodel;

import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.PartPose;
import work.nemonet.littlemaidneo.client.renderer.MultiModelRenderState;

public class ModelLittleMaid_RX0 extends LMModel<MultiModelRenderState> {

    public ModelLittleMaid_RX0() { this(0.0F); }
    public ModelLittleMaid_RX0(float psize) { this(psize, 0.0F); }
    public ModelLittleMaid_RX0(float psize, float pyoffset) { this(psize, pyoffset, 128, 64); }

    public ModelLittleMaid_RX0(float psize, float pyoffset, int texW, int texH) {
        super(
            buildAndBake(pyoffset, texW, texH, CubeDeformation.NONE.extend(psize - 0.2F)),
            buildAndBake(pyoffset, texW, texH, CubeDeformation.NONE.extend(0.1F + psize - 0.2F)),
            buildAndBake(pyoffset, texW, texH, CubeDeformation.NONE.extend(0.5F + psize - 0.2F))
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
                .texOffs(32, 7).addBox(-3, 0, -1, 6, 3, 3, deform), PartPose.offset(0, 0, 0));
        bipedBody.addOrReplaceChild("bust", CubeListBuilder.create()
                .texOffs(32, 0).addBox(-3, -2.5F, 0, 6, 4, 3, CubeDeformation.NONE.extend(-0.04F)), PartPose.offset(0, 0, 0));

        var bipedTrunk = bipedTorso.addOrReplaceChild("biped_trunk", CubeListBuilder.create(), PartPose.offset(0, 0, 0));
        var bipedWaist = bipedTrunk.addOrReplaceChild("biped_waist", CubeListBuilder.create(), PartPose.offset(0, 0, 0));
        bipedWaist.addOrReplaceChild("waist_main", CubeListBuilder.create()
                .texOffs(24, 46).addBox(-2.5F, 0, -1.95F, 5, 7, 3, deform), PartPose.offset(0, 0, 0));
        bipedWaist.addOrReplaceChild("hip_right", CubeListBuilder.create()
                .texOffs(50, 0).addBox(0, -1.5F, -2, 3, 4, 4, deform), PartPose.offset(0, 0, 0));
        bipedWaist.addOrReplaceChild("hip_left", CubeListBuilder.create()
                .texOffs(50, 8).addBox(-3, -1.5F, -2, 3, 4, 4, deform), PartPose.offset(0, 0, 0));

        var bipedPelvic = bipedTrunk.addOrReplaceChild("biped_pelvic", CubeListBuilder.create(), PartPose.offset(0, 7, 0));

        var bipedHead = bipedNeck.addOrReplaceChild("biped_head", CubeListBuilder.create(), PartPose.offset(0, 0, 0));
        bipedHead.addOrReplaceChild("head_main", CubeListBuilder.create()
                .texOffs(0, 0).addBox(-4, -8, -4, 8, 8, 8, deform), PartPose.offset(0, 0, 0));
        bipedHead.addOrReplaceChild("forelock", CubeListBuilder.create(), PartPose.offset(0, 0, 0));
        bipedHead.addOrReplaceChild("ribbon", CubeListBuilder.create(), PartPose.offset(0, 0, 0));

        var bipedRightArm = bipedNeck.addOrReplaceChild("biped_right_arm", CubeListBuilder.create(), PartPose.offset(0, 5, -1));
        bipedRightArm.addOrReplaceChild("arm_main", CubeListBuilder.create()
                .texOffs(8, 47).addBox(-2, -0.5F, -1, 2, 7, 2, deform), PartPose.offset(0, 0, 0));
        bipedRightArm.addOrReplaceChild("forearm", CubeListBuilder.create()
                .texOffs(0, 40).addBox(-1, -1, -1, 2, 8, 2, CubeDeformation.NONE.extend(-0.05F)), PartPose.offset(0, 0, 0));

        var bipedLeftArm = bipedNeck.addOrReplaceChild("biped_left_arm", CubeListBuilder.create(), PartPose.offset(0, 5, -1));
        bipedLeftArm.addOrReplaceChild("arm_main", CubeListBuilder.create()
                .texOffs(48, 47).mirror().addBox(0, -0.5F, -1, 2, 7, 2, deform), PartPose.offset(0, 0, 0));
        bipedLeftArm.addOrReplaceChild("forearm", CubeListBuilder.create()
                .texOffs(56, 40).mirror().addBox(-1, -1, -1, 2, 8, 2, CubeDeformation.NONE.extend(-0.05F)), PartPose.offset(0, 0, 0));

        var bipedRightLeg = bipedPelvic.addOrReplaceChild("biped_right_leg", CubeListBuilder.create(), PartPose.offset(0, 0, 0));
        bipedRightLeg.addOrReplaceChild("leg_main", CubeListBuilder.create()
                .texOffs(0, 29).addBox(-3, 0, -2, 3, 7, 4, deform), PartPose.offset(0, 0, 0));
        bipedRightLeg.addOrReplaceChild("shin", CubeListBuilder.create()
                .texOffs(0, 16).addBox(-3, 0, -3, 3, 9, 4, CubeDeformation.NONE.extend(-0.2F)), PartPose.offset(0, 0, 0));

        var bipedLeftLeg = bipedPelvic.addOrReplaceChild("biped_left_leg", CubeListBuilder.create(), PartPose.offset(0, 0, 0));
        bipedLeftLeg.addOrReplaceChild("leg_main", CubeListBuilder.create()
                .texOffs(50, 29).mirror().addBox(0, 0, -2, 3, 7, 4, deform), PartPose.offset(0, 0, 0));
        bipedLeftLeg.addOrReplaceChild("shin", CubeListBuilder.create()
                .texOffs(50, 16).mirror().addBox(0, 0, -3, 3, 9, 4, CubeDeformation.NONE.extend(-0.2F)), PartPose.offset(0, 0, 0));

        var skirt = bipedPelvic.addOrReplaceChild("skirt", CubeListBuilder.create(), PartPose.offset(0, 0, 0));
        skirt.addOrReplaceChild("skirt_main", CubeListBuilder.create()
                .texOffs(20, 26).addBox(-3, 0, -3, 6, 8, 6, CubeDeformation.NONE.extend(0.05F)), PartPose.offset(0, 0, 0));
    }

}
