package work.nemonet.littlemaidneo.client.renderer;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import net.minecraft.client.model.HeadedModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import work.nemonet.littlemaidneo.entity.LittleMaidEntity;
import work.nemonet.littlemaidneo.entity.compound.IHasMultiModel;
import work.nemonet.littlemaidneo.maidmodel.LMModel;
import work.nemonet.littlemaidneo.maidmodel.ModelLittleMaidBase;
import work.nemonet.littlemaidneo.maidmodel.ModelRenderer;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.CubeDeformation;

public class LMMultiModel<S extends MaidRenderState> extends LMModel<S> implements HeadedModel {
    private LittleMaidEntity entity;
    private final ModelPart modelPart = new ModelPart(ImmutableList.of(), ImmutableMap.of());

    public LMMultiModel() {
        super(
            new ModelPart(ImmutableList.of(), ImmutableMap.of()),
            new ModelPart(ImmutableList.of(), ImmutableMap.of()),
            new ModelPart(ImmutableList.of(), ImmutableMap.of())
        );
    }

    @Override
    protected void buildMesh(MeshDefinition mesh, CubeDeformation deform) {
    }

    @Override
    public void setupAnim(S state) {
        this.entity = state.maidEntity;
        super.setupAnim(state);
    }

    @Override
    public ModelPart getHead() {
        if (this.entity == null) return modelPart;
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
