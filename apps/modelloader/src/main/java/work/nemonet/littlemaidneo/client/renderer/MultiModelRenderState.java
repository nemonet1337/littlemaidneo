package work.nemonet.littlemaidneo.client.renderer;

import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import work.nemonet.littlemaidneo.entity.compound.IHasMultiModel;
import work.nemonet.littlemaidneo.maidmodel.IModelCaps;
import work.nemonet.littlemaidneo.multimodel.IMultiModel;
import work.nemonet.littlemaidneo.resource.util.ArmorSets;
import work.nemonet.littlemaidneo.maidmodel.LMModel;

public class MultiModelRenderState extends LivingEntityRenderState {
    public IHasMultiModel multiModel;
    public LivingEntity entity;
    public HumanoidArm mainArm = HumanoidArm.RIGHT;
    public ItemStack mainHandItem = ItemStack.EMPTY;
    public ItemStack offHandItem = ItemStack.EMPTY;

    public IMultiModel skinModel;
    public Identifier skinTexture;
    public Identifier skinTextureLight;

    public final ArmorSets<IMultiModel> innerModels = new ArmorSets<>();
    public final ArmorSets<IMultiModel> outerModels = new ArmorSets<>();
    public final ArmorSets<Identifier> innerTextures = new ArmorSets<>();
    public final ArmorSets<Identifier> innerTexturesLight = new ArmorSets<>();
    public final ArmorSets<Identifier> outerTextures = new ArmorSets<>();
    public final ArmorSets<Identifier> outerTexturesLight = new ArmorSets<>();
    public final ArmorSets<Boolean> armorsVisible = new ArmorSets<>();
    public final ArmorSets<Boolean> armorsGlint = new ArmorSets<>();

    public IModelCaps caps;

    public LMModel<?> skinModelNew;
    public Identifier skinTextureNew;
    public Identifier skinTextureLightNew;

    public record ArmorRenderState(
        LMModel<?> innerModel, LMModel<?> outerModel,
        Identifier innerTexture, Identifier innerLightTexture,
        Identifier outerTexture, Identifier outerLightTexture,
        boolean visible, boolean glint
    ) {}

    public ArmorRenderState[] armorStates = new ArmorRenderState[4];
}
