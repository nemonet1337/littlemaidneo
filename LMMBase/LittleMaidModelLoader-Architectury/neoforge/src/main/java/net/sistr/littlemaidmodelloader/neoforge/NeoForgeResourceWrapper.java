package net.sistr.littlemaidmodelloader.neoforge;

import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.PackLocationInfo;
import net.minecraft.server.packs.PackResources;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.metadata.MetadataSectionSerializer;
import net.minecraft.server.packs.metadata.pack.PackMetadataSection;
import net.minecraft.server.packs.resources.IoSupplier;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Optional;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public class NeoForgeResourceWrapper implements PackResources {
    public static final NeoForgeResourceWrapper INSTANCE = new NeoForgeResourceWrapper();
    public static final PackMetadataSection PACK_INFO =
            new PackMetadataSection(Component.literal("LittleMaid ModelLoader!!!"), 15, Optional.empty());
    protected static final HashMap<ResourceLocation, Resource> PATHS = Maps.newHashMap();

    private static final PackLocationInfo LOCATION_INFO = new PackLocationInfo(
            "lmmodelloader",
            Component.literal("LMModelLoader"),
            null,
            Optional.empty());

    @Nullable
    @Override
    public IoSupplier<InputStream> getRootResource(@NotNull String... segments) {
        return null;
    }

    @Nullable
    @Override
    public IoSupplier<InputStream> getResource(@NotNull PackType type, @NotNull ResourceLocation location) {
        Resource resource = PATHS.get(location);
        if (resource == null) {
            return null;
        }
        return resource::getInputStream;
    }

    @Override
    public void listResources(@NotNull PackType type, @NotNull String namespace, @NotNull String path, @NotNull ResourceOutput output) {
        PATHS.entrySet().stream()
                .filter(entry -> entry.getKey().getNamespace().equals(namespace))
                .filter(entry -> entry.getKey().getPath().startsWith(path))
                .forEach(e -> output.accept(e.getKey(), e.getValue()::getInputStream));
    }

    @NotNull
    @Override
    public Set<String> getNamespaces(@NotNull PackType type) {
        return Sets.newHashSet("littlemaidmodelloader");
    }

    @Nullable
    @Override
    public <T> T getMetadataSection(@NotNull MetadataSectionSerializer<T> metaReader) throws IOException {
        if (metaReader.getMetadataSectionName().equals("pack")) {
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

    public static void addResourcePath(ResourceLocation resourcePath, String path, Path homePath, boolean isArchive) {
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
