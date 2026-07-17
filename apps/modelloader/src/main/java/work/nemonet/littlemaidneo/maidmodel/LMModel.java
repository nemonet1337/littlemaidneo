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
import work.nemonet.littlemaidneo.multimodel.IMultiModel;
import work.nemonet.littlemaidneo.multimodel.layer.MMMatrixStack;
import work.nemonet.littlemaidneo.multimodel.layer.MMPose;
import work.nemonet.littlemaidneo.multimodel.layer.MMRenderContext;

import net.minecraft.util.Mth;
import java.util.Random;

public abstract class LMModel<S extends MultiModelRenderState> extends EntityModel<S> implements IMultiModel {

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

    @Override
    public void setupTransform(IModelCaps caps, MMMatrixStack matrices, float animationProgress, float bodyYaw, float tickDelta) {
    }

    @Override
    public void animateModel(IModelCaps caps, float limbAngle, float limbDistance, float tickDelta) {
    }

    @Override
    public void setAngles(IModelCaps caps, float limbAngle, float limbDistance, float animationProgress, float headYaw, float headPitch) {
    }

    @Override
    public void render(MMRenderContext context) {
    }

    @Override
    public void adjustHandItem(MMMatrixStack matrices, boolean isLeft) {
    }

    @Override
    public int getTextureWidth() { return 64; }

    @Override
    public int getTextureHeight() { return 32; }

    @Override
    public float getInnerArmorSize() { return 0.1F; }

    @Override
    public float getOuterArmorSize() { return 0.5F; }

    @Override
    public float getWidth(IModelCaps caps, MMPose pose) { return 0.5F; }

    @Override
    public float getHeight(IModelCaps caps, MMPose pose) { return 1.35F; }

    @Override
    public float getEyeHeight(IModelCaps caps, MMPose pose) { return getHeight(caps, pose) * 0.85F; }

    @Override
    public float getyOffset(IModelCaps caps) { return getHeight(caps, MMPose.STANDING) * 0.9F; }

    @Override
    public float getMountedYOffset(IModelCaps caps) { return 0.35F; }

    @Override
    public float getLeashOffset(IModelCaps caps) { return 0.4F; }

    @Override
    public void showAllParts(IModelCaps caps) {
        skinRoot.visible = true;
        innerRoot.visible = true;
        outerRoot.visible = true;
    }

    @Override
    public int showArmorParts(int parts, int index) { return -1; }

    @Override
    public void renderItems(IModelCaps pEntityCaps) {
    }

    @Override
    public void renderFirstPersonHand(IModelCaps pEntityCaps) {
    }

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
}
