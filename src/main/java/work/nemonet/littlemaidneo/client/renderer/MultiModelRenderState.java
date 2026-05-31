package work.nemonet.littlemaidneo.client.renderer;

import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import work.nemonet.littlemaidneo.entity.compound.IHasMultiModel;

@OnlyIn(Dist.CLIENT)
public class MultiModelRenderState extends LivingEntityRenderState {
    public IHasMultiModel multiModel;
    public LivingEntity entity;
    public HumanoidArm mainArm = HumanoidArm.RIGHT;
    public ItemStack mainHandItem = ItemStack.EMPTY;
    public ItemStack offHandItem = ItemStack.EMPTY;
}
