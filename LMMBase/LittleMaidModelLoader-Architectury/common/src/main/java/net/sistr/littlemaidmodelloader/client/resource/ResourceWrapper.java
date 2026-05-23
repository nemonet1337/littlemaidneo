package net.sistr.littlemaidmodelloader.client.resource;

import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.PackLocationInfo;
import net.minecraft.server.packs.PackResources;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.metadata.MetadataSectionSerializer;
import net.minecraft.server.packs.metadata.pack.PackMetadataSection;
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

// 1.21.1移植: YarnマッピングからMojangマッピングへ変更
// - ResourceLocation → ResourceLocation
// - Text → Component
// - ResourcePack → PackResources
// - ResourceType → PackType
// - InputSupplier → IoSupplier
// - PackResourceMetadata → PackMetadataSection
// - ResourceMetadataReader → MetadataSectionSerializer
// - findResources → listResources
// - isAlwaysStable → 削除
// - openRoot → getRootResource
// - open → getResource
// - getName → packId
// - location()メソッド追加 (1.21.1新規)
//外部から読み込んだリソースをマイクラに送るラッパー
@Environment(EnvType.CLIENT)
public class ResourceWrapper implements PackResources {
    public static final ResourceWrapper INSTANCE = new ResourceWrapper();
    // 1.21.1: PackMetadataSectionのコンストラクタが変更 (Component, int, Optional<FeatureFlagSet>)
    public static final PackMetadataSection PACK_INFO =
            new PackMetadataSection(Component.literal("LittleMaid ModelLoader!!!"), 15, java.util.Optional.empty());
    protected static final HashMap<ResourceLocation, Resource> PATHS = Maps.newHashMap();
    
    // 1.21.1: PackLocationInfoを保持
    private static final PackLocationInfo LOCATION_INFO = new PackLocationInfo(
            "lmmodelloader", 
            Component.literal("LMModelLoader"), 
            null, 
            java.util.Optional.empty());

    @Nullable
    @Override
    public IoSupplier<InputStream> getRootResource(String... segments) {
        return null;
    }

    //引数のResourceLocationはlittlemaidmodelloader:textures/...の形式
    @Nullable
    @Override
    public IoSupplier<InputStream> getResource(PackType type, ResourceLocation location) {
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

    //初期化時に読み込まれる
    @Override
    public Set<String> getNamespaces(PackType type) {
        return Sets.newHashSet("littlemaidmodelloader");
    }

    @Nullable
    @Override
    public <T> T getMetadataSection(MetadataSectionSerializer<T> metaReader) {
        if (metaReader.getMetadataSectionName().equals("pack")) {
            return (T) PACK_INFO;
        }
        return null;
    }

    @Override
    public String packId() {
        return "LMModelLoader";
    }

    // 1.21.1: 新規追加されたlocation()メソッド
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
                //try with resourcesしてはいけない
                //取った結果を返すとき、closeしてしまう
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
