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
import work.nemonet.littlemaidneo.entity.compound.MultiModelCompound;
import work.nemonet.littlemaidneo.entity.compound.SoundPlayableCompound;
import work.nemonet.littlemaidneo.resource.holder.TextureHolder;
import work.nemonet.littlemaidneo.resource.manager.LMTextureManager;

import java.util.function.BiConsumer;

import work.nemonet.littlemaidneo.common.MultiModelHolder;
import work.nemonet.littlemaidneo.common.SoundHolder;

public class MultiModelEntity extends PathfinderMob implements MultiModelHolder, SoundHolder {

    // モデル選択画面はクライアント側 (mods モジュール) の責務。
    // モジュール依存を mods -> modelloader の一方向に保つため、画面オープン処理はフックとして注入する。
    private static BiConsumer<Level, MultiModelEntity> modelSelectScreenOpener = (level, entity) -> {};

    public static void setModelSelectScreenOpener(BiConsumer<Level, MultiModelEntity> opener) {
        modelSelectScreenOpener = opener;
    }

    private final MultiModelCompound multiModel;
    private final SoundPlayableCompound soundPlayer;

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
            modelSelectScreenOpener.accept(this.level(), this);
            return InteractionResult.SUCCESS;
        }
        return super.mobInteract(player, hand);
    }

    @Override
    public MultiModelCompound getMultiModel() {
        return multiModel;
    }

    @Override
    public SoundPlayableCompound getSoundPlayer() {
        return soundPlayer;
    }
}
