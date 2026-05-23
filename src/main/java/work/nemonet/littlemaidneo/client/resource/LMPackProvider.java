package work.nemonet.littlemaidneo.client.resource;

import net.minecraft.network.chat.Component;
import net.minecraft.server.packs.PackLocationInfo;
import net.minecraft.server.packs.PackResources;
import net.minecraft.server.packs.PackSelectionConfig;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.repository.Pack;
import net.minecraft.server.packs.repository.PackSource;
import net.minecraft.server.packs.repository.RepositorySource;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

import java.util.function.Consumer;

@OnlyIn(Dist.CLIENT)
public class LMPackProvider implements RepositorySource {
    public static final PackSource RESOURCE_PACK_SOURCE = PackSource.create(
            packName -> Component.translatable("pack.nameAndSource", packName, Component.translatable("pack.source.littlemaidneo")),
            true);

    @Override
    public void loadPacks(Consumer<Pack> profileAdder) {
        Component title = Component.translatable("pack.name.littlemaidneo");
        PackLocationInfo locationInfo = new PackLocationInfo(
                "littlemaidneo",
                title,
                RESOURCE_PACK_SOURCE,
                null);
        PackSelectionConfig selectionConfig = new PackSelectionConfig(
                true,
                Pack.Position.TOP,
                false);
        Pack.ResourcesSupplier resourcesSupplier = new Pack.ResourcesSupplier() {
            @Override
            public PackResources openPrimary(PackLocationInfo info) {
                return ResourceWrapper.INSTANCE;
            }

            @Override
            public PackResources openFull(PackLocationInfo info, Pack.Metadata metadata) {
                return ResourceWrapper.INSTANCE;
            }
        };
        Pack pack = Pack.readMetaAndCreate(
                locationInfo,
                resourcesSupplier,
                PackType.CLIENT_RESOURCES,
                selectionConfig);
        if (pack != null) {
            profileAdder.accept(pack);
        }
    }
}
