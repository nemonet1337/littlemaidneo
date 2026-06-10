package work.nemonet.littlemaidneo;

import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.ResourceManager;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.AddClientReloadListenersEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;
import net.neoforged.neoforge.event.AddPackFindersEvent;
import work.nemonet.littlemaidneo.client.LMKeys;
import work.nemonet.littlemaidneo.client.renderer.MaidModelRenderer;
import work.nemonet.littlemaidneo.client.renderer.MaidSoulRenderer;
import work.nemonet.littlemaidneo.client.renderer.MultiModelRenderer;
import work.nemonet.littlemaidneo.client.resource.LMPackProvider;
import work.nemonet.littlemaidneo.client.screen.LittleMaidScreen;
import work.nemonet.littlemaidneo.resource.manager.LMTextureManager;
import work.nemonet.littlemaidneo.resource.util.ResourceHelper;
import work.nemonet.littlemaidneo.setup.ModRegistration;

import java.util.Collection;

@Mod(value = LittleMaidNeo.MODID, dist = Dist.CLIENT)
public class LittleMaidNeoClient {

    public LittleMaidNeoClient(IEventBus modEventBus, ModContainer container) {
        // modelloader 側の MultiModelEntity は画面クラスへ直接依存できないため、
        // モデル選択画面のオープン処理をここで注入する
        work.nemonet.littlemaidneo.entity.MultiModelEntity.setModelSelectScreenOpener(
                work.nemonet.littlemaidneo.client.util.ClientScreenHelper::openModelSelectScreen);

        modEventBus.addListener(this::onClientSetup);
        modEventBus.addListener(this::onAddPackFinders);
        modEventBus.addListener(this::onRegisterReloadListeners);
        modEventBus.addListener(this::onRegisterRenderers);
        modEventBus.addListener(this::onRegisterKeyMappings);
        modEventBus.addListener(this::onRegisterMenuScreens);
    }

    private void onClientSetup(FMLClientSetupEvent event) {
        event.enqueueWork(LMKeys::init);
    }

    private void onRegisterMenuScreens(RegisterMenuScreensEvent event) {
        event.register(ModRegistration.LITTLE_MAID_SCREEN_HANDLER.get(), LittleMaidScreen::new);
    }

    private void onRegisterRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(ModRegistration.MULTI_MODEL_ENTITY.get(), MultiModelRenderer::new);
        event.registerEntityRenderer(ModRegistration.DUMMY_MODEL_ENTITY.get(), MultiModelRenderer::new);
        event.registerEntityRenderer(ModRegistration.LITTLE_MAID_ENTITY.get(), MaidModelRenderer::new);
        event.registerEntityRenderer(ModRegistration.MAID_SOUL_ENTITY.get(), MaidSoulRenderer::new);
    }

    private void onRegisterKeyMappings(RegisterKeyMappingsEvent event) {
        LMKeys.onRegisterKeyMappings(event);
    }

    private void onAddPackFinders(AddPackFindersEvent event) {
        event.addRepositorySource(new LMPackProvider());
    }

    private void onRegisterReloadListeners(AddClientReloadListenersEvent event) {
        event.addListener(Identifier.fromNamespaceAndPath(LittleMaidNeo.MODID, "texture_loader"),
                (net.minecraft.server.packs.resources.ResourceManagerReloadListener) resourceManager -> {
            Collection<Identifier> resourceLocations = resourceManager
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
