package work.nemonet.littlemaidneo.resource.loader;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import work.nemonet.littlemaidneo.LittleMaidNeo;
import work.nemonet.littlemaidneo.config.LMNModelConfig;
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
    @SuppressWarnings("unchecked")
    public Runnable parse(String path, Path folderPath, InputStream inputStream, boolean isArchive) {
        String classpath = path.replace("/", ".");
        classpath = classpath.substring(0, path.lastIndexOf(".class"));
        // クラスロード＋ASM変換＋skin/inner/outer の 3×newInstance（最重 CPU 構築）を並列フェーズで実行。
        Class<?> modelClass;
        try {
            modelClass = classForName(classpath);
        } catch (Exception e) {
            LOGGER.error("読み込めませんでした。古いモデルの可能性があります : " + path);
            if (LMNModelConfig.isDebugMode()) e.printStackTrace();
            return null;
        }
        if (modelClass == null || !ModelMultiBase.class.isAssignableFrom(modelClass)) return null;
        int lastSplitter = classpath.lastIndexOf("_");
        if (lastSplitter == -1) return null;
        String className = classpath.toLowerCase().substring(lastSplitter + 1);
        return modelManager.buildHolder((Class<? extends ModelMultiBase>) modelClass)
                .<Runnable>map(holder -> () -> modelManager.putModel(className, holder))
                .orElse(null);
    }

    public Class<?> classForName(String className) throws ClassNotFoundException {
        try {
            return Class.forName(className, true, classLoader);
        } catch (Error e) {
            throw new ClassNotFoundException(className + ":classForName_Error:[" + e + "]");
        }
    }
}
