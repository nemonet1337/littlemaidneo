package work.nemonet.littlemaidneo.entity.ai.behavior;

import com.google.common.collect.ImmutableMap;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.behavior.Behavior;
import net.minecraft.world.entity.ai.memory.MemoryStatus;
import net.minecraft.world.entity.projectile.throwableitemprojectile.Snowball;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import work.nemonet.littlemaidneo.entity.LittleMaidEntity;
import work.nemonet.littlemaidneo.resource.util.LMSounds;
import work.nemonet.littlemaidneo.setup.ModRegistration;
import org.jetbrains.annotations.Nullable;

import java.util.Comparator;

public class MaidPlaySnowBehavior extends AbstractMaidBehavior {
    private final int maxCraftSnowballTime = 60;
    private final int maxLookTargetTime = 30;
    private final int maxWaitNextTime = 30;
    private int state;
    private int timer;
    @Nullable
    private LivingEntity target;

    public MaidPlaySnowBehavior() {
        super(ImmutableMap.of(
                ModRegistration.IS_WAITING.get(), MemoryStatus.VALUE_ABSENT
        ));
    }

    @Override
    protected boolean checkExtraStartConditions(ServerLevel level, LittleMaidEntity entity) {
        return canPlaySnow(entity);
    }

    private boolean canPlaySnow(LittleMaidEntity entity) {
        var time = entity.level().getOverworldClockTime();
        time = time % 24000;
        // 朝～昼以外はやらない
        if (time < 0 || 12500 < time) {
            return false;
        }
        var block = entity.getInBlockState();
        return block.is(BlockTags.SNOW);
    }

    @Override
    protected boolean canStillUse(ServerLevel level, LittleMaidEntity entity, long gameTime) {
        return canPlaySnow(entity);
    }

    @Override
    protected void start(ServerLevel level, LittleMaidEntity entity, long gameTime) {
        state = 0;
        timer = 0;
        target = null;
        entity.setPlayingSnow(true);
    }

    @Override
    protected void stop(ServerLevel level, LittleMaidEntity entity, long gameTime) {
        entity.setShiftKeyDown(false);
        entity.setPlayingSnow(false);
        target = null;
    }

    @Override
    protected void tick(ServerLevel level, LittleMaidEntity entity, long gameTime) {
        // 雪玉を作る
        if (state == 0) {
            if (timer == 0) {
                entity.play(LMSounds.COLLECT_SNOW);
            }
            if (timer % 15 == 0 && timer % 30 != 0) {
                entity.swing(InteractionHand.MAIN_HAND);
                entity.level().playSound(null, entity.getX(), entity.getY(), entity.getZ(),
                        SoundEvents.SNOW_HIT, SoundSource.NEUTRAL, 1.0f, 1.0f);
            }

            entity.setShiftKeyDown(true);
            var lookAt = entity.position()
                    .add(entity.getLookAngle()
                            .multiply(1, 0, 1)
                            .normalize()
                            .scale(entity.getEyeHeight(entity.getPose())));
            entity.getLookControl().setLookAt(lookAt);

            timer++;
            if (timer >= maxCraftSnowballTime) {
                state = 1;
                timer = 0;
            }
        }
        // 当てる相手を探す
        else if (state == 1) {
            entity.setShiftKeyDown(false);

            var world = entity.level();

            if (target == null) {
                timer = 0;
                this.target = world.getEntitiesOfClass(LivingEntity.class,
                                entity.getBoundingBox().inflate(10),
                                e -> entity != e)
                        .stream()
                        .sorted(Comparator.comparingDouble(entity::distanceToSqr))
                        .filter(e -> entity.getSensing().hasLineOfSight(e))
                        .findAny()
                        .orElse(null);
            } else {
                if (entity.getSensing().hasLineOfSight(target)) {
                    timer++;
                    entity.getLookControl().setLookAt(target);
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
            entity.setShiftKeyDown(false);

            if (target == null) {
                state = 1;
                timer = 0;
            } else {
                if (timer == 0) {
                    shootSnowBall(entity.level(), entity);
                    entity.swing(InteractionHand.MAIN_HAND);
                    entity.play(LMSounds.SHOOT);
                    entity.setYRot(entity.getYHeadRot());
                }
                entity.getLookControl().setLookAt(target);
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
        if (!world.isClientSide()) {
            Snowball snowballEntity = new Snowball(world, user, Items.SNOWBALL.getDefaultInstance());
            snowballEntity.shootFromRotation(user, user.getXRot(), user.getYHeadRot(), 0.0f, 1.5f, 1.0f);
            world.addFreshEntity(snowballEntity);
        }
    }
}
