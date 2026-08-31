package work.nemonet.littlemaidneo.entity;

import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import work.nemonet.littlemaidneo.entity.compound.IHasMultiModel;
import work.nemonet.littlemaidneo.entity.compound.MultiModelView;
import work.nemonet.littlemaidneo.maidmodel.LMModel;
import work.nemonet.littlemaidneo.resource.util.ArmorPart;
import work.nemonet.littlemaidneo.resource.util.ArmorSets;
import work.nemonet.littlemaidneo.resource.util.TexturePair;
import work.nemonet.littlemaidneo.setup.ModRegistration;

import java.util.Optional;

/**
 * GUI プレビュー用の軽量マルチモデルエンティティ。描画クエリだけを実装する。
 */
public class DummyModelEntity extends LivingEntity implements MultiModelView {
    private LMModel<?> skinModel;
    private TexturePair skinTexture;
    private final ArmorSets<ArmorPart> armorsData = new ArmorSets<>();
    private final ArmorSets<Boolean> armorsVisible = new ArmorSets<>();

    public DummyModelEntity(Level worldIn) {
        this(ModRegistration.DUMMY_MODEL_ENTITY.get(), worldIn);
    }

    public DummyModelEntity(EntityType<DummyModelEntity> type, Level worldIn) {
        super(type, worldIn);
    }

    public void setSkinModel(LMModel<?> model) {
        skinModel = model;
    }

    public void setSkinTexture(TexturePair skinTexture) {
        this.skinTexture = skinTexture;
    }

    public void setArmorData(ArmorPart data, IHasMultiModel.Part part) {
        armorsData.setArmor(data, part);
    }

    public void setArmorVisible(boolean visible, IHasMultiModel.Part part) {
        this.armorsVisible.setArmor(visible, part);
    }

    public void setAllArmorVisible(boolean visible) {
        for (IHasMultiModel.Part part : IHasMultiModel.Part.values()) {
            this.armorsVisible.setArmor(visible, part);
        }
    }

    @Override
    public ItemStack getItemBySlot(EquipmentSlot slot) {
        return ItemStack.EMPTY;
    }

    @Override
    public void setItemSlot(EquipmentSlot slot, ItemStack stack) {
    }

    @Override
    public HumanoidArm getMainArm() {
        return HumanoidArm.RIGHT;
    }

    @Override
    public Optional<LMModel<?>> getModel(IHasMultiModel.Layer layer, IHasMultiModel.Part part) {
        if (layer == IHasMultiModel.Layer.SKIN) {
            return Optional.ofNullable(skinModel);
        }
        return armorsData.getArmor(part).map(armorPart -> armorPart.getModel(layer));
    }

    @Override
    public Optional<Identifier> getTexture(IHasMultiModel.Layer layer, IHasMultiModel.Part part, boolean isLight) {
        if (layer == IHasMultiModel.Layer.SKIN) {
            if (skinTexture == null) {
                return Optional.empty();
            }
            return Optional.ofNullable(skinTexture.getTexture(isLight));
        }
        return armorsData.getArmor(part).map(armorPart -> armorPart.getTexture(layer, isLight));
    }

    @Override
    public boolean isArmorVisible(IHasMultiModel.Part part) {
        return armorsVisible.getArmor(part).orElse(false);
    }

    @Override
    public boolean isArmorGlint(IHasMultiModel.Part part) {
        return false;
    }
}
