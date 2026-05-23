package work.nemonet.littlemaidneo.multimodel.layer;

import com.mojang.blaze3d.vertex.VertexConsumer;

public class MMVertexConsumer {
    private final VertexConsumer vertexConsumer;

    public MMVertexConsumer(VertexConsumer vertexConsumer) {
        this.vertexConsumer = vertexConsumer;
    }

    public void vertex(float x, float y, float z, float red, float green, float blue, float alpha, float u, float v,
                       int overlay, int light, float normalX, float normalY, float normalZ) {
        this.vertexConsumer.addVertex(x, y, z)
                .setColor(red, green, blue, alpha)
                .setUv(u, v)
                .setOverlay(overlay)
                .setLight(light)
                .setNormal(normalX, normalY, normalZ);
    }

    public VertexConsumer getVanillaVertexConsumer() {
        return this.vertexConsumer;
    }
}
