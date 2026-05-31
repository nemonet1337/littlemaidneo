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

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

public class TargetTagManagerImpl implements TargetTagManager {
    private static final Map<TargetIdentifier, Set<TargetingSystem.TargetTag>> TARGET_TAG_MAP = new HashMap<>();
    private static boolean staticInitialized;
    private final Level world;
    private final Map<TargetIdentifier, Set<TargetingSystem.TargetTag>> targetTagMap = new HashMap<>();
    private boolean isInitialized;
    private int hash = -1;

    public TargetTagManagerImpl(Level world) {
        this.world = world;
    }

    // コンストラクタで実行するとスタックオーバーフローになるので分離
    private void init() {
        if (!staticInitialized) {
            BuiltInRegistries.ENTITY_TYPE.stream()
                    .filter(type -> type.canSummon() && type.canSerialize())
                    .map(type -> (net.minecraft.world.entity.Entity) type.create(world, EntitySpawnReason.MOB_SUMMONED))
                    .filter(e -> e != null)
                    .forEach(e -> {
                        if (!(e instanceof LivingEntity)) {
                            return;
                        }
                        Set<TargetingSystem.TargetTag> set = new HashSet<>();
                        // 非モンスター系と一部中立モブは先制攻撃禁止
                        if (!(e instanceof Enemy)
                                || e instanceof Piglin
                                || e instanceof ZombifiedPiglin
                                || e instanceof EnderMan) {
                            set.add(TargetingSystem.TargetTag.PREEMPTIVE_ATTACK_PROHIBITED);
                        }
                        // クリーパー, ウォーデンは接近禁止
                        if (e instanceof Creeper || e instanceof Warden) {
                            set.add(TargetingSystem.TargetTag.APPROACH_PROHIBITED);
                            set.add(TargetingSystem.TargetTag.MELEE_WEAPON_PROHIBITED);
                        }
                        // エンダーマンは遠距離攻撃禁止
                        if (e instanceof EnderMan) {
                            set.add(TargetingSystem.TargetTag.RANGED_WEAPON_PROHIBITED);
                        }
                        // ペット系、NPC系は攻撃禁止
                        if (e instanceof TamableAnimal
                                || e instanceof Npc
                                || e instanceof Merchant
                                || e instanceof ArmorStand) {
                            set.add(TargetingSystem.TargetTag.ATTACK_PROHIBITED);
                            set.add(TargetingSystem.TargetTag.PREEMPTIVE_ATTACK_PROHIBITED);
                        }
                        // 先制攻撃禁止かつ非モンスターなら攻撃禁止
                        if (set.contains(TargetingSystem.TargetTag.PREEMPTIVE_ATTACK_PROHIBITED)
                                && !(e instanceof Enemy)) {
                            set.add(TargetingSystem.TargetTag.ATTACK_PROHIBITED);
                        }
                        // 家畜系は攻撃可
                        if (e instanceof Cow
                                || e instanceof Chicken
                                || e instanceof Sheep
                                || e instanceof Pig
                                || e instanceof PolarBear
                                || e instanceof Rabbit
                        ) {
                            set.remove(TargetingSystem.TargetTag.ATTACK_PROHIBITED);
                        }
                        // ウォーデンは攻撃禁止
                        if (e instanceof Warden) {
                            set.add(TargetingSystem.TargetTag.ATTACK_PROHIBITED);
                        }
                        TARGET_TAG_MAP.put(new TargetIdentifier(e), set);
                    });
            staticInitialized = true;
            LittleMaidNeo.LOGGER.info("TargetTagMap Count: {}", TARGET_TAG_MAP.size());
        }
        var tmp = new HashMap<>(TARGET_TAG_MAP);
        tmp.putAll(targetTagMap);
        this.targetTagMap.putAll(tmp);
        this.hash = this.targetTagMap.hashCode();
    }

    @Override
    public Set<TargetingSystem.TargetTag> getTargetTag(TargetIdentifier id) {
        if (!this.isInitialized) {
            init();
            this.isInitialized = true;
        }
        if (!this.targetTagMap.containsKey(id)) {
            return Set.of(TargetingSystem.TargetTag.PREEMPTIVE_ATTACK_PROHIBITED);
        }
        return this.targetTagMap.get(id);
    }

    @Override
    public void writeTargetTags(ValueOutput output) {
        if (!this.isInitialized) {
            init();
            this.isInitialized = true;
        }
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
