package net.sistr.littlemaidmodelloader.multimodel.layer;

import com.mojang.blaze3d.vertex.VertexConsumer;

// 1.21.1移植: YarnマッピングからMojangマッピングへ変更
// - VertexConsumer はパッケージ変更 (net.minecraft.client.render → com.mojang.blaze3d.vertex)
// - vertexメソッドのシグネチャ変更: 1.21.1では個別のsetterメソッドを使用する形式に変更
public class MMVertexConsumer {
    private final VertexConsumer vertexConsumer;

    public MMVertexConsumer(VertexConsumer vertexConsumer) {
        this.vertexConsumer = vertexConsumer;
    }

    // 1.21.1ではVertexConsumerのvertexメソッドが変更されたため、
    // 新しいAPIパターンに合わせてチェーンメソッドを使用
    public void vertex(float x, float y, float z, float red, float green, float blue, float alpha, float u, float v,
                       int overlay, int light, float normalX, float normalY, float normalZ) {
        // 1.21.1: VertexConsumerの新しいAPIを使用
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
