package work.nemonet.littlemaidneo.entity.goal;

import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.projectile.Snowball;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import work.nemonet.littlemaidneo.resource.util.LMSounds;
import work.nemonet.littlemaidneo.entity.LittleMaidEntity;
import org.jetbrains.annotations.Nullable;
import java.util.Comparator;
import java.util.EnumSet;

public class PlaySnowGoal extends Goal {
    private final LittleMaidEntity mob;
    private final int maxCraftSnowballTime = 60;
    private final int maxLookTargetTime = 30;
    private final int maxWaitNextTime = 30;
    private int state;
    private int timer;
    @Nullable
    private LivingEntity target;

    public PlaySnowGoal(LittleMaidEntity mob) {
        this.mob = mob;
        this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        var time = this.mob.level().getDayTime();
        time = time % 24000;
        // 朝～昼以外はやらない
        if (time < 0 || 12500 < time) {
            return false;
        }
        var block = this.mob.getInBlockState();
        return block.is(BlockTags.SNOW);
    }

    @Override
    public boolean canContinueToUse() {
        return super.canContinueToUse();
    }

    @Override
    public void start() {
        state = 0;
        timer = 0;
        target = null;
        this.mob.setPlayingSnow(true);
    }

    @Override
    public void tick() {
        // 雪玉を作る
        if (state == 0) {
            if (timer == 0) {
                this.mob.play(LMSounds.COLLECT_SNOW);
            }
            if (timer % 15 == 0 && timer % 30 != 0) {
                this.mob.swing(InteractionHand.MAIN_HAND);
                this.mob.level().playSound(null, this.mob.getX(), this.mob.getY(), this.mob.getZ(),
                        SoundEvents.SNOW_HIT, SoundSource.NEUTRAL, 1.0f, 1.0f);
            }

            this.mob.setShiftKeyDown(true);
            var lookAt = this.mob.position()
                    .add(this.mob.getLookAngle()
                            .multiply(1, 0, 1)
                            .normalize()
                            .scale(this.mob.getEyeHeight(this.mob.getPose())));
            this.mob.getLookControl().setLookAt(lookAt);

            timer++;
            if (timer >= maxCraftSnowballTime) {
                state = 1;
                timer = 0;
            }
        }
        // 当てる相手を探す
        else if (state == 1) {
            this.mob.setShiftKeyDown(false);

            var world = this.mob.level();

            if (target == null) {
                timer = 0;
                this.target = world.getEntitiesOfClass(LivingEntity.class,
                                this.mob.getBoundingBox().inflate(10),
                                entity -> this.mob != entity)
                        .stream()
                        .sorted(Comparator.comparingDouble(this.mob::distanceToSqr))
                        .filter(entity -> this.mob.getSensing().hasLineOfSight(entity))
                        .findAny()
                        .orElse(null);
            } else {
                if (this.mob.getSensing().hasLineOfSight(target)) {
                    timer++;
                    this.mob.getLookControl().setLookAt(target);
                } else {
                    timer = 0;
                    this.target = null;
                }
            }

            if (timer >= maxLookTargetTime) {
                state = 2;
                timer = 0;
            }
        }
        // 投げる
        else {
            this.mob.setShiftKeyDown(false);

            if (target == null) {
                state = 1;
                timer = 0;
            } else {
                if (timer == 0) {
                    shootSnowBall(this.mob.level(), this.mob);
                    this.mob.swing(InteractionHand.MAIN_HAND);
                    this.mob.play(LMSounds.SHOOT);
                    this.mob.setYRot(this.mob.getYHeadRot());
                }
                this.mob.getLookControl().setLookAt(target);
            }

            timer++;
            if (timer >= maxWaitNextTime) {
                state = 0;
                timer = 0;
                target = null;
            }
        }
    }

    private void shootSnowBall(Level world, LivingEntity user) {
        world.playSound(null, user.getX(), user.getY(), user.getZ(),
                SoundEvents.SNOWBALL_THROW, SoundSource.NEUTRAL,
                0.5f, 0.4f / (world.getRandom().nextFloat() * 0.4f + 0.8f));
        if (!world.isClientSide) {
            Snowball snowballEntity = new Snowball(world, user);
            snowballEntity.setItem(Items.SNOWBALL.getDefaultInstance());
            snowballEntity.shootFromRotation(user, user.getXRot(), user.getYHeadRot(), 0.0f, 1.5f, 1.0f);
            world.addFreshEntity(snowballEntity);
        }
    }

    @Override
    public void stop() {
        this.mob.setShiftKeyDown(false);
        this.mob.setPlayingSnow(false);
    }

    @Override
    public boolean requiresUpdateEveryTick() {
        return true;
    }
}
