package work.nemonet.littlemaidneo.entity.util;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

/**
 * Item Data Map {@code littlemaidneo:maid_job} の 1 エントリ。
 * datapack でジョブと優先度を付けられる。
 *
 * <p>{@code job} は {@link MaidJob} の小文字名で書く。旧誤字 {@code "pharmcist"} は
 * {@link MaidJob#CODEC} の読み込み時に正規化される。
 */
public record MaidJobEntry(MaidJob job, int priority) {
    public static final Codec<MaidJobEntry> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            MaidJob.CODEC.fieldOf("job").forGetter(MaidJobEntry::job),
            Codec.INT.optionalFieldOf("priority", 400).forGetter(MaidJobEntry::priority)
    ).apply(instance, MaidJobEntry::new));
}
