package work.nemonet.littlemaidneo.client.renderer;

import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import work.nemonet.littlemaidneo.entity.compound.IHasMultiModel;
import work.nemonet.littlemaidneo.multimodel.IMultiModel;
import work.nemonet.littlemaidneo.maidmodel.IModelCaps;
import work.nemonet.littlemaidneo.resource.util.ArmorSets;

public class MultiModelRenderState extends LivingEntityRenderState {
    public IHasMultiModel multiModel;
    public LivingEntity entity;
    public HumanoidArm mainArm = HumanoidArm.RIGHT;
    public ItemStack mainHandItem = ItemStack.EMPTY;
    public ItemStack offHandItem = ItemStack.EMPTY;

    // スキン情報
    public IMultiModel skinModel;
    public net.minecraft.resources.Identifier skinTexture;
    public net.minecraft.resources.Identifier skinTextureLight;

    // アーマー情報
    public final ArmorSets<IMultiModel> innerModels = new ArmorSets<>();
    public final ArmorSets<IMultiModel> outerModels = new ArmorSets<>();
    public final ArmorSets<net.minecraft.resources.Identifier> innerTextures = new ArmorSets<>();
    public final ArmorSets<net.minecraft.resources.Identifier> innerTexturesLight = new ArmorSets<>();
    public final ArmorSets<net.minecraft.resources.Identifier> outerTextures = new ArmorSets<>();
    public final ArmorSets<net.minecraft.resources.Identifier> outerTexturesLight = new ArmorSets<>();
    public final ArmorSets<Boolean> armorsVisible = new ArmorSets<>();
    public final ArmorSets<Boolean> armorsGlint = new ArmorSets<>();

    public IModelCaps caps;
}
