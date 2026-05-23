package work.nemonet.littlemaidneo.entity;

import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.material.PushReaction;
import net.minecraft.world.phys.Vec3;
import work.nemonet.littlemaidneo.entity.util.MaidManager;
import work.nemonet.littlemaidneo.setup.Registration;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;

//メイドソウル
//体重21g！
public class MaidSoulEntity extends Entity {
    @Nullable
    private LittleMaidEntity.MaidSoul maidSoul; // メイドソウルはクライアント側はnull
    private int waveProgress;
    private boolean maidManagerRegistered;

    public MaidSoulEntity(EntityType<?> type, Level world) {
        super(type, world);
        this.noPhysics = true;
    }

    public MaidSoulEntity(Level world, LittleMaidEntity.MaidSoul maidSoul) {
        this(Registration.MAID_SOUL_ENTITY.get(), world);
        this.maidSoul = maidSoul;
    }

    @Override
    public void tick() {
        if (!this.level().isClientSide()
                && !this.maidManagerRegistered
                && this.maidSoul != null) {
            this.maidSoul.getOwnerUUID()
                    .map(id -> ((ServerLevel) this.level()).getEntity(id))
                    .filter(owner -> owner instanceof MaidManager)
                    .ifPresent(owner -> {
                        ((MaidManager) owner).registerMaid(this);
                        this.maidManagerRegistered = true;
                    });
        }
        int loop = 20 * 4;
        // 上端下端のときrange = 0
        // waveProgressが0/半分/最後のときが上端下端
        float range = Mth.sin(
                Mth.PI
                        * ((float) ((this.waveProgress + loop / 4) % (loop / 2)) / (loop / 2f)))
                * 0.4f + 0.1f;
        int rotateTicks = 20 * 1;
        float rotate = Mth.PI * 2 * ((float) (this.tickCount % rotateTicks) / rotateTicks);
        float waveHeight = 1f;
        float x = (Mth.sin(rotate)) * range;
        float z = (Mth.cos(rotate)) * range;
        float y = Mth.sin(
                Mth.PI * 2
                        * ((float) (this.waveProgress % loop) / loop))
                * (waveHeight / 2);

        var particle = ParticleTypes.ELECTRIC_SPARK;

        float yOffset = 0.25f;
        var world = level();
        world.addParticle(
                particle,
                this.getX() + x,
                this.getY() + y + yOffset,
                this.getZ() + z,
                0, 0, 0);
        world.addParticle(
                particle,
                this.getX() - x,
                this.getY() + y + yOffset,
                this.getZ() - z,
                0, 0, 0);
        world.addParticle(
                particle,
                this.getX() - x,
                this.getY() - y + yOffset,
                this.getZ() - z,
                0, 0, 0);
        world.addParticle(
                particle,
                this.getX() + x,
                this.getY() - y + yOffset,
                this.getZ() + z,
                0, 0, 0);

        this.waveProgress++;

        if (world instanceof ServerLevel serverWorld
                && this.maidSoul != null
                && maidSoul.getOwnerUUID().isPresent()) {
            var owner = serverWorld.getEntity(maidSoul.getOwnerUUID().get());
            if (owner != null) {
                var toOwnerVec = owner.position().subtract(this.position()).normalize();
                var distanceSq = Math.max(this.distanceToSqr(owner.getEyePosition()), 0.5 * 0.5);
                var addVec = toOwnerVec.scale(0.0125 / distanceSq);
                if (addVec.lengthSqr() > 0.001 * 0.001) {
                    setDeltaMovement(getDeltaMovement().add(addVec));
                }
            }
        }

        var velocity = getDeltaMovement();
        double vx = Math.min(velocity.x(), 0.2);
        double vy = Math.min(velocity.y(), 0.2);
        double vz = Math.min(velocity.z(), 0.2);
        if (world.noCollision(this, getBoundingBox().move(vx, vy, vz))) {
            double nx = this.getX() + vx;
            double ny = this.getY() + vy;
            double nz = this.getZ() + vz;
            this.setPos(nx, ny, nz);
            setDeltaMovement(velocity.scale(0.95f));
        } else {
            // 進行方向が埋まっていて、逆方向が開いてるなら弾かれる
            if (world.noCollision(this, getBoundingBox().move(-vx, -vy, -vz))) {
                double nx = this.getX() - vx;
                double ny = this.getY() - vy;
                double nz = this.getZ() - vz;
                this.setPos(nx, ny, nz);
                setDeltaMovement(velocity.scale(-0.95f));
            } else {
                setDeltaMovement(Vec3.ZERO);
            }

        }

        // 埋まった場合はちょっとづつ浮く
        if (!world.noCollision(this)) {
            this.setPos(this.getX(), this.getY() + 0.1, this.getZ());
        }
    }

    @Override
    public void playerTouch(Player player) {
        super.playerTouch(player);
        if (maidSoul == null) {
            return;
        }
        maidSoul.getOwnerUUID()
                .filter(id -> id.equals(player.getUUID()))
                .ifPresent(id -> pickupSoul(player));
    }

    protected void pickupSoul(Player player) {
        player.take(this, 1);
        if (this.level() instanceof ServerLevel serverWorld) {
            ((MaidManager) player).registerMaid(this.maidSoul);
            serverWorld.playSound(null, this.getX(), this.getY(), this.getZ(),
                    SoundEvents.FIREWORK_ROCKET_TWINKLE, SoundSource.PLAYERS,
                    1.0f, 1.0f);
            float size = 0.5f;
            int count = 20;
            double delta = 1.0;
            // TODO エフェクト調整
            serverWorld.sendParticles(
                    new DustParticleOptions(new Vector3f(1.0f, 0.0f, 0.0f), size),
                    this.getX(), this.getY(), this.getZ(),
                    count, delta, delta, delta, 0);
            serverWorld.sendParticles(
                    new DustParticleOptions(new Vector3f(0.0f, 1.0f, 0.0f), size),
                    this.getX(), this.getY(), this.getZ(),
                    count, delta, delta, delta, 0);
            serverWorld.sendParticles(
                    new DustParticleOptions(new Vector3f(0.0f, 0.0f, 1.0f), size),
                    this.getX(), this.getY(), this.getZ(),
                    count, delta, delta, delta, 0);
            // TODO 憑依ステータス効果
        }
        this.discard();
    }

    @Override
    protected void defineSynchedData(net.minecraft.network.syncher.SynchedEntityData.Builder builder) {

    }

    @Override
    protected void readAdditionalSaveData(CompoundTag nbt) {
        if (nbt.contains("maidSoul")) {
            this.maidSoul = LittleMaidEntity.MaidSoul.fromNbt(nbt.getCompound("maidSoul"));
        }
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag nbt) {
        if (this.maidSoul != null) {
            nbt.put("maidSoul", this.maidSoul.getNbt().copy());
        }
    }

    @Override
    protected boolean canAddPassenger(Entity passenger) {
        return false;
    }

    @Override
    protected boolean couldAcceptPassenger() {
        return false;
    }

    @Override
    protected void addPassenger(Entity passenger) {
        throw new IllegalStateException("Should never addPassenger without checking couldAcceptPassenger()");
    }

    @Override
    public PushReaction getPistonPushReaction() {
        return PushReaction.IGNORE;
    }

    @Override
    public boolean isIgnoringBlockTriggers() {
        return true;
    }

    public int getWaveProgress() {
        return waveProgress;
    }

    public LittleMaidEntity.MaidSoul getSoul() {
        return this.maidSoul;
    }
}
