package work.nemonet.littlemaidneo.entity.soul;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.UUIDUtil;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import java.util.UUID;
import java.util.Optional;

import net.minecraft.world.level.storage.TagValueOutput;
import net.minecraft.util.ProblemReporter;
import net.minecraft.util.Util;
import work.nemonet.littlemaidneo.entity.LittleMaidEntity;

public record MaidSoulData(CompoundTag nbt, UUID uuid, String name) {
    public static final Codec<MaidSoulData> CODEC = RecordCodecBuilder.create(instance ->
            instance.group(
                    CompoundTag.CODEC.fieldOf("nbt").forGetter(MaidSoulData::nbt),
                    UUIDUtil.CODEC.fieldOf("uuid").forGetter(MaidSoulData::uuid),
                    Codec.STRING.fieldOf("name").forGetter(MaidSoulData::name)
            ).apply(instance, MaidSoulData::new)
    );

    public static final StreamCodec<FriendlyByteBuf, MaidSoulData> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.COMPOUND_TAG, MaidSoulData::nbt,
            UUIDUtil.STREAM_CODEC, MaidSoulData::uuid,
            ByteBufCodecs.STRING_UTF8, MaidSoulData::name,
            MaidSoulData::new
    );

    public static MaidSoulData create(LittleMaidEntity maid) {
        TagValueOutput output = TagValueOutput.createWithContext(
                ProblemReporter.DISCARDING,
                maid.registryAccess());
        maid.saveWithoutId(output);
        CompoundTag tag = output.buildResult();
        tag.putString("Name", maid.getName().getString());
        tag.putInt("dataVersion", 1);
        return new MaidSoulData(tag, maid.getUUID(), maid.getName().getString());
    }

    public static MaidSoulData fromNbt(CompoundTag nbt) {
        CompoundTag fixedNbt = MaidDataFixer.fix(nbt.copy());
        UUID uuid = fixedNbt
                .getIntArray("UUID")
                .filter(a -> a.length == 4)
                .map(UUIDUtil::uuidFromIntArray)
                .orElse(Util.NIL_UUID);
        String name = fixedNbt.getStringOr("Name", "");
        return new MaidSoulData(fixedNbt, uuid, name);
    }

    public Optional<UUID> getOwnerUUID() {
        return nbt.getIntArray("Owner")
                .filter(a -> a.length == 4)
                .map(UUIDUtil::uuidFromIntArray);
    }

    public CompoundTag getNbt() {
        return nbt;
    }

    public UUID getUuid() {
        return uuid;
    }

    public String getName() {
        return name;
    }
}
