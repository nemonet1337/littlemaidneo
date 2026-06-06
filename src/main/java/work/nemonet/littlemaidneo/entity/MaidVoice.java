package work.nemonet.littlemaidneo.entity;

import net.minecraft.world.item.Items;
import net.minecraft.world.level.biome.Biome;
import work.nemonet.littlemaidneo.resource.util.LMSounds;

/**
 * メイドさんの環境音・ボイス再生処理を担うヘルパークラス。
 */
public final class MaidVoice {
    private MaidVoice() {}

    public static void playAmbientSound(LittleMaidEntity mob) {
        if (mob.level().isClientSide() ||
                mob.isDeadOrDying() ||
                mob.getConfigHolder()
                        .getParameter("LivingVoiceRate")
                        .map(s -> {
                            try {
                                return Float.parseFloat(s);
                            } catch (Exception e) {
                                return null;
                            }
                        })
                        .orElse(0.2f) < mob.getRandom().nextFloat()) {
            return;
        }
        if (mob.getHealth() / mob.getMaxHealth() < 0.3F) {
            mob.play(LMSounds.LIVING_WHINE);
        } else {
            if (mob.tickCount % 4 == 0 &&
                    mob.level().canSeeSky(mob.blockPosition())) {
                Biome biome = mob.level().getBiome(mob.blockPosition()).value();
                if (biome.coldEnoughToSnow(
                        mob.blockPosition(),
                        mob.level().getSeaLevel())) {
                    mob.play(LMSounds.LIVING_COLD);
                } else if (2 <= biome.getBaseTemperature()) {
                    mob.play(LMSounds.LIVING_HOT);
                }
            } else if (mob.tickCount % 4 == 1 && mob.level().isRaining()) {
                var pos = mob.blockPosition();
                Biome biome = mob.level().getBiome(pos).value();
                if (biome.getPrecipitationAt(pos, pos.getY()) == Biome.Precipitation.RAIN)
                    mob.play(LMSounds.LIVING_RAIN);
                else if (biome.getPrecipitationAt(pos, pos.getY()) == Biome.Precipitation.SNOW)
                    mob.play(LMSounds.LIVING_SNOW);
            } else {
                if (mob.getMainHandItem().getItem() == Items.CLOCK ||
                        mob.getOffhandItem().getItem() == Items.CLOCK) {
                    int time = (int) (mob.level().getGameTime() % 24000);
                    if (time < 1500 || 23500 <= time) {
                        mob.play(LMSounds.LIVING_MORNING);
                    } else if (12500 <= time) {
                        mob.play(LMSounds.LIVING_NIGHT);
                    } else {
                        mob.play(LMSounds.LIVING_DAYTIME);
                    }
                } else {
                    mob.play(LMSounds.LIVING_DAYTIME);
                }
            }
        }
    }
}
