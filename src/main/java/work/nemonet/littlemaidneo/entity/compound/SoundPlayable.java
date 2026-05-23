package work.nemonet.littlemaidneo.entity.compound;

import work.nemonet.littlemaidneo.resource.holder.ConfigHolder;

public interface SoundPlayable {

    void play(String soundName);

    void setConfigHolder(ConfigHolder configHolder);

    ConfigHolder getConfigHolder();
}
