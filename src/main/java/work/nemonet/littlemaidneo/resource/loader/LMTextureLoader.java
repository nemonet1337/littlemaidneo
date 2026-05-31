package work.nemonet.littlemaidneo.resource.loader;

import net.minecraft.resources.Identifier;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.loading.FMLEnvironment;
import work.nemonet.littlemaidneo.LittleMaidNeo;
import work.nemonet.littlemaidneo.client.resource.ResourceWrapper;
import work.nemonet.littlemaidneo.resource.manager.LMTextureManager;
import work.nemonet.littlemaidneo.resource.util.ResourceHelper;

import java.io.InputStream;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class LMTextureLoader implements LMLoader {
    private final LMTextureManager textureManager;
    private final HashMap<String, String> converter = new HashMap<>();

    public LMTextureLoader(LMTextureManager textureManager) {
        this.textureManager = textureManager;
    }

    public void addPathConverter(String target, String to) {
        converter.put(target, to);
    }

    @Override
    public boolean canLoad(String path, Path folderPath, InputStream inputStream, boolean isArchive) {
        return FMLEnvironment.getDist() == Dist.CLIENT && path.endsWith(".png")
                && ResourceHelper.getParentFolderName(path, isArchive).isPresent()
                && ResourceHelper.getIndex(path) != -1;
    }

    @Override
    public void load(String path, Path folderPath, InputStream inputStream, boolean isArchive) {
        Identifier texturePath = getResourceLocation(path, isArchive)
                .orElseThrow(() -> new IllegalArgumentException("引数が不正です。"));
        String textureName = ResourceHelper.getTexturePackName(path, isArchive)
                .orElseThrow(() -> new IllegalArgumentException("引数が不正です。"));
        String modelName = ResourceHelper.getModelName(textureName);
        textureManager.addTexture(ResourceHelper.getFileName(path, isArchive), textureName, modelName,
                ResourceHelper.getIndex(path), texturePath);
        if (FMLEnvironment.getDist() == Dist.CLIENT) {
            ResourceWrapper.addResourcePath(texturePath, path, folderPath, isArchive);
        }
    }

    private Optional<Identifier> getResourceLocation(String path, boolean isArchive) {
        String texturePath = path.toLowerCase();
        if (!isArchive) texturePath = texturePath.replace("\\", "/");
        for (Map.Entry<String, String> entry : converter.entrySet()) {
            texturePath = texturePath.replace(entry.getKey(), entry.getValue());
        }
        int firstSplitter = texturePath.indexOf("/");
        if (firstSplitter == -1) return Optional.empty();
        texturePath = texturePath.replaceAll("[^a-z0-9/._\\-]", "-");
        String namePath = texturePath.substring(firstSplitter + 1);
        return Optional.of(Identifier.fromNamespaceAndPath(LittleMaidNeo.MODID, namePath));
    }
}
