package work.nemonet.littlemaidneo.maidmodel;

import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.PartPose;
import work.nemonet.littlemaidneo.client.renderer.MultiModelRenderState;

public class ModelMulti_Slim64 extends LMModel<MultiModelRenderState> {

    public ModelMulti_Slim64() { this(0.0F); }
    public ModelMulti_Slim64(float psize) { this(psize, 0.0F); }
    public ModelMulti_Slim64(float psize, float pyoffset) { this(psize, pyoffset, 64, 64); }

    public ModelMulti_Slim64(float psize, float pyoffset, int texW, int texH) {
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
        var mainFrame = root.addOrReplaceChild("main_frame", CubeListBuilder.create(), PartPose.offset(0, pyoffset, 0));
        var bipedTorso = mainFrame.addOrReplaceChild("biped_torso", CubeListBuilder.create(), PartPose.offset(0, 0, 0));
        var bipedNeck = bipedTorso.addOrReplaceChild("biped_neck", CubeListBuilder.create(), PartPose.offset(0, 0, 0));
        var bipedBody = bipedTorso.addOrReplaceChild("biped_body", CubeListBuilder.create(), PartPose.offset(0, 0, 0));
        bipedBody.addOrReplaceChild("body_main", CubeListBuilder.create()
                .texOffs(16, 16).addBox(-4, 0, -2, 8, 12, 4, deform), PartPose.offset(0, 0, 0));
        bipedBody.addOrReplaceChild("biped_bodywear", CubeListBuilder.create()
                .texOffs(16, 32).addBox(-4, 0, -2, 8, 12, 4, CubeDeformation.NONE.extend(0.25F)), PartPose.offset(0, 0, 0));
        bipedBody.addOrReplaceChild("biped_cloak", CubeListBuilder.create()
                .texOffs(0, 0).addBox(-5, 0, -1, 10, 16, 1, deform), PartPose.offset(0, 0, 0));

        var bipedPelvic = bipedTorso.addOrReplaceChild("biped_pelvic", CubeListBuilder.create(), PartPose.offset(0, 12, 0));

        var bipedHead = bipedNeck.addOrReplaceChild("biped_head", CubeListBuilder.create(), PartPose.offset(0, 0, 0));
        bipedHead.addOrReplaceChild("head_main", CubeListBuilder.create()
                .texOffs(0, 0).addBox(-4, -8, -4, 8, 8, 8, deform), PartPose.offset(0, 0, 0));
        bipedHead.addOrReplaceChild("headwear", CubeListBuilder.create()
                .texOffs(32, 0).addBox(-4, -8, -4, 8, 8, 8, CubeDeformation.NONE.extend(0.5F)), PartPose.offset(0, 0, 0));
        bipedHead.addOrReplaceChild("ears", CubeListBuilder.create()
                .texOffs(24, 0).addBox(-3, -6, -1, 6, 6, 1, deform), PartPose.offset(0, 0, 0));
        bipedHead.addOrReplaceChild("eye_right", CubeListBuilder.create()
                .texOffs(0, 4).addBox(-4, -5, -4.001F, 4, 4, 0, deform), PartPose.offset(0, 0, 0));
        bipedHead.addOrReplaceChild("eye_left", CubeListBuilder.create()
                .texOffs(0, 0).addBox(0, -5, -4.001F, 4, 4, 0, deform), PartPose.offset(0, 0, 0));
        bipedHead.addOrReplaceChild("head_mount", CubeListBuilder.create(), PartPose.offset(0, -4, 0));
        bipedHead.addOrReplaceChild("head_top", CubeListBuilder.create(), PartPose.offset(0, -12, 0));

        var bipedRightArm = bipedNeck.addOrReplaceChild("biped_right_arm", CubeListBuilder.create(), PartPose.offset(-5, 2.5F, 0));
        bipedRightArm.addOrReplaceChild("arm_main", CubeListBuilder.create()
                .texOffs(40, 16).addBox(-2, -2, -2, 3, 12, 4, deform), PartPose.offset(0, 0, 0));
        bipedRightArm.addOrReplaceChild("arm_right_armwear", CubeListBuilder.create()
                .texOffs(40, 32).addBox(-2, -2, -2, 3, 12, 4, CubeDeformation.NONE.extend(0.25F)), PartPose.offset(0, 0, 0));
        bipedRightArm.addOrReplaceChild("arm_item", CubeListBuilder.create(), PartPose.offset(-1.5F, 7.2F, -1F));

        var bipedLeftArm = bipedNeck.addOrReplaceChild("biped_left_arm", CubeListBuilder.create(), PartPose.offset(5, 2.5F, 0));
        bipedLeftArm.addOrReplaceChild("arm_main", CubeListBuilder.create()
                .texOffs(32, 48).mirror().addBox(-1, -2, -2, 3, 12, 4, deform), PartPose.offset(0, 0, 0));
        bipedLeftArm.addOrReplaceChild("arm_left_armwear", CubeListBuilder.create()
                .texOffs(48, 48).mirror().addBox(-1, -2, -2, 3, 12, 4, CubeDeformation.NONE.extend(0.25F)), PartPose.offset(0, 0, 0));
        bipedLeftArm.addOrReplaceChild("arm_item", CubeListBuilder.create(), PartPose.offset(1.5F, 7.2F, -1F));

        var bipedRightLeg = bipedPelvic.addOrReplaceChild("biped_right_leg", CubeListBuilder.create(), PartPose.offset(-1.9F, 0, 0));
        bipedRightLeg.addOrReplaceChild("leg_main", CubeListBuilder.create()
                .texOffs(0, 16).addBox(-2, 0, -2, 4, 12, 4, deform), PartPose.offset(0, 0, 0));
        bipedRightLeg.addOrReplaceChild("leg_right_legwear", CubeListBuilder.create()
                .texOffs(0, 32).addBox(-2, 0, -2, 4, 12, 4, CubeDeformation.NONE.extend(0.25F)), PartPose.offset(0, 0, 0));

        var bipedLeftLeg = bipedPelvic.addOrReplaceChild("biped_left_leg", CubeListBuilder.create(), PartPose.offset(1.9F, 0, 0));
        bipedLeftLeg.addOrReplaceChild("leg_main", CubeListBuilder.create()
                .texOffs(16, 48).mirror().addBox(-2, 0, -2, 4, 12, 4, deform), PartPose.offset(0, 0, 0));
        bipedLeftLeg.addOrReplaceChild("leg_left_legwear", CubeListBuilder.create()
                .texOffs(0, 48).mirror().addBox(-2, 0, -2, 4, 12, 4, CubeDeformation.NONE.extend(0.25F)), PartPose.offset(0, 0, 0));
    }
}
