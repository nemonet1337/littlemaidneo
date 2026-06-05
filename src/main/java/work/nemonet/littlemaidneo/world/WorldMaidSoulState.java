package work.nemonet.littlemaidneo.world;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.mojang.serialization.Codec;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;
import work.nemonet.littlemaidneo.LittleMaidNeo;
import work.nemonet.littlemaidneo.entity.soul.MaidSoulData;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class WorldMaidSoulState extends SavedData {
    private final Map<UUID, List<MaidSoulData>> maidSoulsMap = Maps.newHashMap();

    public static final Codec<WorldMaidSoulState> CODEC = CompoundTag.CODEC.xmap(
            WorldMaidSoulState::fromNbt,
            WorldMaidSoulState::toNbt
    );

    public static final SavedDataType<WorldMaidSoulState> TYPE = new SavedDataType<>(
            Identifier.fromNamespaceAndPath(LittleMaidNeo.MODID, "maidsouls"),
            WorldMaidSoulState::new,
            CODEC,
            DataFixTypes.LEVEL
    );

    public void add(UUID ownerId, MaidSoulData maidSoul) {
        maidSoulsMap.computeIfAbsent(ownerId, id -> Lists.newArrayList()).add(maidSoul);
    }

    public List<MaidSoulData> get(UUID ownerId) {
        return maidSoulsMap.computeIfAbsent(ownerId, id -> Lists.newArrayList());
    }

    public void remove(UUID ownerId) {
        this.maidSoulsMap.remove(ownerId);
    }

    private CompoundTag toNbt() {
        CompoundTag nbt = new CompoundTag();
        for (Map.Entry<UUID, List<MaidSoulData>> entry : maidSoulsMap.entrySet()) {
            ListTag listTag = new ListTag();
            for (MaidSoulData soul : entry.getValue()) {
                listTag.add(soul.getNbt());
            }
            nbt.put(entry.getKey().toString(), listTag);
        }
        return nbt;
    }

    private static WorldMaidSoulState fromNbt(CompoundTag nbt) {
        WorldMaidSoulState state = new WorldMaidSoulState();
        for (String key : nbt.keySet()) {
            try {
                UUID uuid = UUID.fromString(key);
                Tag tag = nbt.get(key);
                if (tag instanceof ListTag listTag) {
                    List<MaidSoulData> souls = new ArrayList<>();
                    for (Tag t : listTag) {
                        if (t instanceof CompoundTag ct) {
                            souls.add(MaidSoulData.fromNbt(ct));
                        }
                    }
                    state.maidSoulsMap.put(uuid, souls);
                }
            } catch (IllegalArgumentException ignored) {
            }
        }
        return state;
    }

    public static WorldMaidSoulState getWorldMaidSoulState(ServerLevel world) {
        return world.getDataStorage().computeIfAbsent(TYPE);
    }
}
