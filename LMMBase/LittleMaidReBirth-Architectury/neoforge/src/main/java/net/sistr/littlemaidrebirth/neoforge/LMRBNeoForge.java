package net.sistr.littlemaidrebirth.neoforge;

import dev.architectury.platform.hooks.EventBusesHooks;
import me.shedaniel.autoconfig.AutoConfig;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.SpawnPlacementTypes;
import net.minecraft.world.level.levelgen.Heightmap;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.event.entity.RegisterSpawnPlacementsEvent;
import net.neoforged.neoforge.registries.RegisterEvent;
import net.sistr.littlemaidrebirth.LMRBMod;
import net.sistr.littlemaidrebirth.advancement.criterion.LMRBCriteria;
import net.sistr.littlemaidrebirth.client.renderer.MaidModelRenderer;
import net.sistr.littlemaidrebirth.client.renderer.MaidSoulRenderer;
import net.sistr.littlemaidrebirth.config.LMRBConfig;
import net.sistr.littlemaidrebirth.entity.LittleMaidEntity;
import net.sistr.littlemaidrebirth.network.Networking;
import net.sistr.littlemaidrebirth.setup.ClientSetup;
import net.sistr.littlemaidrebirth.setup.ModSetup;
import net.sistr.littlemaidrebirth.setup.Registration;

@Mod(LMRBMod.MODID)
public class LMRBNeoForge {

    public LMRBNeoForge(IEventBus modBus, ModContainer container) {
        EventBusesHooks.whenAvailable(LMRBMod.MODID, bus -> {
        });

        LMRBMod.init();

        container.registerExtensionPoint(IConfigScreenFactory.class,
                (modContainer, parent) -> AutoConfig.getConfigScreen(LMRBConfig.class, parent).get());

        modBus.addListener(this::registerCriteria);
        modBus.addListener(this::modInit);
        modBus.addListener(this::spawnRestrictionInit);
        modBus.addListener(this::clientInit);
        modBus.addListener(this::renderInit);
    }

    public void registerCriteria(RegisterEvent event) {
        event.register(Registries.TRIGGER_TYPE, helper -> {
            LMRBCriteria.init();
        });
    }

    public void modInit(FMLCommonSetupEvent event) {
        ModSetup.init();
        Networking.INSTANCE.init();
    }

    public void spawnRestrictionInit(RegisterSpawnPlacementsEvent event) {
        event.register(Registration.LITTLE_MAID_MOB.get(),
                SpawnPlacementTypes.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                (type, world, spawnReason, pos, random) -> LittleMaidEntity.isValidNaturalSpawn(world, pos),
                RegisterSpawnPlacementsEvent.Operation.OR);
    }

    public void clientInit(FMLClientSetupEvent event) {
        ClientSetup.init();
    }

    // ClientSetupよりこちらの方が実行が早いため、ClientSetupからArchitecturyのメソッド登録しようとすると無視される
    public void renderInit(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(Registration.LITTLE_MAID_MOB.get(), MaidModelRenderer::new);
        event.registerEntityRenderer(Registration.MAID_SOUL_ENTITY.get(), MaidSoulRenderer::new);
    }

}
