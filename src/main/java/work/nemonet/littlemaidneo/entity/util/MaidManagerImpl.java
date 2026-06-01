package work.nemonet.littlemaidneo.entity.util;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import work.nemonet.littlemaidneo.entity.LittleMaidEntity;
import work.nemonet.littlemaidneo.entity.MaidSoulEntity;

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
                    if (!entity.isAlive() || !(entity.level() instanceof net.minecraft.server.level.ServerLevel serverLevel) || serverLevel.getServer().getLevel(entity.level().dimension()) == null) {
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
