package work.nemonet.littlemaidneo.entity.targeting;

import com.mojang.serialization.Codec;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.level.Level;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityTypes;
import work.nemonet.littlemaidneo.tags.LMEntityTags;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class TargetTagManagerImpl implements TargetTagManager {
    private final Map<TargetIdentifier, Set<TargetingSystem.TargetTag>> targetTagMap = new HashMap<>();
    private final Map<TargetIdentifier, Set<TargetingSystem.TargetTag>> defaultTagCache = new HashMap<>();
    private int hash = -1;

    public TargetTagManagerImpl(Level world) {
    }

    @Override
    public Set<TargetingSystem.TargetTag> getTargetTag(TargetIdentifier id) {
        if (this.targetTagMap.containsKey(id)) {
            return this.targetTagMap.get(id);
        }
        if (this.defaultTagCache.containsKey(id)) {
            return this.defaultTagCache.get(id);
        }

        EntityType<?> type = id.getEntityType();
        Set<TargetingSystem.TargetTag> tags = new HashSet<>();

        // 1. Tag判定
        var holder = BuiltInRegistries.ENTITY_TYPE.wrapAsHolder(type);
        if (holder.is(LMEntityTags.ATTACK_PROHIBITED)) {
            tags.add(TargetingSystem.TargetTag.ATTACK_PROHIBITED);
        }
        if (holder.is(LMEntityTags.APPROACH_PROHIBITED)) {
            tags.add(TargetingSystem.TargetTag.APPROACH_PROHIBITED);
        }
        if (holder.is(LMEntityTags.PREEMPTIVE_ATTACK_PROHIBITED)) {
            tags.add(TargetingSystem.TargetTag.PREEMPTIVE_ATTACK_PROHIBITED);
        }
        if (holder.is(LMEntityTags.RANGED_WEAPON_PROHIBITED)) {
            tags.add(TargetingSystem.TargetTag.RANGED_WEAPON_PROHIBITED);
        }
        if (holder.is(LMEntityTags.MELEE_WEAPON_PROHIBITED)) {
            tags.add(TargetingSystem.TargetTag.MELEE_WEAPON_PROHIBITED);
        }

        if (!tags.isEmpty()) {
            var immutableTags = Set.copyOf(tags);
            this.defaultTagCache.put(id, immutableTags);
            return immutableTags;
        }

        // 2. 安全なEntityTypeおよびMobCategoryによる判定
        MobCategory category = type.getCategory();
        if (category == MobCategory.MISC) {
            tags.add(TargetingSystem.TargetTag.ATTACK_PROHIBITED);
            tags.add(TargetingSystem.TargetTag.PREEMPTIVE_ATTACK_PROHIBITED);
        } else {
            boolean isMonster = category == MobCategory.MONSTER;

            // 26.2: エンティティ定数は EntityType → EntityTypes に分離
            if (type == EntityTypes.CREEPER || type == EntityTypes.WARDEN) {
                tags.add(TargetingSystem.TargetTag.APPROACH_PROHIBITED);
                tags.add(TargetingSystem.TargetTag.MELEE_WEAPON_PROHIBITED);
            }
            if (type == EntityTypes.ENDERMAN) {
                tags.add(TargetingSystem.TargetTag.RANGED_WEAPON_PROHIBITED);
            }
            
            // 先制攻撃禁止の判定
            if (!isMonster || type == EntityTypes.PIGLIN || type == EntityTypes.ZOMBIFIED_PIGLIN || type == EntityTypes.ENDERMAN) {
                tags.add(TargetingSystem.TargetTag.PREEMPTIVE_ATTACK_PROHIBITED);
            }

            // 攻撃禁止の判定
            if (type == EntityTypes.VILLAGER || type == EntityTypes.WANDERING_TRADER || type == EntityTypes.ARMOR_STAND
                    || type == EntityTypes.IRON_GOLEM || type == EntityTypes.SNOW_GOLEM || type == EntityTypes.ALLAY
                    || type == EntityTypes.BAT || type == EntityTypes.CAT || type == EntityTypes.WOLF
                    || type == EntityTypes.PARROT || type == EntityTypes.OCELOT || type == EntityTypes.FOX
                    || type == EntityTypes.PANDA || type == EntityTypes.BEE || type == EntityTypes.STRIDER
                    || type == EntityTypes.DOLPHIN || type == EntityTypes.AXOLOTL) {
                tags.add(TargetingSystem.TargetTag.ATTACK_PROHIBITED);
            }

            if (type == EntityTypes.WARDEN) {
                tags.add(TargetingSystem.TargetTag.ATTACK_PROHIBITED);
            }
        }

        if (tags.isEmpty()) {
            tags = Set.of();
        }

        var immutableTags = Set.copyOf(tags);
        this.defaultTagCache.put(id, immutableTags);
        return immutableTags;
    }

    @Override
    public void writeTargetTags(ValueOutput output) {
        write(this.targetTagMap, output);
    }

    public static void write(Map<TargetIdentifier, Set<TargetingSystem.TargetTag>> targetTagMap, ValueOutput output) {
        var list = output.childrenList("targetTagMap");
        for (Map.Entry<TargetIdentifier, Set<TargetingSystem.TargetTag>> entry : targetTagMap.entrySet()) {
            var id = entry.getKey();
            var tags = entry.getValue();
            var entryOutput = list.addChild();
            entryOutput.putString("id", id.toString());
            var tagsList = entryOutput.list("tags", Codec.BYTE);
            for (TargetingSystem.TargetTag tag : tags) {
                tagsList.add((byte) tag.ordinal());
            }
        }
    }

    @Override
    public void readTargetTags(ValueInput input) {
        read(this.targetTagMap, input);
        this.hash = this.targetTagMap.hashCode();
    }

    public static void read(Map<TargetIdentifier, Set<TargetingSystem.TargetTag>> targetTagMap, ValueInput input) {
        targetTagMap.clear();
        for (var entryInput : input.childrenListOrEmpty("targetTagMap")) {
            var id = entryInput.getString("id").flatMap(TargetIdentifier::tryParse);
            if (id.isEmpty()) continue;
            var tags = new HashSet<TargetingSystem.TargetTag>();
            for (byte ordinal : entryInput.listOrEmpty("tags", Codec.BYTE)) {
                if (ordinal >= 0 && ordinal < TargetingSystem.TargetTag.values().length) {
                    tags.add(TargetingSystem.TargetTag.values()[ordinal]);
                }
            }
            targetTagMap.put(id.get(), tags);
        }
    }

    @Override
    public Sync getTargetTagsSync() {
        return new Sync() {
            @Override
            public int hash() {
                return TargetTagManagerImpl.this.hash;
            }

            @Override
            public Map<TargetIdentifier, Set<TargetingSystem.TargetTag>> getData() {
                return Map.copyOf(TargetTagManagerImpl.this.targetTagMap);
            }

            @Override
            public void syncFrom(Sync source) {
                TargetTagManagerImpl.this.targetTagMap.clear();
                TargetTagManagerImpl.this.targetTagMap.putAll(source.getData());
                TargetTagManagerImpl.this.hash = source.hash();
            }
        };
    }
}
