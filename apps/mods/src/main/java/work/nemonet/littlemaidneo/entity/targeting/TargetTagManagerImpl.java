package work.nemonet.littlemaidneo.entity.targeting;

import com.mojang.serialization.Codec;
import work.nemonet.littlemaidneo.LittleMaidNeo;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.monster.*;
import net.minecraft.world.entity.animal.*;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.animal.chicken.Chicken;
import net.minecraft.world.entity.animal.cow.Cow;
import net.minecraft.world.entity.animal.pig.Pig;
import net.minecraft.world.entity.animal.polarbear.PolarBear;
import net.minecraft.world.entity.animal.rabbit.Rabbit;
import net.minecraft.world.entity.animal.sheep.Sheep;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.entity.monster.EnderMan;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.monster.zombie.ZombifiedPiglin;
import net.minecraft.world.entity.monster.piglin.Piglin;
import net.minecraft.world.entity.monster.warden.Warden;
import net.minecraft.world.entity.npc.Npc;
import net.minecraft.world.item.trading.Merchant;
import net.minecraft.world.level.Level;
import net.minecraft.world.entity.EntityType;
import work.nemonet.littlemaidneo.tags.LMEntityTags;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

public class TargetTagManagerImpl implements TargetTagManager {
    private final Level world;
    private final Map<TargetIdentifier, Set<TargetingSystem.TargetTag>> targetTagMap = new HashMap<>();
    private final Map<TargetIdentifier, Set<TargetingSystem.TargetTag>> defaultTagCache = new HashMap<>();
    private int hash = -1;

    public TargetTagManagerImpl(Level world) {
        this.world = world;
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

        // 2. instanceofフォールバック
        if (this.world != null) {
            try {
                net.minecraft.world.entity.Entity e = type.create(this.world, EntitySpawnReason.MOB_SUMMONED);
                if (e instanceof LivingEntity) {
                    if (!(e instanceof Enemy)
                            || e instanceof Piglin
                            || e instanceof ZombifiedPiglin
                            || e instanceof EnderMan) {
                        tags.add(TargetingSystem.TargetTag.PREEMPTIVE_ATTACK_PROHIBITED);
                    }
                    if (e instanceof Creeper || e instanceof Warden) {
                        tags.add(TargetingSystem.TargetTag.APPROACH_PROHIBITED);
                        tags.add(TargetingSystem.TargetTag.MELEE_WEAPON_PROHIBITED);
                    }
                    if (e instanceof EnderMan) {
                        tags.add(TargetingSystem.TargetTag.RANGED_WEAPON_PROHIBITED);
                    }
                    if (e instanceof TamableAnimal
                            || e instanceof Npc
                            || e instanceof Merchant
                            || e instanceof ArmorStand) {
                        tags.add(TargetingSystem.TargetTag.ATTACK_PROHIBITED);
                        tags.add(TargetingSystem.TargetTag.PREEMPTIVE_ATTACK_PROHIBITED);
                    }
                    if (tags.contains(TargetingSystem.TargetTag.PREEMPTIVE_ATTACK_PROHIBITED)
                            && !(e instanceof Enemy)) {
                        tags.add(TargetingSystem.TargetTag.ATTACK_PROHIBITED);
                    }
                    if (e instanceof Cow
                            || e instanceof Chicken
                            || e instanceof Sheep
                            || e instanceof Pig
                            || e instanceof PolarBear
                            || e instanceof Rabbit
                    ) {
                        tags.remove(TargetingSystem.TargetTag.ATTACK_PROHIBITED);
                    }
                    if (e instanceof Warden) {
                        tags.add(TargetingSystem.TargetTag.ATTACK_PROHIBITED);
                    }
                }
            } catch (Exception ex) {
                // Ignore
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
