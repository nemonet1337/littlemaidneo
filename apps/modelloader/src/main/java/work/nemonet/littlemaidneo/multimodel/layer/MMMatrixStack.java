package work.nemonet.littlemaidneo.multimodel.layer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import org.joml.Quaternionf;

public class MMMatrixStack {
    private final PoseStack poseStack;

    public MMMatrixStack(PoseStack poseStack) {
        this.poseStack = poseStack;
    }

    public PoseStack getVanillaMatrixStack() {
        return poseStack;
    }

    public void push() { poseStack.pushPose(); }
    public void pop() { poseStack.popPose(); }
    public void translate(double x, double y, double z) { poseStack.translate(x, y, z); }
    public void scale(float x, float y, float z) { poseStack.scale(x, y, z); }
    public void multiply(float x, float y, float z, float w) { poseStack.mulPose(new Quaternionf(x, y, z, w)); }
    public void rotateXRad(float rad) { poseStack.mulPose(Axis.XP.rotation(rad)); }
    public void rotateXDeg(float deg) { poseStack.mulPose(Axis.XP.rotationDegrees(deg)); }
    public void rotateYRad(float rad) { poseStack.mulPose(Axis.YP.rotation(rad)); }
    public void rotateYDeg(float deg) { poseStack.mulPose(Axis.YP.rotationDegrees(deg)); }
    public void rotateZRad(float rad) { poseStack.mulPose(Axis.ZP.rotation(rad)); }
    public void rotateZDeg(float deg) { poseStack.mulPose(Axis.ZP.rotationDegrees(deg)); }
}
