package work.nemonet.littlemaidneo.common;

import work.nemonet.littlemaidneo.entity.compound.SoundPlayable;
import work.nemonet.littlemaidneo.entity.compound.SoundPlayableCompound;
import work.nemonet.littlemaidneo.resource.holder.ConfigHolder;

public interface SoundHolder extends SoundPlayable {

    SoundPlayableCompound getSoundPlayer();

    @Override
    default void play(String soundName) {
        getSoundPlayer().play(soundName);
    }

    @Override
    default void setConfigHolder(ConfigHolder configHolder) {
        getSoundPlayer().setConfigHolder(configHolder);
    }

    @Override
    default ConfigHolder getConfigHolder() {
        return getSoundPlayer().getConfigHolder();
    }
}
