package work.nemonet.littlemaidneo.entity.compound;

import net.minecraft.world.entity.Entity;
import work.nemonet.littlemaidneo.client.resource.LMSoundManager;
import work.nemonet.littlemaidneo.network.NetworkHandler;
import work.nemonet.littlemaidneo.resource.holder.ConfigHolder;
import work.nemonet.littlemaidneo.resource.manager.LMConfigManager;

import java.util.function.Supplier;

public class SoundPlayableCompound implements SoundPlayable {
    private final Entity entity;
    private final Supplier<String> packName;
    private ConfigHolder configHolder;

    public SoundPlayableCompound(Entity entity, Supplier<String> packName) {
        this.entity = entity;
        this.packName = packName;
        update();
    }

    public void update() {
        LMConfigManager configManager = LMConfigManager.INSTANCE;
        configHolder = configManager.getTextureSoundConfig(getPackName())
                .orElse(configManager.getAnyConfig());
    }

    public String getPackName() {
        return packName.get();
    }

    public void setConfigHolder(ConfigHolder configHolder) {
        this.configHolder = configHolder;
    }

    public ConfigHolder getConfigHolder() {
        return this.configHolder;
    }

    @Override
    public void play(String soundName) {
        if (entity.level().isClientSide()) {
            configHolder.getSoundFileName(soundName.toLowerCase())
                    .ifPresent(soundFileName ->
                            LMSoundManager.INSTANCE.play(soundFileName, entity.getSoundSource(),
                                    entity.getX(), entity.getEyeY(), entity.getZ()));
        } else {
            NetworkHandler.sendLMSoundS2C(entity, soundName);
        }
    }
}
