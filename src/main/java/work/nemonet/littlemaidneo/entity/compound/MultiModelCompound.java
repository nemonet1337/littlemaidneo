package work.nemonet.littlemaidneo.entity.compound;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import work.nemonet.littlemaidneo.maidmodel.EntityCaps;
import work.nemonet.littlemaidneo.maidmodel.IModelCaps;
import work.nemonet.littlemaidneo.multimodel.IMultiModel;
import work.nemonet.littlemaidneo.resource.holder.TextureHolder;
import work.nemonet.littlemaidneo.resource.manager.LMModelManager;
import work.nemonet.littlemaidneo.resource.manager.LMTextureManager;
import work.nemonet.littlemaidneo.resource.util.ArmorPart;
import work.nemonet.littlemaidneo.resource.util.ArmorSets;
import work.nemonet.littlemaidneo.resource.util.TextureColors;
import work.nemonet.littlemaidneo.resource.util.TexturePair;

import java.util.Optional;

public class MultiModelCompound implements IHasMultiModel {

    private final LivingEntity entity;
    private final IModelCaps caps;

    private final TextureHolder defaultMainPackage;
    private final TextureHolder defaultArmorPackage;

    private TextureHolder skinTexHolder;
    private IMultiModel skinModel;
    private TexturePair skinTexture;

    private final ArmorSets<TextureHolder> armorsTexHolder = new ArmorSets<>();
    private final ArmorSets<ArmorPart> armorsData = new ArmorSets<>();

    private TextureColors color;
    private boolean isContract;

    public MultiModelCompound(LivingEntity entity, TextureHolder defaultMainPackage, TextureHolder defaultArmorPackage) {
        this.entity = entity;
        this.caps = new EntityCaps(entity);
        this.defaultMainPackage = defaultMainPackage;
        this.defaultArmorPackage = defaultArmorPackage;
        this.color = TextureColors.BROWN;
        update();
    }

    public void update() {
        updateMain();
        updateArmor();
    }

    public void updateMain() {
        if (skinTexHolder == null) {
            skinTexHolder = defaultMainPackage;
        }
        LMModelManager modelManager = LMModelManager.INSTANCE;
        skinModel = modelManager.getOrDefaultModel(skinTexHolder.getModelName(), Layer.SKIN);
        skinTexture = new TexturePair(skinTexHolder.getTexture(color, isContract, false).orElse(null),
                skinTexHolder.getTexture(color, isContract, true).orElse(null));
    }

    public void updateArmor() {
        int index = 0;
        for (ItemStack stack : entity.getArmorSlots()) {
            if (4 < index) break;
            updateArmorPart(Part.getPart(index++), getName(stack.getItem()), getDamagePercent(stack));
        }
    }

    private String getName(Item item) {
        if (entity.level().isClientSide && item instanceof ArmorItem armorItem) {
            return armorItem.getMaterial().unwrapKey()
                    .map(key -> key.location().getPath())
                    .orElse("unknown").toLowerCase();
        }
        ResourceLocation location = BuiltInRegistries.ITEM.getKey(item);
        return location.toString();
    }

    private float getDamagePercent(ItemStack stack) {
        if (stack.isDamageableItem() && 0 < stack.getMaxDamage()) {
            return (float) stack.getDamageValue() / (float) stack.getMaxDamage();
        }
        return 0F;
    }

    private void updateArmorPart(Part part, String armorName, float damagePercent) {
        TextureHolder textureHolder = armorsTexHolder.getArmor(part).orElse(defaultArmorPackage);
        armorsTexHolder.setArmor(textureHolder, part);
        LMModelManager manager = LMModelManager.INSTANCE;
        ArmorPart.Builder dataBuilder = ArmorPart.Builder.newInstance();
        dataBuilder.innerModel(manager.getOrDefaultModel(textureHolder.getModelName(), Layer.INNER));
        dataBuilder.outerModel(manager.getOrDefaultModel(textureHolder.getModelName(), Layer.OUTER));
        dataBuilder.innerTex(textureHolder.getArmorTexture(Layer.INNER, armorName, damagePercent, false).orElse(null));
        dataBuilder.innerTexLight(textureHolder.getArmorTexture(Layer.INNER, armorName, damagePercent, true).orElse(null));
        dataBuilder.outerTex(textureHolder.getArmorTexture(Layer.OUTER, armorName, damagePercent, false).orElse(null));
        dataBuilder.outerTexLight(textureHolder.getArmorTexture(Layer.OUTER, armorName, damagePercent, true).orElse(null));
        armorsData.setArmor(dataBuilder.build(), part);
    }

    @Override
    public void setTextureHolder(TextureHolder textureHolder, Layer layer, Part part) {
        if (layer == Layer.SKIN) {
            skinTexHolder = textureHolder;
            updateMain();
        } else {
            armorsTexHolder.setArmor(textureHolder, part);
            int index = 0;
            for (ItemStack stack : entity.getArmorSlots()) {
                if (part.getIndex() == index++) {
                    updateArmorPart(part, getName(stack.getItem()), getDamagePercent(stack));
                }
            }
        }
    }

    @Override
    public TextureHolder getTextureHolder(Layer layer, Part part) {
        if (layer == Layer.SKIN) {
            return skinTexHolder;
        } else {
            return armorsTexHolder.getArmor(part)
                    .orElseThrow(() -> new IllegalStateException("防具テクスチャホルダーが存在しません。"));
        }
    }

    @Override
    public Optional<IMultiModel> getModel(Layer layer, Part part) {
        if (layer == Layer.SKIN) {
            return Optional.ofNullable(skinModel);
        } else {
            IMultiModel model = armorsData.getArmor(part)
                    .orElseThrow(() -> new IllegalStateException("防具データが存在しません"))
                    .getModel(layer);
            return Optional.ofNullable(model);
        }
    }

    @Override
    public Optional<ResourceLocation> getTexture(Layer layer, Part part, boolean isLight) {
        if (layer == Layer.SKIN) {
            return Optional.ofNullable(skinTexture.getTexture(isLight));
        } else {
            ResourceLocation resourceLocation = armorsData.getArmor(part)
                    .orElseThrow(() -> new IllegalStateException("防具データが存在しません"))
                    .getTexture(layer, isLight);
            return Optional.ofNullable(resourceLocation);
        }
    }

    @Override
    public IModelCaps getCaps() {
        return this.caps;
    }

    @Override
    public boolean isArmorVisible(Part part) {
        int index = 0;
        for (ItemStack stack : entity.getArmorSlots()) {
            if (part.getIndex() == index++ && !stack.isEmpty()) return true;
        }
        return false;
    }

    @Override
    public boolean isArmorGlint(Part part) {
        int index = 0;
        for (ItemStack stack : entity.getArmorSlots()) {
            if (part.getIndex() == index++ && !stack.isEmpty()) return stack.isEnchanted();
        }
        return false;
    }

    @Override
    public boolean isAllowChangeTexture(Entity changer, TextureHolder textureHolder, Layer layer, Part part) {
        return true;
    }

    public void setColorMM(TextureColors color) {
        this.color = color;
        updateMain();
    }

    public TextureColors getColorMM() {
        return color;
    }

    public void setContractMM(boolean contract) {
        this.isContract = contract;
        updateMain();
    }

    @Override
    public boolean isContractMM() {
        return isContract;
    }

    public void writeToNbt(CompoundTag nbt) {
        nbt.putByte("SkinColor", (byte) getColorMM().getIndex());
        nbt.putBoolean("IsContract", isContractMM());
        nbt.putString("SkinTexture", getTextureHolder(Layer.SKIN, Part.HEAD).getTextureName());
        for (Part part : Part.values()) {
            nbt.putString("ArmorTextureInner" + part.getPartName(),
                    getTextureHolder(Layer.INNER, part).getTextureName());
            nbt.putString("ArmorTextureOuter" + part.getPartName(),
                    getTextureHolder(Layer.OUTER, part).getTextureName());
        }
    }

    public void readFromNbt(CompoundTag nbt) {
        if (nbt.contains("SkinColor")) {
            setColorMM(TextureColors.getColor(nbt.getByte("SkinColor")));
        }
        setContractMM(nbt.getBoolean("IsContract"));
        LMTextureManager textureManager = LMTextureManager.INSTANCE;
        if (nbt.contains("SkinTexture")) {
            textureManager.getTexture(nbt.getString("SkinTexture"))
                    .ifPresent(th -> setTextureHolder(th, Layer.SKIN, Part.HEAD));
        }
        for (Part part : Part.values()) {
            String inner = "ArmorTextureInner" + part.getPartName();
            String outer = "ArmorTextureOuter" + part.getPartName();
            if (nbt.contains(inner)) {
                textureManager.getTexture(nbt.getString(inner))
                        .ifPresent(th -> setTextureHolder(th, Layer.INNER, part));
            }
            if (nbt.contains(outer)) {
                textureManager.getTexture(nbt.getString(outer))
                        .ifPresent(th -> setTextureHolder(th, Layer.OUTER, part));
            }
        }
    }

    public void writeToPacket(FriendlyByteBuf packet) {
        packet.writeEnum(getColorMM());
        packet.writeBoolean(isContractMM());
        packet.writeUtf(getTextureHolder(Layer.SKIN, Part.HEAD).getTextureName());
        for (Part part : Part.values()) {
            packet.writeUtf(getTextureHolder(Layer.INNER, part).getTextureName());
            packet.writeUtf(getTextureHolder(Layer.OUTER, part).getTextureName());
        }
    }

    public void readFromPacket(FriendlyByteBuf packet) {
        setColorMM(packet.readEnum(TextureColors.class));
        setContractMM(packet.readBoolean());
        LMTextureManager textureManager = LMTextureManager.INSTANCE;
        textureManager.getTexture(packet.readUtf())
                .ifPresent(th -> setTextureHolder(th, Layer.SKIN, Part.HEAD));
        for (Part part : Part.values()) {
            textureManager.getTexture(packet.readUtf())
                    .ifPresent(th -> setTextureHolder(th, Layer.INNER, part));
            textureManager.getTexture(packet.readUtf())
                    .ifPresent(th -> setTextureHolder(th, Layer.OUTER, part));
        }
    }
}
