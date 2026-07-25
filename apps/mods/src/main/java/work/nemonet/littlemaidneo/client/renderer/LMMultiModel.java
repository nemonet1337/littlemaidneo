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
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.CubeDeformation;

public class LMMultiModel<S extends MultiModelRenderState> extends LMModel<S> implements HeadedModel {
    private LMModel<?> delegate;
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
    public ModelPart getSkinRoot() {
        return delegate != null ? delegate.getSkinRoot() : super.getSkinRoot();
    }

    @Override
    public ModelPart getInnerRoot() {
        return delegate != null ? delegate.getInnerRoot() : super.getInnerRoot();
    }

    @Override
    public ModelPart getOuterRoot() {
        return delegate != null ? delegate.getOuterRoot() : super.getOuterRoot();
    }

    @Override
    public void setupAnim(S state) {
        this.delegate = state.skinModel;
        setupDelegate(state.skinModel, state);
        if (state.armorStates != null) {
            for (MultiModelRenderState.ArmorRenderState ars : state.armorStates) {
                if (ars == null || !ars.visible()) continue;
                setupDelegate(ars.innerModel(), state);
                setupDelegate(ars.outerModel(), state);
            }
        }
    }

    @SuppressWarnings("unchecked")
    private void setupDelegate(LMModel<?> model, S state) {
        if (model != null) ((LMModel<MultiModelRenderState>) model).setupAnim(state);
    }

    @Override
    public ModelPart getHead() {
        if (this.delegate != null) {
            ModelPart head = findHead(this.delegate.getSkinRoot());
            if (head != null) return head;
        }
        return modelPart;
    }

    private static ModelPart findHead(ModelPart root) {
        ModelPart mainFrame = LMModel.getChildSafe(root, "main_frame");
        ModelPart base = mainFrame != null ? mainFrame : root;
        ModelPart torso = LMModel.getChildSafe(base, "biped_torso");
        ModelPart neck = torso != null ? LMModel.getChildSafe(torso, "biped_neck") : LMModel.getChildSafe(base, "biped_neck");
        ModelPart head = neck != null ? LMModel.getChildSafe(neck, "biped_head") : LMModel.getChildSafe(base, "biped_head");
        return head != null ? head : mainFrame;
    }
}
