package work.nemonet.littlemaidneo.maidmodel;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.world.item.ItemStack;
import work.nemonet.littlemaidneo.maidmodel.compat.GLCompat;
import work.nemonet.littlemaidneo.multimodel.layer.MMMatrixStack;
import work.nemonet.littlemaidneo.multimodel.layer.MMVertexConsumer;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;

import java.lang.reflect.Constructor;
import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.List;

public class ModelRenderer {

    public float scale = 0.0625F;

    public static PoseStack poseStack;
    public static VertexConsumer buffer;
    public static int light;
    public static int overlay;
    public static float red;
    public static float green;
    public static float blue;
    public static float alpha;

    public static void setParam(MMMatrixStack matrixStack, MMVertexConsumer buf, int light, int overlay,
                                float red, float green, float blue, float alpha) {
        ModelRenderer.poseStack = matrixStack.getVanillaMatrixStack();
        ModelRenderer.buffer = buf.getVanillaVertexConsumer();
        ModelRenderer.light = light;
        ModelRenderer.overlay = overlay;
        GLCompat.glColor3f(red, green, blue);
        ModelRenderer.alpha = alpha;
    }

    public float textureWidth;
    public float textureHeight;
    private int textureOffsetX;
    private int textureOffsetY;
    public float rotationPointX;
    public float rotationPointY;
    public float rotationPointZ;
    public float rotateAngleX;
    public float rotateAngleY;
    public float rotateAngleZ;
    protected boolean compiled;
    protected final int displayList;
    public boolean mirror;
    public boolean showModel;
    public final boolean isHidden;
    public boolean isRendering;
    public final List<ModelBoxBase> cubeList;
    public List<ModelRenderer> childModels;
    public final String boxName;
    protected final ModelBase baseModel;
    public ModelRenderer pearent;
    public float offsetX;
    public float offsetY;
    public float offsetZ;
    public float scaleX;
    public float scaleY;
    public float scaleZ;

    public static final float radFactor = 180F / (float) Math.PI;
    public static final float degFactor = (float) Math.PI / 180F;

    public int rotatePriority;
    public static final int RotXYZ = 0;
    public static final int RotXZY = 1;
    public static final int RotYXZ = 2;
    public static final int RotYZX = 3;
    public static final int RotZXY = 4;
    public static final int RotZYX = 5;

    protected final ItemStack itemstack;

    public final boolean adjust;
    public final FloatBuffer matrix;
    public boolean isInvertX;

    // matrix の読み戻しは loadMatrix() を使う部品（Arms/HeadTop/HeadMount 等）でのみ必要。
    // 初回 loadMatrix 呼び出しで true になり、以後 renderObject が毎フレーム捕捉する。
    private boolean needsMatrixCapture;

    public ModelRenderer(ModelBase pModelBase, String pName) {
        textureWidth = 64.0F; textureHeight = 32.0F;
        compiled = false; displayList = 0;
        mirror = false; showModel = true; isHidden = false; isRendering = true;
        cubeList = new ArrayList<>();
        baseModel = pModelBase;
        pModelBase.boxList.add(this);
        boxName = pName;
        setTextureSize(pModelBase.textureWidth, pModelBase.textureHeight);
        rotatePriority = RotXYZ;
        itemstack = null;
        adjust = true;
        matrix = BufferUtils.createFloatBuffer(16);
        isInvertX = false;
        scaleX = 1.0F; scaleY = 1.0F; scaleZ = 1.0F;
        pearent = null;
    }

    public ModelRenderer(ModelBase pModelBase, int px, int py) {
        this(pModelBase, null);
        setTextureOffset(px, py);
    }

    public ModelRenderer(ModelBase pModelBase) {
        this(pModelBase, null);
    }

    public ModelRenderer(ModelBase pModelBase, int px, int py, float pScaleX, float pScaleY, float pScaleZ) {
        this(pModelBase, px, py);
        this.scaleX = pScaleX; this.scaleY = pScaleY; this.scaleZ = pScaleZ;
    }

    public ModelRenderer(ModelBase pModelBase, float pScaleX, float pScaleY, float pScaleZ) {
        this(pModelBase);
        this.scaleX = pScaleX; this.scaleY = pScaleY; this.scaleZ = pScaleZ;
    }

    public void addChild(ModelRenderer pModelRenderer) {
        if (childModels == null) childModels = new ArrayList<>();
        childModels.add(pModelRenderer);
        pModelRenderer.pearent = this;
    }

    public ModelRenderer setTextureOffset(int pOffsetX, int pOffsetY) {
        textureOffsetX = pOffsetX; textureOffsetY = pOffsetY;
        return this;
    }

    public ModelRenderer addBox(String pName, float pX, float pY, float pZ, int pWidth, int pHeight, int pDepth) {
        addParts(ModelBox.class, pName, pX, pY, pZ, pWidth, pHeight, pDepth, 0.0F);
        return this;
    }

    public ModelRenderer addBox(float pX, float pY, float pZ, int pWidth, int pHeight, int pDepth) {
        addParts(ModelBox.class, pX, pY, pZ, pWidth, pHeight, pDepth, 0.0F);
        return this;
    }

    public ModelRenderer addBox(float pX, float pY, float pZ, int pWidth, int pHeight, int pDepth, float pSizeAdjust) {
        addParts(ModelBox.class, pX, pY, pZ, pWidth, pHeight, pDepth, pSizeAdjust);
        return this;
    }

    public ModelRenderer setRotationPoint(float pX, float pY, float pZ) {
        rotationPointX = pX; rotationPointY = pY; rotationPointZ = pZ;
        return this;
    }

    public void render(float par1, boolean pIsRender) {
        GLCompat.modelRenderer = this;
        if (isHidden) return;
        if (!showModel) return;
        if (!compiled) compileDisplayList(par1);
        GLCompat.glPushMatrix();
        GLCompat.glTranslatef(offsetX, offsetY, offsetZ);
        if (rotationPointX != 0.0F || rotationPointY != 0.0F || rotationPointZ != 0.0F) {
            GLCompat.glTranslatef(rotationPointX * par1, rotationPointY * par1, rotationPointZ * par1);
        }
        if (rotateAngleX != 0.0F || rotateAngleY != 0.0F || rotateAngleZ != 0.0F) setRotation();
        renderObject(par1, pIsRender);
        GLCompat.glPopMatrix();
    }

    public void render(float par1) { render(par1, true); }

    public void renderWithRotation(float par1) {
        if (isHidden) return;
        if (!showModel) return;
        if (!compiled) compileDisplayList(par1);
        GLCompat.glPushMatrix();
        GLCompat.glTranslatef(rotationPointX * par1, rotationPointY * par1, rotationPointZ * par1);
        setRotation();
        GLCompat.glCallList(displayList);
        GLCompat.glPopMatrix();
    }

    public void postRender(float par1) {
        if (isHidden) return;
        if (!showModel) return;
        if (!compiled) compileDisplayList(par1);
        if (pearent != null) pearent.postRender(par1);
        GLCompat.glTranslatef(offsetX, offsetY, offsetZ);
        if (rotationPointX != 0.0F || rotationPointY != 0.0F || rotationPointZ != 0.0F) {
            GLCompat.glTranslatef(rotationPointX * par1, rotationPointY * par1, rotationPointZ * par1);
        }
        if (rotateAngleX != 0.0F || rotateAngleY != 0.0F || rotateAngleZ != 0.0F) setRotation();
    }

    protected void compileDisplayList(float par1) {
        compiled = true;
        scale = par1;
    }

    public ModelRenderer setTextureSize(int pWidth, int pHeight) {
        textureWidth = pWidth; textureHeight = pHeight;
        return this;
    }

    public ModelRenderer addCubeList(ModelBoxBase pModelBoxBase) {
        cubeList.add(pModelBoxBase);
        return this;
    }

    protected ModelBoxBase getModelBoxBase(Class<? extends ModelBoxBase> pModelBoxBase, Object... pArg) {
        try {
            Constructor<? extends ModelBoxBase> lconstructor = pModelBoxBase.getConstructor(ModelRenderer.class, Object[].class);
            return lconstructor.newInstance(this, pArg);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    protected Object[] getArg(Object... pArg) {
        Object[] lobject = new Object[pArg.length + 2];
        lobject[0] = textureOffsetX; lobject[1] = textureOffsetY;
        System.arraycopy(pArg, 0, lobject, 2, pArg.length);
        return lobject;
    }

    public ModelRenderer addParts(Class<? extends ModelBoxBase> pModelBoxBase, String pName, Object... pArg) {
        pName = boxName + "." + pName;
        addCubeList(getModelBoxBase(pModelBoxBase, getArg(pArg)).setBoxName(pName));
        return this;
    }

    public ModelRenderer addParts(Class<? extends ModelBoxBase> pModelBoxBase, Object... pArg) {
        addCubeList(getModelBoxBase(pModelBoxBase, getArg(pArg)));
        return this;
    }

    public ModelRenderer addPartsTexture(Class<? extends ModelBoxBase> pModelBoxBase, String pName, Object... pArg) {
        pName = boxName + "." + pName;
        addCubeList(getModelBoxBase(pModelBoxBase, pArg).setBoxName(pName));
        return this;
    }

    public ModelRenderer addPartsTexture(Class<? extends ModelBoxBase> pModelBoxBase, Object... pArg) {
        addCubeList(getModelBoxBase(pModelBoxBase, pArg));
        return this;
    }

    public ModelRenderer addPlate(float pX, float pY, float pZ, int pWidth, int pHeight, int pFacePlane) {
        addParts(ModelPlate.class, pX, pY, pZ, pWidth, pHeight, pFacePlane, 0.0F);
        return this;
    }

    public ModelRenderer addPlate(float pX, float pY, float pZ, int pWidth, int pHeight, int pFacePlane, float pSizeAdjust) {
        addParts(ModelPlate.class, pX, pY, pZ, pWidth, pHeight, pFacePlane, pSizeAdjust);
        return this;
    }

    public ModelRenderer addPlate(String pName, float pX, float pY, float pZ, int pWidth, int pHeight, int pFacePlane) {
        addParts(ModelPlate.class, pName, pX, pY, pZ, pWidth, pHeight, pFacePlane, 0.0F);
        return this;
    }

    public void clearCubeList() {
        cubeList.clear();
        compiled = false;
        if (childModels != null) childModels.clear();
    }

    public void renderItems(ModelMultiBase pModelMulti, IModelCaps pEntityCaps, boolean pRealBlock, int pIndex) {
    }

    public void renderItemsHead(ModelMultiBase pModelMulti, IModelCaps pEntityCaps) {
    }

    public void setRotatePriority(int pValue) { rotatePriority = pValue; }

    protected void setRotation() {
        // mulPose(Axis.*.rotation(angle)) は回転ごとに Quaternionf を確保する。
        // pose / normal 行列を単位軸で in-place 回転することで確保を避ける（出力は等価）。
        PoseStack.Pose entry = poseStack.last();
        switch (rotatePriority) {
            case RotXYZ -> {
                if (rotateAngleZ != 0.0F) mulRotate(entry, rotateAngleZ, 0.0F, 0.0F, 1.0F);
                if (rotateAngleY != 0.0F) mulRotate(entry, rotateAngleY, 0.0F, 1.0F, 0.0F);
                if (rotateAngleX != 0.0F) mulRotate(entry, rotateAngleX, 1.0F, 0.0F, 0.0F);
            }
            case RotXZY -> {
                if (rotateAngleY != 0.0F) mulRotate(entry, rotateAngleY, 0.0F, 1.0F, 0.0F);
                if (rotateAngleZ != 0.0F) mulRotate(entry, rotateAngleZ, 0.0F, 0.0F, 1.0F);
                if (rotateAngleX != 0.0F) mulRotate(entry, rotateAngleX, 1.0F, 0.0F, 0.0F);
            }
            case RotYXZ -> {
                if (rotateAngleZ != 0.0F) mulRotate(entry, rotateAngleZ, 0.0F, 0.0F, 1.0F);
                if (rotateAngleX != 0.0F) mulRotate(entry, rotateAngleX, 1.0F, 0.0F, 0.0F);
                if (rotateAngleY != 0.0F) mulRotate(entry, rotateAngleY, 0.0F, 1.0F, 0.0F);
            }
            case RotYZX -> {
                if (rotateAngleX != 0.0F) mulRotate(entry, rotateAngleX, 1.0F, 0.0F, 0.0F);
                if (rotateAngleZ != 0.0F) mulRotate(entry, rotateAngleZ, 0.0F, 0.0F, 1.0F);
                if (rotateAngleY != 0.0F) mulRotate(entry, rotateAngleY, 0.0F, 1.0F, 0.0F);
            }
            case RotZXY -> {
                if (rotateAngleY != 0.0F) mulRotate(entry, rotateAngleY, 0.0F, 1.0F, 0.0F);
                if (rotateAngleX != 0.0F) mulRotate(entry, rotateAngleX, 1.0F, 0.0F, 0.0F);
                if (rotateAngleZ != 0.0F) mulRotate(entry, rotateAngleZ, 0.0F, 0.0F, 1.0F);
            }
            case RotZYX -> {
                if (rotateAngleX != 0.0F) mulRotate(entry, rotateAngleX, 1.0F, 0.0F, 0.0F);
                if (rotateAngleY != 0.0F) mulRotate(entry, rotateAngleY, 0.0F, 1.0F, 0.0F);
                if (rotateAngleZ != 0.0F) mulRotate(entry, rotateAngleZ, 0.0F, 0.0F, 1.0F);
            }
            default -> { }
        }
    }

    // pose と normal の両行列を同一の単位軸回転で更新する（PoseStack#mulPose 相当・無確保）。
    private void mulRotate(PoseStack.Pose entry, float angle, float ax, float ay, float az) {
        entry.pose().rotate(angle, ax, ay, az);
        entry.normal().rotate(angle, ax, ay, az);
    }

    protected void renderObject(float par1, boolean pRendering) {
        // 全部品で毎フレーム行列を読み戻すのは無駄。loadMatrix() を実際に使う部品でのみ捕捉する。
        if (needsMatrixCapture) {
            GLCompat.glGetFloat(GL11.GL_MODELVIEW_MATRIX, matrix);
        }
        if (pRendering && isRendering) {
            if (scaleX != 1.0F || scaleY != 1.0F || scaleZ != 1.0F) {
                // スケール指定がある部品のみ push/scale/pop する。
                // スケール 1 のとき glScalef は恒等変換であり、それを包む push/pop（Pose 確保）は純粋な無駄。
                GLCompat.glPushMatrix();
                GLCompat.glScalef(scaleX, scaleY, scaleZ);
                GLCompat.glCallList(displayList);
                GLCompat.glPopMatrix();
            } else {
                GLCompat.glCallList(displayList);
            }
        }
        if (childModels != null) {
            for (ModelRenderer childModel : childModels) {
                childModel.render(par1, pRendering);
            }
        }
    }

    public ModelRenderer loadMatrix() {
        // 初回呼び出し以降、この部品は renderObject で行列を捕捉する（初回のみ1フレーム遅延）。
        needsMatrixCapture = true;
        GLCompat.glLoadMatrix(matrix);
        if (isInvertX) GLCompat.glScalef(-1F, 1F, 1F);
        return this;
    }

    public boolean getMirror() { return mirror; }
    public ModelRenderer setMirror(boolean flag) { mirror = flag; return this; }
    public boolean getVisible() { return showModel; }
    public void setVisible(boolean flag) { showModel = flag; }

    public float getRotateAngleX() { return rotateAngleX; }
    public float getRotateAngleDegX() { return rotateAngleX * radFactor; }
    public float setRotateAngleX(float value) { return rotateAngleX = value; }
    public void setRotateAngleDegX(float value) {
        rotateAngleX = value * degFactor;
    }
    public void addRotateAngleX(float value) {
        rotateAngleX += value;
    }
    public void addRotateAngleDegX(float value) {
        rotateAngleX += value * degFactor;
    }

    public float getRotateAngleY() { return rotateAngleY; }
    public float getRotateAngleDegY() { return rotateAngleY * radFactor; }
    public void setRotateAngleY(float value) {
        rotateAngleY = value;
    }
    public void setRotateAngleDegY(float value) {
        rotateAngleY = value * degFactor;
    }
    public void addRotateAngleY(float value) {
        rotateAngleY += value;
    }
    public void addRotateAngleDegY(float value) {
        rotateAngleY += value * degFactor;
    }

    public float getRotateAngleZ() { return rotateAngleZ; }
    public float getRotateAngleDegZ() { return rotateAngleZ * radFactor; }
    public void setRotateAngleZ(float value) {
        rotateAngleZ = value;
    }
    public void setRotateAngleDegZ(float value) {
        rotateAngleZ = value * degFactor;
    }
    public void addRotateAngleZ(float value) {
        rotateAngleZ += value;
    }
    public void addRotateAngleDegZ(float value) {
        rotateAngleZ += value * degFactor;
    }

    public ModelRenderer setRotateAngle(float x, float y, float z) {
        rotateAngleX = x; rotateAngleY = y; rotateAngleZ = z; return this;
    }

    public ModelRenderer setRotateAngleDeg(float x, float y, float z) {
        rotateAngleX = x * degFactor; rotateAngleY = y * degFactor; rotateAngleZ = z * degFactor; return this;
    }

    public float getRotationPointX() { return rotationPointX; }
    public float setRotationPointX(float value) { return rotationPointX = value; }
    public float addRotationPointX(float value) { return rotationPointX += value; }

    public float getRotationPointY() { return rotationPointY; }
    public void setRotationPointY(float value) {
        rotationPointY = value;
    }
    public void addRotationPointY(float value) {
        rotationPointY += value;
    }

    public float getRotationPointZ() { return rotationPointZ; }
    public float setRotationPointZ(float value) { return rotationPointZ = value; }
    public void addRotationPointZ(float value) {
        rotationPointZ += value;
    }

    public ModelRenderer setScale(float pX, float pY, float pZ) {
        scaleX = pX; scaleY = pY; scaleZ = pZ; return this;
    }

    public float setScaleX(float pValue) { return scaleX = pValue; }
    public float setScaleY(float pValue) { return scaleY = pValue; }
    public float setScaleZ(float pValue) { return scaleZ = pValue; }
}
