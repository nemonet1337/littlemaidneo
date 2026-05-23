package work.nemonet.littlemaidneo.setup;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import work.nemonet.littlemaidneo.LittleMaidNeo;
import work.nemonet.littlemaidneo.client.screen.component.MultiModelGUIUtil;
import work.nemonet.littlemaidneo.entity.MultiModelEntity;

public class Registration {
    public static final DeferredRegister<EntityType<?>> ENTITIES =
            DeferredRegister.create(Registries.ENTITY_TYPE, LittleMaidNeo.MODID);

    public static final DeferredHolder<EntityType<?>, EntityType<MultiModelEntity>> MULTI_MODEL_ENTITY =
            ENTITIES.register("multi_model_entity", () ->
                    EntityType.Builder.<MultiModelEntity>of(MultiModelEntity::new, MobCategory.MISC)
                            .sized(0.5F, 1.35F)
                            .build(LittleMaidNeo.MODID + ":multi_model_entity"));

    public static final DeferredHolder<EntityType<?>, EntityType<MultiModelGUIUtil.DummyModelEntity>> DUMMY_MODEL_ENTITY =
            ENTITIES.register("dummy_model_entity", () ->
                    EntityType.Builder.<MultiModelGUIUtil.DummyModelEntity>of(MultiModelGUIUtil.DummyModelEntity::new, MobCategory.MISC)
                            .sized(0.5F, 1.35F)
                            .build(LittleMaidNeo.MODID + ":dummy_model_entity"));
}
