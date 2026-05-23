package net.sistr.littlemaidmodelloader.entity;

import net.minecraft.client.Minecraft;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.sistr.littlemaidmodelloader.client.screen.ModelSelectScreen;
import net.sistr.littlemaidmodelloader.entity.compound.IHasMultiModel;
import net.sistr.littlemaidmodelloader.entity.compound.MultiModelCompound;
import net.sistr.littlemaidmodelloader.entity.compound.SoundPlayable;
import net.sistr.littlemaidmodelloader.entity.compound.SoundPlayableCompound;
import net.sistr.littlemaidmodelloader.resource.holder.TextureHolder;
import net.sistr.littlemaidmodelloader.resource.holder.ConfigHolder;
import net.sistr.littlemaidmodelloader.resource.manager.LMTextureManager;
import net.sistr.littlemaidmodelloader.resource.util.TextureColors;
import org.jetbrains.annotations.Nullable;

// 1.21.1移植: YarnマッピングからMojangマッピングへ変更
// - PathAwareEntity → PathfinderMob
// - SwimGoal → FloatGoal
// - LookAtEntityGoal → LookAtPlayerGoal
// - LookAroundGoal → RandomLookGoal（未使用のため削除）
// - EntityAttributes → Attributes
// - DefaultAttributeContainer → AttributeSupplier
// - NbtCompound → CompoundTag
// - World → Level
// - Hand → InteractionHand
// - ActionResult → InteractionResult
// - getWorld() → level()
// - isClient() → isClientSide
// - interactMob() → mobInteract()
public class MultiModelEntity extends PathfinderMob implements IHasMultiModel, SoundPlayable {

    private MultiModelCompound multiModel;
    private SoundPlayableCompound soundPlayer;

    public MultiModelEntity(EntityType<MultiModelEntity> type, Level level) {
        super(type, level);
        LMTextureManager textureManager = LMTextureManager.INSTANCE;
        TextureHolder defaultTexture = textureManager.getTexture("default_0").orElseThrow();
        multiModel = new MultiModelCompound(this, defaultTexture, defaultTexture);
        soundPlayer = new SoundPlayableCompound(this, () -> multiModel.getTextureHolder(Layer.SKIN, Part.HEAD).getTextureName());
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(7, new LookAtPlayerGoal(this, Player.class, 6.0F));
        this.goalSelector.addGoal(8, new LookAtPlayerGoal(this, PathfinderMob.class, 6.0F));
    }

    // 1.21.1: DefaultAttributeContainer.Builder → AttributeSupplier.Builder
    public static AttributeSupplier.Builder createAttributes() {
        return PathfinderMob.createMobAttributes()
                .add(Attributes.MOVEMENT_SPEED, 0.3D)
                .add(Attributes.FOLLOW_RANGE, 16.0D)
                .add(Attributes.ATTACK_KNOCKBACK);
    }

    @Override
    public void addAdditionalSaveData(CompoundTag nbt) {
        super.addAdditionalSaveData(nbt);
        multiModel.writeToNbt(nbt);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag nbt) {
        super.readAdditionalSaveData(nbt);
        multiModel.readFromNbt(nbt);
    }

    // 1.21.1: interactMob → mobInteract, ActionResult → InteractionResult
    @Override
    public InteractionResult mobInteract(Player player, InteractionHand hand) {
        if (player.level().isClientSide) {
            Minecraft.getInstance().setScreen(new ModelSelectScreen(Component.translatable("screen.littlemaidmodelloader.model_select"), this.level(), this));
            return InteractionResult.SUCCESS;
        }
        return super.mobInteract(player, hand);
    }

    @Override
    public void setTextureHolder(TextureHolder textureHolder, Layer layer, Part part) {
        multiModel.setTextureHolder(textureHolder, layer, part);
    }

    @Override
    public TextureHolder getTextureHolder(Layer layer, Part part) {
        return multiModel.getTextureHolder(layer, part);
    }

    @Override
    public java.util.Optional<net.sistr.littlemaidmodelloader.multimodel.IMultiModel> getModel(Layer layer, Part part) {
        return multiModel.getModel(layer, part);
    }

    @Override
    public java.util.Optional<net.minecraft.resources.ResourceLocation> getTexture(Layer layer, Part part, boolean isLight) {
        return multiModel.getTexture(layer, part, isLight);
    }

    @Override
    public net.sistr.littlemaidmodelloader.maidmodel.IModelCaps getCaps() {
        return multiModel.getCaps();
    }

    @Override
    public boolean isArmorVisible(Part part) {
        return multiModel.isArmorVisible(part);
    }

    @Override
    public boolean isArmorGlint(Part part) {
        return multiModel.isArmorGlint(part);
    }

    @Override
    public boolean isAllowChangeTexture(@Nullable net.minecraft.world.entity.Entity changer, TextureHolder textureHolder, Layer layer, Part part) {
        return multiModel.isAllowChangeTexture(changer, textureHolder, layer, part);
    }

    public void setColor(TextureColors color) {
        multiModel.setColorMM(color);
    }

    public TextureColors getColor() {
        return multiModel.getColorMM();
    }

    public void setContract(boolean contract) {
        multiModel.setContractMM(contract);
    }

    @Override
    public boolean isContractMM() {
        return multiModel.isContractMM();
    }

    @Override
    public void setColorMM(TextureColors color) {
        multiModel.setColorMM(color);
    }

    @Override
    public TextureColors getColorMM() {
        return multiModel.getColorMM();
    }

    @Override
    public void setContractMM(boolean isContract) {
        multiModel.setContractMM(isContract);
    }

    @Override
    public void setConfigHolder(ConfigHolder configHolder) {
        soundPlayer.setConfigHolder(configHolder);
    }

    @Override
    public ConfigHolder getConfigHolder() {
        return soundPlayer.getConfigHolder();
    }

    @Override
    public void play(String soundName) {
        soundPlayer.play(soundName);
    }

}
