package net.sistr.littlemaidrebirth.mixin;

import com.mojang.authlib.GameProfile;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.sistr.littlemaidrebirth.entity.LittleMaidEntity;
import net.sistr.littlemaidrebirth.entity.targeting.TargetIdentifier;
import net.sistr.littlemaidrebirth.entity.targeting.TargetTagManager;
import net.sistr.littlemaidrebirth.entity.targeting.TargetTagManagerImpl;
import net.sistr.littlemaidrebirth.entity.targeting.TargetingSystem;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Set;

@Mixin(Player.class)
public abstract class MixinPlayerEntity extends LivingEntity implements TargetTagManager {
    // TODO TargetTagManagerはServerPlayer側で実装すべきかもしれない
    @Unique
    private TargetTagManager targetTagManager;

    protected MixinPlayerEntity(EntityType<? extends LivingEntity> entityType, Level world) {
        super(entityType, world);
    }

    @Inject(method = "<init>", at = @At("RETURN"))
    public void onInit(Level world, BlockPos pos, float yaw, GameProfile gameProfile, CallbackInfo ci) {
        this.targetTagManager = new TargetTagManagerImpl(world);
    }

    @Inject(method = "readAdditionalSaveData", at = @At("RETURN"))
    public void onRead(CompoundTag nbt, CallbackInfo ci) {
        this.readTargetTags(nbt);
    }

    @Inject(method = "addAdditionalSaveData", at = @At("RETURN"))
    public void onWrite(CompoundTag nbt, CallbackInfo ci) {
        this.writeTargetTags(nbt);
    }

    @Override
    public Set<TargetingSystem.TargetTag> getTargetTag(TargetIdentifier id) {
        return this.targetTagManager.getTargetTag(id);
    }

    @Override
    public void readTargetTags(CompoundTag nbt) {
        this.targetTagManager.readTargetTags(nbt);
    }

    @Override
    public void writeTargetTags(CompoundTag nbt) {
        this.targetTagManager.writeTargetTags(nbt);
    }

    @Override
    public Sync getTargetTagsSync() {
        return this.targetTagManager.getTargetTagsSync();
    }

    @Override
    public void positionRider(Entity passenger, MoveFunction positionUpdater) {
        if (!(passenger instanceof LittleMaidEntity)) {
            super.positionRider(passenger, positionUpdater);
            return;
        }
        if (!this.hasPassenger(passenger)) {
            return;
        }
        float z = -6 / 16f * 0.9375F;
        // 1.21.1: getMountedHeightOffset/getHeightOffset は廃止
        // プレイヤーのBB高さをベースにオフセットを計算
        float y = (float) (this.getBbHeight() * 0.75 - 4 / 16f * 0.9375F
                + ((LittleMaidEntity) passenger).getRidingYOffset());
        Vec3 pos = new Vec3(z, 0.0, 0.0).yRot((float) (-this.yBodyRot * (Math.PI / 180.0) - Math.PI / 2.0));
        positionUpdater.accept(passenger, this.getX() + pos.x, this.getY() + (double) y, this.getZ() + pos.z);
        this.copyEntityData(passenger);
    }

    @Override
    public void onPassengerTurned(Entity passenger) {
        if (!(passenger instanceof LittleMaidEntity)) {
            super.onPassengerTurned(passenger);
            return;
        }
        copyEntityData(passenger);
    }

    protected void copyEntityData(Entity entity) {
        float yaw = this.yBodyRot;
        entity.setYBodyRot(yaw);
        float f = Mth.wrapDegrees(yaw - this.getYRot());
        float f1 = Mth.clamp(f, -105.0F, 105.0F);
        entity.yRotO += f1 - f;
        entity.setYRot(yaw + f1 - f);
        entity.setYHeadRot(yaw);
    }

    @Inject(method = "stopSleepInBed(ZZ)V", at = @At("RETURN"))
    private void onStopSleepInBed(boolean skipSleepTimer, boolean updateSleepingPlayers, CallbackInfo ci) {

    }
}
