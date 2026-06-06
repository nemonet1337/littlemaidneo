package work.nemonet.littlemaidneo.entity;

import work.nemonet.littlemaidneo.entity.compound.IHasMultiModel.Layer;
import work.nemonet.littlemaidneo.entity.compound.IHasMultiModel.Part;
import work.nemonet.littlemaidneo.resource.util.TextureColors;
import work.nemonet.littlemaidneo.resource.holder.ConfigHolder;
import work.nemonet.littlemaidneo.resource.manager.LMConfigManager;
import work.nemonet.littlemaidneo.resource.manager.LMModelManager;
import work.nemonet.littlemaidneo.resource.manager.LMTextureManager;

import java.util.Arrays;
import java.util.List;

/**
 * メイドさんのスポーン時などの個体差（テクスチャ、ボイス）をランダム初期化するクラス。
 */
public final class MaidRandomizer {
    private MaidRandomizer() {}

    public static void setRandomTexture(LittleMaidEntity mob) {
        int idFactor = mob.getIdFactor();
        var textureHolderList = LMTextureManager.INSTANCE.getAllTextures()
                .stream()
                .filter(h -> h.hasSkinTexture(false))
                .filter(h -> LMModelManager.INSTANCE.hasModel(h.getModelName()))
                .toList();
        if (textureHolderList.isEmpty()) {
            return;
        }
        var textureHolder = textureHolderList.get(
                idFactor % textureHolderList.size());
        var colorList = Arrays.stream(TextureColors.values())
                .filter(c -> textureHolder.getTexture(c, false, false).isPresent())
                .toList();
        if (colorList.isEmpty()) {
            return;
        }
        var color = colorList.get(idFactor % colorList.size());
        mob.setColorMM(color);
        mob.setTextureHolder(textureHolder, Layer.SKIN, Part.HEAD);
        if (textureHolder.hasArmorTexture()) {
            mob.setTextureHolder(textureHolder, Layer.INNER, Part.HEAD);
            mob.setTextureHolder(textureHolder, Layer.INNER, Part.BODY);
            mob.setTextureHolder(textureHolder, Layer.INNER, Part.LEGS);
            mob.setTextureHolder(textureHolder, Layer.INNER, Part.FEET);
            mob.setTextureHolder(textureHolder, Layer.OUTER, Part.HEAD);
            mob.setTextureHolder(textureHolder, Layer.OUTER, Part.BODY);
            mob.setTextureHolder(textureHolder, Layer.OUTER, Part.LEGS);
            mob.setTextureHolder(textureHolder, Layer.OUTER, Part.FEET);
        }
    }

    public static void setRandomVoice(LittleMaidEntity mob) {
        int idFactor = mob.getIdFactor();
        if (LittleMaidEntity.getConfig().spawn.silentDefaultVoice) {
            mob.setConfigHolder(LMConfigManager.EMPTY_CONFIG);
        } else {
            List<ConfigHolder> configs = LMConfigManager.INSTANCE.getAllConfig();
            mob.setConfigHolder(configs.get(idFactor % configs.size()));
        }
        String defaultSoundPackName = LittleMaidEntity.getConfig().spawn.defaultSoundPackName;
        if (!defaultSoundPackName.isEmpty()) {
            LMConfigManager.INSTANCE.getAllConfig()
                    .stream()
                    .filter(c -> c.packName().equalsIgnoreCase(defaultSoundPackName))
                    .findAny()
                    .ifPresent(mob::setConfigHolder);
        }
    }
}
