package work.nemonet.littlemaidneo.entity;

import com.google.common.collect.Lists;
import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import net.minecraft.commands.arguments.EntityAnchorArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.core.UUIDUtil;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.util.ProblemReporter;
import net.minecraft.util.Util;
import net.minecraft.world.*;
import net.minecraft.world.damagesource.DamageEffects;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.OwnableEntity;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.SlotAccess;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.AvoidEntityGoal;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.OpenDoorGoal;
import net.minecraft.world.entity.ai.goal.PanicGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.navigation.GroundPathNavigation;
import net.minecraft.world.entity.boss.enderdragon.EnderDragonPart;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.CrossbowAttackMob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.entity.projectile.arrow.AbstractArrow;
import net.minecraft.world.entity.projectile.throwableitemprojectile.Snowball;
import net.minecraft.world.item.ArrowItem;
import net.minecraft.world.item.BowItem;
import net.minecraft.world.item.CrossbowItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.ProjectileWeaponItem;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.level.storage.TagValueOutput;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.neoforge.entity.IEntityWithComplexSpawn;
import org.jetbrains.annotations.Nullable;
import work.nemonet.littlemaidneo.LittleMaidNeo;
import work.nemonet.littlemaidneo.advancement.criterion.LMRBCriteria;
import work.nemonet.littlemaidneo.api.mode.Mode;
import work.nemonet.littlemaidneo.api.mode.ModeManager;
import work.nemonet.littlemaidneo.config.LMRBConfig;
import work.nemonet.littlemaidneo.entity.compound.IHasMultiModel;
import work.nemonet.littlemaidneo.entity.compound.MultiModelCompound;
import work.nemonet.littlemaidneo.entity.compound.SoundPlayable;
import work.nemonet.littlemaidneo.entity.compound.SoundPlayableCompound;
import work.nemonet.littlemaidneo.entity.goal.*;
import work.nemonet.littlemaidneo.entity.mode.HasMode;
import work.nemonet.littlemaidneo.entity.mode.HasModeImpl;
import work.nemonet.littlemaidneo.entity.mode.ModeWrapperGoal;
import work.nemonet.littlemaidneo.entity.targeting.TargetIdentifier;
import work.nemonet.littlemaidneo.entity.targeting.TargetTagManager;
import work.nemonet.littlemaidneo.entity.targeting.TargetTagManagerImpl;
import work.nemonet.littlemaidneo.entity.targeting.TargetingSystem;
import work.nemonet.littlemaidneo.entity.util.*;
import work.nemonet.littlemaidneo.maidmodel.IModelCaps;
import work.nemonet.littlemaidneo.mixin.CrossbowItemInvoker;
import work.nemonet.littlemaidneo.multimodel.IMultiModel;
import work.nemonet.littlemaidneo.multimodel.layer.MMPose;
import work.nemonet.littlemaidneo.network.NetworkHandler;
import work.nemonet.littlemaidneo.resource.holder.ConfigHolder;
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
import net.minecraft.world.entity.ai.Brain;
import net.minecraft.world.entity.ai.ActivityData;
import net.minecraft.world.entity.schedule.Activity;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import com.mojang.serialization.Dynamic;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;

//メイドさん本体
public class LittleMaidEntity
        extends TamableAnimal
        implements
        IEntityWithComplexSpawn,
        HasInventory,
        Contractable,
        HasMode,
        IHasMultiModel,
        SoundPlayable,
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

    // LMM_FLAGSのindex
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
    // エンチャントの瓶はランダムな経験値を排出するため、その平均値を作成コストとする
    // LMInteractionHandler から参照するためパッケージプライベート
    static final int EXPERIENCE_BOTTLE_COST = 7;

    // 移譲s
    public final LMHasInventory littleMaidInventory = new LMHasInventory();
    public final LMItemContractable<LittleMaidEntity> itemContractable = new LMItemContractable<>(
            this,
            () -> getConfig().contract.consumeSalaryInterval,
            () -> getConfig().contract.unpaidDaysLimit,
            (ItemStack stack) -> stack.is(LMTags.Items.MAIDS_SALARY));
    public final HasModeImpl hasModeImpl = new HasModeImpl(
            this,
            this,
            new HashSet<>(),
            mode -> {
                setModeName(mode != null ? mode.getName() : "");
            });
    public final MultiModelCompound multiModel;
    public final SoundPlayableCompound soundPlayer;
    private final LMScreenHandlerFactory screenFactory = new LMScreenHandlerFactory(this);
    private final IModelCaps caps = new LittleMaidModelCaps(this);
    private final TargetTagManager targetTagManager;

    private final Map<Mob, Predicate<Mob>> fleeEntities = new HashMap<>();

    @Nullable
    private BlockPos freedomPos;

    // 首傾げのやつ
private float interestedAngle;
private float prevInterestedAngle;

    private int playSoundCool;
    private int idFactor;
    public int experiencePickUpDelay;
    // accelerationTicks はサーバー権威。クライアントへは ACCELERATE フラグ(SynchedEntityData)と
    // スポーンパケット(writeVarInt/readVarInt)で同期されるため、クライアント側の生値は描画補助以上の用途に使わない。
    private int accelerationTicks;
    private boolean maidManagerRegistered;

    // コンストラクタ
    public LittleMaidEntity(EntityType<LittleMaidEntity> type, Level worldIn) {
        super(type, worldIn);
        this.moveControl = new FixedMoveControl(this);
        this.lookControl = new work.nemonet.littlemaidneo.entity.ai.control.MaidLookControl(this);
        ((GroundPathNavigation) getNavigation()).setCanOpenDoors(true);
        multiModel = new MultiModelCompound(
                this,
                LMTextureManager.INSTANCE.getTexture("Default").orElseThrow(() -> new IllegalStateException(
                        "デフォルトテクスチャが存在しません。")),
                LMTextureManager.INSTANCE.getTexture("Default").orElseThrow(() -> new IllegalStateException(
                        "デフォルトテクスチャが存在しません。")));
        soundPlayer = new SoundPlayableCompound(this,
                () -> multiModel.getTextureHolder(Layer.SKIN, Part.HEAD).getTextureName());
        addDefaultModes(this);
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

    // 自然スポーン条件: 足元が完全な当たり判定を持つブロックで、明るさが 8 超であること。
    // スポーン条件の細分化（明るさ閾値・バイオーム等のコンフィグ化）は機能バックログ（Phase 6）で扱う。
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

    // 視線制御の役割分担:
    //   - 移動 (WALK_TARGET) を消費するのは MoveToTargetSink。MaidFollowOwner/Stare/Freedom が WALK_TARGET を設定するため必須。
    //   - 頭部向きは GoalSelector の LookAtPlayerGoal / RandomLookAroundGoal が担当し、MaidStareBehavior は
    //     getLookControl().setLookAt(...) で直接制御する。いずれも最終的に MaidLookControl で角度クランプされる。
    //   - バニラの LookAtTargetSink は LOOK_TARGET メモリを消費するが、本 Mod では LOOK_TARGET を設定する
    //     プロデューサが存在せず常に no-op だったため登録しない（孤立した不活性 Behavior の混入を防ぐ）。
    private static final Brain.Provider<LittleMaidEntity> BRAIN_PROVIDER = Brain.<LittleMaidEntity>provider(
            ImmutableList.of(
                    ModRegistration.IS_WAITING.get(),
                    ModRegistration.OWNER.get(),
                    MemoryModuleType.WALK_TARGET,
                    MemoryModuleType.CANT_REACH_WALK_TARGET_SINCE,
                    // MoveToTargetSink は PATH メモリを必須要件（VALUE_ABSENT）に持つ。
                    // 未登録だと checkMemory が常に false を返し MoveToTargetSink が起動せず、
                    // 各 Behavior が設定した WALK_TARGET が消費されないため移動が一切発生しない。
                    MemoryModuleType.PATH
            ),
            ImmutableList.of(
                    ModRegistration.LITTLE_MAID_SENSOR.get()
            ),
            entity -> ImmutableList.of(
                    ActivityData.<LittleMaidEntity>create(Activity.CORE, 0, ImmutableList.of(
                            new work.nemonet.littlemaidneo.entity.ai.behavior.MaidWaitBehavior(),
                            new work.nemonet.littlemaidneo.entity.ai.behavior.MaidFollowOwnerBehavior(),
                            new work.nemonet.littlemaidneo.entity.ai.behavior.MaidStareBehavior(),
                            new work.nemonet.littlemaidneo.entity.ai.behavior.MaidFreedomBehavior(),
                            new net.minecraft.world.entity.ai.behavior.MoveToTargetSink()
                    ))
            )
    );

    @Override
    protected Brain<?> makeBrain(Brain.Packed packedBrain) {
        return BRAIN_PROVIDER.makeBrain(this, packedBrain);
    }

    @SuppressWarnings("unchecked")
    @Override
    public Brain<LittleMaidEntity> getBrain() {
        return (Brain<LittleMaidEntity>) super.getBrain();
    }

    // 登録メソッドたち

    @Override
    protected void registerGoals() {
        int priority = -1;
        LMRBConfig config = getConfig();

        // 緊急テレポート
        this.goalSelector.addGoal(
                priority,
                new LMTeleportTameOwnerGoal(
                        this,
                        () -> config.movement.emergencyTeleportStartDistance) {
                    @Override
                    public boolean canUse() {
                        return (isEmergency() &&
                                LittleMaidEntity.this.hurtTime > 0 &&
                                !TameableUtil.isWait(LittleMaidEntity.this) &&
                                super.canUse());
                    }
                });

        this.goalSelector.addGoal(++priority, new FloatGoal(this));
        this.goalSelector.addGoal(++priority, new OpenDoorGoal(this, true));

        this.goalSelector.addGoal(
                ++priority,
                new LMHealMyselfGoal(
                        this,
                        () -> config.health.healInterval,
                        () -> config.health.healAmount,
                        stack -> stack.is(LMTags.Items.MAIDS_SALARY)));

        this.goalSelector.addGoal(
                ++priority,
                new LMCollectSalaryFromContainerGoal<>(this));



        this.goalSelector.addGoal(
                ++priority,
                new LMTeleportTameOwnerGoal(
                        this,
                        () -> config.movement.teleportStartDistance));

        // 危険な敵からの逃避
        this.goalSelector.addGoal(
                ++priority,
                new AvoidEntityGoal<>(
                        this,
                        Mob.class,
                        config.target.dangerousAvoidDistance,
                        config.movement.followSpeed,
                        config.movement.sprintSpeed,
                        entity -> fleeEntities.containsKey(entity)) {
                    @Override
                    public void tick() {
                        fleeEntities
                                .entrySet()
                                .removeIf(entry -> entry.getValue().test(entry.getKey()));
                        super.tick();
                    }

                    @Override
                    public void stop() {
                        super.stop();
                        this.mob.getNavigation().stop();
                    }
                });

        this.goalSelector.addGoal(
                ++priority,
                new ModeWrapperGoal<>(this) {
                    @Override
                    public boolean canUse() {
                        return (!this.owner.isStrike() &&
                                (config.health.enableWorkInEmergency ||
                                        !isEmergency())
                                &&
                                super.canUse());
                    }

                    @Override
                    public boolean canContinueToUse() {
                        return (!this.owner.isStrike() &&
                                (config.health.enableWorkInEmergency ||
                                        !isEmergency())
                                &&
                                super.canContinueToUse());
                    }
                });





        this.goalSelector.addGoal(
                ++priority,
                new LMStoreItemToContainerGoal<>(
                        this,
                        stack -> stack.is(LMTags.Items.MAIDS_SALARY) ||
                                this.hasModeImpl
                                        .getMode()
                                        .filter(mode -> mode.getModeType().isModeItem(stack))
                                        .isPresent(),
                        () -> config.work.searchContainerRange));

        this.goalSelector.addGoal(
                ++priority,
                new LMMoveToDropItemGoal(
                        this,
                        () -> config.movement.pickupItemRange,
                        () -> config.movement.pickupItemFrequency,
                        () -> config.movement.pickupItemSpeed) {
                    @Override
                    public boolean canUse() {
                        return (TameableUtil.hasTameOwner(LittleMaidEntity.this) &&
                                (config.health.enableWorkInEmergency ||
                                        !isEmergency())
                                &&
                                super.canUse());
                    }

                    @Override
                    public List<ItemEntity> findAroundDropItem() {
                        return TameableUtil.getTameOwner(maid)
                                .map(owner -> {
                                    return super.findAroundDropItem()
                                            .stream()
                                            .filter(item -> !this.isOwnerRange(item, owner))
                                            .collect(Collectors.toList());
                                    // ご主人様が存在しない場合は普通にとる
                                })
                                .orElse(super.findAroundDropItem());
                    }
                });



        this.goalSelector.addGoal(++priority, new PlaySnowGoal(this));

        this.goalSelector.addGoal(
                ++priority,
                new RedstoneTraceGoal(this, () -> config.movement.tracerSpeed));


        // 野良
        this.goalSelector.addGoal(
                ++priority,
                new LMMoveToDropItemGoal(
                        this,
                        () -> config.movement.pickupItemRange,
                        () -> config.movement.pickupItemFrequency,
                        () -> config.movement.pickupItemSpeed) {
                    @Override
                    public boolean canUse() {
                        return (!TameableUtil.hasTameOwner(LittleMaidEntity.this) &&
                                config.misc.canPickupItemByNoOwner &&
                                (config.health.enableWorkInEmergency ||
                                        !isEmergency())
                                &&
                                super.canUse());
                    }
                });
        this.goalSelector.addGoal(
                ++priority,
                new PanicGoal(this, config.movement.escapeSpeed) {
                    @Override
                    public boolean canUse() {
                        return (!TameableUtil.hasTameOwner(LittleMaidEntity.this) &&
                                super.canUse());
                    }
                });


        // 視線
        // 既定の確率 0.02 では稀にしかプレイヤーを見ないため、メイドさんが
        // 近くのプレイヤーを目で追うよう確率を引き上げる（プレイヤー優先）。
        this.goalSelector.addGoal(
                ++priority,
                new LookAtPlayerGoal(this, Player.class, 8.0F, 0.8F, false));
        this.goalSelector.addGoal(
                priority,
                new LookAtPlayerGoal(this, LivingEntity.class, 8.0F));
        this.goalSelector.addGoal(priority, new RandomLookAroundGoal(this));

        // ターゲット系
        this.targetSelector.addGoal(0, new LMTargetGoal(this));
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

    public void addDefaultModes(LittleMaidEntity maid) {
        this.hasModeImpl.addAllMode(ModeManager.INSTANCE.createModes(maid));
    }

    // 読み書き系

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

        output.putInt("accelerationTicks", accelerationTicks);
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
                .getString("SoundConfigName")
                .ifPresent(name -> LMConfigManager.INSTANCE.getConfig(name).ifPresent(
                        this::setConfigHolder));

        accelerationTicks = input.getIntOr("accelerationTicks", 0);
    }

    // idFactor は initIdFactor()（コンストラクタおよび setUUID() 内）で UUID から確定する。
    // 本メソッドはコンストラクタ末尾で idFactor 確定後に呼ばれるため、ここでは確定済みの値を前提にできる。
    public void setRandomTexture() {
        var textureHolderList = LMTextureManager.INSTANCE.getAllTextures()
                .stream()
                .filter(h -> h.hasSkinTexture(false)) // 野生テクスチャがある
                .filter(h -> LMModelManager.INSTANCE.hasModel(h.getModelName()))
                .toList();
        if (textureHolderList.isEmpty()) {
            return;
        }
        var textureHolder = textureHolderList.get(
                idFactor % textureHolderList.size());
        var colorList = Arrays.stream(TextureColors.values())
                .filter(c -> textureHolder.getTexture(c, false, false).isPresent())
                .toList();
        if (colorList.isEmpty()) {
            return;
        }
        var color = colorList.get(idFactor % colorList.size());
        this.setColorMM(color);
        this.setTextureHolder(textureHolder, Layer.SKIN, Part.HEAD);
        if (textureHolder.hasArmorTexture()) {
            setTextureHolder(textureHolder, Layer.INNER, Part.HEAD);
            setTextureHolder(textureHolder, Layer.INNER, Part.BODY);
            setTextureHolder(textureHolder, Layer.INNER, Part.LEGS);
            setTextureHolder(textureHolder, Layer.INNER, Part.FEET);
            setTextureHolder(textureHolder, Layer.OUTER, Part.HEAD);
            setTextureHolder(textureHolder, Layer.OUTER, Part.BODY);
            setTextureHolder(textureHolder, Layer.OUTER, Part.LEGS);
            setTextureHolder(textureHolder, Layer.OUTER, Part.FEET);
        }
    }

    public void setRandomVoice() {
        if (getConfig().spawn.silentDefaultVoice) {
            soundPlayer.setConfigHolder(LMConfigManager.EMPTY_CONFIG);
        } else {
            List<ConfigHolder> configs = LMConfigManager.INSTANCE.getAllConfig();
            soundPlayer.setConfigHolder(configs.get(idFactor % configs.size()));
        }
        String defaultSoundPackName = getConfig().spawn.defaultSoundPackName;
        if (!defaultSoundPackName.isEmpty()) {
            LMConfigManager.INSTANCE.getAllConfig()
                    .stream()
                    .filter(c -> c.packName().equalsIgnoreCase(defaultSoundPackName))
                    .findAny()
                    .ifPresent(soundPlayer::setConfigHolder);
        }
    }

    // 鯖
    @Override
    public void writeSpawnData(RegistryFriendlyByteBuf buf) {
        // モデル
        buf.writeEnum(getColorMM());
        buf.writeBoolean(isContractMM());
        buf.writeUtf(getTextureHolder(Layer.SKIN, Part.HEAD).getTextureName());
        for (Part part : Part.values()) {
            buf.writeUtf(getTextureHolder(Layer.INNER, part).getTextureName());
            buf.writeUtf(getTextureHolder(Layer.OUTER, part).getTextureName());
        }
        // サウンド
        buf.writeUtf(getConfigHolder().getName());
        // 頭の装飾品が表示されない対策
        // 原因はインベントリを開くまで同期されないため
        ItemStack.OPTIONAL_STREAM_CODEC.encode(buf, getInventory().getItem(17));
        // architectury側のミスでPitchYawが逆に与えられているのを修正
        buf.writeFloat(this.getXRot());
        buf.writeFloat(this.getYRot());
        buf.writeVarInt(this.accelerationTicks);
    }

    // 蔵
    @Override
    public void readSpawnData(RegistryFriendlyByteBuf buf) {
        // モデル
        // readString()はクラ処理。このメソッドでは、クラ側なので問題なし
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
        // サウンド
        LMConfigManager.INSTANCE.getConfig(buf.readUtf()).ifPresent(
                this::setConfigHolder);

        getInventory().setItem(17, ItemStack.OPTIONAL_STREAM_CODEC.decode(buf));
        this.setXRot(buf.readFloat());
        this.setYRot(buf.readFloat());
        this.accelerationTicks = buf.readVarInt();
    }

    @Override
    public void handleEntityEvent(byte status) {
        switch (status) {
            case 70 -> {
                // 雇用時
                spawnTamingParticles(true);
                play(LMSounds.GET_CAKE);
            }
            case 71 -> {
                // 再雇用時
                spawnTamingParticles(true);
                play(LMSounds.RECONTRACT);
            }
            case 72 -> {
                // 砂糖あげた時
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
            case 76 -> showTransAmParticles(); // トランザムのエフェクト
            default -> super.handleEntityEvent(status);
        }
    }

    protected void showFreedomParticle() {
        for (int i = 0; i < 7; ++i) {
            double d = this.random.nextGaussian() * 0.02;
            double e = this.random.nextGaussian() * 0.02;
            double f = this.random.nextGaussian() * 0.02;
            int rgb = ((int) (this.random.nextFloat() * 255) << 16) |
                    ((int) (this.random.nextFloat() * 255) << 8) |
                    (int) (this.random.nextFloat() * 255);
            this.level().addParticle(
                    new DustParticleOptions(rgb, 1.0f),
                    this.getRandomX(1.0),
                    this.getRandomY() + 0.5,
                    this.getRandomZ(1.0),
                    d,
                    e,
                    f);
        }
    }

    protected void showTracerParticle() {
        for (int i = 0; i < 7; ++i) {
            double d = this.random.nextGaussian() * 0.02;
            double e = this.random.nextGaussian() * 0.02;
            double f = this.random.nextGaussian() * 0.02;
            this.level().addParticle(
                    ParticleTypes.CLOUD,
                    this.getRandomX(1.0),
                    this.getRandomY() + 0.5,
                    this.getRandomZ(1.0),
                    d,
                    e,
                    f);
        }
    }

    // バニラメソッズ

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

    protected void inTickMultiplePost() {
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
        hasModeImpl.tick();

        // つまみ食い
        if (this.tickCount % 40 == 0 && this.getHealth() < this.getMaxHealth()) {
            tryEatingFromInventory();
        }

        // 水没時のお座り（待機）解除
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
        // 乗り物にライド中の処理は省略
        var aabb = this.getBoundingBox().inflate(1.0, 0.5, 1.0);
        var aroundItems = this.level().getEntities(this, aabb);
        var exps = Lists.newArrayList();
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
            if (entity.isRemoved())
                continue;
            if (entity instanceof LMCollidable collidable) {
                collidable.onCollision_LMRB(this);
            }
        }
        if (!exps.isEmpty()) {
            var collidable = ((LMCollidable) Util.getRandom(exps, this.random));
            if (collidable != null) {
                collidable.onCollision_LMRB(this);
            }
        }
    }

    @Override
    public boolean removeWhenFarAway(double distanceSquared) {
        return (getConfig().spawn.canDespawn &&
                TameableUtil.getTameOwnerUuid(this).isEmpty());
    }

    // canSpawn 等でも使われる経路コスト評価。足場が完全ブロックなら高評価(10.0)、
    // それ以外は明るさベースのコストを返す。閾値のコンフィグ化は機能バックログ（Phase 6）。
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

    // 騎乗オフセットはモデル定義（getMountedYOffset / getyOffset）から算出し、
    // getPassengerRidingPosition / getVehicleAttachmentPoint で反映する。

    /**
     * 上に乗ってるエンティティへのオフセット (1.21.1ではgetPassengerRidingPositionに統合)
     */
    public double getMountedYOffset() {
        IMultiModel model = getModel(Layer.SKIN, Part.HEAD).orElse(
                LMModelManager.INSTANCE.getDefaultModel());
        return model.getMountedYOffset(getCaps());
    }

    /**
     * 騎乗時のオフセット
     */
    public double getRidingYOffset() {
        IMultiModel model = getModel(Layer.SKIN, Part.HEAD).orElse(
                LMModelManager.INSTANCE.getDefaultModel());
        return model.getyOffset(getCaps()) - getBbHeight();
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

    // 毎回 EntityDimensions を生成するが、頻繁に呼ばれるメソッドではないためキャッシュしない。
    // キャッシュ化はモデル変更・ポーズ・成長スケール全ての無効化が必要で、得られる効果に対し
    // 複雑さ（無効化漏れによるヒットボックス不整合）のリスクが見合わないため見送る（設計判断）。
    @Override
    public EntityDimensions getDefaultDimensions(Pose pose) {
        IMultiModel model = getModel(Layer.SKIN, Part.HEAD).orElse(
                LMModelManager.INSTANCE.getDefaultModel());
        float height = model.getHeight(getCaps(), MMPose.convertPose(pose));
        float width = model.getWidth(getCaps(), MMPose.convertPose(pose));
        float eyeHeight = model.getEyeHeight(getCaps(), MMPose.convertPose(pose));
        EntityDimensions dimensions = EntityDimensions.scalable(width, height);
        dimensions = dimensions.scale(getAgeScale());
        dimensions = dimensions.withEyeHeight(eyeHeight * getAgeScale());
        return dimensions;
    }

    // 1.21.1: changeDimension is final, use afterChangingDimensions instead
    @Override
    public void restoreFrom(Entity entity) {
        super.restoreFrom(entity);
        // ディメンション移動の時に、自由行動地点を削除する
        if (entity instanceof LittleMaidEntity oldMaid &&
                oldMaid.getMaidMode() == MaidMode.FREEDOM) {
            this.setFreedomPos(null);
        }
    }

    // 環境音（昼夜・天候・体力・時計所持）に応じた周囲ボイス再生。
    // 発声頻度は外部ボイスパックの "LivingVoiceRate" パラメタ（保護コア B・既定 0.2）で制御する。
    @Override
    public void playAmbientSound() {
        if (this.level().isClientSide() ||
                this.dead ||
                getConfigHolder()
                        .getParameter("LivingVoiceRate")
                        .map(s -> {
                            try {
                                return Float.parseFloat(s);
                            } catch (Exception e) {
                                return null;
                            }
                        })
                        .orElse(0.2f) < random.nextFloat()) {
            return;
        }
        if (getHealth() / getMaxHealth() < 0.3F) {
            play(LMSounds.LIVING_WHINE);
        } else {
            if (tickCount % 4 == 0 &&
                    this.level().canSeeSky(this.blockPosition())) {
                Biome biome = this.level().getBiome(blockPosition()).value();
                if (biome.coldEnoughToSnow(
                        blockPosition(),
                        this.level().getSeaLevel())) {
                    play(LMSounds.LIVING_COLD);
                } else if (2 <= biome.getBaseTemperature()) {
                    play(LMSounds.LIVING_HOT);
                }
            } else if (tickCount % 4 == 1 && this.level().isRaining()) {
                var pos = blockPosition();
                Biome biome = this.level().getBiome(pos).value();
                if (biome.getPrecipitationAt(pos, pos.getY()) == Biome.Precipitation.RAIN)
                    play(LMSounds.LIVING_RAIN);
                else if (biome.getPrecipitationAt(pos, pos.getY()) == Biome.Precipitation.SNOW)
                    play(LMSounds.LIVING_SNOW);
            } else {
                if (this.getMainHandItem().getItem() == Items.CLOCK ||
                        this.getOffhandItem().getItem() == Items.CLOCK) {
                    int time = (int) (this.level().getGameTime() % 24000);
                    // 時間約23500-1500はse_living_morning
                    // 時間約12500-23500はse_living_night
                    if (time < 1500 || 23500 <= time) {
                        play(LMSounds.LIVING_MORNING);
                    } else if (12500 <= time) {
                        play(LMSounds.LIVING_NIGHT);
                    } else {
                        play(LMSounds.LIVING_DAYTIME);
                    }
                } else {
                    play(LMSounds.LIVING_DAYTIME);
                }
            }
        }
    }

    @Override
    public void die(DamageSource source) {
        super.die(source);
        // 死亡ボイスは必ず聞かせる
        this.playSoundCool = 0;
        play(LMSounds.DEATH);

        if (!this.level().isClientSide()) {
            TameableUtil.getTameOwner(this).ifPresent(owner -> {
                if (owner instanceof net.minecraft.server.level.ServerPlayer player) {
                    player.sendSystemMessage(
                            net.minecraft.network.chat.Component.translatable(
                                    "chat.littlemaidneo.maid_died",
                                    this.getDisplayName(),
                                    source.getLocalizedDeathMessage(this)));
                }
            });
        }
    }

    @Override
    public void remove(RemovalReason reason) {
        super.remove(reason);
        if (this.level() instanceof ServerLevel serverWorld &&
                reason.shouldDestroy()) {
            TameableUtil.getTameOwnerUuid(this).ifPresent(id -> {
                var maidSoulEntity = new MaidSoulEntity(
                        serverWorld,
                        new MaidSoul(this));
                maidSoulEntity.setPos(this.getX(), this.getY(), this.getZ());
                maidSoulEntity.setDeltaMovement(
                        new Vec3(
                                random.nextGaussian() * 0.02,
                                0.2,
                                random.nextGaussian() * 0.02));
                serverWorld.addFreshEntity(maidSoulEntity);
            });
        }
    }

    public void installMaidSoul(MaidSoul maidSoul) {
        load(
                TagValueInput.create(
                        ProblemReporter.DISCARDING,
                        this.registryAccess(),
                        maidSoul.getNbt()));
        this.setHealth(getMaxHealth());
        this.unsetRemoved();
        this.dead = false;
        this.deathTime = 0;
    }

    // 攻撃時: 吸血/通常ボイスを再生し、攻撃が通った場合はメインハンド武器の耐久を減らす。
    // hurtEnemy は他 Mod がプレイヤー前提で実装している可能性があるため try/catch で保護する。
    @Override
    public boolean doHurtTarget(ServerLevel serverLevel, Entity target) {
        boolean result = super.doHurtTarget(serverLevel, target);
        if (this.isBloodSuck()) {
            this.play(LMSounds.ATTACK_BLOOD_SUCK);
        } else {
            this.play(LMSounds.ATTACK);
        }
        // PlayerEntityのattack処理を参考に、武器の耐久地を減らす処理を実装する
        if (result) {
            ItemStack mainHandStack = this.getMainHandItem();
            Entity entity = target;
            if (target instanceof EnderDragonPart) {
                entity = ((EnderDragonPart) target).parentMob;
            }
            if (!mainHandStack.isEmpty() && entity instanceof LivingEntity) {
                // バニラではこのメソッドの第三引数にはプレイヤーエンティティしか渡されない
                // そのため、他Modにおいて必ずプレイヤーであると仮定して実装した場合にクラッシュする可能性がある
                // その対策にtry/catchを置いておく
                try {
                    mainHandStack
                            .getItem()
                            .hurtEnemy(mainHandStack, (LivingEntity) entity, this);
                } catch (Exception e) {
                    LittleMaidNeo.LOGGER.error(
                            "メイドさんの攻撃時に例外が発生しました。",
                            e);
                }
                if (mainHandStack.isEmpty()) {
                    this.setItemInHand(
                            InteractionHand.MAIN_HAND,
                            ItemStack.EMPTY);
                }
            }
        }
        return result;
    }

    // 被ダメージ処理: 不死/落下/非Mod耐性などのコンフィグ判定 → フレンドファイア除外 →
    // 戦闘/非戦闘モードによるダメージ係数適用 → 待機解除 → 状況別の被弾ボイス再生。
    @Override
    public boolean hurtServer(
            ServerLevel serverLevel,
            DamageSource source,
            float amount) {
        if (this.dead) {
            return super.hurtServer(serverLevel, source, amount);
        }
        // 味方のが当たってもちゃんと動くようにフレンド判定より前
        if (amount <= 0 && source.getDirectEntity() instanceof Snowball) {
            play(LMSounds.HURT_SNOW);
            return false;
        }
        LMRBConfig config = getConfig();
        if (config.health.nonMobDamageImmunity && source.getEntity() == null) {
            return false;
        }
        if (config.health.immortal &&
                !source.is(DamageTypes.FELL_OUT_OF_WORLD) &&
                !source.isCreativePlayer()) {
            return false;
        }
        if (config.health.fallImmunity && source.is(DamageTypes.FALL)) {
            return false;
        }
        Entity attacker = source.getEntity();
        // Friendからの攻撃を除外
        if (!config.health.enableFriendlyFire &&
                attacker instanceof LivingEntity &&
                isFriend((LivingEntity) attacker)) {
            return false;
        }

        float factor = config.health.generalMaidDamageFactor;
        if ((config.health.enableWorkInEmergency || !isEmergency()) &&
                !TameableUtil.isWait(this) &&
                this.getMode().map(Mode::isBattleMode).orElse(false)) {
            factor *= config.health.battleModeMaidDamageFactor;
        } else {
            factor *= config.health.nonBattleModeMaidDamageFactor;
        }
        amount *= factor;

        boolean isHurtTime = 0 < this.hurtTime;
        boolean result = super.hurtServer(serverLevel, source, amount);
        if (!isHurtTime) {
            if (result &&
                    0 < amount &&
                    TameableUtil.isWait(this) &&
                    TameableUtil.getTameOwnerUuid(this).isPresent()) {
                TameableUtil.setWait(this, false);
            }
            if (!result || amount <= 0F) {
                play(LMSounds.HURT_NO_DAMAGE);
            } else if (amount > 0F && this.isBlocking()) {
                play(LMSounds.HURT_GUARD);
            } else if (source.is(DamageTypes.FALL)) {
                play(LMSounds.HURT_FALL);
            } else if (source.type().effects() == DamageEffects.BURNING) {
                play(LMSounds.HURT_FIRE);
            } else {
                play(LMSounds.HURT);
            }
        }
        return result;
    }

    public boolean isEmergency() {
        LMRBConfig config = getConfig();
        // 危機閾値以下の体力の場合、危機状態とする
        return (this.getHealth() / this.getMaxHealth() <= config.health.emergencyMaidHealthThreshold);
    }

    @Override
    public void setHealth(float health) {
        LMRBConfig config = getConfig();
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
        if (isBloodSuck())
            play(LMSounds.LAUGHTER);

        int xp = other.getExperienceReward(world, source.getEntity());
        if (xp > 0) {
            this.xpReward = Math.min(100000, this.xpReward + xp);
        }

        return super.killedEntity(world, other, source);
    }

    // 射撃

    // 弓/クロスボウによる遠隔攻撃。弾の取得・矢の生成・射出・効果音までを担当する。
    @Override
    public void performRangedAttack(LivingEntity target, float pullProgress) {
        var stack = this.getMainHandItem();
        // 弾が無い場合は実行されないはずだが、念のためチェック
        var arrowStack = this.getProjectile(stack);
        // メイドさんの弓は Infinity（無限矢）を意図的にサポートせず、常に矢を消費する仕様。
        // 弾が無ければ射撃自体を行わない。
        boolean isInfinite = false;
        if (arrowStack.isEmpty() && !isInfinite) {
            return;
        }
        if (stack.getItem() instanceof BowItem bowItem) {
            var arrow = ProjectileUtil.getMobArrow(
                    this,
                    arrowStack,
                    pullProgress,
                    stack);
            if (arrowStack.getItem() instanceof ArrowItem && !isInfinite) {
                arrow.pickup = AbstractArrow.Pickup.ALLOWED;
            }
            arrow = EPEntityUtil.arrowCustomHook(bowItem, arrow);
            double xDiff = target.getX() - this.getX();
            double yDiff = target.getEyeY() - arrow.getY();
            double zDiff = target.getZ() - this.getZ();
            double horizonLen = Math.sqrt(xDiff * xDiff + zDiff * zDiff);
            arrow.shoot(
                    xDiff,
                    yDiff + horizonLen * 0.025,
                    zDiff,
                    pullProgress *
                            3.0f *
                            getConfig().work.archerShootVelocityFactor,
                    14 - 2 * 4);
            this.playSound(
                    SoundEvents.ARROW_SHOOT,
                    1.0f,
                    1.0f / (this.getRandom().nextFloat() * 0.4f + 1.2f) +
                            pullProgress * 0.5f);
            this.level().addFreshEntity(arrow);
            arrowStack.shrink(1);
        } else if (stack.getItem() instanceof CrossbowItem) {
            this.performCrossbowAttack(
                    this,
                    CrossbowItemInvoker.getSpeed(
                            stack.get(DataComponents.CHARGED_PROJECTILES)));
        }
    }

    // クロスボウ

    public boolean isCharging() {
        return this.entityData.get(CHARGING);
    }

    @Override
    public void setChargingCrossbow(boolean charging) {
        this.entityData.set(CHARGING, charging);
    }

    // 1.21.1: CrossbowAttackMobからshootCrossbowProjectile/shootメソッドが削除された。
    // performCrossbowAttack(default) が CrossbowItem.performShooting を直接呼ぶ形に変更されている。
    // クロスボウには弓の archerShootVelocityFactor を適用していない（バニラ初速のまま）。
    // 弾道調整が必要になった場合は performCrossbowAttack をオーバーライドする。

    @Override
    public void onCrossbowAttackPerformed() {
    }

    // 安全移動: 落下/危険ブロックでメイドさんが死なないよう、縁での移動ベクトルを押し戻す。
    // ロジックは LMSafeMovement へ委譲（override 本体のみ残す）。
    @Override
    protected Vec3 maybeBackOffFromEdge(Vec3 movement, MoverType type) {
        return LMSafeMovement.maybeBackOffFromEdge(this, movement, type);
    }

    // --- LMSafeMovement への移譲ブリッジ（protected メソッド / フィールドの同パッケージ公開） ---
    // 危険高度のしきい値。マイナスの値も返すことを利用しているため、バージョンアップ/mixin での仕様変更に注意。
    float getDangerHeightThreshold_LM() {
        int fallDamage = calculateFallDamage(0, 1);
        return -fallDamage;
    }

    double fallDistance_LM() {
        return this.fallDistance;
    }

    // 経験値（Mob.xpReward は protected のため LMInteractionHandler 向けに公開）
    int getXpReward_LM() {
        return this.xpReward;
    }

    // リード接続位置。getEyeHeight() ベースで算出するため、モデルごとに eyeHeight が
    // 異なっても（getDefaultDimensions で per-model に設定済み）破綻せず追従する。
    @Override
    public Vec3 getLeashOffset() {
        return new Vec3(0.0, this.getEyeHeight() - 0.15f, 1f / 16f);
    }

    // 右クリック処理。アイテム別の分岐ロジックは LMInteractionHandler へ委譲（override 本体のみ残す）。
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
                LMRBCriteria.CONTRACT_MAID.trigger((ServerPlayer) player, this);
            }
        } else {
            this.level().broadcastEntityEvent(this, (byte) 71);
        }
        this.setOwnerUUID(player.getUUID());
        setContractMM(true);
        // 契約状態の更新
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

    // GUI開くやつ
    public void openInventory(Player player) {
        if (player.level().isClientSide()) {
            return;
        }
        setLastHurtByMob(null);
        getNavigation().stop();
        final LittleMaidEntity maid = this;
        ((ServerPlayer) player).openMenu(screenFactory, buf -> {
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

    // インベントリ関連

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

    // 防具耐久消費。バニラ準拠でダメージの 1/4（最低 1）を各防具スロットに適用する。
    // DAMAGE_RESISTANT な装備および非 EQUIPPABLE はスキップ。
    @Override
    protected void hurtArmor(DamageSource source, float amount) {
        if (!(amount <= 0.0f)) {
            if ((amount /= 4.0f) < 1.0f) {
                amount = 1.0f;
            }
            EquipmentSlot[] armorSlots = {
                    EquipmentSlot.FEET,
                    EquipmentSlot.LEGS,
                    EquipmentSlot.CHEST,
                    EquipmentSlot.HEAD,
            };
            for (EquipmentSlot slot : armorSlots) {
                ItemStack stack = this.getItemBySlot(slot);
                if (stack.isEmpty())
                    continue;
                var resistant = stack.get(DataComponents.DAMAGE_RESISTANT);
                if (resistant != null && resistant.isResistantTo(source))
                    continue;
                if (!stack.has(DataComponents.EQUIPPABLE))
                    continue;
                stack.hurtAndBreak((int) amount, this, slot);
            }
        }
    }

    @Override
    protected void hurtHelmet(DamageSource source, float amount) {
        if (!(amount <= 0.0f)) {
            if ((amount /= 4.0f) < 1.0f) {
                amount = 1.0f;
            }
            var stack = getItemBySlot(EquipmentSlot.HEAD);
            if (stack.isEmpty())
                return;
            var resistant = stack.get(DataComponents.DAMAGE_RESISTANT);
            if (resistant != null && resistant.isResistantTo(source))
                return;
            if (!stack.has(DataComponents.EQUIPPABLE))
                return;
            stack.hurtAndBreak((int) amount, this, EquipmentSlot.HEAD);
        }
    }

    // コマンド等によるメイドインベントリの操作用（/item replaceコマンドなどから参照されます）
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

    // 射撃武器に対応する弾を返す。手持ち優先 → インベントリ走査の順で探索し、
    // EPEntityUtil.arrowCustomHook で他 Mod の矢カスタムフックを通す。
    @Override
    public ItemStack getProjectile(ItemStack stack) {
        if (!(stack.getItem() instanceof ProjectileWeaponItem ranged)) {
            return ItemStack.EMPTY;
        }
        Predicate<ItemStack> predicate = ranged.getSupportedHeldProjectiles();
        ItemStack itemStack = ProjectileWeaponItem.getHeldProjectile(
                this,
                predicate);
        if (!itemStack.isEmpty()) {
            return EPEntityUtil.arrowCustomHook(this, stack, itemStack);
        }
        predicate = ranged.getAllSupportedProjectiles();
        var inv = getInventory();
        for (int i = 0; i < inv.getContainerSize(); ++i) {
            ItemStack itemStack2 = inv.getItem(i);
            if (predicate.test(itemStack2)) {
                return EPEntityUtil.arrowCustomHook(this, stack, itemStack2);
            }
        }
        return EPEntityUtil.arrowCustomHook(this, stack, ItemStack.EMPTY);
    }

    // 防具の更新
    @Override
    public void setItemSlot(EquipmentSlot slot, ItemStack stack) {
        super.setItemSlot(slot, stack);

        if (slot.isArmor()) {
            multiModel.updateArmor();
        }
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

    // idFactor は UUID から決まる安定した擬似乱数シード（テクスチャ/ボイスの個体差に使用）。
    // UUID 変更時に必ず再計算されるよう setUUID をフックする。
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

    // テイム関連

    public void setOwnerUUID(@Nullable UUID uuid) {
        if (uuid != null) {
            TameableUtil.setTameOwnerUuid(this, uuid);
        }
        this.setContract(true);
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

    // 1.21.1: method_48926 -> level() に変更
    // OwnableEntityインターフェースの要求を満たす
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

    // 加速機能

    public int getTickMultiple() {
        return this.isAcceleration()
                ? getConfig().misc.accelerationMultiple
                : 1;
    }

    public void setAccelerationTicks(int ticks) {
        this.accelerationTicks = ticks;
        if (ticks > 0) {
            this.entityData.set(ACCELERATE, true);
        }
    }

    public void decAccelerationTicks() {
        if (this.accelerationTicks > 0) {
            this.accelerationTicks--;
        }
        if (this.accelerationTicks <= 0) {
            this.accelerationTicks = 0;
            this.entityData.set(ACCELERATE, false);
        }
    }

    public int getAccelerationTicks() {
        return this.accelerationTicks;
    }

    public boolean isAcceleration() {
        return this.entityData.get(ACCELERATE);
    }

    // お給料

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

    // お給料受け取り

    @Override
    public void listenSalaryBoxPos(BlockPos pos) {
        itemContractable.listenSalaryBoxPos(pos);
    }

    // モード機能

    @Override
    public Optional<Mode> getMode() {
        if (this.isStrike()) {
            return Optional.empty();
        }
        return hasModeImpl.getMode();
    }

    @Override
    public void writeModeData(ValueOutput output) {
        hasModeImpl.writeModeData(output);
    }

    @Override
    public void readModeData(ValueInput input) {
        hasModeImpl.readModeData(input);
    }

    public void addMode(Mode mode) {
        hasModeImpl.addMode(mode);
    }

    public void addAllMode(Collection<Mode> mode) {
        hasModeImpl.addAllMode(mode);
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
        // 注: 本来 isFriend と ATTACK_PROHIBITED は別概念。専用のフレンドタグ体系の導入は
        //     機能バックログ（Phase 7・TargetingSystem 拡張）で扱う。現状は以下の暫定判定:
        // 暫定でテイム済みのモブは攻撃対象から外す
        if (entity instanceof OwnableEntity tameable &&
                TameableUtil.hasTameOwner(tameable)) {
            return true;
        }
        // 暫定: ご主人がいるなら、プレイヤーを攻撃対象にしない
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

    // 構え

    public boolean isAimingBow() {
        return this.getLMMFlag(AIMING_INDEX);
    }

    public void setAimingBow(boolean aiming) {
        this.setLMMFlag(AIMING_INDEX, aiming);
    }

    // マルチモデル関連

    @Override
    public boolean isAllowChangeTexture(
            Entity entity,
            TextureHolder textureHolder,
            Layer layer,
            Part part) {
        return multiModel.isAllowChangeTexture(
                entity,
                textureHolder,
                layer,
                part);
    }

    @Override
    public void setTextureHolder(
            TextureHolder textureHolder,
            Layer layer,
            Part part) {
        multiModel.setTextureHolder(textureHolder, layer, part);
        if (layer == Layer.SKIN) {
            refreshDimensions();
        }
    }

    @Override
    public TextureHolder getTextureHolder(Layer layer, Part part) {
        return multiModel.getTextureHolder(layer, part);
    }

    @Override
    public void setColorMM(TextureColors textureColor) {
        multiModel.setColorMM(textureColor);
    }

    @Override
    public TextureColors getColorMM() {
        return multiModel.getColorMM();
    }

    @Override
    public void setContractMM(boolean isContract) {
        multiModel.setContractMM(isContract);
    }

    /**
     * マルチモデルの使用テクスチャが契約時のものかどうか
     * ※実際に契約状態かどうかをチェックする場合、
     * {@link TameableUtil#getTameOwnerUuid(OwnableEntity)}がisPresent()かでチェックすること
     */
    @Override
    public boolean isContractMM() {
        return multiModel.isContractMM();
    }

    @Override
    public Optional<IMultiModel> getModel(Layer layer, Part part) {
        return multiModel.getModel(layer, part);
    }

    @Override
    public Optional<Identifier> getTexture(
            Layer layer,
            Part part,
            boolean isLight) {
        return multiModel.getTexture(layer, part, isLight);
    }

    @Override
    public IModelCaps getCaps() {
        return caps;
    }

    @Override
    public boolean isArmorVisible(Part part) {
        return multiModel.isArmorVisible(part);
    }

    @Override
    public boolean isArmorGlint(Part part) {
        return multiModel.isArmorGlint(part);
    }

    public boolean isPlayingSnow() {
        return this.getLMMFlag(PLAYING_SNOW_INDEX);
    }

    public void setPlayingSnow(boolean isPlayingSnow) {
        this.setLMMFlag(PLAYING_SNOW_INDEX, isPlayingSnow);
    }

    // 音声関係

    // 通常ボイス再生。クールダウン(playSoundCool)中は再生しない。
    // 強制再生が必要な場合はクールダウンを無視する playForce() を使う。
    // クールダウン長は getConfig().misc.playSoundInterval でコンフィグ化済み。
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
        soundPlayer.play(soundName);
    }

    @Override
    public void setConfigHolder(ConfigHolder configHolder) {
        soundPlayer.setConfigHolder(configHolder);
    }

    @Override
    public ConfigHolder getConfigHolder() {
        return soundPlayer.getConfigHolder();
    }

    public static LMRBConfig getConfig() {
        return LMRBConfig.get();
    }

    // 注: 旧 LMStareAtHeldItemGoal（手持ちアイテム注視 Goal）は Phase 7 で
    //     MaidStareBehavior（Brain Behavior）へ移行済み。Goal 版は孤立デッドコードとなったため削除した。

    public static class MaidSoul {

        private final CompoundTag nbt;
        private final UUID uuid;
        private final String name;

        public MaidSoul(LittleMaidEntity maid) {
            TagValueOutput output = TagValueOutput.createWithContext(
                    ProblemReporter.DISCARDING,
                    maid.registryAccess());
            maid.saveWithoutId(output);
            CompoundTag tag = output.buildResult();
            tag.putString("Name", maid.getName().getString());
            this.nbt = tag;
            this.name = maid.getName().getString();
            this.uuid = maid.getUUID();
        }

        private MaidSoul(CompoundTag nbt, UUID uuid, String name) {
            this.nbt = nbt;
            this.uuid = uuid;
            this.name = name;
        }

        public static MaidSoul fromNbt(CompoundTag nbt) {
            UUID uuid = nbt
                    .getIntArray("UUID")
                    .filter(a -> a.length == 4)
                    .map(UUIDUtil::uuidFromIntArray)
                    .orElse(Util.NIL_UUID);
            String name = nbt.getStringOr("Name", "");
            return new MaidSoul(nbt, uuid, name);
        }

        public CompoundTag getNbt() {
            return nbt;
        }

        public UUID getUuid() {
            return this.uuid;
        }

        public Optional<UUID> getOwnerUUID() {
            return nbt
                    .getIntArray("Owner")
                    .filter(a -> a.length == 4)
                    .map(UUIDUtil::uuidFromIntArray);
        }

        public String getName() {
            return this.name;
        }
    }

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
                this.level().broadcastEntityEvent(this, (byte) 72); // 音符パーティクル
                break;
            }
        }
    }

    protected void showTransAmParticles() {
        for (int i = 0; i < 20; ++i) {
            double d = this.random.nextGaussian() * 0.02;
            double e = this.random.nextGaussian() * 0.02;
            double f = this.random.nextGaussian() * 0.02;
            this.level().addParticle(
                    ParticleTypes.FLAME,
                    this.getRandomX(1.0),
                    this.getRandomY() + 0.5,
                    this.getRandomZ(1.0),
                    d,
                    e,
                    f);
        }
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
}
