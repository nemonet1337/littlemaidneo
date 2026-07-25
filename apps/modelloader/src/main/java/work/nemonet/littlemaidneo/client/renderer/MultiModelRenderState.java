package work.nemonet.littlemaidneo.client.renderer;

import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import work.nemonet.littlemaidneo.entity.compound.IHasMultiModel;
import work.nemonet.littlemaidneo.maidmodel.LMModel;
import work.nemonet.littlemaidneo.resource.util.ArmorSets;

public class MultiModelRenderState extends LivingEntityRenderState {
    public IHasMultiModel multiModel;
    public LivingEntity entity;
    public HumanoidArm mainArm = HumanoidArm.RIGHT;
    public ItemStack mainHandItem = ItemStack.EMPTY;
    public ItemStack offHandItem = ItemStack.EMPTY;

    public LMModel<?> skinModel;
    public Identifier skinTexture;
    public Identifier skinTextureLight;

    public final ArmorSets<LMModel<?>> innerModels = new ArmorSets<>();
    public final ArmorSets<LMModel<?>> outerModels = new ArmorSets<>();
    public final ArmorSets<Identifier> innerTextures = new ArmorSets<>();
    public final ArmorSets<Identifier> innerTexturesLight = new ArmorSets<>();
    public final ArmorSets<Identifier> outerTextures = new ArmorSets<>();
    public final ArmorSets<Identifier> outerTexturesLight = new ArmorSets<>();
    public final ArmorSets<Boolean> armorsVisible = new ArmorSets<>();
    public final ArmorSets<Boolean> armorsGlint = new ArmorSets<>();

    // メイド特化ステート (旧 caps グループC)
    public float interestedAngle;
    public boolean isBegging;
    public boolean isFreedomMode;
    public boolean isTracerMode;
    public boolean isPlayingSnow;
    public boolean isWorking;
    public boolean isPlanter;
    public boolean isOverdrive;
    public String activeJobName;

    // 共通ステート (旧 caps グループA/B)
    public boolean isWait;
    public boolean isContract;
    public boolean isBloodSuck;
    public boolean isHoldingClock;
    public boolean isAimingBow;
    public float roll;
    public float leaningPitch;
    public boolean isFallFlying;
    public boolean isSwimming;
    public boolean isBlocking;
    public boolean isLeashed;
    public float swingProgressRight;
    public float swingProgressLeft;

    public record ArmorRenderState(
        LMModel<?> innerModel, LMModel<?> outerModel,
        Identifier innerTexture, Identifier innerLightTexture,
        Identifier outerTexture, Identifier outerLightTexture,
        boolean visible, boolean glint
    ) {}

    public ArmorRenderState[] armorStates = new ArmorRenderState[4];
}
