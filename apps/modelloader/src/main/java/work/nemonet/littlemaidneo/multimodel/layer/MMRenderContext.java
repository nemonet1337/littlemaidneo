package work.nemonet.littlemaidneo.multimodel.layer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;

public record MMRenderContext(MMMatrixStack matrices, MMVertexConsumer vertices, int light, int overlay, float red,
                              float green, float blue, float alpha) {
    public MMRenderContext(PoseStack matrices, VertexConsumer vertices,
                           int light, int overlay, float red, float green, float blue, float alpha) {
        this(new MMMatrixStack(matrices), new MMVertexConsumer(vertices), light, overlay, red, green, blue, alpha);
    }

    public void render(Renderer renderer) {
        renderer.render(matrices, vertices, light, overlay, red, green, blue, alpha);
    }

    public interface Renderer {
        void render(MMMatrixStack matrices, MMVertexConsumer vertices, int light, int overlay,
                    float red, float green, float blue, float alpha);
    }
}
