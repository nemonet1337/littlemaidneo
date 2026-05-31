package work.nemonet.littlemaidneo.mixin;

import com.mojang.datafixers.util.Either;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.level.storage.TagValueOutput;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Unit;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.OwnableEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import work.nemonet.littlemaidneo.entity.compound.SoundPlayable;
import work.nemonet.littlemaidneo.resource.util.LMSounds;
import work.nemonet.littlemaidneo.entity.LittleMaidEntity;
import work.nemonet.littlemaidneo.entity.MaidSoulEntity;
import work.nemonet.littlemaidneo.entity.targeting.TargetTagManager;
import work.nemonet.littlemaidneo.entity.util.MaidManager;
import work.nemonet.littlemaidneo.entity.util.MaidManagerImpl;
import work.nemonet.littlemaidneo.entity.util.TameableUtil;
import work.nemonet.littlemaidneo.world.WorldMaidSoulState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;
import java.util.stream.Stream;

@Mixin(ServerPlayer.class)
public abstract class MixinServerPlayerEntity extends MixinPlayerEntity implements MaidManager {


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
        TagValueOutput output = TagValueOutput.createWithContext(ProblemReporter.DISCARDING, this.registryAccess());
        ((MaidManager) oldPlayer).writeMaidManager(output);
        this.readMaidManager(TagValueInput.create(ProblemReporter.DISCARDING, this.registryAccess(), output.buildResult()));
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
    private void onReadSP(ValueInput input, CallbackInfo ci) {
        this.readMaidManager(input);
        migrateWorldMaidSoulState();
    }

    @Inject(method = "addAdditionalSaveData", at = @At("RETURN"))
    private void onWriteSP(ValueOutput output, CallbackInfo ci) {
        this.checkMaidUnload();
        this.writeMaidManager(output);
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
        WorldMaidSoulState worldMaidSoulState = WorldMaidSoulState.getWorldMaidSoulState((ServerLevel) this.level());
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
    public void writeMaidManager(ValueOutput output) {
        this.maidManager.writeMaidManager(output);
    }

    @Override
    public void readMaidManager(ValueInput input) {
        this.maidManager.readMaidManager(input);
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
