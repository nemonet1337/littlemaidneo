package work.nemonet.littlemaidneo.entity;

import net.minecraft.commands.arguments.EntityAnchorArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.player.Player;
import work.nemonet.littlemaidneo.advancement.criterion.LMRBCriteria;
import work.nemonet.littlemaidneo.entity.util.MaidManager;
import work.nemonet.littlemaidneo.entity.util.MaidMode;
import work.nemonet.littlemaidneo.entity.util.TameableUtil;
import work.nemonet.littlemaidneo.setup.ModRegistration;

/**
 * メイドさんの復活演出（魂からの蘇生・パーティクル/サウンド）を担う。
 *
 * <p>旧 {@code LittleMaidEntity.resurrectionMaid}（static）から抽出（R-3）。挙動は不変。
 */
public final class MaidResurrection {

    private MaidResurrection() {
    }

    public static boolean resurrect(ServerLevel world, BlockPos pos, Player player) {
        var maidSouls = player.getData(ModRegistration.MAID_MANAGER_ATTACHMENT.get()).getMaidSouls();
        if (maidSouls.isEmpty()) {
            return false;
        }
        for (LittleMaidEntity.MaidSoul maidSoul : maidSouls) {
            var maid = ModRegistration.LITTLE_MAID_ENTITY.get().create(world, EntitySpawnReason.TRIGGERED);
            if (maid != null) {
                maid.installMaidSoul(maidSoul);
                maid.snapTo(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5);

                maid.setMaidMode(MaidMode.ESCORT);
                TameableUtil.setWait(maid, true);
                maid.lookAt(
                        EntityAnchorArgument.Anchor.EYES,
                        player.getEyePosition());
                maid.getLookControl().setLookAt(player);

                maid.clearFire();
                maid.addEffect(
                        new MobEffectInstance(MobEffects.RESISTANCE, 100, 10));

                world.addFreshEntity(maid);

                LMRBCriteria.RESURRECT_MAID.trigger(
                        (ServerPlayer) player,
                        maid);
            }
        }
        player.getData(ModRegistration.MAID_MANAGER_ATTACHMENT.get()).clearMaidSouls();

        world.removeBlock(pos, false);
        world.playSound(
                null,
                pos.getX() + 0.5,
                pos.getY(),
                pos.getZ() + 0.5,
                SoundEvents.FIREWORK_ROCKET_TWINKLE,
                SoundSource.PLAYERS,
                1.0f,
                2.0f);
        world.playSound(
                null,
                pos.getX() + 0.5,
                pos.getY(),
                pos.getZ() + 0.5,
                SoundEvents.FIREWORK_ROCKET_BLAST,
                SoundSource.PLAYERS,
                1.0f,
                2.0f);
        world.sendParticles(
                ParticleTypes.EXPLOSION,
                pos.getX() + 0.5,
                pos.getY() + 0.5,
                pos.getZ() + 0.5,
                1,
                0,
                0,
                0,
                0);
        float size = 0.5f;
        int count = 10;
        double delta = 1.5;
        world.sendParticles(
                new DustParticleOptions(0xFF0000, size),
                pos.getX() + 0.5,
                pos.getY() + 0.5,
                pos.getZ() + 0.5,
                count,
                delta,
                delta,
                delta,
                0);
        world.sendParticles(
                new DustParticleOptions(0xFFA600, size),
                pos.getX() + 0.5,
                pos.getY() + 0.5,
                pos.getZ() + 0.5,
                count,
                delta,
                delta,
                delta,
                0);
        world.sendParticles(
                new DustParticleOptions(0xFFFF00, size),
                pos.getX() + 0.5,
                pos.getY() + 0.5,
                pos.getZ() + 0.5,
                count,
                delta,
                delta,
                delta,
                0);
        world.sendParticles(
                new DustParticleOptions(0x00FF00, size),
                pos.getX() + 0.5,
                pos.getY() + 0.5,
                pos.getZ() + 0.5,
                count,
                delta,
                delta,
                delta,
                0);
        world.sendParticles(
                new DustParticleOptions(0x00FFFF, size),
                pos.getX() + 0.5,
                pos.getY() + 0.5,
                pos.getZ() + 0.5,
                count,
                delta,
                delta,
                delta,
                0);
        world.sendParticles(
                new DustParticleOptions(0x0000FF, size),
                pos.getX() + 0.5,
                pos.getY() + 0.5,
                pos.getZ() + 0.5,
                count,
                delta,
                delta,
                delta,
                0);
        world.sendParticles(
                new DustParticleOptions(0x7F00FF, size),
                pos.getX() + 0.5,
                pos.getY() + 0.5,
                pos.getZ() + 0.5,
                count,
                delta,
                delta,
                delta,
                0);
        world.sendParticles(
                ParticleTypes.HEART,
                pos.getX() + 0.5,
                pos.getY() + 0.5,
                pos.getZ() + 0.5,
                count,
                delta,
                delta,
                delta,
                0);

        return true;
    }
}
