package work.nemonet.littlemaidneo.client.renderer;

import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelPart;
import work.nemonet.littlemaidneo.entity.compound.IHasMultiModel;
import work.nemonet.littlemaidneo.maidmodel.LMModel;

import java.util.List;
import java.util.Map;

public class MultiModel<S extends MultiModelRenderState> extends EntityModel<S> {

    public MultiModel() {
        super(new ModelPart(List.of(), Map.of()), MultiModelRenderLayer::getDefault);
    }

    @Override
    public void setupAnim(S state) {
        super.setupAnim(state);
        if (state.multiModel == null) return;
        IHasMultiModel mm = state.multiModel;
        mm.getModel(IHasMultiModel.Layer.SKIN, IHasMultiModel.Part.HEAD).ifPresent(model -> {
            if (model instanceof LMModel) {
                @SuppressWarnings("unchecked")
                LMModel<S> lmModel = (LMModel<S>) model;
                lmModel.setupAnim(state);
            } else {
                model.animateModel(mm.getCaps(), state.walkAnimationPos, state.walkAnimationSpeed, state.partialTick);
                model.setAngles(mm.getCaps(), state.walkAnimationPos, state.walkAnimationSpeed,
                        state.ageInTicks, state.yRot, state.xRot);
            }
        });
    }
}
