package work.nemonet.littlemaidneo.maidmodel;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import work.nemonet.littlemaidneo.maidmodel.compat.GLCompat;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector4f;
import org.lwjgl.opengl.GL11;

public abstract class ModelBoxBase {
    protected PositionTextureVertex[] vertexPositions;
    protected TexturedQuad[] quadList;
    public float posX1;
    public float posY1;
    public float posZ1;
    public float posX2;
    public float posY2;
    public float posZ2;
    public String boxName;

    public ModelBoxBase(ModelRenderer pMRenderer, Object... pArg) {
    }

    public final void render(PoseStack poseStack, VertexConsumer buffer,
                             int light, int overlay, float red, float green, float blue, float alpha,
                             float scale) {
        for (TexturedQuad texturedQuad : quadList) {
            texturedQuad.draw(poseStack, buffer, light, overlay, red, green, blue, alpha, scale);
        }
    }

    public ModelBoxBase setBoxName(String pName) {
        boxName = pName;
        return this;
    }
public static class PositionTextureVertex {
        public final Vector3f vector3D;
        public final float texturePositionX;
        public final float texturePositionY;

        public PositionTextureVertex(float x, float y, float z, float u, float v) {
            this(new Vector3f(x, y, z), u, v);
        }

        public PositionTextureVertex setTexturePosition(float u, float v) {
            return new PositionTextureVertex(this, u, v);
        }

        public PositionTextureVertex(PositionTextureVertex textureVertex, float texturePositionXIn, float texturePositionYIn) {
            this.vector3D = textureVertex.vector3D;
            this.texturePositionX = texturePositionXIn;
            this.texturePositionY = texturePositionYIn;
        }

        public PositionTextureVertex(Vector3f vec, float u, float v) {
            this.vector3D = vec;
            this.texturePositionX = u;
            this.texturePositionY = v;
        }
    }
public static class TexturedQuad {
        public PositionTextureVertex[] vertexPositions;
        public final int nVertices;
        private Vector3f normalCache;

        public TexturedQuad(PositionTextureVertex[] vertices) {
            this.vertexPositions = vertices;
            this.nVertices = vertices.length;
            this.normalCache = calcNormal();
        }

        public TexturedQuad(PositionTextureVertex[] vertices, int texcoordU1, int texcoordV1, int texcoordU2, int texcoordV2, float textureWidth, float textureHeight) {
            this(vertices);
            float f = 0.0F / textureWidth;
            float f1 = 0.0F / textureHeight;
            vertices[0] = vertices[0].setTexturePosition((float) texcoordU2 / textureWidth - f, (float) texcoordV1 / textureHeight + f1);
            vertices[1] = vertices[1].setTexturePosition((float) texcoordU1 / textureWidth + f, (float) texcoordV1 / textureHeight + f1);
            vertices[2] = vertices[2].setTexturePosition((float) texcoordU1 / textureWidth + f, (float) texcoordV2 / textureHeight - f1);
            vertices[3] = vertices[3].setTexturePosition((float) texcoordU2 / textureWidth - f, (float) texcoordV2 / textureHeight - f1);
        }

        public void flipFace() {
            PositionTextureVertex[] vertices = new PositionTextureVertex[this.vertexPositions.length];
            for (int i = 0; i < this.vertexPositions.length; ++i) {
                vertices[i] = this.vertexPositions[this.vertexPositions.length - i - 1];
            }
            this.vertexPositions = vertices;
            this.normalCache = calcNormal();
        }

        public final void draw(PoseStack poseStack, VertexConsumer buffer,
                               int light, int overlay, float red, float green, float blue, float alpha, float scale) {
            PoseStack.Pose entry = poseStack.last();
            Matrix4f matrix4f = entry.pose();
            Matrix3f matrix3f = entry.normal();

            // 法線はクアッドにつき 1 回だけ変換する（頂点ループの外、アロケーションなし）。
            // JOML Matrix3f#transform と同一の積和順序なので出力はビット単位で一致する。
            float ncx = normalCache.x();
            float ncy = normalCache.y();
            float ncz = normalCache.z();
            float normalX = matrix3f.m00() * ncx + matrix3f.m10() * ncy + matrix3f.m20() * ncz;
            float normalY = matrix3f.m01() * ncx + matrix3f.m11() * ncy + matrix3f.m21() * ncz;
            float normalZ = matrix3f.m02() * ncx + matrix3f.m12() * ncy + matrix3f.m22() * ncz;

            boolean useTextureMatrix = GLCompat.mode == GL11.GL_TEXTURE;

            for (int i = 0; i < 4; ++i) {
                ModelBoxBase.PositionTextureVertex vertex = this.vertexPositions[i];
                float x = vertex.vector3D.x() * scale;
                float y = vertex.vector3D.y() * scale;
                float z = vertex.vector3D.z() * scale;

                float u = vertex.texturePositionX;
                float v = vertex.texturePositionY;
                if (useTextureMatrix) {
                    // テクスチャ行列が有効なときだけ UV を変換する（まれな経路のみアロケート）。
                    Vector4f uv = new Vector4f(u, v, 0.0F, 1.0F);
                    GLCompat.textureStack.last().pose().transform(uv);
                    u = uv.x();
                    v = uv.y();
                }

                // addVertex(Matrix4f, ...) が内部で座標変換するため new Vector4f を確保しない。
                // エンティティ描画の行列はアフィン（w 行 = 0,0,0,1）なので w 正規化は不要。
                buffer.addVertex(matrix4f, x, y, z)
                        .setColor(red, green, blue, alpha)
                        .setUv(u, v)
                        .setOverlay(overlay)
                        .setLight(light)
                        .setNormal(normalX, normalY, normalZ);
            }
        }

        private Vector3f calcNormal() {
            Vector3f n1 = new Vector3f(this.vertexPositions[0].vector3D);
            Vector3f n2 = new Vector3f(this.vertexPositions[2].vector3D);
            n1.sub(this.vertexPositions[1].vector3D);
            n2.sub(this.vertexPositions[1].vector3D);
            n2.cross(n1);
            n2.normalize();
            return n2;
        }
    }
}
