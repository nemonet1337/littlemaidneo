package work.nemonet.littlemaidneo.client.resource;

import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.Sound;
import net.minecraft.client.sounds.WeighedSoundEvents;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.valueproviders.ConstantFloat;
import work.nemonet.littlemaidneo.config.LMNModelConfig;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * 外部ボイスパックの .ogg を再生する。
 * <p>
 * バニラ {@code SoundManager} は {@code sounds.json} 経由で {@link WeighedSoundEvents} を構築するが、
 * 外部パックは {@link ResourceWrapper} に .ogg を載せるだけで {@code sounds.json} を持たない。
 * そのため {@link #getSound(String)} は {@code getSoundEvent} に依存せず、
 * 登録済みリソースパスから {@link Sound}/{@link WeighedSoundEvents} を直接構築する。
 */
public class LMSoundManager {
    public static final LMSoundManager INSTANCE = new LMSoundManager();
    private final Map<String, Identifier> soundPaths = new HashMap<>();
    private final Map<String, WeighedSoundEvents> soundEvents = new HashMap<>();

    public void addSound(String packName, String parentName, String fileName, Identifier location) {
        String key = (packName + "." + parentName + "." + fileName).toLowerCase();
        soundPaths.put(key, location);
        soundEvents.put(key, buildSoundEvents(location));
    }

    /**
     * {@code location} は {@link ResourceWrapper} に登録した実ファイル ID
     * （例: {@code littlemaidneo:sounds/pack/file.ogg}）。
     * バニラ {@link Sound#getPath()} は {@code sounds/} + id + {@code .ogg} を付けるため、
     * Sound 側の location は {@link Sound#SOUND_LISTER}{@code .fileToId} で逆変換する。
     */
    private static WeighedSoundEvents buildSoundEvents(Identifier resourceLocation) {
        Identifier soundId = Sound.SOUND_LISTER.fileToId(resourceLocation);
        Sound sound = new Sound(
                soundId,
                ConstantFloat.of(1.0f),
                ConstantFloat.of(1.0f),
                1,
                Sound.Type.FILE,
                false,
                false,
                16);
        WeighedSoundEvents events = new WeighedSoundEvents(soundId, null);
        events.addSound(sound);
        return events;
    }

    public Optional<WeighedSoundEvents> getSound(String soundFileLocation) {
        // "namespace:path" 形式はバニラ / 他 Mod のサウンドイベント ID として解決
        if (soundFileLocation.contains(":")) {
            Identifier loc = Identifier.parse(soundFileLocation.toLowerCase());
            WeighedSoundEvents registered = Minecraft.getInstance().getSoundManager().getSoundEvent(loc);
            if (registered != null) {
                return Optional.of(registered);
            }
            return Optional.empty();
        }

        WeighedSoundEvents events = soundEvents.get(soundFileLocation);
        if (events != null) {
            return Optional.of(events);
        }
        // 呼び出し側が大文字混在キーを渡した場合に備える
        return Optional.ofNullable(soundEvents.get(soundFileLocation.toLowerCase()));
    }

    public void play(String soundFileName, SoundSource soundSource, double x, double y, double z) {
        getSound(soundFileName).ifPresent(soundSet -> {
            LMSoundInstance soundInstance = new LMSoundInstance(soundSet, soundSource,
                    LMNModelConfig.getVoiceVolume(), x, y, z);
            Minecraft.getInstance().getSoundManager().play(soundInstance);
        });
    }
}
