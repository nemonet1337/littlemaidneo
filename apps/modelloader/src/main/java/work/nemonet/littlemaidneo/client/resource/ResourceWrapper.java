package work.nemonet.littlemaidneo.client.resource;

import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import net.minecraft.SharedConstants;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.PackLocationInfo;
import net.minecraft.server.packs.PackResources;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.metadata.MetadataSectionType;
import net.minecraft.server.packs.metadata.pack.PackMetadataSection;
import net.minecraft.util.InclusiveRange;
import net.minecraft.server.packs.resources.IoSupplier;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
public class ResourceWrapper implements PackResources {
    public static final ResourceWrapper INSTANCE = new ResourceWrapper();
    // pack format を固定値にすると MC 更新のたびに「非互換パック」扱いになり
    // 外部モデルテクスチャが解決できなくなるため、実行中バージョンの format を常に名乗る
    public static final PackMetadataSection PACK_INFO =
            new PackMetadataSection(Component.literal("LittleMaid ModelLoader!!!"),
                    new InclusiveRange<>(SharedConstants.getCurrentVersion().packVersion(PackType.CLIENT_RESOURCES)));
    protected static final HashMap<Identifier, Resource> PATHS = Maps.newHashMap();

    private static final PackLocationInfo LOCATION_INFO = new PackLocationInfo(
            "littlemaidneo",
            Component.literal("LMModelLoader"),
            null,
            java.util.Optional.empty());

    @Nullable
    @Override
    public IoSupplier<InputStream> getRootResource(String... segments) {
        return null;
    }

    @Nullable
    @Override
    public IoSupplier<InputStream> getResource(PackType type, Identifier location) {
        Resource resource = PATHS.get(location);
        if (resource == null) {
            return null;
        }
        return resource::getInputStream;
    }

    @Override
    public void listResources(PackType type, String namespace, String path, ResourceOutput consumer) {
        PATHS.entrySet().stream()
                .filter(entry -> entry.getKey().getNamespace().equals(namespace))
                .filter(entry -> entry.getKey().getPath().startsWith(path))
                .forEach(e -> consumer.accept(e.getKey(), e.getValue()::getInputStream));
    }

    @Override
    public Set<String> getNamespaces(PackType type) {
        return Sets.newHashSet("littlemaidneo");
    }

    @Nullable
    @Override
    public <T> T getMetadataSection(MetadataSectionType<T> metaReader) {
        if (metaReader.name().equals("pack")) {
            return (T) PACK_INFO;
        }
        return null;
    }

    @Override
    public String packId() {
        return "LMModelLoader";
    }

    @Override
    public PackLocationInfo location() {
        return LOCATION_INFO;
    }

    @Override
    public void close() {

    }

    public static void addResourcePath(Identifier resourcePath, String path, Path homePath, boolean isArchive) {
        PATHS.put(resourcePath, new Resource(path, homePath, isArchive));
    }

    private record Resource(String path, Path homePath, boolean isArchive) {

        public InputStream getInputStream() throws IOException {
            if (isArchive) {
                String resourcePath = homePath.toString();
                ZipFile zipfile = new ZipFile(resourcePath);
                ZipEntry zipentry = zipfile.getEntry(path);
                if (zipentry == null) {
                    zipfile.close();
                    throw new NoSuchFileException(path);
                } else {
                    return zipfile.getInputStream(zipentry);
                }
            } else {
                return Files.newInputStream(Paths.get(homePath.toString(), path));
            }
        }
    }
}
