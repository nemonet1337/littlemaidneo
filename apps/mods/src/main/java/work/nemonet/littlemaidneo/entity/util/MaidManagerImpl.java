package work.nemonet.littlemaidneo.entity.util;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import work.nemonet.littlemaidneo.entity.LittleMaidEntity;
import work.nemonet.littlemaidneo.entity.MaidSoulEntity;
import work.nemonet.littlemaidneo.entity.soul.MaidSoulData;

import java.util.*;

public class MaidManagerImpl implements MaidManager {
    private final Map<UUID, LMInfo> maidMap = new HashMap<>();

    @Override
    public void registerMaid(LittleMaidEntity maid) {
        putKeepingGroup(maid.getUUID(), MaidLMInfo.create(maid, true));
    }

    @Override
    public void registerMaid(MaidSoulEntity soul) {
        putKeepingGroup(soul.getSoul().getUuid(), SoulEntityLMInfo.create(soul, true));
    }

    @Override
    public void registerMaid(MaidSoulData soul) {
        putKeepingGroup(soul.getUuid(), SoulLMInfo.create(soul));
    }

    @Override
    public boolean setGroup(UUID id, String group) {
        LMInfo info = maidMap.get(id);
        if (info == null) {
            return false;
        }
        info.setGroup(group);
        return true;
    }

    @Override
    public List<LMInfo> getMaidList() {
        return List.copyOf(maidMap.values());
    }

    @Override
    public void writeMaidManager(ValueOutput output) {
        write(output, getMaidList());
    }

    @Override
    public void readMaidManager(ValueInput input) {
        this.maidMap.clear();
        List<LMInfo> infos = new ArrayList<>();
        read(input, infos);
        for (LMInfo info : infos) {
            maidMap.put(info.id(), info);
        }
    }

    @Override
    public List<MaidSoulData> getMaidSouls() {
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

        for (LMInfo lmInfo : this.maidMap.values()) {
            if (lmInfo.status() == Status.ALIVE && lmInfo instanceof MaidLMInfo maidInfo) {
                Optional<Entity> entity = maidInfo.getEntity();
                if (entity.isEmpty()) {
                    // 参照喪失・セッション跨ぎの stale エントリ
                    updates.put(maidInfo.id(), keepGroup(maidInfo, MaidLMInfo.unloaded(maidInfo)));
                    continue;
                }
                Entity e = entity.get();
                if (!isEntityInLiveWorld(e)) {
                    updates.put(maidInfo.id(), keepGroup(maidInfo, MaidLMInfo.create((LittleMaidEntity) e, false)));
                }
            } else if (lmInfo.status() == Status.SOUL_ENTITY && lmInfo instanceof SoulEntityLMInfo soulInfo) {
                Optional<Entity> entity = soulInfo.getEntity();
                if (entity.isEmpty()) {
                    updates.put(soulInfo.id(), keepGroup(soulInfo, SoulEntityLMInfo.unloaded(soulInfo)));
                    continue;
                }
                Entity e = entity.get();
                if (!isEntityInLiveWorld(e)) {
                    updates.put(soulInfo.id(), keepGroup(soulInfo, SoulEntityLMInfo.create((MaidSoulEntity) e, false)));
                }
            }
        }

        this.maidMap.putAll(updates);
    }

    private void putKeepingGroup(UUID id, LMInfo next) {
        maidMap.put(id, keepGroup(maidMap.get(id), next));
    }

    private static <T extends LMInfo> T keepGroup(LMInfo previous, T next) {
        if (previous != null) {
            next.setGroup(previous.group());
        }
        return next;
    }

    private static boolean isEntityInLiveWorld(Entity entity) {
        if (!entity.isAlive()) {
            return false;
        }
        if (!(entity.level() instanceof net.minecraft.server.level.ServerLevel serverLevel)) {
            return false;
        }
        return serverLevel.getServer().getLevel(entity.level().dimension()) != null;
    }

    public static void write(ValueOutput output, List<MaidManager.LMInfo> lmInfos) {
        var list = output.childrenList("maidList");
        for (MaidManager.LMInfo info : lmInfos) {
            info.write(list.addChild());
        }
    }

    public static void read(ValueInput input, List<MaidManager.LMInfo> lmInfos) {
        for (var entry : input.childrenListOrEmpty("maidList")) {
            MaidManager.LMInfo info = MaidManager.LMInfo.read(entry);
            if (info != null) {
                lmInfos.add(info);
            }
        }
    }
}
