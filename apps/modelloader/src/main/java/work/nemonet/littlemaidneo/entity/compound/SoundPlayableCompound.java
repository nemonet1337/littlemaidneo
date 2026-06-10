package work.nemonet.littlemaidneo.entity.compound;

import net.minecraft.world.entity.Entity;
import work.nemonet.littlemaidneo.client.resource.LMSoundManager;
import work.nemonet.littlemaidneo.resource.holder.ConfigHolder;
import work.nemonet.littlemaidneo.resource.manager.LMConfigManager;

import java.util.function.BiConsumer;
import java.util.function.Supplier;

public class SoundPlayableCompound implements SoundPlayable {
    // サーバー側の S2C 音声同期はネットワーク層（mods モジュール）の責務。
    // モジュール依存を mods → modelloader の一方向に保つため、送信処理はフックとして注入する。
    private static BiConsumer<Entity, String> soundSyncSender = (entity, soundName) -> {};

    public static void setSoundSyncSender(BiConsumer<Entity, String> sender) {
        soundSyncSender = sender;
    }

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
            soundSyncSender.accept(entity, soundName);
        }
    }
}
