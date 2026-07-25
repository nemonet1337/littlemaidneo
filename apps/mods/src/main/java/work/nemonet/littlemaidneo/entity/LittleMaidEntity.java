package work.nemonet.littlemaidneo.entity;

import com.google.common.collect.Lists;
import java.util.*;
import java.util.function.Predicate;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.util.ProblemReporter;
import net.minecraft.util.Util;
import net.minecraft.world.*;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.navigation.GroundPathNavigation;
import net.minecraft.world.entity.monster.CrossbowAttackMob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.entity.IEntityWithComplexSpawn;
import org.jetbrains.annotations.Nullable;
import work.nemonet.littlemaidneo.advancement.criterion.LMNCriteria;
import work.nemonet.littlemaidneo.common.MultiModelHolder;
import work.nemonet.littlemaidneo.common.SoundHolder;
import work.nemonet.littlemaidneo.config.LMNConfig;
import work.nemonet.littlemaidneo.entity.compound.MultiModelCompound;
import work.nemonet.littlemaidneo.entity.compound.SoundPlayableCompound;
import work.nemonet.littlemaidneo.entity.targeting.TargetIdentifier;
import work.nemonet.littlemaidneo.entity.targeting.TargetTagManager;
import work.nemonet.littlemaidneo.entity.targeting.TargetTagManagerImpl;
import work.nemonet.littlemaidneo.entity.targeting.TargetingSystem;
import work.nemonet.littlemaidneo.entity.util.*;
import work.nemonet.littlemaidneo.maidmodel.LMModel;
import work.nemonet.littlemaidneo.network.NetworkHandler;
import work.nemonet.littlemaidneo.resource.holder.TextureHolder;
import work.nemonet.littlemaidneo.resource.manager.LMConfigManager;
import work.nemonet.littlemaidneo.resource.manager.LMModelManager;
import work.nemonet.littlemaidneo.resource.manager.LMTextureManager;
import work.nemonet.littlemaidneo.resource.util.LMSounds;
import work.nemonet.littlemaidneo.resource.util.TextureColors;
import work.nemonet.littlemaidneo.setup.ModRegistration;
import work.nemonet.littlemaidneo.tags.LMTags;
import work.nemonet.littlemaidneo.util.LMCollidable;
import work.nemonet.littlemaidneo.util.ReachAttributeUtil;
import work.nemonet.littlemaidneo.entity.soul.MaidSoulData;
import net.minecraft.world.entity.ai.Brain;
import net.minecraft.world.entity.ai.ActivityData;
import net.minecraft.world.entity.schedule.Activity;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.behavior.BehaviorControl;
import com.google.common.collect.ImmutableList;

//????????????
public class LittleMaidEntity
        extends TamableAnimal
        implements
        IEntityWithComplexSpawn,
        HasInventory,
        Contractable,
        MultiModelHolder,
        SoundHolder,
        HasMaidMode,
        CrossbowAttackMob,
        SalaryBoxPosListener,
        TargetTagManager {

    @Override
    public boolean isFood(ItemStack stack) {
        return (stack.is(Items.WHEAT_SEEDS) ||
                stack.is(Items.BEETROOT_SEEDS) ||
                stack.is(Items.MELON_SEEDS) ||
                stack.is(Items.PUMPKIN_SEEDS));
    }

    // LMM_FLAGS??index
    private static final int WAIT_INDEX = 0;
    private static final int AIMING_INDEX = 1;
    private static final int BEGGING_INDEX = 2;
    private static final int BLOOD_SUCK_INDEX = 3;
    private static final int STRIKE_INDEX = 4;
    private static final int PLAYING_SNOW_INDEX = 5;
    private static final EntityDataAccessor<Byte> LMM_FLAGS = SynchedEntityData.defineId(
            LittleMaidEntity.class,
            EntityDataSerializers.BYTE);
    private static final EntityDataAccessor<Byte> MOVING_MODE = SynchedEntityData.defineId(
            LittleMaidEntity.class,
            EntityDataSerializers.BYTE);
    private static final EntityDataAccessor<String> MODE_NAME = SynchedEntityData.defineId(
            LittleMaidEntity.class,
            EntityDataSerializers.STRING);
    private static final EntityDataAccessor<Boolean> CHARGING = SynchedEntityData.defineId(
            LittleMaidEntity.class,
            EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> ACCELERATE = SynchedEntityData.defineId(
            LittleMaidEntity.class,
            EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Byte> MASTER_STANCE = SynchedEntityData.defineId(
            LittleMaidEntity.class,
            EntityDataSerializers.BYTE);
    private static final EntityDataAccessor<Integer> CONTRACT_TIME = SynchedEntityData.defineId(
            LittleMaidEntity.class,
            EntityDataSerializers.INT);
    // ?????????????????????????????????????????????????????????????????????
    // LMInteractionHandler ???????????????????????????????
    static final int EXPERIENCE_BOTTLE_COST = 7;

    // ????s
    public final LMHasInventory littleMaidInventory = new LMHasInventory();
    public final LMItemContractable<LittleMaidEntity> itemContractable = new LMItemContractable<>(
            this,
            () -> getConfig().contract.consumeSalaryInterval,
            () -> getConfig().contract.unpaidDaysLimit,
            (ItemStack stack) -> stack.is(LMTags.Items.MAIDS_SALARY));
    public work.nemonet.littlemaidneo.entity.ai.behavior.MaidCombatBehavior combatBehavior;
    public work.nemonet.littlemaidneo.entity.ai.behavior.MaidCookingBehavior cookingBehavior;
    public work.nemonet.littlemaidneo.entity.ai.behavior.MaidHealerBehavior healerBehavior;
    public work.nemonet.littlemaidneo.entity.ai.behavior.MaidPharmcistBehavior pharmcistBehavior;
    public work.nemonet.littlemaidneo.entity.ai.behavior.MaidRipperBehavior ripperBehavior;
    public work.nemonet.littlemaidneo.entity.ai.behavior.MaidTorcherBehavior torcherBehavior;

    public work.nemonet.littlemaidneo.entity.ai.behavior.MaidLookAroundBehavior lookAroundBehavior;
    public work.nemonet.littlemaidneo.entity.ai.behavior.MaidPanicBehavior panicBehavior;
    public work.nemonet.littlemaidneo.entity.ai.behavior.MaidAvoidBehavior avoidBehavior;

    public String getActiveJobName() {
        if (this.isStrike()) {
            return "none";
        }
        return this.getBrain().getMemory(ModRegistration.ACTIVE_JOB_NAME.get()).orElse("none");
    }

    public String getActiveBattleMode() {
        return this.getBrain().getMemory(ModRegistration.ACTIVE_BATTLE_MODE.get()).orElse("none");
    }
    public final MultiModelCompound multiModel;
    public final SoundPlayableCompound soundPlayer;
    private final LMScreenHandlerFactory screenFactory = new LMScreenHandlerFactory(this);
    private final TargetTagManager targetTagManager;

    @Override
    public MultiModelCompound getMultiModel() {
        return multiModel;
    }

    @Override
    public SoundPlayableCompound getSoundPlayer() {
        return soundPlayer;
    }

    public final Map<Mob, java.util.function.Predicate<Mob>> fleeEntities = new HashMap<>();

    @Nullable
    private BlockPos freedomPos;

    // ?????????
private float interestedAngle;
private float prevInterestedAngle;

    private int playSoundCool;
    private int idFactor;
    public int experiencePickUpDelay;
    private final MaidAcceleration acceleration = new MaidAcceleration(this);
    private boolean maidManagerRegistered;

    // ?????????????
    public LittleMaidEntity(EntityType<LittleMaidEntity> type, Level worldIn) {
        super(type, worldIn);
        this.moveControl = new FixedMoveControl(this);
        this.lookControl = new work.nemonet.littlemaidneo.entity.ai.MaidLookControl(this);
        getNavigation().setCanOpenDoors(true);
        if (this.getNavigation() instanceof GroundPathNavigation nav) {
            nav.setCanFloat(true);
        }
        this.setPathfindingMalus(net.minecraft.world.level.pathfinder.PathType.WATER, 0.0F);
        multiModel = new MultiModelCompound(
                this,
                LMTextureManager.INSTANCE.getTexture("Default").orElseThrow(() -> new IllegalStateException(
                        "??????????????????????????????")),
                LMTextureManager.INSTANCE.getTexture("Default").orElseThrow(() -> new IllegalStateException(
                        "??????????????????????????????")));
        soundPlayer = new SoundPlayableCompound(this,
                () -> multiModel.getTextureHolder(Layer.SKIN, Part.HEAD).getTextureName());
        initIdFactor();
        setRandomTexture();
        setRandomVoice();
        this.targetTagManager = new TargetTagManagerImpl(worldIn);
    }

    public static AttributeSupplier.Builder createLittleMaidAttributes() {
        AttributeSupplier.Builder builder = createMobAttributes()
                .add(Attributes.MOVEMENT_SPEED, 0.3D)
                .add(Attributes.ATTACK_DAMAGE)
                .add(Attributes.ATTACK_SPEED)
                .add(Attributes.LUCK)
                .add(Attributes.FOLLOW_RANGE, 48.0D);
        ReachAttributeUtil.addAttribute(builder);
        return builder;
    }

    // ???????????????: ?????????????????????????????????????? 8 ???????????
    // ??????????????????????????????????????????????????????????????????????hase 6?????????
    public static boolean isValidNaturalSpawn(
            LevelAccessor world,
            BlockPos pos) {
        return (world
                .getBlockState(pos.below())
                .isCollisionShapeFullBlock(world, pos) &&
                world.getRawBrightness(pos, 0) > 8);
    }

    public static boolean resurrectionMaid(
            ServerLevel world,
            BlockPos pos,
            Player player) {
        return MaidResurrection.resurrect(world, pos, player);
    }

    // ????????????????:
    //   - ???? (WALK_TARGET) ????????????? MoveToTargetSink??aidFollowOwner/Stare/Freedom ?? WALK_TARGET ??????????????????
    //   - ????????? GoalSelector ?? LookAtPlayerGoal / RandomLookAroundGoal ????????aidStareBehavior ??
    //     getLookControl().setLookAt(...) ??????????????????????????? MaidLookControl ????????????????????
    //   - ??????? LookAtTargetSink ?? LOOK_TARGET ???????????????????? Mod ???? LOOK_TARGET ?????????
    //     ?????????????????????? no-op ???????????????????????????????? Behavior ???????????????
    private static final Brain.Provider<LittleMaidEntity> BRAIN_PROVIDER = Brain.provider(
            ImmutableList.of(
                    ModRegistration.IS_WAITING.get(),
                    ModRegistration.OWNER.get(),
                    ModRegistration.ACTIVE_JOB_NAME.get(),
                    ModRegistration.ACTIVE_BATTLE_MODE.get(),
                    MemoryModuleType.WALK_TARGET,
                    MemoryModuleType.CANT_REACH_WALK_TARGET_SINCE,
                    MemoryModuleType.PATH,
                    MemoryModuleType.DOORS_TO_CLOSE
            ),
            ImmutableList.of(
                    ModRegistration.LITTLE_MAID_SENSOR.get()
            ),
            entity -> ImmutableList.of(
                    ActivityData.create(Activity.CORE, 0, ImmutableList.<BehaviorControl<? super LittleMaidEntity>>of(
                            new work.nemonet.littlemaidneo.entity.ai.behavior.MaidSwim(0.8f),
                            net.minecraft.world.entity.ai.behavior.InteractWithDoor.create(),
                            entity.avoidBehavior,
                            entity.panicBehavior,
                            new work.nemonet.littlemaidneo.entity.ai.behavior.MaidTeleportBehavior(),
                            new work.nemonet.littlemaidneo.entity.ai.behavior.MaidWaitBehavior(),
                            new work.nemonet.littlemaidneo.entity.ai.behavior.MaidHealSelfBehavior(),
                            new work.nemonet.littlemaidneo.entity.ai.behavior.MaidTargetBehavior(),
                            entity.combatBehavior,
                            entity.cookingBehavior,
                            entity.healerBehavior,
                            entity.pharmcistBehavior,
                            entity.ripperBehavior,
                            entity.torcherBehavior,
                            new work.nemonet.littlemaidneo.entity.ai.behavior.MaidCollectSalaryBehavior(),
                            new work.nemonet.littlemaidneo.entity.ai.behavior.MaidStoreItemBehavior(),
                            new work.nemonet.littlemaidneo.entity.ai.behavior.MaidMoveToDropItemBehavior(),
                            new work.nemonet.littlemaidneo.entity.ai.behavior.MaidFollowOwnerBehavior(),
                            new work.nemonet.littlemaidneo.entity.ai.behavior.MaidFreedomBehavior(),
                            new work.nemonet.littlemaidneo.entity.ai.behavior.MaidTraceBehavior(),
                            new work.nemonet.littlemaidneo.entity.ai.behavior.MaidPlaySnowBehavior(),
                            entity.lookAroundBehavior,
                            new work.nemonet.littlemaidneo.entity.ai.behavior.MaidStareBehavior(),
                            new net.minecraft.world.entity.ai.behavior.MoveToTargetSink()
                    ))
            )
    );

    private void initBehaviors() {
        if (this.combatBehavior == null) {
            this.combatBehavior = new work.nemonet.littlemaidneo.entity.ai.behavior.MaidCombatBehavior();
            this.cookingBehavior = new work.nemonet.littlemaidneo.entity.ai.behavior.MaidCookingBehavior();
            this.healerBehavior = new work.nemonet.littlemaidneo.entity.ai.behavior.MaidHealerBehavior();
            this.pharmcistBehavior = new work.nemonet.littlemaidneo.entity.ai.behavior.MaidPharmcistBehavior();
            this.ripperBehavior = new work.nemonet.littlemaidneo.entity.ai.behavior.MaidRipperBehavior();
            this.torcherBehavior = new work.nemonet.littlemaidneo.entity.ai.behavior.MaidTorcherBehavior();
            this.lookAroundBehavior = new work.nemonet.littlemaidneo.entity.ai.behavior.MaidLookAroundBehavior();
            this.panicBehavior = new work.nemonet.littlemaidneo.entity.ai.behavior.MaidPanicBehavior(1.5f);
            this.avoidBehavior = new work.nemonet.littlemaidneo.entity.ai.behavior.MaidAvoidBehavior();
        }
    }

    @Override
    protected Brain<?> makeBrain(Brain.Packed packedBrain) {
        initBehaviors();
        return BRAIN_PROVIDER.makeBrain(this, packedBrain);
    }

    @SuppressWarnings("unchecked")
    @Override
    public Brain<LittleMaidEntity> getBrain() {
        return (Brain<LittleMaidEntity>) super.getBrain();
    }

    // ??????????????

    @Override
    protected void registerGoals() {
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(LMM_FLAGS, (byte) 0);
        builder.define(MOVING_MODE, (byte) 0);
        builder.define(MODE_NAME, "");
        builder.define(CHARGING, false);
        builder.define(ACCELERATE, false);
        builder.define(MASTER_STANCE, (byte) 0);
        builder.define(CONTRACT_TIME, 0);
    }



    // ??????????

    @Override
    public void addAdditionalSaveData(ValueOutput output) {
        super.addAdditionalSaveData(output);
        output.putByte("maidVersion", (byte) 2);

        writeInventory(output);
        output.putInt("XpTotal", this.xpReward);
        if (TameableUtil.getTameOwnerUuid(this).isPresent()) {
            output.putBoolean("Wait", TameableUtil.isWait(this));
            output.store("MaidMode", MaidMode.CODEC, this.getMaidMode());
            writeContractable(output);
            writeModeData(output);
            output.putBoolean("isBloodSuck", isBloodSuck());
            BlockPos fp = freedomPos;
            if (this.getMaidMode() == MaidMode.FREEDOM && fp != null) {
                output.store("FreedomPos", BlockPos.CODEC, fp);
            }
            writeTargetTags(output);
        }
        this.multiModel.writeToNbt(output);
        output.putString("SoundConfigName", getConfigHolder().getName());

        acceleration.save(output);
    }

    @Override
    public void readAdditionalSaveData(ValueInput input) {
        super.readAdditionalSaveData(input);
        int maidVersion = input.getByteOr("maidVersion", (byte) 0) & 255;

        if (maidVersion <= 1) {
            var defaultAttributes = createLittleMaidAttributes().build();
            var entityAttributes = new net.minecraft.core.Holder[] {
                    Attributes.MOVEMENT_SPEED,
                    Attributes.FOLLOW_RANGE,
            };
            for (var attribute : entityAttributes) {
                @SuppressWarnings("unchecked")
                var holder = (net.minecraft.core.Holder<Attribute>) attribute;
                var customInstance = this.getAttributes().getInstance(holder);
                if (customInstance != null) {
                    customInstance.setBaseValue(
                            defaultAttributes.getBaseValue(holder));
                }
            }
        }

        readInventory(input);
        this.xpReward = input.getIntOr("XpTotal", 0);

        if (TameableUtil.hasTameOwner(this)) {
            TameableUtil.setWait(this, input.getBooleanOr("Wait", false));
            setMaidMode(
                    input.read("MaidMode", MaidMode.CODEC).orElse(MaidMode.FREEDOM));
            readContractable(input);
            readModeData(input);
            setBloodSuck(input.getBooleanOr("isBloodSuck", false));
            if (this.getMaidMode() == MaidMode.FREEDOM) {
                freedomPos = input
                        .read("FreedomPos", BlockPos.CODEC)
                        .orElse(null);
            }
            readTargetTags(input);
        }
        this.multiModel.readFromNbt(input);
        this.refreshDimensions();
        input
                .getString("SoundConfigName").flatMap(LMConfigManager.INSTANCE::getConfig).ifPresent(this::setConfigHolder);

        acceleration.load(input);
    }

    // idFactor ?? initIdFactor()??????????????????? setUUID() ????? UUID ????????????
    // ?????????????????????????????? idFactor ?????????????????????????????????????????????????
    public void setRandomTexture() {
        MaidRandomizer.setRandomTexture(this);
    }

    public void setRandomVoice() {
        MaidRandomizer.setRandomVoice(this);
    }

    // ??
    @Override
    public void writeSpawnData(RegistryFriendlyByteBuf buf) {
        // ?????
        buf.writeEnum(getColorMM());
        buf.writeBoolean(isContractMM());
        buf.writeUtf(getTextureHolder(Layer.SKIN, Part.HEAD).getTextureName());
        for (Part part : Part.values()) {
            buf.writeUtf(getTextureHolder(Layer.INNER, part).getTextureName());
            buf.writeUtf(getTextureHolder(Layer.OUTER, part).getTextureName());
        }
        // ????????
        buf.writeUtf(getConfigHolder().getName());
        // ??????????????????????????
        // ????????????????????????????????????
        ItemStack.OPTIONAL_STREAM_CODEC.encode(buf, getInventory().getItem(17));
        // architectury?????????PitchYaw????????????????????????
        buf.writeFloat(this.getXRot());
        buf.writeFloat(this.getYRot());
        acceleration.writeSpawnData(buf);
    }

    // ??
    @Override
    public void readSpawnData(RegistryFriendlyByteBuf buf) {
        // ?????
        // readString()??????????????????????????????????????????????
        setColorMM(buf.readEnum(TextureColors.class));
        setContractMM(buf.readBoolean());
        LMTextureManager textureManager = LMTextureManager.INSTANCE;
        textureManager
                .getTexture(buf.readUtf())
                .ifPresent(textureHolder -> setTextureHolder(textureHolder, Layer.SKIN, Part.HEAD));
        for (Part part : Part.values()) {
            textureManager
                    .getTexture(buf.readUtf())
                    .ifPresent(textureHolder -> setTextureHolder(textureHolder, Layer.INNER, part));
            textureManager
                    .getTexture(buf.readUtf())
                    .ifPresent(textureHolder -> setTextureHolder(textureHolder, Layer.OUTER, part));
        }
        // ????????
        LMConfigManager.INSTANCE.getConfig(buf.readUtf()).ifPresent(
                this::setConfigHolder);

        getInventory().setItem(17, ItemStack.OPTIONAL_STREAM_CODEC.decode(buf));
        this.setXRot(buf.readFloat());
        this.setYRot(buf.readFloat());
        acceleration.readSpawnData(buf);
    }

    @Override
    public void handleEntityEvent(byte status) {
        switch (status) {
            case 70 -> {
                // ?????
                spawnTamingParticles(true);
                play(LMSounds.GET_CAKE);
            }
            case 71 -> {
                // ???????
                spawnTamingParticles(true);
                play(LMSounds.RECONTRACT);
            }
            case 72 -> {
                // ??????????
                double maxInterval = getConfig().contract.consumeSalaryInterval;
                double remaining = Math.max(0, maxInterval - this.getContractTime());
                double ratio = remaining / maxInterval;
                float noteColor = (float)(ratio * 2.0);
                this.level().addParticle(
                        ParticleTypes.NOTE,
                        this.getX(),
                        this.getY() + this.getBbHeight(),
                        this.getZ(),
                        noteColor,
                        0,
                        0);
            }
            case 73 -> showFreedomParticle(); // toFreedom
            case 74 -> spawnTamingParticles(false); // toEscort
            case 75 -> showTracerParticle(); // toTracer
            case 76 -> showTransAmParticles(); // ????????????????????
            default -> super.handleEntityEvent(status);
        }
    }

    protected void showFreedomParticle() {
        MaidParticle.showFreedomParticle(this);
    }

    protected void showTracerParticle() {
        MaidParticle.showTracerParticle(this);
    }

    // ????????????

    @Override
    public void tick() {
        if (!this.level().isClientSide() && !this.maidManagerRegistered) {
            TameableUtil.getTameOwner(this)
                    .ifPresent(owner -> {
                        owner.getData(ModRegistration.MAID_MANAGER_ATTACHMENT.get()).registerMaid(this);
                        this.maidManagerRegistered = true;
                    });
        }
        int tickMultiple = getTickMultiple();
        for (int i = 0; i < tickMultiple; i++) {
            inTickMultiplePre();
            super.tick();
            inTickMultiplePost();
        }
    }
    protected void inTickMultiplePost() {
    }

    protected void inTickMultiplePre() {
        if (this.experiencePickUpDelay > 0) {
            --this.experiencePickUpDelay;
        }
        if (this.level().isClientSide()) {
            tickInterestedAngle();
        }
        playSoundCool = Math.max(0, playSoundCool - 1);
        decAccelerationTicks();

        if (this.onClimbable() && this.horizontalCollision) {
            this.setDeltaMovement(this.getDeltaMovement().add(0.0D, 0.2D, 0.0D));
        }
    }

    @Override
    public void aiStep() {
        updateSwingTime();
        super.aiStep();
    }

    @Override
    protected void customServerAiStep(ServerLevel serverLevel) {
        this.getBrain().tick(serverLevel, this);
        super.customServerAiStep(serverLevel);
        if (TameableUtil.hasTameOwner(this) ||
                getConfig().misc.canPickupItemByNoOwner) {
            pickupItem();
        }
        itemContractable.tick();
        work.nemonet.littlemaidneo.entity.util.MaidJobManager.tick(this);

        // ?????????
        if (this.tickCount % 40 == 0 && this.getHealth() < this.getMaxHealth()) {
            tryEatingFromInventory();
        }

        // ?????????????????????????
        if (TameableUtil.isWait(this) && this.isInWater()) {
            TameableUtil.setWait(this, false);
        }
    }

    protected void pickupItem() {
        if (!getConfig().misc.canPickupExperienceOrb &&
                !getConfig().misc.canPickupItem) {
            return;
        }
        if (this.getHealth() <= 0 || this.isSpectator()) {
            return;
        }
        var aabb = this.getBoundingBox().inflate(1.0, 0.5, 1.0);
        // LMCollidable（ItemEntity / ExperienceOrb の Mixin）だけをセクション走査の段階で絞り込む。
        // 無条件の getEntities は毎 tick 周囲の全エンティティを収集するため、多数のメイドさんがいると重い。
        var aroundItems = this.level().getEntities(this, aabb,
                e -> e instanceof LMCollidable && !e.isRemoved());
        var exps = Lists.<Entity>newArrayList();
        for (Entity entity : aroundItems) {
            if (entity instanceof ExperienceOrb) {
                if (getConfig().misc.canPickupExperienceOrb) {
                    exps.add(entity);
                }
                continue;
            }
            if (!getConfig().misc.canPickupItem) {
                continue;
            }
            ((LMCollidable) entity).onCollision_LM(this);
        }
        if (!exps.isEmpty()) {
            var collidable = ((LMCollidable) Util.getRandom(exps, this.random));
            if (collidable != null) {
                collidable.onCollision_LM(this);
            }
        }
    }

    @Override
    public boolean removeWhenFarAway(double distanceSquared) {
        return (getConfig().spawn.canDespawn &&
                TameableUtil.getTameOwnerUuid(this).isEmpty());
    }

    // canSpawn ?????????????????????????????????????????????????????(10.0)??
    // ???????????????????????????????????????????????????????????????hase 6????
    @Override
    public float getWalkTargetValue(BlockPos pos, LevelReader world) {
        return world
                .getBlockState(pos.below())
                .isCollisionShapeFullBlock(world, pos)
                        ? 10.0F
                        : world.getPathfindingCostFromLightLevels(pos);
    }

    @Override
    public boolean canAttack(LivingEntity target) {
        return super.canAttack(target) && !isFriend(target);
    }

    @Nullable
    @Override
    public AgeableMob getBreedOffspring(ServerLevel world, AgeableMob entity) {
        return null;
    }

    // ????????????????????????etMountedYOffset / getyOffset????????????
    // getPassengerRidingPosition / getVehicleAttachmentPoint ??????????

    public double getMountedYOffset() {
        return 0.35F;
    }

    /**
     * ???????????????
     */
    public double getRidingYOffset() {
        return 1.35F * 0.9F - getBbHeight();
    }

    @Override
    public Vec3 getPassengerRidingPosition(Entity passenger) {
        double yOffset = getMountedYOffset();
        return this.position().add(0.0, yOffset, 0.0);
    }

    @Override
    public Vec3 getVehicleAttachmentPoint(Entity vehicle) {
        Vec3 defaultPoint = super.getVehicleAttachmentPoint(vehicle);
        return new Vec3(defaultPoint.x, -getRidingYOffset(), defaultPoint.z);
    }

    // ??? EntityDimensions ????????????????????????????????????????????????????????
    // ???????????????????????????????????????????????????????????????????????????
    // ????????????????????????????????????????????????????????????????????????????
    @Override
    public EntityDimensions getDefaultDimensions(Pose pose) {
        float height = 1.35F;
        float width = 0.5F;
        float eyeHeight = height * 0.85F;
        EntityDimensions dimensions = EntityDimensions.scalable(width, height);
        dimensions = dimensions.scale(getAgeScale());
        dimensions = dimensions.withEyeHeight(eyeHeight * getAgeScale());
        return dimensions;
    }

    // 1.21.1: changeDimension is final, use afterChangingDimensions instead
    @Override
    public void restoreFrom(Entity entity) {
        super.restoreFrom(entity);
        // ?????????????????????????????????????????
        if (entity instanceof LittleMaidEntity oldMaid &&
                oldMaid.getMaidMode() == MaidMode.FREEDOM) {
            this.setFreedomPos(null);
        }
    }

    // ?????????????????????????????????????????????????
    // ????????????????????????? "LivingVoiceRate" ????????????????? B?????? 0.2????????????
    @Override
    public void playAmbientSound() {
        MaidVoice.playAmbientSound(this);
    }

    @Override
    public boolean doHurtTarget(ServerLevel serverLevel, Entity target) {
        return MaidCombat.doHurtTarget(this, serverLevel, target, super.doHurtTarget(serverLevel, target));
    }

    // ??????????????: ????/????/??od??????????????????????? ?? ?????????????????? ??
    // ????/????????????????????????????? ?? ???????? ?? ?????????????????????
    @Override
    public boolean hurtServer(
            ServerLevel serverLevel,
            DamageSource source,
            float amount) {
        return MaidCombat.hurtServer(this, serverLevel, source, amount, 0 < this.hurtTime, super::hurtServer);
    }

    public boolean isEmergency() {
        LMNConfig config = getConfig();
        // ?????????????????????????????????
        return (this.getHealth() / this.getMaxHealth() <= config.health.emergencyMaidHealthThreshold);
    }

    @Override
    public void setHealth(float health) {
        LMNConfig config = getConfig();
        if (config.health.disableMaidDeath && health <= 0) {
            super.setHealth(1);
            return;
        }
        super.setHealth(health);
    }

    @Override
    public boolean killedEntity(
            ServerLevel world,
            LivingEntity other,
            DamageSource source) {
        return MaidCombat.killedEntity(this, world, other, source, super.killedEntity(world, other, source));
    }

    // ???

    // ??/?????????????????????????????????????????????????????????????????
    @Override
    public void performRangedAttack(LivingEntity target, float pullProgress) {
        MaidCombat.performRangedAttack(this, target, pullProgress);
    }

    // ?????????

    public boolean isCharging() {
        return this.entityData.get(CHARGING);
    }

    @Override
    public void setChargingCrossbow(boolean charging) {
        this.entityData.set(CHARGING, charging);
    }

    // 1.21.1: CrossbowAttackMob???shootCrossbowProjectile/shoot???????????????????
    // performCrossbowAttack(default) ?? CrossbowItem.performShooting ??????????????????????????
    // ???????????????? archerShootVelocityFactor ????????????????????????????????
    // ?????????????????????????? performCrossbowAttack ???????????????????

    @Override
    public void onCrossbowAttackPerformed() {
    }

    // ???????: ????/????????????????????????????????????????????????????????????
    // ????????? LMSafeMovement ????????verride ??????????????
    @Override
    protected Vec3 maybeBackOffFromEdge(Vec3 movement, MoverType type) {
        return LMSafeMovement.maybeBackOffFromEdge(this, movement, type);
    }

    // --- LMSafeMovement ????????????????rotected ??????? / ?????????????????????????? ---
    // ???????????????????????????????????????????????????????????????????/mixin ???????????????????
    float getDangerHeightThreshold_LM() {
        int fallDamage = calculateFallDamage(0, 1);
        return -fallDamage;
    }

    double fallDistance_LM() {
        return this.fallDistance;
    }

    // ???????????????etEyeHeight() ??????????????????????????? eyeHeight ??
    // ????????????etDefaultDimensions ?? per-model ???????????????????????????
    @Override
    public Vec3 getLeashOffset() {
        return new Vec3(0.0, this.getEyeHeight() - 0.15f, 1f / 16f);
    }

    // ????????????????????????????????????? LMInteractionHandler ????????verride ??????????????
    @Override
    public InteractionResult mobInteract(Player player, InteractionHand hand) {
        return LMInteractionHandler.mobInteract(this, player, hand);
    }

    public InteractionResult changeState(Player player, ItemStack stack) {
        this.level().broadcastEntityEvent(this, (byte) 72);
        this.playSound(
                SoundEvents.ITEM_PICKUP,
                1.0F,
                this.random.nextFloat() * 0.1F + 1.0F);
        this.setFreedomPos(this.blockPosition());
        this.getNavigation().stop();
        TameableUtil.switchWait(this);
        if (!player.getAbilities().instabuild) {
            stack.shrink(1);
            if (stack.isEmpty()) {
                player.getInventory().removeItem(stack);
            }
        }
        return InteractionResult.SUCCESS;
    }

    public InteractionResult contract(
            Player player,
            ItemStack stack,
            boolean isReContract) {
        if (!isReContract) {
            this.level().broadcastEntityEvent(this, (byte) 70);
            if (player instanceof ServerPlayer) {
                LMNCriteria.CONTRACT_MAID.trigger((ServerPlayer) player, this);
            }
        } else {
            this.level().broadcastEntityEvent(this, (byte) 71);
        }
        this.setOwnerUUID(player.getUUID());
        setContractMM(true);
        // ????????????
        if (!this.level().isClientSide()) {
            NetworkHandler.sendSyncMultiModelS2C(this, this);
        }
        setStrike(false);
        itemContractable.setUnpaidTimes(0);
        getNavigation().stop();
        setMaidMode(MaidMode.ESCORT);
        if (!player.getAbilities().instabuild) {
            stack.shrink(1);
            if (stack.isEmpty()) {
                player.getInventory().removeItem(stack);
            }
        }
        return InteractionResult.SUCCESS;
    }

    public void addExperience(int experience) {
        this.xpReward = Mth.clamp(
                this.xpReward + experience,
                0,
                Integer.MAX_VALUE);
    }

    // GUI??????
    public void openInventory(Player player) {
        if (player.level().isClientSide()) {
            return;
        }
        setLastHurtByMob(null);
        getNavigation().stop();
        final LittleMaidEntity maid = this;
        player.openMenu(screenFactory, buf -> {
            buf.writeVarInt(maid.getId());
            buf.writeByte(maid.getUnpaidDays());
            buf.writeByte(maid.getWorkItemSlotSize());
        });
    }

    /**
     * 0:wait
     * 1:freedom
     * 2:tracer
     * 3:aiming
     * 4:begging
     * 5:blood suck
     */
    public void setLMMFlag(int index, boolean value) {
        int i = this.entityData.get(LMM_FLAGS);
        int mask = (1 << index);
        if (value) {
            i |= mask;
        } else {
            i &= ~mask;
        }
        this.entityData.set(LMM_FLAGS, (byte) i);
    }

    public boolean getLMMFlag(int index) {
        return (this.entityData.get(LMM_FLAGS) & (1 << index)) != 0;
    }

    @Override
    public MaidMode getMaidMode() {
        return MaidMode.fromId(this.entityData.get(MOVING_MODE));
    }

    @Override
    public void setMaidMode(MaidMode movingMode) {
        this.entityData.set(MOVING_MODE, (byte) movingMode.getId());
        if (movingMode == MaidMode.ESCORT && this.isOrderedToSit()) {
            this.setOrderedToSit(false);
        }
    }

    // Flee

    public void addFleeEntity(Mob entity, Predicate<Mob> removePredicate) {
        this.fleeEntities.put(entity, removePredicate);
    }

    // ??????????????

    @Override
    public Container getInventory() {
        return this.littleMaidInventory.getInventory();
    }

    @Override
    public void writeInventory(ValueOutput output) {
        this.littleMaidInventory.writeInventory(output);
    }

    @Override
    public void readInventory(ValueInput input) {
        this.littleMaidInventory.readInventory(input);
    }

    public int getWorkItemSlotSize() {
        return this.littleMaidInventory.getWorkItemSlotSize();
    }

    public void setWorkItemSlotNum(int num) {
        this.littleMaidInventory.setWorkItemSlotSize(num);
    }

    // ????????????????????????????????? 1/4????? 1??????????????????????????
    // DAMAGE_RESISTANT ???????????? EQUIPPABLE ???????????
    @Override
    protected void hurtArmor(DamageSource source, float amount) {
        MaidCombat.hurtArmor(this, source, amount);
    }

    @Override
    protected void hurtHelmet(DamageSource source, float amount) {
        MaidCombat.hurtHelmet(this, source, amount);
    }

    // ?????????????????????????????????????/item replace?????????????????????????
    @Override
    public SlotAccess getSlot(int mappedIndex) {
        var inv = getInventory();
        int i = mappedIndex - 200;
        if (0 <= i && i < inv.getContainerSize()) {
            return SlotAccess.of(
                    () -> inv.getItem(i),
                    stack -> inv.setItem(i, stack));
        }
        return super.getSlot(mappedIndex);
    }

    // ????????????????????????????????? ?? ???????????????????????????
    // EPEntityUtil.arrowCustomHook ???? Mod ????????????????????????
    @Override
    public ItemStack getProjectile(ItemStack stack) {
        return MaidCombat.getProjectile(this, stack);
    }

    // ??????????
    @Override
    public void setItemSlot(EquipmentSlot slot, ItemStack stack) {
        super.setItemSlot(slot, stack);

        if (slot.isArmor()) {
            multiModel.updateArmor();
        }
    }

    @Override
    public boolean isBlocking() {
        if (this.isUsingItem()) {
            ItemStack activeItem = this.getUseItem();
            return !activeItem.isEmpty() && activeItem.getItem() instanceof net.minecraft.world.item.ShieldItem;
        }
        return false;
    }

    @Override
    protected void dropEquipment(ServerLevel serverLevel) {
        Container inv = this.getInventory();
        for (int i = 0; i < inv.getContainerSize(); i++) {
            ItemStack stack = inv.getItem(i);
            if (stack.isEmpty() ||
                    (stack.has(
                            net.minecraft.core.component.DataComponents.ENCHANTMENTS) &&
                            net.minecraft.world.item.enchantment.EnchantmentHelper.has(
                                    stack,
                                    net.minecraft.world.item.enchantment.EnchantmentEffectComponents.PREVENT_EQUIPMENT_DROP)))
                continue;
            this.spawnAtLocation(serverLevel, stack);
            inv.setItem(i, ItemStack.EMPTY);
        }
        for (EquipmentSlot slot : EquipmentSlot.values()) {
            ItemStack stack = this.getItemBySlot(slot);
            if (stack.isEmpty() ||
                    net.minecraft.world.item.enchantment.EnchantmentHelper.has(
                            stack,
                            net.minecraft.world.item.enchantment.EnchantmentEffectComponents.PREVENT_EQUIPMENT_DROP))
                continue;
            this.spawnAtLocation(serverLevel, stack);
            this.setItemSlot(slot, ItemStack.EMPTY);
        }
    }

    @Override
    public int getBaseExperienceReward(ServerLevel serverLevel) {
        return this.xpReward;
    }

    // idFactor ?? UUID ???????????????????????????????????????/???????????????????????
    // UUID ?????????????????????? setUUID ???????????
    @Override
    public void setUUID(UUID uuid) {
        super.setUUID(uuid);
        initIdFactor();
    }

    public void initIdFactor() {
        this.idFactor = Math.abs(this.getUUID().hashCode());
    }

    public int getIdFactor() {
        return idFactor;
    }

    // ?????????

    public void setOwnerUUID(@Nullable UUID uuid) {
        if (uuid != null) {
            TameableUtil.setTameOwnerUuid(this, uuid);
        }
        this.setContract(true);
    }

    public void installMaidSoul(MaidSoulData maidSoul) {
        load(
                TagValueInput.create(
                        ProblemReporter.DISCARDING,
                        registryAccess(),
                        maidSoul.getNbt()));
        setUUID(maidSoul.getUuid());
        setOwnerUUID(maidSoul.getOwnerUUID().orElse(null));
    }

    public void setFreedomPos(@Nullable BlockPos freedomPos) {
        this.freedomPos = freedomPos;
    }

    public Optional<BlockPos> getFreedomPos() {
        if (this.getMaidMode() != MaidMode.FREEDOM) {
            return Optional.empty();
        }
        if (freedomPos == null) {
            freedomPos = this.blockPosition();
        }
        return Optional.of(freedomPos);
    }

    @Override
    public void setInSittingPose(boolean inSittingPose) {
    }

    @Override
    public boolean isInSittingPose() {
        return TameableUtil.isWait(this);
    }

    @Override
    public void setOrderedToSit(boolean sitting) {
        this.setLMMFlag(WAIT_INDEX, sitting);
    }

    @Override
    public boolean isOrderedToSit() {
        return this.getLMMFlag(WAIT_INDEX);
    }

    @Override
    public boolean isTame() {
        return TameableUtil.getTameOwnerUuid(this).isPresent();
    }

    // 1.21.1: method_48926 -> level() ?????
    // OwnableEntity???????????????????????????
    public net.minecraft.world.level.EntityGetter getWorld() {
        return this.level();
    }

    public boolean isBegging() {
        return this.getLMMFlag(BEGGING_INDEX);
    }

    public void setBegging(boolean begging) {
        this.setLMMFlag(BEGGING_INDEX, begging);
    }

    public boolean isBloodSuck() {
        return this.getLMMFlag(BLOOD_SUCK_INDEX);
    }

    public void setBloodSuck(boolean isBloodSuck) {
        this.setLMMFlag(BLOOD_SUCK_INDEX, isBloodSuck);
    }
public float getInterestedAngle(float tickDelta) {
        return ((prevInterestedAngle +
                (interestedAngle - prevInterestedAngle) * tickDelta) *
                ((getId() % 2 == 0 ? 0.08F : -0.08F) * (float) Math.PI));
    }
private void tickInterestedAngle() {
        prevInterestedAngle = interestedAngle;
        if (isBegging()) {
            interestedAngle = interestedAngle + (1.0F - interestedAngle) * 0.4F;
        } else {
            interestedAngle = interestedAngle + (0.0F - interestedAngle) * 0.4F;
        }
    }

    // ???????

    public int getTickMultiple() {
        return acceleration.getTickMultiple();
    }

    public void setAccelerationTicks(int ticks) {
        acceleration.setAccelerationTicks(ticks);
    }

    public void decAccelerationTicks() {
        acceleration.decAccelerationTicks();
    }

    public int getAccelerationTicks() {
        return acceleration.getAccelerationTicks();
    }

    public boolean isAcceleration() {
        return acceleration.isAcceleration();
    }

    boolean isAcceleration_LM() {
        return this.entityData.get(ACCELERATE);
    }

    void setAccelerationData_LM(boolean accelerate) {
        this.entityData.set(ACCELERATE, accelerate);
    }

    // ??????

    @Override
    public boolean isContract() {
        return TameableUtil.getTameOwnerUuid(this).isPresent();
    }

    @Override
    public void setContract(boolean isContract) {
        itemContractable.setContract(isContract);
    }

    @Override
    public boolean isStrike() {
        return this.getLMMFlag(STRIKE_INDEX);
    }

    @Override
    public void setStrike(boolean strike) {
        itemContractable.setStrike(strike);
        this.setLMMFlag(STRIKE_INDEX, strike);
    }

    @Override
    public void writeContractable(ValueOutput output) {
        itemContractable.writeContractable(output);
    }

    @Override
    public void readContractable(ValueInput input) {
        itemContractable.readContractable(input);
        if (itemContractable.isStrike()) {
            this.setStrike(true);
        }
    }

    public int getUnpaidDays() {
        return itemContractable.getUnpaidTimes();
    }

    // ????????????

    @Override
    public void listenSalaryBoxPos(BlockPos pos) {
        itemContractable.listenSalaryBoxPos(pos);
    }

    // 移譲s

    public void writeModeData(ValueOutput output) {
        cookingBehavior.writeBehaviorData(output.child("cooking"));
        pharmcistBehavior.writeBehaviorData(output.child("pharmcist"));
    }

    public void readModeData(ValueInput input) {
        input.child("cooking").ifPresent(cookingBehavior::readBehaviorData);
        input.child("pharmcist").ifPresent(pharmcistBehavior::readBehaviorData);
    }

    public void setModeName(String modeName) {
        this.entityData.set(MODE_NAME, modeName);
    }
public Optional<String> getModeName() {
        String modeName = this.entityData.get(MODE_NAME);
        if (modeName.isEmpty())
            return Optional.empty();
        return Optional.of(modeName);
    }

    // TargetTag

    @Override
    public Set<TargetingSystem.TargetTag> getTargetTag(TargetIdentifier id) {
        return TameableUtil.getTameOwner(this)
                .map(owner -> {
                    TargetTagManager t = owner.getData(ModRegistration.TARGET_TAG_ATTACHMENT.get());
                    var otherSync = t.getTargetTagsSync();
                    var thisSync = this.getTargetTagsSync();
                    if (otherSync.hash() != thisSync.hash()) {
                        thisSync.syncFrom(otherSync);
                    }
                    return t;
                })
                .orElse(this.targetTagManager)
                .getTargetTag(id);
    }

    @Override
    public void writeTargetTags(ValueOutput output) {
        this.targetTagManager.writeTargetTags(output);
    }

    @Override
    public void readTargetTags(ValueInput input) {
        this.targetTagManager.readTargetTags(input);
    }

    @Override
    public Sync getTargetTagsSync() {
        return this.targetTagManager.getTargetTagsSync();
    }

    @Override
    public boolean wantsToAttack(LivingEntity target, LivingEntity owner) {
        return !isFriend(target);
    }

    public boolean isFriend(LivingEntity entity) {
        // ??: ???? isFriend ?? ATTACK_PROHIBITED ????????????????????????????????????
        //     ??????????????hase 7??TargetingSystem ??????????????????????????????:
        // ??????????????????????????????????
        if (entity instanceof OwnableEntity tameable &&
                TameableUtil.hasTameOwner(tameable)) {
            return true;
        }
        // ????: ?????????????????????????????????????????
        if (TameableUtil.hasTameOwner(this) && entity instanceof Player) {
            return true;
        }
        if (TameableUtil.isTameOwner(this, entity) ||
                (entity instanceof OwnableEntity tameable &&
                        TameableUtil.equalTameOwner(this, tameable))) {
            return true;
        }
        return getTargetTag(new TargetIdentifier(entity)).contains(
                TargetingSystem.TargetTag.ATTACK_PROHIBITED);
    }

    // ???

    public boolean isAimingBow() {
        return this.getLMMFlag(AIMING_INDEX);
    }

    public void setAimingBow(boolean aiming) {
        this.setLMMFlag(AIMING_INDEX, aiming);
    }

    // ?????????????

    @Override
    public void setTextureHolder(
            TextureHolder textureHolder,
            Layer layer,
            Part part) {
        MultiModelHolder.super.setTextureHolder(textureHolder, layer, part);
        if (layer == Layer.SKIN) {
            refreshDimensions();
        }
    }

    public boolean isPlayingSnow() {
        return this.getLMMFlag(PLAYING_SNOW_INDEX);
    }

    public void setPlayingSnow(boolean isPlayingSnow) {
        this.setLMMFlag(PLAYING_SNOW_INDEX, isPlayingSnow);
    }

    // ????????

    // ?????????????????????????(playSoundCool)??????????????
    // ??????????????????????????????????????? playForce() ????????
    // ???????????????? getConfig().misc.playSoundInterval ??????????????????
    @Override
    public void play(String soundName) {
        if (0 < this.playSoundCool) {
            return;
        }
        this.playSoundCool = getConfig().misc.playSoundInterval;
        if (isBloodSuck()) {
            if (soundName.equals(LMSounds.FIND_TARGET_N)) {
                soundName = LMSounds.FIND_TARGET_B;
            } else if (soundName.equals(LMSounds.ATTACK)) {
                soundName = LMSounds.ATTACK_BLOOD_SUCK;
            }
        }
        SoundHolder.super.play(soundName);
    }

    public static LMNConfig getConfig() {
        return LMNConfig.get();
    }

    // ??: ?? LMStareAtHeldItemGoal????????????????? Goal??? Phase 7 ??
    //     MaidStareBehavior??rain Behavior????????????oal ???????????????????????????????????



    public int getContractTime() {
        return this.entityData.get(CONTRACT_TIME);
    }

    public void setContractTime(int contractTime) {
        this.entityData.set(CONTRACT_TIME, contractTime);
    }

    public void playForce(String soundName) {
        if (isBloodSuck()) {
            if (soundName.equals(LMSounds.FIND_TARGET_N)) {
                soundName = LMSounds.FIND_TARGET_B;
            } else if (soundName.equals(LMSounds.ATTACK)) {
                soundName = LMSounds.ATTACK_BLOOD_SUCK;
            }
        }
        soundPlayer.play(soundName);
    }

    private void tryEatingFromInventory() {
        Container inv = this.getInventory();
        for (int i = 0; i < inv.getContainerSize(); i++) {
            ItemStack stack = inv.getItem(i);
            if (!stack.isEmpty() && stack.is(LMTags.Items.MAIDS_SALARY)) {
                stack.shrink(1);
                if (stack.isEmpty()) {
                    inv.setItem(i, ItemStack.EMPTY);
                }
                var config = getConfig();
                this.heal(config.health.healAmount);
                this.playSound(SoundEvents.GENERIC_EAT.value(), 0.5f, 0.5f + this.random.nextFloat() * 0.5f);
                this.level().broadcastEntityEvent(this, (byte) 72); // ??????????????
                break;
            }
        }
    }

    protected void showTransAmParticles() {
        MaidParticle.showTransAmParticles(this);
    }

    @Override
    public boolean shouldShowName() {
        if (net.neoforged.fml.loading.FMLEnvironment.getDist() == net.neoforged.api.distmarker.Dist.CLIENT) {
            if (work.nemonet.littlemaidneo.client.util.ClientScreenHelper.shouldShowOwnerName(this)) {
                return true;
            }
        }
        return super.shouldShowName();
    }

    @Override
    public net.minecraft.network.chat.Component getDisplayName() {
        net.minecraft.network.chat.Component name = super.getDisplayName();
        if (net.neoforged.fml.loading.FMLEnvironment.getDist() == net.neoforged.api.distmarker.Dist.CLIENT) {
            var ownerNameOpt = work.nemonet.littlemaidneo.client.util.ClientScreenHelper.getOwnerNameForClient(this);
            if (ownerNameOpt.isPresent()) {
                return net.minecraft.network.chat.Component.literal(name.getString() + " (" + net.minecraft.network.chat.Component.translatable("chat.littlemaidneo.owner_name_prefix").getString() + ": " + ownerNameOpt.get() + ")");
            }
        }
        return name;
    }

    public int getXpReward_LM() {
        return this.xpReward;
    }

    public void setXpReward_LM(int xpReward) {
        this.xpReward = xpReward;
    }
}
