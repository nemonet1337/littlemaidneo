package work.nemonet.littlemaidneo.maidmodel;

import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.util.Mth;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public abstract class ModelBase extends AbstractModelBase {

    public static final float PI = (float) Math.PI;

    public EntityRenderer<?, ?> render;

    public int textureWidth = 64;
    public int textureHeight = 32;
    public final float[] onGrounds = new float[]{0.0F, 0.0F};
    public int dominantArm = 0;
    public boolean isRiding = false;
    public boolean isChild = true;
    public final List<ModelRenderer> boxList = new ArrayList<>();

    public boolean motionSitting = false;

    public void render(IModelCaps pEntityCaps, float limbSwing, float limbSwingAmount,
                       float ticksExisted, float pheadYaw, float pheadPitch, float scale, boolean pIsRender) {
    }

    public void setRotationAngles(float limbSwing, float limbSwingAmount, float pTicksExisted,
                                  float pHeadYaw, float pHeadPitch, float scale, IModelCaps pEntityCaps) {
    }

    public void setLivingAnimations(IModelCaps pEntityCaps, float limbSwing, float limbSwingAmount, float pRenderPartialTicks) {
    }

    public ModelRenderer getRandomModelBox(Random par1Random) {
        int li = par1Random.nextInt(this.boxList.size());
        ModelRenderer lmr = this.boxList.get(li);
        for (int lj = 0; lj < boxList.size(); lj++) {
            if (!lmr.cubeList.isEmpty()) break;
            if (++li >= boxList.size()) li = 0;
            lmr = this.boxList.get(li);
        }
        return lmr;
    }

    @Deprecated
    public Object getTextureOffset(String par1Str) {
        return null;
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

    public static boolean mh_stringNullOrLengthZero(String s) {
        return s == null || s.isEmpty();
    }

    public static int mh_getRandomIntegerInRange(Random random, int minimum, int maximum) {
        return minimum >= maximum ? minimum : random.nextInt(maximum - minimum + 1) + minimum;
    }
}
