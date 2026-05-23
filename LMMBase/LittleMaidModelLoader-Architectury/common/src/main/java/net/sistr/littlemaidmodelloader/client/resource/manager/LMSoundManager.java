package net.sistr.littlemaidmodelloader.client.resource.manager;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.client.sounds.WeighedSoundEvents;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundSource;
import net.sistr.littlemaidmodelloader.LMMLMod;
import net.sistr.littlemaidmodelloader.client.resource.LMSoundInstance;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

// 1.21.1移植: YarnマッピングからMojangマッピングへ変更
// - ResourceLocation → ResourceLocation
// - Minecraft → Minecraft
// - SoundCategory → SoundSource
// - WeightedSoundSet → WeighedSoundEvents
// - Soundクラスの直接使用を避け、WeighedSoundEventsを使用
//TODO MixモードとFillモード - リソースパックスクリーン改造
@Environment(EnvType.CLIENT)
public class LMSoundManager {
    public static final LMSoundManager INSTANCE = new LMSoundManager();
    private final Map<String, ResourceLocation> soundPaths = new HashMap<>();

    public void addSound(String packName, String parentName, String fileName, ResourceLocation location) {
        String key = (packName + "." + parentName + "." + fileName).toLowerCase();
        soundPaths.put(key, location);
    }

    public Optional<WeighedSoundEvents> getSound(String soundFileLocation) {
        if (soundFileLocation.contains(":")) {
            ResourceLocation loc = ResourceLocation.parse(soundFileLocation.toLowerCase());
            return Optional.ofNullable(Minecraft.getInstance().getSoundManager().getSoundEvent(loc));
        }

        ResourceLocation location = soundPaths.get(soundFileLocation);
        if (location != null) {
            return Optional.ofNullable(Minecraft.getInstance().getSoundManager().getSoundEvent(location));
        }
        return Optional.empty();
    }

    public void play(String soundFileName, SoundSource soundSource, double x, double y, double z) {
        getSound(soundFileName).ifPresent(soundSet -> {
            LMSoundInstance soundInstance = new LMSoundInstance(soundSet, soundSource, 
                    LMMLMod.getConfig().getVoiceVolume(), x, y, z);
            Minecraft.getInstance().getSoundManager().play(soundInstance);
        });
    }
}
