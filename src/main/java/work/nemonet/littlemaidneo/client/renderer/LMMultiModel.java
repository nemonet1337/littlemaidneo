package work.nemonet.littlemaidneo.client.renderer;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.model.HeadedModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import work.nemonet.littlemaidneo.client.renderer.MultiModel;
import work.nemonet.littlemaidneo.entity.compound.IHasMultiModel;
import work.nemonet.littlemaidneo.maidmodel.ModelLittleMaidBase;
import work.nemonet.littlemaidneo.maidmodel.ModelRenderer;
import work.nemonet.littlemaidneo.entity.LittleMaidEntity;

/**
 * LM専用に拡張
 */
public class LMMultiModel<T extends LittleMaidEntity> extends MultiModel<T> implements HeadedModel {
    private T entity;
    private final ModelPart modelPart = new ModelPart(ImmutableList.of(), ImmutableMap.of());

    @Override
    public void prepareMobModel(T entity, float limbAngle, float limbDistance, float tickDelta) {
        this.entity = entity;
        super.prepareMobModel(entity, limbAngle, limbDistance, tickDelta);
    }

    @Override
    public void setupAnim(T entity, float limbAngle, float limbDistance, float animationProgress, float headYaw,
            float headPitch) {
        this.entity = entity;
        super.setupAnim(entity, limbAngle, limbDistance, animationProgress, headYaw, headPitch);
    }

    @Override
    public void renderToBuffer(PoseStack matrices, VertexConsumer vertices, int light, int overlay, int color) {
        if (this.entity == null) {
            return;
        }
        // TODO: isAccelerationの色変更は新しいARGB統合colorに対応が必要
        super.renderToBuffer(matrices, vertices, light, overlay, color);
    }

    @Override
    public ModelPart getHead() {
        this.entity.getModel(IHasMultiModel.Layer.SKIN, IHasMultiModel.Part.HEAD)
                .filter(model -> model instanceof ModelLittleMaidBase)
                .map(model -> (ModelLittleMaidBase) model)
                .ifPresent(model -> {
                    modelPart.x = 0;
                    modelPart.y = 0;
                    modelPart.z = 0;
                    modelPart.zRot = 0;
                    modelPart.yRot = 0;
                    modelPart.xRot = 0;
                    ModelRenderer modelRenderer;
                    ItemStack stack = this.entity.getItemBySlot(EquipmentSlot.HEAD);
                    if (this.entity.getEquipmentSlotForItem(stack) == EquipmentSlot.HEAD) {
                        modelRenderer = model.bipedHead;
                    } else {
                        modelRenderer = model.bipedHead;
                    }
                    while (modelRenderer != null) {
                        modelPart.x += (modelRenderer.rotationPointX + modelRenderer.offsetX * 16.0f) * 0.9375F;
                        modelPart.y += (modelRenderer.rotationPointY + modelRenderer.offsetY * 16.0f) * 0.9375F;
                        modelPart.z += (modelRenderer.rotationPointZ + modelRenderer.offsetZ * 16.0f) * 0.9375F;
                        modelPart.zRot += modelRenderer.rotateAngleZ;
                        modelPart.yRot += modelRenderer.rotateAngleY;
                        modelPart.xRot += modelRenderer.rotateAngleX;
                        modelRenderer = modelRenderer.pearent;
                    }
                });
        return modelPart;
    }
}
