package work.nemonet.littlemaidneo.resource.manager;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import work.nemonet.littlemaidneo.common.LMNLib;
import work.nemonet.littlemaidneo.entity.compound.IHasMultiModel;
import work.nemonet.littlemaidneo.maidmodel.LMModel;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;

public class LMModelManager {
    public static final LMModelManager INSTANCE = new LMModelManager();
    private static final Logger LOGGER = LogManager.getLogger();
    private LMModel<?> defaultLMModel;
    private final Map<String, ModelFactory> lmModels = new HashMap<>();

    public boolean hasModel(String modelName) {
        return lmModels.get(modelName.toLowerCase()) != null;
    }

    public Optional<LMModel<?>> getLMModel(String modelName, IHasMultiModel.Layer layer) {
        ModelFactory f = lmModels.get(modelName.toLowerCase());
        if (f == null) return Optional.empty();
        return Optional.of(f.getModel(layer));
    }

    public LMModel<?> getOrDefaultLMModel(String modelName, IHasMultiModel.Layer layer) {
        ModelFactory f = lmModels.get(modelName.toLowerCase());
        if (f == null) return defaultLMModel;
        return f.getModel(layer);
    }

    public void setDefaultLMModel(LMModel<?> model) {
        this.defaultLMModel = model;
    }

    public LMModel<?> getDefaultLMModel() {
        return defaultLMModel;
    }

    public static final class ModelFactory {
        private final Supplier<LMModel<?>> skinFactory;
        private final Supplier<LMModel<?>> innerFactory;
        private final Supplier<LMModel<?>> outerFactory;
        private LMModel<?> skin;
        private LMModel<?> inner;
        private LMModel<?> outer;

        public ModelFactory(Supplier<LMModel<?>> skinFactory, Supplier<LMModel<?>> innerFactory, Supplier<LMModel<?>> outerFactory) {
            this.skinFactory = skinFactory;
            this.innerFactory = innerFactory;
            this.outerFactory = outerFactory;
        }

        public LMModel<?> getModel(IHasMultiModel.Layer layer) {
            return switch (layer) {
                case SKIN -> skin != null ? skin : (skin = skinFactory.get());
                case INNER -> inner != null ? inner : (inner = innerFactory.get());
                case OUTER -> outer != null ? outer : (outer = outerFactory.get());
            };
        }
    }

    public void addLMModel(String name, Supplier<LMModel<?>> skin, Supplier<LMModel<?>> inner, Supplier<LMModel<?>> outer) {
        lmModels.put(name.toLowerCase(), new ModelFactory(skin, inner, outer));
    }

    public java.util.Set<String> getModelNames() {
        return java.util.Collections.unmodifiableSet(lmModels.keySet());
    }
}
