package work.nemonet.littlemaidneo.client.resource;

import net.minecraft.client.Minecraft;
import net.minecraft.client.sounds.WeighedSoundEvents;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundSource;
import work.nemonet.littlemaidneo.config.LMNModelConfig;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
public class LMSoundManager {
    public static final LMSoundManager INSTANCE = new LMSoundManager();
    private final Map<String, Identifier> soundPaths = new HashMap<>();

    public void addSound(String packName, String parentName, String fileName, Identifier location) {
        String key = (packName + "." + parentName + "." + fileName).toLowerCase();
        soundPaths.put(key, location);
    }

    public Optional<WeighedSoundEvents> getSound(String soundFileLocation) {
        if (soundFileLocation.contains(":")) {
            Identifier loc = Identifier.parse(soundFileLocation.toLowerCase());
            return Optional.ofNullable(Minecraft.getInstance().getSoundManager().getSoundEvent(loc));
        }

        Identifier location = soundPaths.get(soundFileLocation);
        if (location != null) {
            return Optional.ofNullable(Minecraft.getInstance().getSoundManager().getSoundEvent(location));
        }
        return Optional.empty();
    }

    public void play(String soundFileName, SoundSource soundSource, double x, double y, double z) {
        getSound(soundFileName).ifPresent(soundSet -> {
            LMSoundInstance soundInstance = new LMSoundInstance(soundSet, soundSource,
                    LMNModelConfig.getVoiceVolume(), x, y, z);
            Minecraft.getInstance().getSoundManager().play(soundInstance);
        });
    }
}
