package work.nemonet.littlemaidneo.client.resource;

import net.minecraft.resources.Identifier;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import work.nemonet.littlemaidneo.config.LMNModelConfig;
import work.nemonet.littlemaidneo.resource.loader.LMLoader;
import work.nemonet.littlemaidneo.resource.util.ResourceHelper;

import java.io.InputStream;
import java.nio.file.Path;
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
    public Runnable parse(String path, Path folderPath, InputStream inputStream, boolean isArchive) {
        // 保護コアB: キー生成（pack.parent.file）・location・探索パスは不変。登録のみ遅延する。
        String packName = ResourceHelper.getFirstParentName(path, folderPath, isArchive).orElse("");
        String parent = ResourceHelper.getParentFolderName(path, isArchive).orElse("");
        String rawFileName = ResourceHelper.getFileName(path, isArchive);
        Identifier location = ResourceHelper.getLocation("sounds", packName, rawFileName);
        String fileName = ResourceHelper.removeNameLastIndex(ResourceHelper.removeExtension(rawFileName));
        return () -> {
            soundManager.addSound(packName, parent, fileName, location);
            ResourceWrapper.addResourcePath(location, path, folderPath, isArchive);
            if (LMNModelConfig.isDebugMode()) LOGGER.debug("Loaded Sound : {} : {}", packName, fileName);
        };
    }
}
