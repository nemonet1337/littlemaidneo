package net.sistr.littlemaidrebirth.world;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;
import net.sistr.littlemaidrebirth.LMRBMod;
import net.sistr.littlemaidrebirth.entity.LittleMaidEntity;

import java.util.List;
import java.util.Map;
import java.util.UUID;

public class WorldMaidSoulState extends SavedData {
    private final Map<UUID, List<LittleMaidEntity.MaidSoul>> maidSoulsMap = Maps.newHashMap();

    public void add(UUID ownerId, LittleMaidEntity.MaidSoul maidSoul) {
        maidSoulsMap.computeIfAbsent(ownerId, (id) -> Lists.newArrayList())
                .add(maidSoul);
    }

    public List<LittleMaidEntity.MaidSoul> get(UUID ownerId) {
        return maidSoulsMap.computeIfAbsent(ownerId, id -> Lists.newArrayList());
    }

    public void remove(UUID ownerId) {
        this.maidSoulsMap.remove(ownerId);
    }

    @Override
    public CompoundTag save(CompoundTag nbt, HolderLookup.Provider registries) {
        var nbtEntries = new ListTag();
        for (Map.Entry<UUID, List<LittleMaidEntity.MaidSoul>> entry : maidSoulsMap.entrySet()) {
            var uuid = entry.getKey();
            var list = entry.getValue();
            var nbtEntry = new CompoundTag();
            nbtEntry.putUUID("id", uuid);
            var nbtMaidSouls = new ListTag();
            for (LittleMaidEntity.MaidSoul maidSoul : list) {
                nbtMaidSouls.add(maidSoul.getNbt());
            }
            nbtEntry.put("maidSouls", nbtMaidSouls);
            nbtEntries.add(nbtEntry);
        }
        nbt.put("maidSoulsEntries", nbtEntries);
        return nbt;
    }

    public static WorldMaidSoulState createFromNbt(CompoundTag nbt) {
        WorldMaidSoulState worldMaidSoulState = new WorldMaidSoulState();
        var nbtEntries = nbt.getList("maidSoulsEntries", Tag.TAG_COMPOUND);
        for (Tag nbtEntry : nbtEntries) {
            var id = ((CompoundTag) nbtEntry).getUUID("id");
            var nbtMaidSouls = ((CompoundTag) nbtEntry).getList("maidSouls", Tag.TAG_COMPOUND);
            List<LittleMaidEntity.MaidSoul> maidSouls = Lists.newArrayList();
            for (Tag nbtMaidSoul : nbtMaidSouls) {
                maidSouls.add(LittleMaidEntity.MaidSoul.fromNbt((CompoundTag) nbtMaidSoul));
            }
            worldMaidSoulState.maidSoulsMap.put(id, maidSouls);
        }
        return worldMaidSoulState;
    }

    public static WorldMaidSoulState getWorldMaidSoulState(ServerLevel world) {
        var persistentStateManager = world.getDataStorage();

        return persistentStateManager.computeIfAbsent(
                new SavedData.Factory<WorldMaidSoulState>(WorldMaidSoulState::new,
                        (nbt, provider) -> WorldMaidSoulState.createFromNbt(nbt),
                        null),
                LMRBMod.MODID + "_maidsouls");
    }

}
