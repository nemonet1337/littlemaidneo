package work.nemonet.littlemaidneo;

import net.minecraft.client.renderer.entity.EntityRenderers;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.AddPackFindersEvent;
import net.neoforged.neoforge.client.event.RegisterClientReloadListenersEvent;
import work.nemonet.littlemaidneo.client.renderer.MultiModelRenderer;
import work.nemonet.littlemaidneo.client.resource.LMPackProvider;
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
    }

    private void onClientSetup(FMLClientSetupEvent event) {
        event.enqueueWork(() -> {
            EntityRenderers.register(Registration.MULTI_MODEL_ENTITY.get(), MultiModelRenderer::new);
            EntityRenderers.register(Registration.DUMMY_MODEL_ENTITY.get(), MultiModelRenderer::new);
        });
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
