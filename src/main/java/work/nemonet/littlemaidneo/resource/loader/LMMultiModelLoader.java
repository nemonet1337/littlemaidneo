package work.nemonet.littlemaidneo.resource.loader;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import work.nemonet.littlemaidneo.LittleMaidNeo;
import work.nemonet.littlemaidneo.config.LMMLConfig;
import work.nemonet.littlemaidneo.maidmodel.ModelMultiBase;
import work.nemonet.littlemaidneo.resource.manager.LMModelManager;

import java.io.InputStream;
import java.nio.file.Path;

public class LMMultiModelLoader implements LMLoader {
    private static final Logger LOGGER = LogManager.getLogger();
    private final LMModelManager modelManager;
    private final ClassLoader classLoader;

    public LMMultiModelLoader(LMModelManager modelManager, ClassLoader classLoader) {
        this.modelManager = modelManager;
        this.classLoader = classLoader;
    }

    @Override
    public boolean canLoad(String path, Path folderPath, InputStream inputStream, boolean isArchive) {
        return path.endsWith(".class") && (path.contains("ModelMulti_") || path.contains("ModelLittleMaid_"));
    }

    @Override
    public void load(String path, Path folderPath, InputStream inputStream, boolean isArchive) {
        String classpath = path.replace("/", ".");
        classpath = classpath.substring(0, path.lastIndexOf(".class"));
        try {
            tryAddModel(classpath, classForName(classpath));
        } catch (Exception e) {
            LOGGER.error("読み込めませんでした。古いモデルの可能性があります : " + path);
            if (LMMLConfig.isDebugMode()) e.printStackTrace();
        }
    }

    @SuppressWarnings("unchecked")
    private void tryAddModel(String classpath, Class<?> modelClass) {
        if (modelClass != null && ModelMultiBase.class.isAssignableFrom(modelClass)) {
            int lastSplitter = classpath.lastIndexOf("_");
            if (lastSplitter == -1) return;
            String className = classpath.toLowerCase().substring(lastSplitter + 1);
            modelManager.addModel(className, (Class<? extends ModelMultiBase>) modelClass);
        }
    }

    public Class<?> classForName(String className) throws ClassNotFoundException {
        try {
            return Class.forName(className, true, classLoader);
        } catch (Error e) {
            throw new ClassNotFoundException(className + ":classForName_Error:[" + e + "]");
        }
    }
}
