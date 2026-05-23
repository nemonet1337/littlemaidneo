package work.nemonet.littlemaidneo;

import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.AddPackFindersEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterClientReloadListenersEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import work.nemonet.littlemaidneo.client.key.LMKeys;
import work.nemonet.littlemaidneo.client.renderer.MaidModelRenderer;
import work.nemonet.littlemaidneo.client.renderer.MaidSoulRenderer;
import work.nemonet.littlemaidneo.client.renderer.MultiModelRenderer;
import work.nemonet.littlemaidneo.client.resource.LMPackProvider;
import work.nemonet.littlemaidneo.client.screen.LittleMaidScreen;
import work.nemonet.littlemaidneo.resource.manager.LMTextureManager;
import work.nemonet.littlemaidneo.resource.util.ResourceHelper;
import work.nemonet.littlemaidneo.setup.Registration;

import java.util.Collection;

@Mod(value = LittleMaidNeo.MODID, dist = Dist.CLIENT)
public class LittleMaidNeoClient {

    public LittleMaidNeoClient(IEventBus modEventBus, ModContainer container) {
        modEventBus.addListener(this::onClientSetup);
        modEventBus.addListener(this::onAddPackFinders);
        modEventBus.addListener(this::onRegisterReloadListeners);
        modEventBus.addListener(this::onRegisterRenderers);
        modEventBus.addListener(this::onRegisterKeyMappings);
    }

    private void onClientSetup(FMLClientSetupEvent event) {
        event.enqueueWork(() -> {
            MenuScreens.register(Registration.LITTLE_MAID_SCREEN_HANDLER.get(), LittleMaidScreen::new);
            LMKeys.init();
        });
    }

    private void onRegisterRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(Registration.MULTI_MODEL_ENTITY.get(), MultiModelRenderer::new);
        event.registerEntityRenderer(Registration.DUMMY_MODEL_ENTITY.get(), MultiModelRenderer::new);
        event.registerEntityRenderer(Registration.LITTLE_MAID_ENTITY.get(), MaidModelRenderer::new);
        event.registerEntityRenderer(Registration.MAID_SOUL_ENTITY.get(), MaidSoulRenderer::new);
    }

    private void onRegisterKeyMappings(RegisterKeyMappingsEvent event) {
        LMKeys.onRegisterKeyMappings(event);
    }

    private void onAddPackFinders(AddPackFindersEvent event) {
        event.addRepositorySource(new LMPackProvider());
    }

    private void onRegisterReloadListeners(RegisterClientReloadListenersEvent event) {
        event.registerReloadListener((ResourceManager resourceManager) -> {
            Collection<ResourceLocation> resourceLocations = resourceManager
                    .listResources("textures/entity/littlemaid", s -> true)
                    .keySet();
            resourceLocations.forEach(resourcePath -> {
                String path = resourcePath.getPath();
                ResourceHelper.getTexturePackName(path, false).ifPresent(textureName -> {
                    String modelName = ResourceHelper.getModelName(textureName);
                    int index = ResourceHelper.getIndex(path);
                    if (index != -1) {
                        LMTextureManager.INSTANCE.addTexture(
                                ResourceHelper.getFileName(path, false),
                                textureName, modelName, index, resourcePath);
                    }
                });
            });
        });
    }
}
