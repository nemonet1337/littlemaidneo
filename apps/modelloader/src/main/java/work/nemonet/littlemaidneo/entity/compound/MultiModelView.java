package work.nemonet.littlemaidneo.entity.compound;

import net.minecraft.resources.Identifier;
import work.nemonet.littlemaidneo.maidmodel.LMModel;

import java.util.Optional;

/**
 * 描画に必要なマルチモデルの読み取り面。
 * GUI プレビューの {@code DummyModelEntity} はこちらだけを実装する。
 */
public interface MultiModelView {

    Optional<LMModel<?>> getModel(IHasMultiModel.Layer layer, IHasMultiModel.Part part);

    Optional<Identifier> getTexture(IHasMultiModel.Layer layer, IHasMultiModel.Part part, boolean isLight);

    boolean isArmorVisible(IHasMultiModel.Part part);

    boolean isArmorGlint(IHasMultiModel.Part part);
}
