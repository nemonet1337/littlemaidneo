package work.nemonet.littlemaidneo.resource.holder;

import work.nemonet.littlemaidneo.resource.util.ResourceHelper;

import java.util.Map;
import java.util.Optional;

public class ConfigHolder {
    private final String packName;
    private final String parentName;
    private final String fileName;
    private final Map<String, String> settings;

    public ConfigHolder(String packName, String parentName, String fileName, Map<String, String> settings) {
        this.packName = packName;
        this.parentName = parentName;
        this.fileName = fileName;
        this.settings = settings;
    }

    public String getName() {
        return packName + "." + parentName + "." + fileName;
    }

    public String getPackName() {
        return packName;
    }

    public String getParentName() {
        return parentName;
    }

    public String getFileName() {
        return fileName;
    }

    public Optional<String> getParameter(String parameterName) {
        return Optional.ofNullable(settings.get(parameterName));
    }

    public Optional<String> getSoundFileName(String soundName) {
        Optional<String> optional = getParameter(soundName);
        if (optional.filter(s -> s.contains(":")).isPresent()) return optional;
        return optional
                .map(ResourceHelper::removeNameLastIndex)
                .map(fileName -> {
                    int firstSplitter = fileName.indexOf(".");
                    if (firstSplitter == -1) return "." + fileName;
                    int lastSplitter = fileName.lastIndexOf(".");
                    if (firstSplitter == lastSplitter) return fileName;
                    int secondLastSplitter = fileName.substring(0, lastSplitter).lastIndexOf(".");
                    return fileName.substring(secondLastSplitter + 1);
                })
                .map(fileName -> (packName + "." + fileName).toLowerCase());
    }
}
