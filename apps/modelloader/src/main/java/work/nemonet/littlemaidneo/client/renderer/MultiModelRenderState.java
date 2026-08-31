package work.nemonet.littlemaidneo.client.renderer;

import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import work.nemonet.littlemaidneo.entity.compound.IHasMultiModel;
import work.nemonet.littlemaidneo.entity.compound.MultiModelView;
import work.nemonet.littlemaidneo.maidmodel.LMModel;
import work.nemonet.littlemaidneo.resource.util.ArmorSets;

public class MultiModelRenderState extends LivingEntityRenderState {
    public MultiModelView multiModel;
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

    /**
     * スキン／防具のモデルとテクスチャを {@code view} からこの state へコピーする。
     */
    public void fillFrom(LivingEntity entity, MultiModelView view, float partialTick) {
        this.multiModel = view;
        this.entity = entity;
        this.mainArm = entity.getMainArm();
        this.mainHandItem = entity.getMainHandItem();
        this.offHandItem = entity.getOffhandItem();
        this.walkAnimationPos = entity.walkAnimation.position(partialTick);
        this.walkAnimationSpeed = entity.walkAnimation.speed(partialTick);

        this.skinModel = view.getModel(IHasMultiModel.Layer.SKIN, IHasMultiModel.Part.HEAD).orElse(null);
        this.skinTexture = view.getTexture(IHasMultiModel.Layer.SKIN, IHasMultiModel.Part.HEAD, false).orElse(null);
        this.skinTextureLight = view.getTexture(IHasMultiModel.Layer.SKIN, IHasMultiModel.Part.HEAD, true).orElse(null);

        this.armorsVisible.clear();
        this.armorsGlint.clear();
        this.innerModels.clear();
        this.outerModels.clear();
        this.innerTextures.clear();
        this.innerTexturesLight.clear();
        this.outerTextures.clear();
        this.outerTexturesLight.clear();

        for (IHasMultiModel.Part part : IHasMultiModel.Part.values()) {
            boolean visible = view.isArmorVisible(part);
            boolean glint = view.isArmorGlint(part);
            this.armorsVisible.setArmor(visible, part);
            this.armorsGlint.setArmor(glint, part);

            LMModel<?> innerModel = view.getModel(IHasMultiModel.Layer.INNER, part).orElse(null);
            LMModel<?> outerModel = view.getModel(IHasMultiModel.Layer.OUTER, part).orElse(null);
            this.innerModels.setArmor(innerModel, part);
            this.outerModels.setArmor(outerModel, part);

            Identifier innerTex = view.getTexture(IHasMultiModel.Layer.INNER, part, false).orElse(null);
            Identifier innerLight = view.getTexture(IHasMultiModel.Layer.INNER, part, true).orElse(null);
            Identifier outerTex = view.getTexture(IHasMultiModel.Layer.OUTER, part, false).orElse(null);
            Identifier outerLight = view.getTexture(IHasMultiModel.Layer.OUTER, part, true).orElse(null);
            this.innerTextures.setArmor(innerTex, part);
            this.innerTexturesLight.setArmor(innerLight, part);
            this.outerTextures.setArmor(outerTex, part);
            this.outerTexturesLight.setArmor(outerLight, part);

            this.armorStates[part.getIndex()] = new ArmorRenderState(
                    innerModel, outerModel,
                    innerTex, innerLight, outerTex, outerLight,
                    visible, glint);
        }
    }
}
