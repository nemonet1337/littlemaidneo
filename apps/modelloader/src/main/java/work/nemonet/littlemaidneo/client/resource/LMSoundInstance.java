package work.nemonet.littlemaidneo.client.resource;

import net.minecraft.client.resources.sounds.Sound;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.client.sounds.SoundManager;
import net.minecraft.client.sounds.WeighedSoundEvents;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;

public class LMSoundInstance implements SoundInstance {
    private final WeighedSoundEvents soundSet;
    private final Sound sound;
    private final Identifier location;
    private final SoundSource source;
    private final float volume;
    private final double x;
    private final double y;
    private final double z;

    public LMSoundInstance(WeighedSoundEvents soundSet, SoundSource source,
                           float volume, double x, double y, double z) {
        this.soundSet = soundSet;
        this.sound = soundSet.getSound(RandomSource.create());
        this.location = sound.getLocation();
        this.source = source;
        this.volume = volume;
        this.x = x;
        this.y = y;
        this.z = z;
    }

    public Identifier getLocation() {
        return location;
    }

    @Override
    public Identifier getIdentifier() {
        return location;
    }

    @Override
    public Sound getSound() {
        return this.sound;
    }

    @Override
    public SoundSource getSource() {
        return source;
    }

    @Override
    public boolean isLooping() {
        return false;
    }

    @Override
    public boolean isRelative() {
        return false;
    }

    @Override
    public int getDelay() {
        return 0;
    }

    @Override
    public float getVolume() {
        return volume;
    }

    @Override
    public float getPitch() {
        return 1f;
    }

    @Override
    public double getX() {
        return x;
    }

    @Override
    public double getY() {
        return y;
    }

    @Override
    public double getZ() {
        return z;
    }

    @Override
    public Attenuation getAttenuation() {
        return Attenuation.LINEAR;
    }

    @Override
    public WeighedSoundEvents resolve(SoundManager soundManager) {
        return this.soundSet;
    }
}
