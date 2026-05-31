package work.nemonet.littlemaidneo.resource.holder;

import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import work.nemonet.littlemaidneo.entity.compound.IHasMultiModel;
import work.nemonet.littlemaidneo.resource.util.TextureColors;
import work.nemonet.littlemaidneo.resource.util.TextureIndexes;

import java.util.*;
import java.util.stream.Collectors;

public class TextureHolder {
    private final String name;
    private final String modelName;
    private final Map<Integer, Identifier> textures = new HashMap<>();
    private final Map<String, Map<Integer, Identifier>> armors = new HashMap<>();

    public TextureHolder(String name, String modelName) {
        this.name = name;
        this.modelName = modelName;
    }

    public String getTextureName() { return name; }
    public String getModelName() { return modelName; }

    public void addTexture(int index, Identifier texturePath) {
        textures.put(index, texturePath);
    }

    public void addArmorTexture(String armorType, int index, Identifier texturePath) {
        Map<Integer, Identifier> armorMap = armors.computeIfAbsent(armorType.toLowerCase(), k -> new HashMap<>());
        armorMap.put(index, texturePath);
    }

    public Optional<Identifier> getTexture(TextureColors color, boolean isContract, boolean isLight) {
        int index = color.getIndex();
        if (isLight) {
            index += isContract ? TextureIndexes.COLOR_WILD_LIGHT.getIndexMin() : TextureIndexes.COLOR_CONTRACT_LIGHT.getIndexMin();
        } else if (!isContract) {
            index += TextureIndexes.COLOR_WILD.getIndexMin();
        }
        return Optional.ofNullable(textures.get(index));
    }

    public Optional<Identifier> getArmorTexture(IHasMultiModel.Layer layer, String armorName, float damagePercent, boolean isLight) {
        if (armors.isEmpty()) return Optional.empty();
        Optional<Identifier> optional = getArmorTextureInner(layer, armorName, damagePercent, isLight);
        if (optional.isPresent()) return optional;
        if (armors.containsKey("default") && !armorName.toLowerCase().equals("default")) {
            return getArmorTextureInner(layer, "default", damagePercent, isLight);
        }
        return armors.keySet().stream()
                .filter(armors::containsKey)
                .map(s -> getArmorTextureInner(layer, s, damagePercent, isLight).orElse(null))
                .filter(Objects::nonNull)
                .findAny();
    }

    public Optional<Identifier> getArmorTextureInner(IHasMultiModel.Layer layer, String armorName, float damagePercent, boolean isLight) {
        if (armors.isEmpty()) return Optional.empty();
        Map<Integer, Identifier> armorTextures = armors.get(armorName.toLowerCase());
        if (armorTextures == null || armorTextures.isEmpty()) return Optional.empty();
        int index = switch (layer) {
            case INNER -> (isLight ? TextureIndexes.ARMOR_1_DAMAGED_LIGHT : TextureIndexes.ARMOR_1_DAMAGED).getIndexMin();
            case OUTER -> (isLight ? TextureIndexes.ARMOR_2_DAMAGED_LIGHT : TextureIndexes.ARMOR_2_DAMAGED).getIndexMin();
            default -> throw new IllegalArgumentException("それは防具ではないかnullである");
        };
        int damageIndex = Mth.clamp((int) (damagePercent * 10F - 1F), 0, 9);
        Identifier armorTexture = armorTextures.get(index + damageIndex);
        if (armorTexture != null) return Optional.of(armorTexture);
        for (int i = 1; i <= damageIndex; i++) {
            Identifier temp = armorTextures.get(index + damageIndex - i);
            if (temp != null) return Optional.of(temp);
        }
        return Optional.empty();
    }

    public Collection<String> getArmorNames() {
        return armors.keySet().stream().sorted(Comparator.naturalOrder()).collect(Collectors.toList());
    }

    public boolean hasSkinTexture(boolean isContract) {
        for (TextureColors color : TextureColors.values()) {
            if (getTexture(color, isContract, false).isPresent() || getTexture(color, isContract, true).isPresent())
                return true;
        }
        return false;
    }

    public boolean hasArmorTexture() {
        return !armors.isEmpty();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        TextureHolder that = (TextureHolder) o;
        return name.equals(that.name) && modelName.equals(that.modelName)
                && textures.equals(that.textures) && armors.equals(that.armors);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, modelName, textures, armors);
    }
}
