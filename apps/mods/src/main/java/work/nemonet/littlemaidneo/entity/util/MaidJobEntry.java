package work.nemonet.littlemaidneo.entity.util;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

/**
 * Item Data Map {@code littlemaidneo:maid_job} の 1 エントリ。
 * datapack でジョブと優先度を付けられる。
 */
public record MaidJobEntry(String job, int priority) {
    /** 旧表記のジョブ名を現行表記へ正規化し、datapack 由来の値も現行名に揃える。 */
    public MaidJobEntry {
        job = MaidJobManager.normalizeJob(job);
    }

    public static final Codec<MaidJobEntry> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.STRING.fieldOf("job").forGetter(MaidJobEntry::job),
            Codec.INT.optionalFieldOf("priority", 400).forGetter(MaidJobEntry::priority)
    ).apply(instance, MaidJobEntry::new));
}
