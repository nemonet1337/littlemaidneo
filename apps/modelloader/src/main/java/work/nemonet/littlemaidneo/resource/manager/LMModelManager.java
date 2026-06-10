package work.nemonet.littlemaidneo.resource.manager;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import work.nemonet.littlemaidneo.common.LMNLib;
import work.nemonet.littlemaidneo.entity.compound.IHasMultiModel;
import work.nemonet.littlemaidneo.maidmodel.ModelMultiBase;
import work.nemonet.littlemaidneo.multimodel.IMultiModel;

import java.lang.reflect.Constructor;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class LMModelManager {
    public static final LMModelManager INSTANCE = new LMModelManager();
    private static final Logger LOGGER = LogManager.getLogger();
    private IMultiModel defaultModel;
    private final Map<String, ModelHolder> models = new HashMap<>();

    public void addModel(String modelName, Class<? extends ModelMultiBase> modelClass) {
        buildHolder(modelClass).ifPresent(holder -> putModel(modelName, holder));
    }

    /**
     * skin/inner/outer の 3 インスタンスを構築して {@link ModelHolder} を返す（重い処理）。
     * 共有状態に触れず各インスタンスは独立に構築されるため、ワーカースレッドから呼んで良い。
     * 抽象クラスや非対応シグネチャの場合は空を返す。
     */
    public Optional<ModelHolder> buildHolder(Class<? extends ModelMultiBase> modelClass) {
        try {
            Constructor<? extends ModelMultiBase> constructor = modelClass.getConstructor(float.class);
            ModelMultiBase skin = constructor.newInstance(0.0F);
            float[] size = skin.getArmorModelsSize();
            ModelMultiBase inner = constructor.newInstance(size[0]);
            ModelMultiBase outer = constructor.newInstance(size[1]);
            return Optional.of(new ModelHolder(skin, inner, outer));
        } catch (Exception e) {
            LOGGER.debug("インスタンス化に失敗しました。抽象クラスまたは非対応のモデルである可能性があります。 : " + modelClass);
            e.printStackTrace();
            return Optional.empty();
        }
    }

    /**
     * 構築済みの {@link ModelHolder} を登録する（軽い処理）。HashMap を保護するため
     * 単一スレッドの登録フェーズからのみ呼ぶこと。
     */
    public void putModel(String modelName, ModelHolder holder) {
        models.put(modelName.toLowerCase(), holder);
        if (LMNLib.LOGGER.isDebugEnabled()) LOGGER.debug("Loaded Model : " + holder.skin().getClass());
    }

    public void addModel(String modelName, IMultiModel skin, IMultiModel inner, IMultiModel outer) {
        models.put(modelName.toLowerCase(), new ModelHolder(skin, inner, outer));
    }

    public boolean hasModel(String modelName) {
        return models.get(modelName.toLowerCase()) != null;
    }

    public Optional<IMultiModel> getModel(String modelName, IHasMultiModel.Layer layer) {
        ModelHolder modelHolder = models.get(modelName.toLowerCase());
        if (modelHolder == null) return Optional.empty();
        return Optional.of(modelHolder.getModel(layer));
    }

    public IMultiModel getOrDefaultModel(String modelName, IHasMultiModel.Layer layer) {
        ModelHolder modelHolder = models.get(modelName.toLowerCase());
        if (modelHolder == null) return getDefaultModel();
        return modelHolder.getModel(layer);
    }

    public void setDefaultModel(IMultiModel defaultModel) {
        this.defaultModel = defaultModel;
    }

    public IMultiModel getDefaultModel() {
        return defaultModel;
    }

    public record ModelHolder(IMultiModel skin, IMultiModel inner, IMultiModel outer) {
        public ModelHolder {
            if (skin == null || inner == null || outer == null) {
                throw new IllegalArgumentException("ModelHolderはnull不許容です");
            }
        }

        public IMultiModel getModel(IHasMultiModel.Layer layer) {
            return switch (layer) {
                case SKIN -> skin;
                case INNER -> inner;
                case OUTER -> outer;
            };
        }
    }

    public java.util.Set<String> getModelNames() {
        return java.util.Collections.unmodifiableSet(models.keySet());
    }
}
