package net.sistr.littlemaidmodelloader.setup;

import dev.architectury.registry.registries.DeferredRegister;
import dev.architectury.registry.registries.RegistrySupplier;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.core.registries.Registries;
import net.sistr.littlemaidmodelloader.client.screen.component.MultiModelGUIUtil;
import net.sistr.littlemaidmodelloader.entity.MultiModelEntity;

import static net.sistr.littlemaidmodelloader.LMMLMod.MODID;

public class Registration {
    public static final DeferredRegister<EntityType<?>> ENTITIES = DeferredRegister.create(MODID, Registries.ENTITY_TYPE);

    public static void init() {
        ENTITIES.register();
    }

    public static final RegistrySupplier<EntityType<MultiModelEntity>> MULTI_MODEL_ENTITY =
            ENTITIES.register("multi_model_entity", () ->
                    EntityType.Builder.<MultiModelEntity>of(MultiModelEntity::new, MobCategory.MISC)
                            .sized(0.5F, 1.35F)
                            .build("multi_model_entity"));
    public static final RegistrySupplier<EntityType<MultiModelGUIUtil.DummyModelEntity>> DUMMY_MODEL_ENTITY =
            ENTITIES.register("dummy_model_entity", () ->
                    EntityType.Builder.<MultiModelGUIUtil.DummyModelEntity>of(MultiModelGUIUtil.DummyModelEntity::new, MobCategory.MISC)
                            .sized(0.5F, 1.35F)
                            .build("dummy_model_entity"));

}
