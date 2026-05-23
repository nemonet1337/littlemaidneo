package work.nemonet.littlemaidneo.maidmodel.compat;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.world.phys.Vec2;
import work.nemonet.littlemaidneo.maidmodel.ModelBoxBase;
import work.nemonet.littlemaidneo.maidmodel.ModelRenderer;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.lwjgl.opengl.GL11;

import java.nio.FloatBuffer;

public final class GLCompat {

    public static int mode = GL11.GL_MODELVIEW;
    public static ModelRenderer modelRenderer;
    public static PoseStack textureStack = new PoseStack();

    private static int renderMode;
    private static ModelBoxBase.PositionTextureVertex vertexCurrent;
    private static ModelBoxBase.PositionTextureVertex vertexPrev1;
    private static ModelBoxBase.PositionTextureVertex vertexPrev2;
    private static Vector3f pos;
    private static Vec2 tex;

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
        if (mode == GL11.GL_MODELVIEW) {
            ModelRenderer.poseStack.mulPose(new org.joml.Quaternionf().rotationAxis((float) Math.toRadians(deg), x, y, z));
        } else if (mode == GL11.GL_TEXTURE) {
            textureStack.last().pose().rotate(new org.joml.Quaternionf().rotationAxis((float) Math.toRadians(deg), x, y, z));
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
        if (i == GL11.GL_TRIANGLE_STRIP) renderMode = i;
    }

    public static void glEnd() {
        if (renderMode == GL11.GL_TRIANGLE_STRIP) {
            vertexCurrent = null; vertexPrev1 = null; vertexPrev2 = null;
            pos = null; tex = null;
        }
        renderMode = 0;
    }

    public static void glVertex3f(float x, float y, float z) {
        if (renderMode == GL11.GL_TRIANGLE_STRIP) {
            pos = new Vector3f(x, y, z);
            combine();
        }
    }

    public static void glNormal3f(float f, float f2, float f3) {}

    public static void glTexCoord2f(float u, float v) {
        if (renderMode == GL11.GL_TRIANGLE_STRIP) {
            tex = new Vec2(u, v);
            combine();
        }
    }

    private static void combine() {
        if (tex != null && pos != null) {
            vertexPrev2 = vertexPrev1;
            vertexPrev1 = vertexCurrent;
            vertexCurrent = new ModelBoxBase.PositionTextureVertex(pos, tex.x, tex.y);
            pos = null; tex = null;
            if (vertexPrev2 != null) {
                ModelBoxBase.TexturedQuad quad = new ModelBoxBase.TexturedQuad(
                        new ModelBoxBase.PositionTextureVertex[]{vertexPrev2, vertexPrev1, vertexCurrent, vertexCurrent});
                quad.draw(ModelRenderer.poseStack, ModelRenderer.buffer, ModelRenderer.light, ModelRenderer.overlay,
                        ModelRenderer.red, ModelRenderer.green, ModelRenderer.blue, ModelRenderer.alpha, 1F);
            }
        }
    }

    public static void glPushAttrib(int i) {}
    public static void glPopAttrib() {}
    public static void glCullFace(int i) {}
    public static void glEnable(int i) {}

    public static void dummy() {}
    public static void dummy(int i) {}
    public static void dummy(float a, float b) {}
    public static void dummy(float a, float b, float c) {}
    public static void dummy(FloatBuffer f) {}
    public static void dummy(int i, FloatBuffer f) {}
}
