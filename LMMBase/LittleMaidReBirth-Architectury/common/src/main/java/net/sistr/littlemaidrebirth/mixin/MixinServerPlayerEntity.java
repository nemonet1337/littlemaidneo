package net.sistr.littlemaidrebirth.mixin;

import com.mojang.datafixers.util.Either;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Unit;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.OwnableEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.sistr.littlemaidmodelloader.entity.compound.SoundPlayable;
import net.sistr.littlemaidmodelloader.resource.util.LMSounds;
import net.sistr.littlemaidrebirth.entity.LittleMaidEntity;
import net.sistr.littlemaidrebirth.entity.MaidSoulEntity;
import net.sistr.littlemaidrebirth.entity.targeting.TargetTagManager;
import net.sistr.littlemaidrebirth.entity.util.MaidManager;
import net.sistr.littlemaidrebirth.entity.util.MaidManagerImpl;
import net.sistr.littlemaidrebirth.entity.util.TameableUtil;
import net.sistr.littlemaidrebirth.world.WorldMaidSoulState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;
import java.util.stream.Stream;

@Mixin(ServerPlayer.class)
public abstract class MixinServerPlayerEntity extends MixinPlayerEntity implements MaidManager {
    @Shadow
    public abstract ServerLevel serverLevel();

    @Unique
    private final MaidManager maidManager = new MaidManagerImpl();

    protected MixinServerPlayerEntity(EntityType<? extends LivingEntity> entityType, Level world) {
        super(entityType, world);
    }

    @Inject(method = "restoreFrom", at = @At("RETURN"))
    public void onRestoreFrom(ServerPlayer oldPlayer, boolean alive, CallbackInfo ci) {
        // ターゲットタグ
        var thisSync = this.getTargetTagsSync();
        var oldSync = ((TargetTagManager) oldPlayer).getTargetTagsSync();
        thisSync.syncFrom(oldSync);

        // メイドさん管理
        migrateWorldMaidSoulState();
        this.checkMaidUnload();
        var nbt = new CompoundTag();
        ((MaidManager) oldPlayer).writeMaidManager(oldPlayer.saveWithoutId(nbt));
        this.readMaidManager(nbt);
    }

    @Inject(method = "startSleepInBed", at = @At("RETURN"))
    public void onStartSleepInBed(BlockPos pos, CallbackInfoReturnable<Either<Player.BedSleepingProblem, Unit>> cir) {
        if (this.isSleeping()) {
            getAroundTamedSoundPlayable()
                    .forEach(e -> e.play(LMSounds.GOOD_NIGHT));
        }
    }

    @Inject(method = "stopSleepInBed", at = @At("RETURN"))
    public void onStopSleepInBed(boolean bl, boolean updateSleepingPlayers, CallbackInfo ci) {
        if (!bl && !updateSleepingPlayers) {
            getAroundTamedSoundPlayable()
                    .forEach(s -> s.play(LMSounds.GOOD_MORNING));
        }
    }

    private Stream<SoundPlayable> getAroundTamedSoundPlayable() {
        return this.level().getEntities(this, this.getBoundingBox().inflate(8),
                        e -> e instanceof OwnableEntity tameable
                                && TameableUtil.getTameOwnerUuid(tameable)
                                .filter(id -> id.equals(this.getUUID()))
                                .isPresent() && e instanceof SoundPlayable
                ).stream()
                .map(e -> (SoundPlayable) e)
                .filter(s -> !(s instanceof LivingEntity)
                        || (((LivingEntity) s).getMainHandItem().getItem() == Items.CLOCK
                        || ((LivingEntity) s).getOffhandItem().getItem() == Items.CLOCK)
                );
    }

    @Inject(method = "readAdditionalSaveData", at = @At("RETURN"))
    private void onReadSP(CompoundTag nbt, CallbackInfo ci) {
        this.readMaidManager(nbt);
        migrateWorldMaidSoulState();
    }

    @Inject(method = "addAdditionalSaveData", at = @At("RETURN"))
    private void onWriteSP(CompoundTag nbt, CallbackInfo ci) {
        this.checkMaidUnload();
        this.writeMaidManager(nbt);
    }

    @Inject(method = "tick", at = @At("HEAD"))
    private void onTick(CallbackInfo ci) {
        if (this.getRandom().nextInt(20) == 0) {
            this.checkMaidUnload();
        }
    }

    /**
     * メイドソウルをワールド管理からプレイヤー管理に移行する
     */
    @Unique
    private void migrateWorldMaidSoulState() {
        WorldMaidSoulState worldMaidSoulState = WorldMaidSoulState.getWorldMaidSoulState(serverLevel());
        worldMaidSoulState.get(this.getUUID())
                .forEach(this.maidManager::registerMaid);
        worldMaidSoulState.remove(this.getUUID());
    }

    @Override
    public void registerMaid(MaidSoulEntity soul) {
        this.maidManager.registerMaid(soul);
    }

    @Override
    public void registerMaid(LittleMaidEntity maid) {
        this.maidManager.registerMaid(maid);
    }

    @Override
    public void registerMaid(LittleMaidEntity.MaidSoul soul) {
        this.maidManager.registerMaid(soul);
    }

    @Override
    public List<MaidManager.LMInfo> getMaidList() {
        return this.maidManager.getMaidList();
    }

    @Override
    public void writeMaidManager(CompoundTag nbt) {
        this.maidManager.writeMaidManager(nbt);
    }

    @Override
    public void readMaidManager(CompoundTag nbt) {
        this.maidManager.readMaidManager(nbt);
    }

    @Override
    public List<LittleMaidEntity.MaidSoul> getMaidSouls() {
        return this.maidManager.getMaidSouls();
    }

    @Override
    public void clearMaidSouls() {
        this.maidManager.clearMaidSouls();
    }

    @Override
    public void checkMaidUnload() {
        this.maidManager.checkMaidUnload();
    }
}
