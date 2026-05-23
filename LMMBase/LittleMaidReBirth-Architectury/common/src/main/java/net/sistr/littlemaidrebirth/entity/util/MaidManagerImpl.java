package net.sistr.littlemaidrebirth.entity.util;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.entity.Entity;
import net.sistr.littlemaidrebirth.entity.LittleMaidEntity;
import net.sistr.littlemaidrebirth.entity.MaidSoulEntity;

import java.util.*;

public class MaidManagerImpl implements MaidManager {
    private final Map<UUID, LMInfo> maidMap = new HashMap<>();

    @Override
    public void registerMaid(LittleMaidEntity maid) {
        maidMap.put(maid.getUUID(), MaidLMInfo.create(maid, true));
    }

    @Override
    public void registerMaid(MaidSoulEntity soul) {
        maidMap.put(soul.getSoul().getUuid(), SoulEntityLMInfo.create(soul, true));
    }

    @Override
    public void registerMaid(LittleMaidEntity.MaidSoul soul) {
        maidMap.put(soul.getUuid(), SoulLMInfo.create(soul));
    }

    @Override
    public List<LMInfo> getMaidList() {
        return List.copyOf(maidMap.values());
    }

    @Override
    public void writeMaidManager(CompoundTag nbt) {
        write(nbt, this.maidMap.values().stream().toList());
    }

    @Override
    public void readMaidManager(CompoundTag nbt) {
        this.maidMap.clear();
        var list = new ArrayList<LMInfo>();
        read(nbt, list);
        list.forEach(lminfo -> maidMap.put(lminfo.id(), lminfo));
    }

    public static void write(CompoundTag nbt, List<LMInfo> list) {
        var listNbt = new ListTag();
        for (LMInfo info : list) {
            CompoundTag infoNbt = new CompoundTag();
            info.write(infoNbt);
            listNbt.add(infoNbt);
        }
        nbt.put("maidList", listNbt);
    }

    public static void read(CompoundTag nbt, List<LMInfo> list) {
        var listNbt = nbt.getList("maidList", Tag.TAG_COMPOUND);
        for (var element : listNbt) {
            CompoundTag infoNbt = (CompoundTag) element;
            LMInfo info = LMInfo.read(infoNbt);
            list.add(info);
        }
    }

    @Override
    public List<LittleMaidEntity.MaidSoul> getMaidSouls() {
        return this.maidMap.values().stream()
                .filter(lmInfo -> lmInfo.status() == Status.SOUL_WITHIN)
                .map(lmInfo -> ((SoulLMInfo) lmInfo).soul())
                .toList();
    }

    @Override
    public void clearMaidSouls() {
        this.maidMap.values().removeIf(lmInfo -> lmInfo.status() == Status.SOUL_WITHIN);
    }

    @Override
    public void checkMaidUnload() {
        Map<UUID, LMInfo> updates = new HashMap<>();
        
        this.maidMap.values().stream()
                .filter(lmInfo -> lmInfo.status() == Status.ALIVE || lmInfo.status() == Status.SOUL_ENTITY)
                .map(info -> info.getEntity())
                .filter(Optional::isPresent)
                .forEach(o -> {
                    var entity = o.get();
                    // エンティティが死亡 or ワールドが読み込まれていない
                    if (!entity.isAlive() || entity.getServer().getLevel(entity.level().dimension()) == null) {
                        if (entity instanceof LittleMaidEntity maid) {
                            updates.put(maid.getUUID(), MaidLMInfo.create(maid, false));
                        } else if (entity instanceof MaidSoulEntity soul) {
                            updates.put(soul.getUUID(), SoulEntityLMInfo.create(soul, false));
                        }
                    }
                });
        
        // ストリーム処理が完了してから一括で更新
        this.maidMap.putAll(updates);
    }
}
