package work.nemonet.littlemaidneo.maidmodel;

import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.PartPose;
import work.nemonet.littlemaidneo.client.renderer.MultiModelRenderState;

public class ModelLittleMaid_Archetype extends LMModel<MultiModelRenderState> {

    public ModelLittleMaid_Archetype() { this(0.0F); }
    public ModelLittleMaid_Archetype(float psize) { this(psize, 0.0F); }
    public ModelLittleMaid_Archetype(float psize, float pyoffset) { this(psize, pyoffset, 64, 32); }

    public ModelLittleMaid_Archetype(float psize, float pyoffset, int texW, int texH) {
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
        bipedHead.addOrReplaceChild("headwear", CubeListBuilder.create()
                .texOffs(24, 0).addBox(-4, 0, 1, 8, 4, 3, deform), PartPose.offset(0, 0, 0));
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

        var skirt = bipedPelvic.addOrReplaceChild("skirt", CubeListBuilder.create(), PartPose.offset(0, 7, 0));
        skirt.addOrReplaceChild("skirt_main", CubeListBuilder.create()
                .texOffs(0, 16).addBox(-4, -2, -4, 8, 8, 8, deform), PartPose.offset(0, 0, 0));

        bipedHead.addOrReplaceChild("chignon_right", CubeListBuilder.create()
                .texOffs(24, 18).addBox(-5, -7, 0.2F, 1, 3, 3, deform), PartPose.offset(0, 0, 0));
        bipedHead.addOrReplaceChild("chignon_left", CubeListBuilder.create()
                .texOffs(24, 18).mirror().addBox(4, -7, 0.2F, 1, 3, 3, deform), PartPose.offset(0, 0, 0));
        bipedHead.addOrReplaceChild("chignon_back", CubeListBuilder.create()
                .texOffs(52, 10).addBox(-2, -7.2F, 4, 4, 4, 2, deform), PartPose.offset(0, 0, 0));
        bipedHead.addOrReplaceChild("tail", CubeListBuilder.create()
                .texOffs(46, 20).addBox(-1.5F, -6.8F, 4, 3, 9, 3, deform), PartPose.offset(0, 0, 0));
        bipedHead.addOrReplaceChild("side_tail_right", CubeListBuilder.create()
                .texOffs(58, 21).addBox(-5.5F, -6.8F, 0.9F, 1, 8, 2, deform), PartPose.offset(0, 0, 0));
        bipedHead.addOrReplaceChild("side_tail_left", CubeListBuilder.create()
                .texOffs(58, 21).mirror().addBox(4.5F, -6.8F, 0.9F, 1, 8, 2, deform), PartPose.offset(0, 0, 0));
    }

}
