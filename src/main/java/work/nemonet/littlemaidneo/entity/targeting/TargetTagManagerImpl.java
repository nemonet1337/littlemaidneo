package work.nemonet.littlemaidneo.entity.targeting;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.monster.*;
import net.minecraft.world.entity.animal.*;
import net.minecraft.nbt.ByteTag;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.animal.Chicken;
import net.minecraft.world.entity.animal.Cow;
import net.minecraft.world.entity.animal.Pig;
import net.minecraft.world.entity.animal.PolarBear;
import net.minecraft.world.entity.animal.Rabbit;
import net.minecraft.world.entity.animal.Sheep;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.entity.monster.EnderMan;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.monster.ZombifiedPiglin;
import net.minecraft.world.entity.monster.piglin.Piglin;
import net.minecraft.world.entity.monster.warden.Warden;
import net.minecraft.world.entity.npc.Npc;
import net.minecraft.world.item.trading.Merchant;
import net.minecraft.world.level.Level;
import work.nemonet.littlemaidneo.LMRBMod;

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
                    .map(type -> type.create(world))
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
            LMRBMod.LOGGER.info("TargetTagMap Count: {}", TARGET_TAG_MAP.size());
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
    public void writeTargetTags(CompoundTag nbt) {
        if (!this.isInitialized) {
            init();
            this.isInitialized = true;
        }
        write(this.targetTagMap, nbt);
    }

    public static void write(Map<TargetIdentifier, Set<TargetingSystem.TargetTag>> targetTagMap, CompoundTag nbt) {
        var list = new ListTag();
        for (Map.Entry<TargetIdentifier, Set<TargetingSystem.TargetTag>> entry : targetTagMap.entrySet()) {
            var id = entry.getKey();
            var tags = entry.getValue();
            var listEntry = new CompoundTag();
            listEntry.putString("id", id.toString());
            var tagsList = new ListTag();
            for (TargetingSystem.TargetTag tag : tags) {
                tagsList.add(ByteTag.valueOf((byte) tag.ordinal()));
            }
            listEntry.put("tags", tagsList);
            list.add(listEntry);
        }
        nbt.put("targetTagMap", list);
    }

    @Override
    public void readTargetTags(CompoundTag nbt) {
        read(this.targetTagMap, nbt);
        this.hash = this.targetTagMap.hashCode();
    }

    public static void read(Map<TargetIdentifier, Set<TargetingSystem.TargetTag>> targetTagMap, CompoundTag nbt) {
        targetTagMap.clear();
        if (!nbt.contains("targetTagMap")) {
            return;
        }
        var list = nbt.getList("targetTagMap", Tag.TAG_COMPOUND);
        for (int i = 0; i < list.size(); i++) {
            var listEntry = list.getCompound(i);
            var id = TargetIdentifier.tryParse(listEntry.getString("id"));
            if (id.isEmpty()) continue;
            var tagsList = listEntry.getList("tags", Tag.TAG_BYTE);
            var tags = new HashSet<TargetingSystem.TargetTag>();
            for (Tag nbtElement : tagsList) {
                byte ordinal = ((ByteTag) nbtElement).getAsByte();
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
