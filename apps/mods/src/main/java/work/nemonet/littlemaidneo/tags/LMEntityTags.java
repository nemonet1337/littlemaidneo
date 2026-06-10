package work.nemonet.littlemaidneo.tags;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EntityType;
import work.nemonet.littlemaidneo.LittleMaidNeo;

public final class LMEntityTags {
    public static final TagKey<EntityType<?>> ATTACK_PROHIBITED        = register("attack_prohibited");
    public static final TagKey<EntityType<?>> APPROACH_PROHIBITED      = register("approach_prohibited");
    public static final TagKey<EntityType<?>> PREEMPTIVE_ATTACK_PROHIBITED = register("preemptive_attack_prohibited");
    public static final TagKey<EntityType<?>> RANGED_WEAPON_PROHIBITED = register("ranged_weapon_prohibited");
    public static final TagKey<EntityType<?>> MELEE_WEAPON_PROHIBITED  = register("melee_weapon_prohibited");

    private static TagKey<EntityType<?>> register(String id) {
        return TagKey.create(Registries.ENTITY_TYPE, Identifier.fromNamespaceAndPath(LittleMaidNeo.MODID, id));
    }
}
