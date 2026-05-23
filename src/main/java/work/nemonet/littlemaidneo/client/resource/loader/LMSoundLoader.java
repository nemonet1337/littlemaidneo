package work.nemonet.littlemaidneo.client.resource.loader;

import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import work.nemonet.littlemaidneo.client.resource.ResourceWrapper;
import work.nemonet.littlemaidneo.client.resource.manager.LMSoundManager;
import work.nemonet.littlemaidneo.config.LMMLConfig;
import work.nemonet.littlemaidneo.resource.loader.LMLoader;
import work.nemonet.littlemaidneo.resource.util.ResourceHelper;

import java.io.InputStream;
import java.nio.file.Path;

@OnlyIn(Dist.CLIENT)
public class LMSoundLoader implements LMLoader {
    private static final Logger LOGGER = LogManager.getLogger();
    private final LMSoundManager soundManager;

    public LMSoundLoader(LMSoundManager soundManager) {
        this.soundManager = soundManager;
    }

    @Override
    public boolean canLoad(String path, Path folderPath, InputStream inputStream, boolean isArchive) {
        return path.endsWith(".ogg") && ResourceHelper.getParentFolderName(path, isArchive).isPresent();
    }

    @Override
    public void load(String path, Path folderPath, InputStream inputStream, boolean isArchive) {
        String packName = ResourceHelper.getFirstParentName(path, folderPath, isArchive).orElse("");
        String parent = ResourceHelper.getParentFolderName(path, isArchive).orElse("");
        String fileName = ResourceHelper.getFileName(path, isArchive);
        ResourceLocation location = ResourceHelper.getLocation("sounds", packName, fileName);
        fileName = ResourceHelper.removeExtension(fileName);
        fileName = ResourceHelper.removeNameLastIndex(fileName);
        soundManager.addSound(packName, parent, fileName, location);
        ResourceWrapper.addResourcePath(location, path, folderPath, isArchive);
        if (LMMLConfig.isDebugMode()) LOGGER.debug("Loaded Sound : " + packName + " : " + fileName);
    }
}
