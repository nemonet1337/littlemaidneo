package work.nemonet.littlemaidneo.client.renderer;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import work.nemonet.littlemaidneo.entity.LittleMaidEntity;

@OnlyIn(Dist.CLIENT)
public class MaidRenderState extends MultiModelRenderState {
    public LittleMaidEntity maidEntity;
}
