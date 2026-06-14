package work.nemonet.littlemaidneo.maidmodel.compat;

import com.mojang.blaze3d.vertex.PoseStack;
import work.nemonet.littlemaidneo.maidmodel.ModelBoxBase;
import work.nemonet.littlemaidneo.maidmodel.ModelRenderer;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.lwjgl.opengl.GL11;

import java.nio.FloatBuffer;

public final class GLCompat {

    public static int mode = GL11.GL_MODELVIEW;
    public static ModelRenderer modelRenderer;
    public static final PoseStack textureStack = new PoseStack();

    private static int renderMode;
    // 即時モード（GL_TRIANGLE_STRIP）エミュレーション用の再利用バッファ。
    // 旧実装は頂点毎に Vector3f/Vec2/PositionTextureVertex/TexturedQuad を確保していた。
    private static final float[] stripX = new float[3];
    private static final float[] stripY = new float[3];
    private static final float[] stripZ = new float[3];
    private static final float[] stripU = new float[3];
    private static final float[] stripV = new float[3];
    private static int stripCount;
    private static boolean hasPos;
    private static boolean hasTex;
    private static float pendingX;
    private static float pendingY;
    private static float pendingZ;
    private static float pendingU;
    private static float pendingV;

    public static void glPushMatrix() {
        if (mode == GL11.GL_MODELVIEW) {
            ModelRenderer.poseStack.pushPose();
        } else if (mode == GL11.GL_TEXTURE) {
            textureStack.pushPose();
        }
    }

    public static void glPopMatrix() {
        if (mode == GL11.GL_MODELVIEW) {
            ModelRenderer.poseStack.popPose();
        } else if (mode == GL11.GL_TEXTURE) {
            textureStack.popPose();
        }
    }

    public static void glTranslatef(float x, float y, float z) {
        if (mode == GL11.GL_MODELVIEW) {
            ModelRenderer.poseStack.translate(x, y, z);
        } else if (mode == GL11.GL_TEXTURE) {
            textureStack.translate(x, y, z);
        }
    }

    public static void glScalef(float x, float y, float z) {
        if (mode == GL11.GL_MODELVIEW) {
            ModelRenderer.poseStack.last().pose().scale(x, y, z);
        } else if (mode == GL11.GL_TEXTURE) {
            textureStack.last().pose().scale(x, y, z);
        }
    }

    public static void glRotatef(float deg, float x, float y, float z) {
        float rad = (float) Math.toRadians(deg);
        // glRotatef / Quaternionf#rotationAxis は軸を正規化する。Matrix#rotate(ang,x,y,z) は
        // 単位軸前提なので、ここで正規化して Quaternionf の確保を避ける（出力は等価）。
        float len = (float) Math.sqrt(x * x + y * y + z * z);
        if (len == 0.0F) return;
        float nx = x / len, ny = y / len, nz = z / len;
        if (mode == GL11.GL_MODELVIEW) {
            PoseStack.Pose entry = ModelRenderer.poseStack.last();
            entry.pose().rotate(rad, nx, ny, nz);
            entry.normal().rotate(rad, nx, ny, nz);
        } else if (mode == GL11.GL_TEXTURE) {
            textureStack.last().pose().rotate(rad, nx, ny, nz);
        }
    }

    public static void glColor3f(float red, float green, float blue) {
        ModelRenderer.red = red;
        ModelRenderer.green = green;
        ModelRenderer.blue = blue;
    }

    public static void glMatrixMode(int mode) {
        GLCompat.mode = mode;
    }

    public static void glGetFloat(int mode, FloatBuffer buf) {
        if (mode == GL11.GL_MODELVIEW_MATRIX) {
            ModelRenderer.poseStack.last().pose().get(buf);
        }
    }

    public static void glLoadMatrix(FloatBuffer buf) {
        if (mode == GL11.GL_MODELVIEW) {
            ModelRenderer.poseStack.last().pose().set(buf);
        } else if (mode == GL11.GL_TEXTURE) {
            textureStack.last().pose().set(buf);
        }
    }

    public static void glMultMatrix(FloatBuffer buf) {
        if (mode == GL11.GL_MODELVIEW) {
            Matrix4f matrix4f = new Matrix4f(); matrix4f.set(buf);
            ModelRenderer.poseStack.last().pose().mul(matrix4f);
        } else if (mode == GL11.GL_TEXTURE) {
            Matrix4f matrix4f = new Matrix4f(); matrix4f.set(buf);
            textureStack.last().pose().mul(matrix4f);
        }
    }

    public static void glCallList(int i) {
        for (ModelBoxBase boxBase : modelRenderer.cubeList) {
            boxBase.render(ModelRenderer.poseStack, ModelRenderer.buffer, ModelRenderer.light, ModelRenderer.overlay,
                    ModelRenderer.red, ModelRenderer.green, ModelRenderer.blue, ModelRenderer.alpha, modelRenderer.scale);
        }
    }

    public static void glLoadIdentity() {
        ModelRenderer.poseStack.last().pose().identity();
    }

    public static void glBegin(int i) {
        if (i == GL11.GL_TRIANGLE_STRIP) {
            renderMode = i;
            stripCount = 0;
            hasPos = false;
            hasTex = false;
        }
    }

    public static void glEnd() {
        if (renderMode == GL11.GL_TRIANGLE_STRIP) {
            stripCount = 0;
            hasPos = false;
            hasTex = false;
        }
        renderMode = 0;
    }

    public static void glVertex3f(float x, float y, float z) {
        if (renderMode == GL11.GL_TRIANGLE_STRIP) {
            pendingX = x; pendingY = y; pendingZ = z;
            hasPos = true;
            combine();
        }
    }

    public static void glTexCoord2f(float u, float v) {
        if (renderMode == GL11.GL_TRIANGLE_STRIP) {
            pendingU = u; pendingV = v;
            hasTex = true;
            combine();
        }
    }

    private static void combine() {
        // 旧実装と同じく、位置とテクスチャ座標が両方そろった時点で1頂点を確定する
        // （= 頂点ごとに texCoord 指定が必要、という挙動も含めて踏襲）。
        if (hasPos && hasTex) {
            // 直近3頂点リングを1つシフトして末尾に確定頂点を入れる。
            stripX[0] = stripX[1]; stripY[0] = stripY[1]; stripZ[0] = stripZ[1];
            stripU[0] = stripU[1]; stripV[0] = stripV[1];
            stripX[1] = stripX[2]; stripY[1] = stripY[2]; stripZ[1] = stripZ[2];
            stripU[1] = stripU[2]; stripV[1] = stripV[2];
            stripX[2] = pendingX; stripY[2] = pendingY; stripZ[2] = pendingZ;
            stripU[2] = pendingU; stripV[2] = pendingV;
            hasPos = false;
            hasTex = false;
            if (stripCount < 3) stripCount++;
            // 3頂点そろって以降、頂点が来るたびに直近3頂点で三角形を描く（旧 prev2!=null 条件と等価）。
            if (stripCount >= 3) {
                emitStripTriangle();
            }
        }
    }

    // 旧 TexturedQuad 経由を置き換え、ストリップ三角形を確保なしで直接バッファへ書き出す。
    // 頂点並び {prev2, prev1, current, current} と法線計算は ModelBoxBase.calcNormal/draw と同一。
    private static void emitStripTriangle() {
        PoseStack.Pose entry = ModelRenderer.poseStack.last();
        Matrix4f pose = entry.pose();
        Matrix3f norm = entry.normal();

        // 法線 = normalize((current - prev1) × (prev2 - prev1))
        float ax = stripX[2] - stripX[1], ay = stripY[2] - stripY[1], az = stripZ[2] - stripZ[1];
        float bx = stripX[0] - stripX[1], by = stripY[0] - stripY[1], bz = stripZ[0] - stripZ[1];
        float cx = ay * bz - az * by;
        float cy = az * bx - ax * bz;
        float cz = ax * by - ay * bx;
        float clen = (float) Math.sqrt(cx * cx + cy * cy + cz * cz);
        if (clen != 0.0F) { cx /= clen; cy /= clen; cz /= clen; }
        // 法線行列でクアッドにつき1回だけ変換する。
        float nx = norm.m00() * cx + norm.m10() * cy + norm.m20() * cz;
        float ny = norm.m01() * cx + norm.m11() * cy + norm.m21() * cz;
        float nz = norm.m02() * cx + norm.m12() * cy + norm.m22() * cz;

        boolean useTextureMatrix = mode == GL11.GL_TEXTURE;
        emitStripVertex(pose, 0, nx, ny, nz, useTextureMatrix);
        emitStripVertex(pose, 1, nx, ny, nz, useTextureMatrix);
        emitStripVertex(pose, 2, nx, ny, nz, useTextureMatrix);
        emitStripVertex(pose, 2, nx, ny, nz, useTextureMatrix);
    }

    private static void emitStripVertex(Matrix4f pose, int i, float nx, float ny, float nz, boolean useTextureMatrix) {
        float u = stripU[i];
        float v = stripV[i];
        if (useTextureMatrix) {
            org.joml.Vector4f uv = new org.joml.Vector4f(u, v, 0.0F, 1.0F);
            textureStack.last().pose().transform(uv);
            u = uv.x();
            v = uv.y();
        }
        ModelRenderer.buffer.addVertex(pose, stripX[i], stripY[i], stripZ[i])
                .setColor(ModelRenderer.red, ModelRenderer.green, ModelRenderer.blue, ModelRenderer.alpha)
                .setUv(u, v)
                .setOverlay(ModelRenderer.overlay)
                .setLight(ModelRenderer.light)
                .setNormal(nx, ny, nz);
    }

}
