package work.nemonet.littlemaidneo.resource.manager;

import com.google.common.collect.ImmutableMap;
import work.nemonet.littlemaidneo.resource.holder.ConfigHolder;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

public class LMConfigManager {
    public static final LMConfigManager INSTANCE = new LMConfigManager();
    public static final ConfigHolder EMPTY_CONFIG = new ConfigHolder("empty", "", "", ImmutableMap.of());
    private final Map<String, ConfigHolder> configs = new HashMap<>();

    public void addConfig(String packName, String parentName, String fileName, Map<String, String> settings) {
        ConfigHolder config = new ConfigHolder(packName, parentName, fileName, settings);
        configs.put(config.getName().toLowerCase(), config);
    }

    public Optional<ConfigHolder> getConfig(String configName) {
        return Optional.ofNullable(configs.get(configName.toLowerCase()));
    }

    public Optional<ConfigHolder> getTextureSoundConfig(String texturePackName) {
        return configs.values().stream()
                .filter(ch -> ch.getFileName().equalsIgnoreCase(texturePackName))
                .findAny();
    }

    public ConfigHolder getAnyConfig() {
        var list = configs.values().stream()
                .filter(ch -> ch.getFileName().equalsIgnoreCase("littlemaidmob"))
                .toList();
        if (list.isEmpty()) return EMPTY_CONFIG;
        return list.get(ThreadLocalRandom.current().nextInt(list.size()));
    }

    public List<ConfigHolder> getAllConfig() {
        List<ConfigHolder> list = configs.values().stream()
                .sorted(Comparator.comparing(ConfigHolder::getName, String::compareTo))
                .collect(Collectors.toList());
        list.add(EMPTY_CONFIG);
        return list;
    }
}
