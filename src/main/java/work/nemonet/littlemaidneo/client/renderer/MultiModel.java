package work.nemonet.littlemaidneo.client.renderer;

import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelPart;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import work.nemonet.littlemaidneo.entity.compound.IHasMultiModel;

import java.util.List;
import java.util.Map;

@OnlyIn(Dist.CLIENT)
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
            model.animateModel(mm.getCaps(), state.walkAnimationPos, state.walkAnimationSpeed, state.partialTick);
            model.setAngles(mm.getCaps(), state.walkAnimationPos, state.walkAnimationSpeed,
                    state.ageInTicks, state.yRot, state.xRot);
        });
    }
}
