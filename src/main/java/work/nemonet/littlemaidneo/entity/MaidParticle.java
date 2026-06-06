package work.nemonet.littlemaidneo.entity;

import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;

/**
 * メイドさんのパーティクル演出を担うヘルパークラス。
 */
public final class MaidParticle {
    private MaidParticle() {}

    public static void showFreedomParticle(LittleMaidEntity mob) {
        for (int i = 0; i < 7; ++i) {
            double d = mob.getRandom().nextGaussian() * 0.02;
            double e = mob.getRandom().nextGaussian() * 0.02;
            double f = mob.getRandom().nextGaussian() * 0.02;
            int rgb = ((int) (mob.getRandom().nextFloat() * 255) << 16) |
                    ((int) (mob.getRandom().nextFloat() * 255) << 8) |
                    (int) (mob.getRandom().nextFloat() * 255);
            mob.level().addParticle(
                    new DustParticleOptions(rgb, 1.0f),
                    mob.getRandomX(1.0),
                    mob.getRandomY() + 0.5,
                    mob.getRandomZ(1.0),
                    d,
                    e,
                    f);
        }
    }

    public static void showTracerParticle(LittleMaidEntity mob) {
        for (int i = 0; i < 7; ++i) {
            double d = mob.getRandom().nextGaussian() * 0.02;
            double e = mob.getRandom().nextGaussian() * 0.02;
            double f = mob.getRandom().nextGaussian() * 0.02;
            mob.level().addParticle(
                    ParticleTypes.CLOUD,
                    mob.getRandomX(1.0),
                    mob.getRandomY() + 0.5,
                    mob.getRandomZ(1.0),
                    d,
                    e,
                    f);
        }
    }

    public static void showTransAmParticles(LittleMaidEntity mob) {
        for (int i = 0; i < 20; ++i) {
            double d = mob.getRandom().nextGaussian() * 0.02;
            double e = mob.getRandom().nextGaussian() * 0.02;
            double f = mob.getRandom().nextGaussian() * 0.02;
            mob.level().addParticle(
                    ParticleTypes.FLAME,
                    mob.getRandomX(1.0),
                    mob.getRandomY() + 0.5,
                    mob.getRandomZ(1.0),
                    d,
                    e,
                    f);
        }
    }
}
