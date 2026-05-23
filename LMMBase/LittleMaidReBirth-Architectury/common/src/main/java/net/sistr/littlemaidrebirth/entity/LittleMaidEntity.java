package net.sistr.littlemaidrebirth.entity;

import com.google.common.collect.Lists;
import dev.architectury.extensions.network.EntitySpawnExtension;
import dev.architectury.registry.menu.MenuRegistry;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.Util;
import net.minecraft.commands.arguments.EntityAnchorArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.goal.*;
import net.minecraft.world.item.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.util.Mth;
import net.minecraft.world.*;
import net.minecraft.world.damagesource.DamageEffects;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
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
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.navigation.GroundPathNavigation;
import net.minecraft.world.entity.boss.EnderDragonPart;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.CrossbowAttackMob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.entity.projectile.Snowball;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArrowItem;
import net.minecraft.world.item.BowItem;
import net.minecraft.world.item.CrossbowItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUtils;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.ProjectileWeaponItem;
import net.minecraft.world.item.SaddleItem;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.EntityGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.pathfinder.PathType;
import net.minecraft.world.level.pathfinder.WalkNodeEvaluator;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.sistr.littlemaidmodelloader.entity.compound.IHasMultiModel;
import net.sistr.littlemaidmodelloader.entity.compound.MultiModelCompound;
import net.sistr.littlemaidmodelloader.entity.compound.SoundPlayable;
import net.sistr.littlemaidmodelloader.entity.compound.SoundPlayableCompound;
import net.sistr.littlemaidmodelloader.maidmodel.IModelCaps;
import net.sistr.littlemaidmodelloader.multimodel.IMultiModel;
import net.sistr.littlemaidmodelloader.multimodel.layer.MMPose;
import net.sistr.littlemaidmodelloader.network.SyncMultiModelPacket;
import net.sistr.littlemaidmodelloader.resource.holder.ConfigHolder;
import net.sistr.littlemaidmodelloader.resource.holder.TextureHolder;
import net.sistr.littlemaidmodelloader.resource.manager.LMConfigManager;
import net.sistr.littlemaidmodelloader.resource.manager.LMModelManager;
import net.sistr.littlemaidmodelloader.resource.manager.LMTextureManager;
import net.sistr.littlemaidmodelloader.resource.util.LMSounds;
import net.sistr.littlemaidmodelloader.resource.util.TextureColors;
import net.sistr.littlemaidrebirth.LMRBMod;
import net.sistr.littlemaidrebirth.advancement.criterion.LMRBCriteria;
import net.sistr.littlemaidrebirth.api.mode.Mode;
import net.sistr.littlemaidrebirth.api.mode.ModeManager;
import net.sistr.littlemaidrebirth.config.LMRBConfig;
import net.sistr.littlemaidrebirth.entity.goal.*;
import net.sistr.littlemaidrebirth.entity.mode.HasMode;
import net.sistr.littlemaidrebirth.entity.mode.HasModeImpl;
import net.sistr.littlemaidrebirth.entity.mode.ModeWrapperGoal;
import net.sistr.littlemaidrebirth.entity.targeting.TargetIdentifier;
import net.sistr.littlemaidrebirth.entity.targeting.TargetTagManager;
import net.sistr.littlemaidrebirth.entity.targeting.TargetTagManagerImpl;
import net.sistr.littlemaidrebirth.entity.targeting.TargetingSystem;
import net.sistr.littlemaidrebirth.entity.util.*;
import net.sistr.littlemaidrebirth.mixin.CrossbowItemInvoker;
import net.sistr.littlemaidrebirth.network.SpawnLittleMaidPacket;
import net.sistr.littlemaidrebirth.setup.Registration;
import net.sistr.littlemaidrebirth.tags.LMTags;
import net.sistr.littlemaidrebirth.util.LMCollidable;
import net.sistr.littlemaidrebirth.util.ReachAttributeUtil;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;

import java.util.*;
import java.util.function.BiPredicate;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Collectors;

//メイドさん本体
//TODO 声タイミング調整
//TODO ドロップアイテム
//TODO 契約期間の残りは砂糖をあげた時の音符の色で判断してください。
//TODO 雪合戦 日が暮れると遊びは終わり
//TODO モードトリガーアイテム指定
//TODO 署名済みではない書き込み可能な本にパラメータを記述して、メイドさんに右クリックで使用すると値が反映されます。
//TODO メイドさんも金リンゴや牛乳を飲めるようになりました。
//TODO つまみ食い
//TODO ダメージ/水没待機解除 実装済みだっけ？
//TODO トランザム
//TODO 経験値
//TODO 座ったメイドでも追従時に立つように
//TODO スト時砂糖ドカ食い
//TODO GUIを開いている時に動きを止める
//TODO リスポ
//TODO 死亡メッセ追加
//TODO はしご
//TODO おさわり厳禁：他人のメイドに触ると殴られる
//TODO 他人のメイドに視線を合わせた時、ご主人の名札を浮かべる
public class LittleMaidEntity extends TamableAnimal implements EntitySpawnExtension, HasInventory,
        Contractable, HasMode, AimingPoseable, IHasMultiModel, SoundPlayable, HasMovingMode,
        CrossbowAttackMob, SalaryBoxPosListener, TargetTagManager {

    @Override
    public boolean isFood(ItemStack stack) {
        return stack.is(Items.WHEAT_SEEDS) || stack.is(Items.BEETROOT_SEEDS) || stack.is(Items.MELON_SEEDS)
                || stack.is(Items.PUMPKIN_SEEDS);
    }

    // LMM_FLAGSのindex
    // TODO enumにまとめる
    private static final int WAIT_INDEX = 0;
    private static final int AIMING_INDEX = 1;
    private static final int BEGGING_INDEX = 2;
    private static final int BLOOD_SUCK_INDEX = 3;
    private static final int STRIKE_INDEX = 4;
    private static final int PLAYING_SNOW_INDEX = 5;
    private static final EntityDataAccessor<Byte> LMM_FLAGS = SynchedEntityData.defineId(LittleMaidEntity.class,
            EntityDataSerializers.BYTE);
    private static final EntityDataAccessor<Byte> MOVING_MODE = SynchedEntityData.defineId(LittleMaidEntity.class,
            EntityDataSerializers.BYTE);
    private static final EntityDataAccessor<String> MODE_NAME = SynchedEntityData.defineId(LittleMaidEntity.class,
            EntityDataSerializers.STRING);
    private static final EntityDataAccessor<Boolean> CHARGING = SynchedEntityData.defineId(LittleMaidEntity.class,
            EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> ACCELERATE = SynchedEntityData.defineId(LittleMaidEntity.class,
            EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Byte> MASTER_STANCE = SynchedEntityData.defineId(LittleMaidEntity.class,
            EntityDataSerializers.BYTE);
    // エンチャントの瓶はランダムな経験値を排出するため、その平均値を作成コストとする
    private static final int EXPERIENCE_BOTTLE_COST = 7;

    // 移譲s
    public final LMHasInventory littleMaidInventory = new LMHasInventory();
    public final LMItemContractable<LittleMaidEntity> itemContractable = new LMItemContractable<>(this,
            () -> getConfig().contract.consumeSalaryInterval,
            () -> getConfig().contract.unpaidDaysLimit,
            (ItemStack stack) -> stack.is(LMTags.Items.MAIDS_SALARY));
    public final HasModeImpl hasModeImpl = new HasModeImpl(this, this, new HashSet<>(),
            mode -> {
                setModeName(mode != null ? mode.getName() : "");
            });
    public final MultiModelCompound multiModel;
    public final SoundPlayableCompound soundPlayer;
    private final LMScreenHandlerFactory screenFactory = new LMScreenHandlerFactory(this);
    private final IModelCaps caps = new LittleMaidModelCaps(this);
    private final TargetTagManager targetTagManager;

    private final Map<Mob, Predicate<Mob>> fleeEntities = new HashMap<>(); // TODO クラス化検討
    @Nullable
    private BlockPos freedomPos;
    // 首傾げのやつ
    @Environment(EnvType.CLIENT)
    private float interestedAngle;
    @Environment(EnvType.CLIENT)
    private float prevInterestedAngle;
    private int playSoundCool;
    private int idFactor;
    public int experiencePickUpDelay;
    // クライアント側のこの値は信用ならない
    private int accelerationTicks;
    private boolean maidManagerRegistered;

    // コンストラクタ
    public LittleMaidEntity(EntityType<LittleMaidEntity> type, Level worldIn) {
        super(type, worldIn);
        this.moveControl = new FixedMoveControl(this);
        ((GroundPathNavigation) getNavigation()).setCanOpenDoors(true);
        multiModel = new MultiModelCompound(this,
                LMTextureManager.INSTANCE.getTexture("Default")
                        .orElseThrow(() -> new IllegalStateException("デフォルトテクスチャが存在しません。")),
                LMTextureManager.INSTANCE.getTexture("Default")
                        .orElseThrow(() -> new IllegalStateException("デフォルトテクスチャが存在しません。")));
        soundPlayer = new SoundPlayableCompound(this,
                () -> multiModel.getTextureHolder(Layer.SKIN, Part.HEAD).getTextureName());
        addDefaultModes(this);
        initIdFactor();
        setRandomTexture();
        setRandomVoice();
        this.targetTagManager = new TargetTagManagerImpl(worldIn);
    }

    // 基本使わない
    public LittleMaidEntity(Level world) {
        this(Registration.LITTLE_MAID_MOB.get(), world);
    }

    // スタティックなメソッド

    // TODO メイドさんに付与する属性の再考
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

    // TODO コンフィグでスポーン条件を設定可能にする
    public static boolean isValidNaturalSpawn(LevelAccessor world, BlockPos pos) {
        return world.getBlockState(pos.below()).isCollisionShapeFullBlock(world, pos)
                && world.getRawBrightness(pos, 0) > 8;
    }

    public static boolean resurrectionMaid(ServerLevel world, BlockPos pos, Player player) {
        var maidSouls = ((MaidManager) player).getMaidSouls();
        if (maidSouls.isEmpty()) {
            return false;
        }
        for (LittleMaidEntity.MaidSoul maidSoul : maidSouls) {
            var maid = Registration.LITTLE_MAID_MOB.get().create(world);
            if (maid != null) {
                maid.installMaidSoul(maidSoul);
                maid.moveTo(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5);

                maid.setMovingMode(MovingMode.ESCORT);
                TameableUtil.setWait(maid, true);
                maid.lookAt(EntityAnchorArgument.Anchor.EYES, player.getEyePosition());
                maid.getLookControl().setLookAt(player);

                maid.clearFire();
                maid.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 100, 10));

                world.addFreshEntity(maid);

                LMRBCriteria.RESURRECT_MAID.trigger((ServerPlayer) player, maid);
            }
        }
        ((MaidManager) player).clearMaidSouls();

        world.removeBlock(pos, false);
        world.playSound(null, pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5,
                SoundEvents.FIREWORK_ROCKET_TWINKLE, SoundSource.PLAYERS, 1.0f, 2.0f);
        world.playSound(null, pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5,
                SoundEvents.FIREWORK_ROCKET_BLAST, SoundSource.PLAYERS, 1.0f, 2.0f);
        // TODO 演出強化
        world.sendParticles(ParticleTypes.EXPLOSION,
                pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5,
                1, 0, 0, 0, 0);
        float size = 0.5f;
        int count = 10;
        double delta = 1.5;
        world.sendParticles(
                new DustParticleOptions(new Vector3f(1.0f, 0.0f, 0.0f), size),
                pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5,
                count, delta, delta, delta, 0);
        world.sendParticles(
                new DustParticleOptions(new Vector3f(1.0f, 0.65f, 0.0f), size),
                pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5,
                count, delta, delta, delta, 0);
        world.sendParticles(
                new DustParticleOptions(new Vector3f(1.0f, 1.0f, 0.0f), size),
                pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5,
                count, delta, delta, delta, 0);
        world.sendParticles(
                new DustParticleOptions(new Vector3f(0.0f, 1.0f, 0.0f), size),
                pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5,
                count, delta, delta, delta, 0);
        world.sendParticles(
                new DustParticleOptions(new Vector3f(0.0f, 1.0f, 1.0f), size),
                pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5,
                count, delta, delta, delta, 0);
        world.sendParticles(
                new DustParticleOptions(new Vector3f(0.0f, 0.0f, 1.0f), size),
                pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5,
                count, delta, delta, delta, 0);
        world.sendParticles(
                new DustParticleOptions(new Vector3f(0.5f, 0.0f, 1.0f), size),
                pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5,
                count, delta, delta, delta, 0);
        world.sendParticles(
                ParticleTypes.HEART,
                pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5,
                count, delta, delta, delta, 0);

        return true;
    }

    // 登録メソッドたち

    @Override
    protected void registerGoals() {
        int priority = -1;
        LMRBConfig config = getConfig();

        // 緊急テレポート
        this.goalSelector.addGoal(priority,
                new LMTeleportTameOwnerGoal(this,
                        () -> config.movement.emergencyTeleportStartDistance) {
                    @Override
                    public boolean canUse() {
                        return isEmergency()
                                && LittleMaidEntity.this.hurtTime > 0
                                && !TameableUtil.isWait(LittleMaidEntity.this)
                                && super.canUse();
                    }
                });

        this.goalSelector.addGoal(++priority, new FloatGoal(this));
        this.goalSelector.addGoal(++priority, new OpenDoorGoal(this, true));

        this.goalSelector.addGoal(++priority, new LMHealMyselfGoal(this,
                () -> config.health.healInterval,
                () -> config.health.healAmount,
                stack -> stack.is(LMTags.Items.MAIDS_SALARY)));

        this.goalSelector.addGoal(++priority, new LMCollectSalaryFromContainerGoal<>(this));

        this.goalSelector.addGoal(++priority, new WaitGoal<>(this));

        this.goalSelector.addGoal(++priority, new LMTeleportTameOwnerGoal(this,
                () -> config.movement.teleportStartDistance));

        // 危険な敵からの逃避
        this.goalSelector.addGoal(++priority, new AvoidEntityGoal<>(this, Mob.class,
                config.target.dangerousAvoidDistance,
                config.movement.followSpeed, config.movement.sprintSpeed,
                entity -> fleeEntities.containsKey(entity)) {
            @Override
            public void tick() {
                fleeEntities.entrySet()
                        .removeIf(entry -> entry.getValue().test(entry.getKey()));
                super.tick();
            }

            @Override
            public void stop() {
                super.stop();
                this.mob.getNavigation().stop();
            }
        });

        this.goalSelector.addGoal(++priority, new ModeWrapperGoal<>(this) {
            @Override
            public boolean canUse() {
                return !this.owner.isStrike()
                        && (config.health.enableWorkInEmergency || !isEmergency())
                        && super.canUse();
            }

            @Override
            public boolean canContinueToUse() {
                return !this.owner.isStrike()
                        && (config.health.enableWorkInEmergency || !isEmergency())
                        && super.canContinueToUse();
            }
        });

        this.goalSelector.addGoal(++priority,
                new HasMMFollowTameOwnerGoal<>(
                        this,
                        () -> config.movement.sprintSpeed,
                        () -> config.movement.sprintStartDistance,
                        () -> config.movement.sprintEndDistance) {
                    @Override
                    public void start() {
                        super.start();
                        this.tameable.setSprinting(true);
                    }

                    @Override
                    public void stop() {
                        super.stop();
                        this.tameable.setSprinting(false);
                    }
                });

        this.goalSelector.addGoal(++priority, new FollowAtHeldItemGoal<>(this,
                () -> config.misc.stareAtSalaryRange,
                stack -> stack.is(LMTags.Items.MAIDS_SALARY),
                () -> config.misc.followAtHeldSalaryRange,
                true));
        this.goalSelector.addGoal(++priority, new LMStareAtHeldItemGoal<>(this,
                () -> config.misc.stareAtSalaryRange,
                stack -> stack.is(LMTags.Items.MAIDS_SALARY),
                true));

        // TODO 頭の装飾品を仕舞わないようにする
        this.goalSelector.addGoal(++priority, new LMStoreItemToContainerGoal<>(this,
                stack -> stack.is(LMTags.Items.MAIDS_SALARY)
                        || this.hasModeImpl.getMode()
                                .filter(mode -> mode.getModeType().isModeItem(stack))
                                .isPresent(),
                () -> config.work.searchContainerRange));

        this.goalSelector.addGoal(++priority, new LMMoveToDropItemGoal(this,
                () -> config.movement.pickupItemRange,
                () -> config.movement.pickupItemFrequency,
                () -> config.movement.pickupItemSpeed) {
            @Override
            public boolean canUse() {
                return TameableUtil.hasTameOwner(LittleMaidEntity.this)
                        && (config.health.enableWorkInEmergency || !isEmergency())
                        && super.canUse();
            }

            @Override
            public List<ItemEntity> findAroundDropItem() {
                return TameableUtil.getTameOwner(maid)
                        .map(owner -> {
                            return super.findAroundDropItem().stream()
                                    .filter(item -> !this.isOwnerRange(item, owner))
                                    .collect(Collectors.toList());
                            // ご主人様が存在しない場合は普通にとる
                        }).orElse(super.findAroundDropItem());
            }
        });

        this.goalSelector.addGoal(++priority,
                new HasMMFollowTameOwnerGoal<>(
                        this,
                        () -> config.movement.followSpeed,
                        () -> config.movement.followStartDistance,
                        () -> config.movement.followEndDistance));

        this.goalSelector.addGoal(++priority, new PlaySnowGoal(this));

        this.goalSelector.addGoal(++priority, new RedstoneTraceGoal(this,
                () -> config.movement.tracerSpeed));
        this.goalSelector.addGoal(++priority, new FreedomGoal<>(this,
                config.movement.freedomSpeed,
                () -> config.movement.freedomRange));

        // 野良
        this.goalSelector.addGoal(++priority, new LMMoveToDropItemGoal(this,
                () -> config.movement.pickupItemRange,
                () -> config.movement.pickupItemFrequency,
                () -> config.movement.pickupItemSpeed) {
            @Override
            public boolean canUse() {
                return !TameableUtil.hasTameOwner(LittleMaidEntity.this)
                        && config.misc.canPickupItemByNoOwner
                        && (config.health.enableWorkInEmergency || !isEmergency())
                        && super.canUse();
            }
        });
        this.goalSelector.addGoal(++priority, new PanicGoal(this, config.movement.escapeSpeed) {
            @Override
            public boolean canUse() {
                return !TameableUtil.hasTameOwner(LittleMaidEntity.this)
                        && super.canUse();
            }
        });
        this.goalSelector.addGoal(++priority, new FollowAtHeldItemGoal<>(this,
                () -> config.misc.stareAtEmployItemRange,
                stack -> stack.is(LMTags.Items.MAIDS_EMPLOYABLE),
                () -> config.misc.followAtHeldEmployItemRange,
                false));
        this.goalSelector.addGoal(++priority, new LMStareAtHeldItemGoal<>(this,
                () -> config.misc.stareAtEmployItemRange,
                stack -> stack.is(LMTags.Items.MAIDS_EMPLOYABLE),
                false));

        this.goalSelector.addGoal(++priority, new WaterAvoidingRandomStrollGoal(this, config.movement.freedomSpeed) {
            @Override
            public boolean canUse() {
                return !TameableUtil.hasTameOwner(LittleMaidEntity.this)
                        && super.canUse();
            }
        });

        // 視線
        this.goalSelector.addGoal(++priority, new LookAtPlayerGoal(this, LivingEntity.class, 8.0F));
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
    }

    public void addDefaultModes(LittleMaidEntity maid) {
        this.hasModeImpl.addAllMode(ModeManager.INSTANCE.createModes(maid));
    }

    // 読み書き系

    @Override
    public void addAdditionalSaveData(CompoundTag nbt) {
        super.addAdditionalSaveData(nbt);
        nbt.putByte("maidVersion", (byte) 2);

        writeInventory(nbt, this.registryAccess());
        nbt.putInt("XpTotal", this.xpReward);
        if (TameableUtil.getTameOwnerUuid(this).isPresent()) {
            nbt.putBoolean("Wait", TameableUtil.isWait(this));
            nbt.putByte("MovingMode", (byte) this.getMovingMode().getId());
            writeContractable(nbt);
            writeModeData(nbt);
            nbt.putBoolean("isBloodSuck", isBloodSuck());
            if (this.getMovingMode() == MovingMode.FREEDOM
                    && freedomPos != null) {
                nbt.put("FreedomPos", NbtUtils.writeBlockPos(freedomPos));
            }
            writeTargetTags(nbt);
        }
        this.multiModel.writeToNbt(nbt);
        nbt.putString("SoundConfigName", getConfigHolder().getName());

        nbt.putInt("accelerationTicks", accelerationTicks);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag nbt) {
        super.readAdditionalSaveData(nbt);
        int maidVersion = nbt.getByte("maidVersion") & 255;

        if (maidVersion <= 1) {
            var defaultAttributes = createLittleMaidAttributes().build();
            var entityAttributes = new net.minecraft.core.Holder[] {
                    Attributes.MOVEMENT_SPEED,
                    Attributes.FOLLOW_RANGE
            };
            for (var attribute : entityAttributes) {
                @SuppressWarnings("unchecked")
                var holder = (net.minecraft.core.Holder<Attribute>) attribute;
                var customInstance = this.getAttributes().getInstance(holder);
                if (customInstance != null) {
                    customInstance.setBaseValue(defaultAttributes.getBaseValue(holder));
                }
            }
        }

        readInventory(nbt, this.registryAccess());
        this.xpReward = nbt.getInt("XpTotal");
        if (maidVersion == 0) {
            var list = nbt.getList("Inventory", 10);
            for (int i = 0; i < list.size(); i++) {
                CompoundTag nbtCompound = list.getCompound(i);
                int j = nbtCompound.getByte("Slot") & 255;
                ItemStack stack = ItemStack.parseOptional(this.registryAccess(), nbtCompound);
                if (!stack.isEmpty()) {
                    if (j == 0) {
                        this.setItemSlot(EquipmentSlot.MAINHAND, stack);
                    } else if (100 <= j && j < 104) {
                        EquipmentSlot armorSlot = switch (j - 100) {
                            case 0 -> EquipmentSlot.FEET;
                            case 1 -> EquipmentSlot.LEGS;
                            case 2 -> EquipmentSlot.CHEST;
                            case 3 -> EquipmentSlot.HEAD;
                            default -> null;
                        };
                        if (armorSlot != null) {
                            this.setItemSlot(armorSlot, stack);
                        }
                    } else if (j == 150) {
                        this.setItemSlot(EquipmentSlot.OFFHAND, stack);
                    }
                }
            }
        }

        if (TameableUtil.hasTameOwner(this)) {
            TameableUtil.setWait(this, nbt.getBoolean("Wait"));
            setMovingMode(MovingMode.fromId(nbt.getByte("MovingMode")));
            readContractable(nbt);
            readModeData(nbt);
            setBloodSuck(nbt.getBoolean("isBloodSuck"));
            if (this.getMovingMode() == MovingMode.FREEDOM
                    && nbt.contains("FreedomPos")) {
                freedomPos = NbtUtils.readBlockPos(nbt, "FreedomPos").orElse(null);
            }
            readTargetTags(nbt);
        }
        this.multiModel.readFromNbt(nbt);
        this.refreshDimensions();
        if (nbt.contains("SoundConfigName")) {
            LMConfigManager.INSTANCE.getConfig(nbt.getString("SoundConfigName"))
                    .ifPresent(this::setConfigHolder);
        }

        accelerationTicks = nbt.getInt("accelerationTicks");
    }

    // TODO IdFactorが確実にセットされたタイミングで実行されるようにする
    public void setRandomTexture() {
        var textureHolderList = LMTextureManager.INSTANCE.getAllTextures().stream()
                .filter(h -> h.hasSkinTexture(false))// 野生テクスチャがある
                .filter(h -> LMModelManager.INSTANCE.hasModel(h.getModelName()))
                .toList();
        if (textureHolderList.isEmpty()) {
            return;
        }
        var textureHolder = textureHolderList.get(idFactor % textureHolderList.size());
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
            LMConfigManager.INSTANCE.getAllConfig().stream()
                    .filter(c -> c.getPackName().equalsIgnoreCase(defaultSoundPackName))
                    .findAny()
                    .ifPresent(soundPlayer::setConfigHolder);
        }
    }

    // 鯖
    @Override
    public void saveAdditionalSpawnData(FriendlyByteBuf buf) {
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
        ItemStack.OPTIONAL_STREAM_CODEC.encode((net.minecraft.network.RegistryFriendlyByteBuf) buf,
                getInventory().getItem(17));
        // architectury側のミスでPitchYawが逆に与えられているのを修正
        buf.writeFloat(this.getXRot());
        buf.writeFloat(this.getYRot());
        buf.writeVarInt(this.accelerationTicks);
    }

    // 蔵
    @Override
    public void loadAdditionalSpawnData(FriendlyByteBuf buf) {
        // モデル
        // readString()はクラ処理。このメソッドでは、クラ側なので問題なし
        setColorMM(buf.readEnum(TextureColors.class));
        setContractMM(buf.readBoolean());
        LMTextureManager textureManager = LMTextureManager.INSTANCE;
        textureManager.getTexture(buf.readUtf())
                .ifPresent(textureHolder -> setTextureHolder(textureHolder, Layer.SKIN, Part.HEAD));
        for (Part part : Part.values()) {
            textureManager.getTexture(buf.readUtf())
                    .ifPresent(textureHolder -> setTextureHolder(textureHolder, Layer.INNER, part));
            textureManager.getTexture(buf.readUtf())
                    .ifPresent(textureHolder -> setTextureHolder(textureHolder, Layer.OUTER, part));
        }
        // サウンド
        LMConfigManager.INSTANCE.getConfig(buf.readUtf())
                .ifPresent(this::setConfigHolder);

        getInventory().setItem(17,
                ItemStack.OPTIONAL_STREAM_CODEC.decode((net.minecraft.network.RegistryFriendlyByteBuf) buf));
        this.setXRot(buf.readFloat());
        this.setYRot(buf.readFloat());
        this.accelerationTicks = buf.readVarInt();
    }

    @Override
    public void handleEntityEvent(byte status) {
        switch (status) {
            case 70 -> {// 雇用時
                spawnTamingParticles(true);
                play(LMSounds.GET_CAKE);
            }
            case 71 -> {// 再雇用時
                spawnTamingParticles(true);
                play(LMSounds.RECONTRACT);
            }
            case 72 -> {// 砂糖あげた時
                this.level().addParticle(ParticleTypes.NOTE,
                        this.getX(),
                        this.getY() + this.getBbHeight(),
                        this.getZ(),
                        6 / 24f, 0, 0);
            }
            case 73 -> showFreedomParticle();// toFreedom
            case 74 -> spawnTamingParticles(false);// toEscort
            case 75 -> showTracerParticle();// toTracer
            default -> super.handleEntityEvent(status);
        }
    }

    protected void showFreedomParticle() {
        for (int i = 0; i < 7; ++i) {
            double d = this.random.nextGaussian() * 0.02;
            double e = this.random.nextGaussian() * 0.02;
            double f = this.random.nextGaussian() * 0.02;
            this.level().addParticle(new DustParticleOptions(
                    new Vector3f(
                            this.random.nextFloat(),
                            this.random.nextFloat(),
                            this.random.nextFloat()),
                    1.0f),
                    this.getRandomX(1.0),
                    this.getRandomY() + 0.5,
                    this.getRandomZ(1.0),
                    d, e, f);
        }
    }

    protected void showTracerParticle() {
        for (int i = 0; i < 7; ++i) {
            double d = this.random.nextGaussian() * 0.02;
            double e = this.random.nextGaussian() * 0.02;
            double f = this.random.nextGaussian() * 0.02;
            this.level().addParticle(ParticleTypes.CLOUD,
                    this.getRandomX(1.0),
                    this.getRandomY() + 0.5,
                    this.getRandomZ(1.0),
                    d, e, f);
        }
    }

    // バニラメソッズ

    @Override
    public void tick() {
        if (!this.level().isClientSide() && !this.maidManagerRegistered) {
            TameableUtil.getTameOwner(this)
                    .filter(owner -> owner instanceof MaidManager)
                    .ifPresent(owner -> {
                        ((MaidManager) owner).registerMaid(this);
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
        if (this.level().isClientSide) {
            tickInterestedAngle();
        }
        playSoundCool = Math.max(0, playSoundCool - 1);
        decAccelerationTicks();
    }

    protected void inTickMultiplePost() {

    }

    @Override
    public void aiStep() {
        updateSwingTime();
        super.aiStep();
    }

    @Override
    protected void customServerAiStep() {
        super.customServerAiStep();
        if (TameableUtil.hasTameOwner(this)
                || getConfig().misc.canPickupItemByNoOwner) {
            pickupItem();
        }
        itemContractable.tick();
        hasModeImpl.tick();
    }

    protected void pickupItem() {
        if (!getConfig().misc.canPickupExperienceOrb
                && !getConfig().misc.canPickupItem) {
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
        return getConfig().spawn.canDespawn
                && TameableUtil.getTameOwnerUuid(this).isEmpty();
    }

    // canSpawnとかでも使われる
    // TODO スポーン条件をコンフィグで設定可能にする
    @Override
    public float getWalkTargetValue(BlockPos pos, LevelReader world) {
        return world.getBlockState(pos.below()).isCollisionShapeFullBlock(world, pos) ? 10.0F
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

    // TODO マウント系の位置を調整

    /**
     * 上に乗ってるエンティティへのオフセット (1.21.1ではgetPassengerRidingPositionに統合)
     */
    public double getMountedYOffset() {
        IMultiModel model = getModel(Layer.SKIN, Part.HEAD)
                .orElse(LMModelManager.INSTANCE.getDefaultModel());
        return model.getMountedYOffset(getCaps());
    }

    /**
     * 騎乗時のオフセット
     */
    public double getRidingYOffset() {
        IMultiModel model = getModel(Layer.SKIN, Part.HEAD)
                .orElse(LMModelManager.INSTANCE.getDefaultModel());
        return model.getyOffset(getCaps()) - getBbHeight();
    }

    // このままだとEntityDimensionsが作っては捨てられてを繰り返すのでパフォーマンスはよろしくない
    // …が、そもそもそんなにたくさん呼ばれるメソッドでもない
    @Override
    public EntityDimensions getDefaultDimensions(Pose pose) {
        EntityDimensions dimensions;
        IMultiModel model = getModel(Layer.SKIN, Part.HEAD)
                .orElse(LMModelManager.INSTANCE.getDefaultModel());
        float height = model.getHeight(getCaps(), MMPose.convertPose(pose));
        float width = model.getWidth(getCaps(), MMPose.convertPose(pose));
        dimensions = EntityDimensions.scalable(width, height);
        return dimensions.scale(getAgeScale());
    }

    // 1.21.1: changeDimension is final, use afterChangingDimensions instead
    @Override
    public void restoreFrom(Entity entity) {
        super.restoreFrom(entity);
        // ディメンション移動の時に、自由行動地点を削除する
        if (entity instanceof LittleMaidEntity oldMaid
                && oldMaid.getMovingMode() == MovingMode.FREEDOM) {
            this.setFreedomPos(null);
        }
    }

    // TODO これ何のメソッド？
    @Override
    public boolean isWithinRestriction(BlockPos pos) {
        // 自身または主人から16ブロック以内
        if (pos.closerThan(pos, 16)
                || TameableUtil.getTameOwner(this)
                        .filter(owner -> owner.blockPosition().closerThan(pos, 16))
                        .isPresent()) {
            return super.isWithinRestriction(pos);
        }
        return false;
    }

    // TODO ボイス周りの調整、コンフィグ化
    @Override
    public void playAmbientSound() {
        if (this.level().isClientSide || this.dead || getConfigHolder()
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
            if (tickCount % 4 == 0 && this.level().canSeeSky(this.blockPosition())) {
                Biome biome = this.level().getBiome(blockPosition()).value();
                if (biome.coldEnoughToSnow(blockPosition())) {
                    play(LMSounds.LIVING_COLD);
                } else if (2 <= biome.getBaseTemperature()) {
                    play(LMSounds.LIVING_HOT);
                }
            } else if (tickCount % 4 == 1 && this.level().isRaining()) {
                var pos = blockPosition();
                Biome biome = this.level().getBiome(pos).value();
                if (biome.getPrecipitationAt(pos) == Biome.Precipitation.RAIN)
                    play(LMSounds.LIVING_RAIN);
                else if (biome.getPrecipitationAt(pos) == Biome.Precipitation.SNOW)
                    play(LMSounds.LIVING_SNOW);
            } else {
                if (this.getMainHandItem().getItem() == Items.CLOCK
                        || this.getOffhandItem().getItem() == Items.CLOCK) {
                    int time = (int) (this.level().getDayTime() % 24000);
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
        // TODO 強制再生メソッドを生やす
        // 死亡ボイスは必ず聞かせる
        this.playSoundCool = 0;
        play(LMSounds.DEATH);
    }

    @Override
    public void remove(RemovalReason reason) {
        super.remove(reason);
        if (this.level() instanceof ServerLevel serverWorld
                && reason.shouldDestroy()) {
            TameableUtil.getTameOwnerUuid(this).ifPresent(id -> {
                var maidSoulEntity = new MaidSoulEntity(serverWorld, new MaidSoul(this));
                maidSoulEntity.setPos(this.getX(), this.getY(), this.getZ());
                maidSoulEntity
                        .setDeltaMovement(new Vec3(random.nextGaussian() * 0.02, 0.2, random.nextGaussian() * 0.02));
                serverWorld.addFreshEntity(maidSoulEntity);
            });
        }
    }

    public void installMaidSoul(MaidSoul maidSoul) {
        load(maidSoul.getNbt());
        this.setHealth(getMaxHealth());
        this.unsetRemoved();
        this.dead = false;
        this.deathTime = 0;
    }

    // TODO 処理の改善
    @Override
    public boolean doHurtTarget(Entity target) {
        boolean result = super.doHurtTarget(target);
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
            if (!this.level().isClientSide && !mainHandStack.isEmpty() && entity instanceof LivingEntity) {
                // バニラではこのメソッドの第三引数にはプレイヤーエンティティしか渡されない
                // そのため、他Modにおいて必ずプレイヤーであると仮定して実装した場合にクラッシュする可能性がある
                // その対策にtry/catchを置いておく
                try {
                    mainHandStack.getItem().hurtEnemy(mainHandStack, (LivingEntity) entity, this);
                } catch (Exception e) {
                    LMRBMod.LOGGER.error("メイドさんの攻撃時に例外が発生しました。", e);
                }
                if (mainHandStack.isEmpty()) {
                    this.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
                }
            }
        }
        return result;
    }

    // TODO 処理の見直し
    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (this.dead) {
            return super.hurt(source, amount);
        }
        if (!this.level().isClientSide) {
            // 味方のが当たってもちゃんと動くようにフレンド判定より前
            if (amount <= 0 && source.getDirectEntity() instanceof Snowball) {
                play(LMSounds.HURT_SNOW);
                return false;
            }
        }
        LMRBConfig config = getConfig();
        if (config.health.nonMobDamageImmunity && source.getEntity() == null) {
            return false;
        }
        if (config.health.immortal && !source.is(DamageTypes.FELL_OUT_OF_WORLD) && !source.isCreativePlayer()) {
            return false;
        }
        if (config.health.fallImmunity && source.is(DamageTypes.FALL)) {
            return false;
        }
        Entity attacker = source.getEntity();
        // Friendからの攻撃を除外
        if (!config.health.enableFriendlyFire && attacker instanceof LivingEntity
                && isFriend((LivingEntity) attacker)) {
            return false;
        }

        float factor = config.health.generalMaidDamageFactor;
        if ((config.health.enableWorkInEmergency || !isEmergency())
                && !TameableUtil.isWait(this) && this.getMode().map(Mode::isBattleMode).orElse(false)) {
            factor *= config.health.battleModeMaidDamageFactor;
        } else {
            factor *= config.health.nonBattleModeMaidDamageFactor;
        }
        amount *= factor;

        boolean isHurtTime = 0 < this.hurtTime;
        boolean result = super.hurt(source, amount);
        if (!this.level().isClientSide && !isHurtTime) {
            if (result && 0 < amount && TameableUtil.isWait(this)
                    && TameableUtil.getTameOwnerUuid(this).isPresent()) {
                TameableUtil.setWait(this, false);
            }
            if (!result || amount <= 0F) {
                play(LMSounds.HURT_NO_DAMAGE);
            } else if (amount > 0F && this.isDamageSourceBlocked(source)) {
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
        return this.getHealth() / this.getMaxHealth() <= config.health.emergencyMaidHealthThreshold;
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
    public boolean killedEntity(ServerLevel world, LivingEntity other) {
        if (isBloodSuck())
            play(LMSounds.LAUGHTER);

        return super.killedEntity(world, other);
    }

    // 射撃

    // TODO try/catchを挟む。処理の見直し
    @Override
    public void performRangedAttack(LivingEntity target, float pullProgress) {
        var stack = this.getMainHandItem();
        // 弾が無い場合は実行されないはずだが、念のためチェック
        var arrowStack = this.getProjectile(stack);
        // 1.21.1: EnchantmentHelper APIが変更されたため、Infinity判定を簡略化
        boolean isInfinite = false; // TODO: Holder<Enchantment>を取得してチェックする
        if (arrowStack.isEmpty() && !isInfinite) {
            return;
        }
        if (stack.getItem() instanceof BowItem bowItem) {
            var arrow = ProjectileUtil.getMobArrow(this, arrowStack, pullProgress, stack);
            if (arrowStack.getItem() instanceof ArrowItem
                    && !isInfinite) {
                arrow.pickup = AbstractArrow.Pickup.ALLOWED;
            }
            arrow = EPEntityUtil.arrowCustomHook(bowItem, arrow);
            double xDiff = target.getX() - this.getX();
            double yDiff = target.getEyeY() - arrow.getY();
            double zDiff = target.getZ() - this.getZ();
            double horizonLen = Math.sqrt(xDiff * xDiff + zDiff * zDiff);
            arrow.shoot(xDiff, yDiff + horizonLen * 0.025, zDiff,
                    pullProgress * 3.0f * getConfig().work.archerShootVelocityFactor,
                    14 - 2 * 4);
            this.playSound(SoundEvents.ARROW_SHOOT,
                    1.0f, 1.0f / (this.getRandom().nextFloat() * 0.4f + 1.2f) + pullProgress * 0.5f);
            this.level().addFreshEntity(arrow);
            arrowStack.shrink(1);
        } else if (stack.getItem() instanceof CrossbowItem) {
            this.performCrossbowAttack(this,
                    CrossbowItemInvoker.getSpeed(stack.get(DataComponents.CHARGED_PROJECTILES)));
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

    // 1.21.1: CrossbowAttackMobからshootCrossbowProjectile/shootメソッドが削除された
    // performCrossbowAttack(default)がCrossbowItem.performShootingを直接呼ぶ形に変更
    // TODO: 弾道調整(archerShootVelocityFactor)が必要な場合performCrossbowAttackをオーバーライドする

    @Override
    public void onCrossbowAttackPerformed() {

    }

    // TODO コメントを差す
    @Override
    protected Vec3 maybeBackOffFromEdge(Vec3 movement, MoverType type) {
        if (type != MoverType.SELF && type != MoverType.PLAYER) {
            return movement;
        }

        LMRBConfig config = getConfig();

        if (!config.health.immortal && !getConfig().health.nonMobDamageImmunity && config.health.enableSafeMove
                && this.canClipAtLedge()) {
            boolean shouldBackByDamage = isDamageSourceEmpty(this.getBoundingBox())
                    && !this.isDamageSourceEmpty(this.getBoundingBox().move(movement.x, 0, movement.z));
            boolean shouldBackByFall = !config.health.fallImmunity
                    && !isSafeFallHeight(this.position().add(movement.x, 0, movement.z));

            if (shouldBackByDamage || shouldBackByFall) {
                BiPredicate<Double, Double> shouldBackPredicate = (x, z) -> false;
                if (shouldBackByDamage) {
                    BiPredicate<Double, Double> finalPredicate = shouldBackPredicate;
                    shouldBackPredicate = (x, z) -> finalPredicate.test(x, z)
                            // 危険物がbox内にある
                            || !this.isDamageSourceEmpty(this.getBoundingBox().move(x, 0, z));
                }

                if (shouldBackByFall) {
                    BiPredicate<Double, Double> finalPredicate = shouldBackPredicate;
                    shouldBackPredicate = (x, z) -> finalPredicate.test(x, z)
                            // 足場がbox内にない
                            || this.level().noCollision(this, this.getBoundingBox()
                                    .move(x, 0, z)
                                    .expandTowards(0, -(getDangerHeightThreshold() - fallDistance), 0))
                            // または、すぐ下に足場がなく、危険物がbox内にある
                            || (this.level().noCollision(this, this.getBoundingBox()
                                    .move(x, 0, z)
                                    .expandTowards(0, -maxUpStep(), 0))
                                    && !this.isDamageSourceEmpty(this.getBoundingBox().move(x, 0, z)
                                            .expandTowards(0, -getDangerHeightThreshold(), 0)));
                }

                movement = pushBack(movement, shouldBackPredicate);
            }
        }

        return movement;
    }

    private Vec3 pushBack(Vec3 movement, BiPredicate<Double, Double> pushBackPredicate) {
        double dot = 0.05;
        double mX = movement.x;
        double mZ = movement.z;
        while (mX != 0.0 && pushBackPredicate.test(mX, 0d)) {
            if (mX < dot && mX >= -dot) {
                mX = 0.0;
                continue;
            }
            if (mX > 0.0) {
                mX -= dot;
                continue;
            }
            mX += dot;
        }
        while (mZ != 0.0 && pushBackPredicate.test(0d, mZ)) {
            if (mZ < dot && mZ >= -dot) {
                mZ = 0.0;
                continue;
            }
            if (mZ > 0.0) {
                mZ -= dot;
                continue;
            }
            mZ += dot;
        }
        while (mX != 0.0 && mZ != 0.0 && pushBackPredicate.test(mX, mZ)) {
            mX = mX < dot && mX >= -dot ? 0.0 : (mX > 0.0 ? mX - dot : mX + dot);
            if (mZ < dot && mZ >= -dot) {
                mZ = 0.0;
                continue;
            }
            if (mZ > 0.0) {
                mZ -= dot;
                continue;
            }
            mZ += dot;
        }
        return new Vec3(mX, movement.y, mZ);
    }

    private boolean isDamageSourceEmpty(AABB box) {
        int minX = Mth.floor(box.minX);
        int maxX = Mth.floor(box.maxX);
        int minY = Mth.floor(box.minY);
        int maxY = Mth.floor(box.maxY);
        int minZ = Mth.floor(box.minZ);
        int maxZ = Mth.floor(box.maxZ);

        for (int x = 0; x < maxX - minX + 1; x++) {
            for (int y = 0; y < maxY - minY + 1; y++) {
                for (int z = 0; z < maxZ - minZ + 1; z++) {
                    PathType pathNodeType = WalkNodeEvaluator.getPathTypeStatic(
                            new net.minecraft.world.level.pathfinder.PathfindingContext(this.level(), this),
                            new BlockPos(minX + x, minY + y, minZ + z).mutable());
                    if (pathNodeType == PathType.DAMAGE_FIRE
                            || pathNodeType == PathType.DAMAGE_OTHER
                            || pathNodeType == PathType.LAVA) {
                        return false;
                    }
                }
            }
        }
        return true;
    }

    private boolean isSafeFallHeight(Vec3 pos) {
        BlockHitResult result = this.level().clip(new ClipContext(
                pos,
                pos.subtract(0, getDangerHeightThreshold() - fallDistance + 0.1, 0),
                ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, this));
        if (result.getType() == HitResult.Type.MISS) {
            return false;
        }
        Vec3 hitPos = result.getLocation();
        if (getDangerHeightThreshold() - fallDistance < pos.y - hitPos.y) {
            return false;
        }
        BlockPos checkPos = new BlockPos(Mth.floor(pos.x), Mth.floor(pos.y - 1), Mth.floor(pos.z));
        for (int i = 0; i < pos.y - hitPos.y + 1; i++) {
            PathType pathNodeType = WalkNodeEvaluator.getPathTypeStatic(
                    new net.minecraft.world.level.pathfinder.PathfindingContext(this.level(), this),
                    checkPos.mutable());
            if (pathNodeType == PathType.WALKABLE || pathNodeType == PathType.BLOCKED) {
                return true;
            }
            if (pathNodeType == PathType.DAMAGE_FIRE
                    || pathNodeType == PathType.DAMAGE_OTHER
                    || pathNodeType == PathType.LAVA) {
                return false;
            }
            checkPos = checkPos.below();
        }
        return false;
    }

    private boolean canClipAtLedge() {
        float canClipHeight = getDangerHeightThreshold() + 1.0f;
        // 着地しているか、落下距離が危険高度未満かつ下に足場があるとき
        return this.onGround() || this.fallDistance < canClipHeight
                && !this.level().noCollision(this, this.getBoundingBox()
                        .expandTowards(0.0, this.fallDistance - canClipHeight, 0.0));
    }

    private float getDangerHeightThreshold() {
        // マイナスの値も返すことを利用しているため、バージョンアップ/mixinでの仕様変更に注意が必要
        int fallDamage = calculateFallDamage(0, 1);
        return -fallDamage;
    }

    // TODO 複数モデルで問題ないかチェック
    @Override
    public Vec3 getLeashOffset() {
        return new Vec3(0.0, this.getEyeHeight() - 0.15f, 1f / 16f);
    }

    // success 動作を実行し、手を振る
    // consume 動作を実行するが、手を振らない
    // pass 動作を実行しないが、他の動作を許可する
    // fail 動作を実行せず、他の動作も許可しない
    // 下二つならここ以外で手に持ったアイテムが使用される場合がある
    // 継承元のコードは無視
    // TODO 処理の見直し、処理を追加可能に
    // TODO 使用アイテムをコンフィグから追加可能に
    @Override
    public InteractionResult mobInteract(Player player, InteractionHand hand) {
        if (player.isShiftKeyDown()) {
            return InteractionResult.PASS;
        }
        ItemStack stack = player.getItemInHand(hand);
        // オーナーが居ない場合
        if (TameableUtil.getTameOwnerUuid(this).isEmpty()) {
            if (stack.is(LMTags.Items.MAIDS_EMPLOYABLE)) {
                return contract(player, stack, false);
            }
            return InteractionResult.PASS;
        }
        // オーナーじゃない場合
        if (!player.getUUID().equals(this.getOwnerUUID())) {
            return InteractionResult.PASS;
        }
        // ストライキ時
        if (isStrike()) {
            if (stack.is(LMTags.Items.MAIDS_EMPLOYABLE)) {
                return contract(player, stack, true);
            }
            this.level().broadcastEntityEvent(this, (byte) 6);
            return InteractionResult.PASS;
        }
        // サドル持ってるとき
        if (stack.getItem() instanceof SaddleItem) {
            if (!this.isPassenger()) {
                if (player.isVehicle()) {
                    player.ejectPassengers();
                }
                this.startRiding(player);
            } else {
                var vehicle = this.getVehicle();
                if (vehicle == player) {
                    this.stopRiding();
                }
            }
            return InteractionResult.sidedSuccess(this.level().isClientSide);
        }
        // 肩車されてるとき
        if (this.getVehicle() == player) {
            return InteractionResult.PASS;
        }
        // 砂糖
        if (stack.is(LMTags.Items.MAIDS_SALARY)) {
            var config = getConfig();
            heal(config.health.healAmount);
            return changeState(player, stack);
        }
        // Freedom切替
        if (stack.getItem() == Items.FEATHER) {
            if (getMovingMode() == MovingMode.ESCORT) {
                this.level().broadcastEntityEvent(this, (byte) 73);
                this.setMovingMode(MovingMode.FREEDOM);
                this.setFreedomPos(this.blockPosition());
            } else {
                this.level().broadcastEntityEvent(this, (byte) 74);
                this.setMovingMode(MovingMode.ESCORT);
            }
            return InteractionResult.sidedSuccess(this.level().isClientSide);
        }
        // Tracer切替
        if ((this.getMovingMode() == MovingMode.FREEDOM
                || this.getMovingMode() == MovingMode.TRACER)
                && stack.getItem() == Items.REDSTONE) {
            if (this.getMovingMode() == MovingMode.FREEDOM) {
                this.level().broadcastEntityEvent(this, (byte) 75);
                this.setMovingMode(MovingMode.TRACER);
            } else {
                this.level().broadcastEntityEvent(this, (byte) 73);
                this.setMovingMode(MovingMode.FREEDOM);
                this.setFreedomPos(this.blockPosition());
            }
            return InteractionResult.sidedSuccess(this.level().isClientSide);
        }
        // ガラス瓶->エンチャントの瓶
        if (this.xpReward >= EXPERIENCE_BOTTLE_COST && stack.is(Items.GLASS_BOTTLE)) {
            this.level().playSound(null, this.getX(), this.getY(), this.getZ(),
                    SoundEvents.BOTTLE_FILL, SoundSource.PLAYERS, 1.0f, 1.0f);
            ItemStack itemStack2 = ItemUtils.createFilledResult(stack, player,
                    Items.EXPERIENCE_BOTTLE.getDefaultInstance());
            player.setItemInHand(hand, itemStack2);
            this.addExperience(-EXPERIENCE_BOTTLE_COST);
            return InteractionResult.sidedSuccess(this.level().isClientSide);
        }
        // モブミルク
        if (getConfig().misc.canMilking && stack.is(Items.BUCKET)) {
            player.playSound(SoundEvents.COW_MILK, 1.0F, 1.0F);
            ItemStack itemStack2 = ItemUtils.createFilledResult(stack, player, Items.MILK_BUCKET.getDefaultInstance());
            player.setItemInHand(hand, itemStack2);
            return InteractionResult.sidedSuccess(this.level().isClientSide);
        }
        if (stack.getItem() == Items.GUNPOWDER) {
            int maxAccelerationStack = getConfig().misc.maxAccelerationStack;
            int accelerationTicks = getConfig().misc.accelerationTicksPerStack;
            // 同期ズレ防止のため、if条件を付加する場合は結果をパケットで送信すること
            int resumeCount = Math.min(maxAccelerationStack, stack.getCount());
            int acTicks = resumeCount * accelerationTicks;
            setAccelerationTicks(acTicks);

            if (!player.getAbilities().instabuild) {
                stack.shrink(resumeCount);
                if (stack.isEmpty()) {
                    player.getInventory().removeItem(stack);
                }
            }

            return InteractionResult.sidedSuccess(this.level().isClientSide);
        }
        openInventory(player);
        return InteractionResult.sidedSuccess(this.level().isClientSide);
    }

    public InteractionResult changeState(Player player, ItemStack stack) {
        this.level().broadcastEntityEvent(this, (byte) 72);
        this.playSound(SoundEvents.ITEM_PICKUP, 1.0F, this.random.nextFloat() * 0.1F + 1.0F);
        this.setFreedomPos(this.blockPosition());
        this.getNavigation().stop();
        TameableUtil.switchWait(this);
        if (!player.getAbilities().instabuild) {
            stack.shrink(1);
            if (stack.isEmpty()) {
                player.getInventory().removeItem(stack);
            }
        }
        return InteractionResult.sidedSuccess(this.level().isClientSide);
    }

    public InteractionResult contract(Player player, ItemStack stack, boolean isReContract) {
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
        if (!this.level().isClientSide) {
            SyncMultiModelPacket.sendS2CPacket(this, this);
        }
        setStrike(false);
        itemContractable.setUnpaidTimes(0);
        getNavigation().stop();
        setMovingMode(MovingMode.ESCORT);
        if (!player.getAbilities().instabuild) {
            stack.shrink(1);
            if (stack.isEmpty()) {
                player.getInventory().removeItem(stack);
            }
        }
        return InteractionResult.sidedSuccess(this.level().isClientSide);
    }

    public void addExperience(int experience) {
        this.xpReward = Mth.clamp(this.xpReward + experience, 0, Integer.MAX_VALUE);
    }

    // GUI開くやつ
    public void openInventory(Player player) {
        if (player.level().isClientSide) {
            return;
        }
        setLastHurtByMob(null);
        getNavigation().stop();
        MenuRegistry.openExtendedMenu((ServerPlayer) player, screenFactory);
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
    public MovingMode getMovingMode() {
        return MovingMode.fromId(this.entityData.get(MOVING_MODE));
    }

    @Override
    public void setMovingMode(MovingMode movingMode) {
        this.entityData.set(MOVING_MODE, (byte) movingMode.getId());
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
    public void writeInventory(CompoundTag tag, net.minecraft.core.HolderLookup.Provider registries) {
        this.littleMaidInventory.writeInventory(tag, registries);
    }

    @Override
    public void readInventory(CompoundTag tag, net.minecraft.core.HolderLookup.Provider registries) {
        this.littleMaidInventory.readInventory(tag, registries);
    }

    public int getWorkItemSlotSize() {
        return this.littleMaidInventory.getWorkItemSlotSize();
    }

    public void setWorkItemSlotNum(int num) {
        this.littleMaidInventory.setWorkItemSlotSize(num);
    }

    // TODO 計算式の見直し
    @Override
    protected void hurtArmor(DamageSource source, float amount) {
        if (!(amount <= 0.0f)) {
            if ((amount /= 4.0f) < 1.0f) {
                amount = 1.0f;
            }
            int i = -1;
            for (ItemStack stack : this.getArmorSlots()) {
                i++;
                if (source.is(DamageTypeTags.IS_FIRE) && stack.has(DataComponents.FIRE_RESISTANT)
                        || !(stack.getItem() instanceof ArmorItem)) {
                    continue;
                }
                EquipmentSlot slot = switch (i) {
                    case 0 -> EquipmentSlot.FEET;
                    case 1 -> EquipmentSlot.LEGS;
                    case 2 -> EquipmentSlot.CHEST;
                    case 3 -> EquipmentSlot.HEAD;
                    default -> null;
                };
                if (slot != null) {
                    stack.hurtAndBreak((int) amount, this, slot);
                }
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
            if (source.is(DamageTypeTags.IS_FIRE) && stack.has(DataComponents.FIRE_RESISTANT)
                    || !(stack.getItem() instanceof ArmorItem)) {
                return;
            }
            stack.hurtAndBreak((int) amount, this, EquipmentSlot.HEAD);
        }
    }

    @Override
    protected void hurtCurrentlyUsedShield(float amount) {
        // TODO ガード実装
    }

    // TODO どこで使われるメソッド？
    @Override
    public SlotAccess getSlot(int mappedIndex) {
        var inv = getInventory();
        int i = mappedIndex - 200;
        if (0 <= i && i < inv.getContainerSize()) {
            return SlotAccess.forContainer(inv, i);
        }
        return super.getSlot(mappedIndex);
    }

    // TODO 処理の見直し
    @Override
    public ItemStack getProjectile(ItemStack stack) {
        if (!(stack.getItem() instanceof ProjectileWeaponItem ranged)) {
            return ItemStack.EMPTY;
        }
        Predicate<ItemStack> predicate = ranged.getSupportedHeldProjectiles();
        ItemStack itemStack = ProjectileWeaponItem.getHeldProjectile(this, predicate);
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
    protected void dropEquipment() {
        Container inv = this.getInventory();
        for (int i = 0; i < inv.getContainerSize(); i++) {
            ItemStack stack = inv.getItem(i);
            if (stack.isEmpty() || stack.has(net.minecraft.core.component.DataComponents.ENCHANTMENTS)
                    && net.minecraft.world.item.enchantment.EnchantmentHelper.has(stack,
                            net.minecraft.world.item.enchantment.EnchantmentEffectComponents.PREVENT_EQUIPMENT_DROP))
                continue;
            this.spawnAtLocation(stack);
            inv.setItem(i, ItemStack.EMPTY);
        }
        for (EquipmentSlot slot : EquipmentSlot.values()) {
            ItemStack stack = this.getItemBySlot(slot);
            if (stack.isEmpty() || net.minecraft.world.item.enchantment.EnchantmentHelper.has(stack,
                    net.minecraft.world.item.enchantment.EnchantmentEffectComponents.PREVENT_EQUIPMENT_DROP))
                continue;
            this.spawnAtLocation(stack);
            this.setItemSlot(slot, ItemStack.EMPTY);
        }
    }

    @Override
    public int getBaseExperienceReward() {
        return this.xpReward;
    }

    // TODO IdFactorの仕様の見直し
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

    @Override
    public void setOwnerUUID(@Nullable UUID uuid) {
        super.setOwnerUUID(uuid);
        this.setContract(true);
    }

    public void setFreedomPos(@Nullable BlockPos freedomPos) {
        this.freedomPos = freedomPos;
    }

    public Optional<BlockPos> getFreedomPos() {
        if (this.getMovingMode() != MovingMode.FREEDOM) {
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

    @Environment(EnvType.CLIENT)
    public float getInterestedAngle(float tickDelta) {
        return (prevInterestedAngle + (interestedAngle - prevInterestedAngle) * tickDelta) *
                ((getId() % 2 == 0 ? 0.08F : -0.08F) * (float) Math.PI);
    }

    @Environment(EnvType.CLIENT)
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
        return this.isAcceleration() ? getConfig().misc.accelerationMultiple : 1;
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
    public void writeContractable(CompoundTag nbt) {
        itemContractable.writeContractable(nbt);
    }

    @Override
    public void readContractable(CompoundTag nbt) {
        itemContractable.readContractable(nbt);
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
    public void writeModeData(CompoundTag tag) {
        hasModeImpl.writeModeData(tag);
    }

    @Override
    public void readModeData(CompoundTag tag) {
        hasModeImpl.readModeData(tag);
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

    @Environment(EnvType.CLIENT)
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
                .map(l -> l instanceof TargetTagManager ? (TargetTagManager) l : null)
                .map(t -> {
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
    public void writeTargetTags(CompoundTag nbt) {
        this.targetTagManager.writeTargetTags(nbt);
    }

    @Override
    public void readTargetTags(CompoundTag nbt) {
        this.targetTagManager.readTargetTags(nbt);
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
        // TODO 暫定でテイム済みのモブは攻撃対象から外す
        // TODO
        // そもそも、isFriend()はAttackProhibitedでは決してない。TargetingSystemにフレンドタグを復活させる必要がある
        if (entity instanceof OwnableEntity tameable
                && TameableUtil.hasTameOwner(tameable)) {
            return true;
        }
        // 暫定: ご主人がいるなら、プレイヤーを攻撃対象にしない
        if (TameableUtil.hasTameOwner(this)
                && entity instanceof Player) {
            return true;
        }
        if (TameableUtil.isTameOwner(this, entity)
                || (entity instanceof OwnableEntity tameable
                        && TameableUtil.equalTameOwner(this, tameable))) {
            return true;
        }
        return getTargetTag(new TargetIdentifier(entity)).contains(TargetingSystem.TargetTag.ATTACK_PROHIBITED);
    }

    // 構え

    @Override
    public boolean isAimingBow() {
        return this.getLMMFlag(AIMING_INDEX);
    }

    @Override
    public void setAimingBow(boolean aiming) {
        this.setLMMFlag(AIMING_INDEX, aiming);
    }

    // マルチモデル関連

    @Override
    public boolean isAllowChangeTexture(Entity entity, TextureHolder textureHolder, Layer layer, Part part) {
        return multiModel.isAllowChangeTexture(entity, textureHolder, layer, part);
    }

    @Override
    public void setTextureHolder(TextureHolder textureHolder, Layer layer, Part part) {
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
    public Optional<ResourceLocation> getTexture(Layer layer, Part part, boolean isLight) {
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

    // TODO 強制再生メソッドを生やす
    // TODO 再生クールダウンをコンフィグ化
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

    // 1.21.1: createSpawnPacket removed, use getAddEntityPacket instead
    @Override
    public Packet<ClientGamePacketListener> getAddEntityPacket(net.minecraft.server.level.ServerEntity entity) {
        return SpawnLittleMaidPacket.create(this);
    }

    public static LMRBConfig getConfig() {
        return LMRBMod.getConfig();
    }

    // MOVEとLOOKでGoalを分離
    public static class LMStareAtHeldItemGoal<T extends LittleMaidEntity> extends TameableStareAtHeldItemGoal<T> {
        private final LittleMaidEntity maid;

        public LMStareAtHeldItemGoal(T mob, Supplier<Float> stareAtRange, Predicate<ItemStack> targetItem,
                boolean isTamed) {
            super(mob, stareAtRange, targetItem, isTamed);
            this.maid = mob;
        }

        @Override
        public void tick() {
            super.tick();
            // 動いてたら傾げない
            this.maid.setBegging(this.maid.getNavigation().isDone());
        }

        @Override
        public void stop() {
            super.stop();
            this.maid.setBegging(false);
        }

    }

    // TODO このクラス置く場所ここで正しい？
    public static class MaidSoul {
        private final CompoundTag nbt;
        private final UUID uuid;
        private final String name;

        public MaidSoul(LittleMaidEntity maid) {
            this.nbt = new CompoundTag();
            maid.saveWithoutId(this.nbt);
            this.nbt.putString("Name", maid.getName().getString());
            this.name = maid.getName().getString();
            this.uuid = maid.getUUID();
        }

        private MaidSoul(CompoundTag nbt, UUID uuid, String name) {
            this.nbt = nbt;
            this.uuid = uuid;
            this.name = name;
        }

        public static MaidSoul fromNbt(CompoundTag nbt) {
            return new MaidSoul(nbt, nbt.getUUID("UUID"), nbt.getString("Name"));
        }

        public CompoundTag getNbt() {
            return nbt;
        }

        public UUID getUuid() {
            return this.uuid;
        }

        public Optional<UUID> getOwnerUUID() {
            return Optional.ofNullable(nbt.getUUID("Owner"));
        }

        public String getName() {
            return this.name;
        }
    }
}
