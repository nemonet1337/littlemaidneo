package work.nemonet.littlemaidneo.mixin;

import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import work.nemonet.littlemaidneo.entity.LittleMaidEntity;
import work.nemonet.littlemaidneo.setup.ModRegistration;

@Mixin(Player.class)
public abstract class MixinPlayerEntity
    extends LivingEntity
{

    protected MixinPlayerEntity(
        EntityType<? extends LivingEntity> entityType,
        Level world
    ) {
        super(entityType, world);
    }

    @Inject(method = "readAdditionalSaveData", at = @At("RETURN"))
    public void onRead(ValueInput input, CallbackInfo ci) {
        var oldTags = input.childrenListOrEmpty("targetTagMap");
        if (!oldTags.isEmpty()) {
            this.getData(ModRegistration.TARGET_TAG_ATTACHMENT.get()).readTargetTags(input);
        }
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
        float z = (-6 / 16f) * 0.9375F;
        // 1.21.1: getMountedHeightOffset/getHeightOffset は廃止
        // プレイヤーのBB高さをベースにオフセットを計算
        float y = (float) (this.getBbHeight() * 0.75 -
            (4 / 16f) * 0.9375F +
            ((LittleMaidEntity) passenger).getRidingYOffset());
        Vec3 pos = new Vec3(z, 0.0, 0.0).yRot(
            (float) (-this.yBodyRot * (Math.PI / 180.0) - Math.PI / 2.0)
        );
        positionUpdater.accept(
            passenger,
            this.getX() + pos.x,
            this.getY() + (double) y,
            this.getZ() + pos.z
        );
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


}
