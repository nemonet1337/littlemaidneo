package work.nemonet.littlemaidneo.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import work.nemonet.littlemaidneo.LittleMaidNeo;
import work.nemonet.littlemaidneo.entity.MaidSoulEntity;
import org.joml.Matrix3f;
import org.joml.Matrix4f;

public class MaidSoulRenderer extends EntityRenderer<MaidSoulEntity, EntityRenderState> {

    private static final Identifier TEXTURE = Identifier.fromNamespaceAndPath(LittleMaidNeo.MODID,
            "textures/entity/maid_soul/maid_soul.png");
    private static final Identifier HEART = Identifier.parse("textures/particle/heart.png");

    public MaidSoulRenderer(EntityRendererProvider.Context ctx) {
        super(ctx);
    }

    @Override
    public EntityRenderState createRenderState() {
        return new EntityRenderState();
    }

    @Override
    public void submit(EntityRenderState state, PoseStack matrices, SubmitNodeCollector submitNodeCollector, CameraRenderState camera) {
        super.submit(state, matrices, submitNodeCollector, camera);
        float progress = (state.ageInTicks % 40) / 40;
        float radius = 0.25f;
        float cos = Mth.cos(progress * Mth.PI * 2) * radius;
        float sin = Mth.sin(progress * Mth.PI * 2) * radius;
        float x = 0;
        float z = 0;
        float y = 0.25f;
        float x1 = x + cos;
        float x2 = x - cos;
        float z1 = z + sin;
        float z2 = z - sin;
        float y1 = y + radius;
        float y2 = y - radius;
        submitNodeCollector.submitCustomGeometry(matrices, RenderTypes.entityCutout(HEART), (pose, consumer) -> {
            Matrix4f posMatrix = pose.pose();
            Matrix3f normMatrix = pose.normal();
            // 反時計回りが表
            vertex(posMatrix, normMatrix, consumer, x1, y1, z1, 1.0f, 0.0f);
            vertex(posMatrix, normMatrix, consumer, x2, y1, z2, 0.0f, 0.0f);
            vertex(posMatrix, normMatrix, consumer, x2, y2, z2, 0.0f, 1.0f);
            vertex(posMatrix, normMatrix, consumer, x1, y2, z1, 1.0f, 1.0f);
            // 裏
            vertex(posMatrix, normMatrix, consumer, x1, y1, z1, 0.0f, 0.0f);
            vertex(posMatrix, normMatrix, consumer, x1, y2, z1, 0.0f, 1.0f);
            vertex(posMatrix, normMatrix, consumer, x2, y2, z2, 1.0f, 1.0f);
            vertex(posMatrix, normMatrix, consumer, x2, y1, z2, 1.0f, 0.0f);
        });
    }

    public void vertex(Matrix4f positionMatrix, Matrix3f normalMatrix, com.mojang.blaze3d.vertex.VertexConsumer vertexConsumer,
            float x, float y, float z, float u, float v) {
        vertexConsumer.addVertex(positionMatrix, x, y, z)
                .setColor(255, 255, 255, 255)
                .setUv(u, v)
                .setOverlay(OverlayTexture.NO_OVERLAY)
                .setLight(LightCoordsUtil.FULL_BRIGHT)
                .setNormal(0, 0, 1);
    }

    public Identifier getTextureLocation(EntityRenderState state) {
        return TEXTURE;
    }
}
