package net.sistr.littlemaidrebirth.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.sistr.littlemaidrebirth.LMRBMod;
import net.sistr.littlemaidrebirth.entity.MaidSoulEntity;
import org.joml.Matrix3f;
import org.joml.Matrix4f;

public class MaidSoulRenderer extends EntityRenderer<MaidSoulEntity> {
    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(LMRBMod.MODID,
            "textures/entity/maid_soul/maid_soul.png");
    private static final ResourceLocation HEART = ResourceLocation.parse("textures/particle/heart.png");

    public MaidSoulRenderer(EntityRendererProvider.Context ctx) {
        super(ctx);
    }

    @Override
    public void render(MaidSoulEntity entity, float yaw, float tickDelta, PoseStack matrices,
            MultiBufferSource vertexConsumers, int light) {
        super.render(entity, yaw, tickDelta, matrices, vertexConsumers, light);
        float progress = (entity.tickCount + tickDelta) % 40 / 40;
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
        var consumer = vertexConsumers.getBuffer(RenderType.entityCutout(HEART));
        var entry = matrices.last();
        var posMatrix = entry.pose();
        var normMatrix = entry.normal();
        // 反時計回りが表
        // 表を見て、右上、左上、左下、右下の順
        vertex(posMatrix, normMatrix, consumer, x1, y1, z1, 1.0f, 0.0f);
        vertex(posMatrix, normMatrix, consumer, x2, y1, z2, 0.0f, 0.0f);
        vertex(posMatrix, normMatrix, consumer, x2, y2, z2, 0.0f, 1.0f);
        vertex(posMatrix, normMatrix, consumer, x1, y2, z1, 1.0f, 1.0f);
        // 裏、左右反転、右上、右下、左下、左上
        vertex(posMatrix, normMatrix, consumer, x1, y1, z1, 0.0f, 0.0f);
        vertex(posMatrix, normMatrix, consumer, x1, y2, z1, 0.0f, 1.0f);
        vertex(posMatrix, normMatrix, consumer, x2, y2, z2, 1.0f, 1.0f);
        vertex(posMatrix, normMatrix, consumer, x2, y1, z2, 1.0f, 0.0f);
    }

    public void vertex(Matrix4f positionMatrix, Matrix3f normalMatrix, VertexConsumer vertexConsumer,
            float x, float y, float z, float u, float v) {
        vertexConsumer.addVertex(positionMatrix, x, y, z)
                .setColor(255, 255, 255, 255)
                .setUv(u, v)
                .setOverlay(OverlayTexture.NO_OVERLAY)
                .setLight(LightTexture.FULL_BLOCK)
                .setNormal(0, 0, 1);
    }

    @Override
    public ResourceLocation getTextureLocation(MaidSoulEntity entity) {
        return TEXTURE;
    }
}
