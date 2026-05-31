package work.nemonet.littlemaidneo.entity;

import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
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
import work.nemonet.littlemaidneo.entity.compound.IHasMultiModel;
import work.nemonet.littlemaidneo.entity.compound.MultiModelCompound;
import work.nemonet.littlemaidneo.entity.compound.SoundPlayable;
import work.nemonet.littlemaidneo.entity.compound.SoundPlayableCompound;
import work.nemonet.littlemaidneo.maidmodel.IModelCaps;
import work.nemonet.littlemaidneo.multimodel.IMultiModel;
import work.nemonet.littlemaidneo.resource.holder.ConfigHolder;
import work.nemonet.littlemaidneo.resource.holder.TextureHolder;
import work.nemonet.littlemaidneo.resource.manager.LMTextureManager;
import work.nemonet.littlemaidneo.resource.util.TextureColors;

import net.minecraft.resources.Identifier;
import org.jetbrains.annotations.Nullable;
import java.util.Optional;

public class MultiModelEntity extends PathfinderMob implements IHasMultiModel, SoundPlayable {

    private MultiModelCompound multiModel;
    private SoundPlayableCompound soundPlayer;

    public MultiModelEntity(EntityType<MultiModelEntity> type, Level level) {
        super(type, level);
        LMTextureManager textureManager = LMTextureManager.INSTANCE;
        TextureHolder defaultTexture = textureManager.getTexture("default_0").orElseThrow();
        multiModel = new MultiModelCompound(this, defaultTexture, defaultTexture);
        soundPlayer = new SoundPlayableCompound(this,
                () -> multiModel.getTextureHolder(Layer.SKIN, Part.HEAD).getTextureName());
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(7, new LookAtPlayerGoal(this, Player.class, 6.0F));
        this.goalSelector.addGoal(8, new LookAtPlayerGoal(this, PathfinderMob.class, 6.0F));
    }

    public static AttributeSupplier.Builder createAttributes() {
        return PathfinderMob.createMobAttributes()
                .add(Attributes.MOVEMENT_SPEED, 0.3D)
                .add(Attributes.FOLLOW_RANGE, 16.0D)
                .add(Attributes.ATTACK_KNOCKBACK);
    }

    @Override
    public void addAdditionalSaveData(ValueOutput output) {
        super.addAdditionalSaveData(output);
        multiModel.writeToNbt(output);
    }

    @Override
    public void readAdditionalSaveData(ValueInput input) {
        super.readAdditionalSaveData(input);
        multiModel.readFromNbt(input);
    }

    @Override
    public InteractionResult mobInteract(Player player, InteractionHand hand) {
        if (player.level().isClientSide()) {
            work.nemonet.littlemaidneo.client.util.ClientScreenHelper.openModelSelectScreen(this.level(), this);
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
    public Optional<IMultiModel> getModel(Layer layer, Part part) {
        return multiModel.getModel(layer, part);
    }

    @Override
    public Optional<Identifier> getTexture(Layer layer, Part part, boolean isLight) {
        return multiModel.getTexture(layer, part, isLight);
    }

    @Override
    public IModelCaps getCaps() {
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
    public boolean isAllowChangeTexture(@Nullable net.minecraft.world.entity.Entity changer,
                                         TextureHolder textureHolder, Layer layer, Part part) {
        return multiModel.isAllowChangeTexture(changer, textureHolder, layer, part);
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
