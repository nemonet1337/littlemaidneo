package net.sistr.littlemaidrebirth.api.mode;

import net.minecraft.world.item.alchemy.Potion;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.AxeItem;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ShearsItem;
import net.minecraft.world.item.SwordItem;
import net.sistr.littlemaidrebirth.entity.mode.*;
import net.sistr.littlemaidrebirth.tags.LMTags;

import static net.sistr.littlemaidrebirth.LMRBMod.MODID;

/**
 * デフォルトのモードを追加するクラス
 * メイド専用
 */
public class Modes {
    public static final ModeType<FencerMode> FENCER_MODE_TYPE;
    public static final ModeType<ArcherMode> ARCHER_MODE_TYPE;
    public static final ModeType<CookingMode> COOKING_MODE_TYPE;
    public static final ModeType<RipperMode> RIPPER_MODE_TYPE;
    public static final ModeType<TorcherMode> TORCHER_MODE_TYPE;
    public static final ModeType<HealerMode> HEALER_MODE_TYPE;

    static {
        FENCER_MODE_TYPE = buildFencerMode().build();
        ARCHER_MODE_TYPE = buildArcherMode().build();
        COOKING_MODE_TYPE = buildCookingMode().build();
        RIPPER_MODE_TYPE = buildRipperMode().build();
        TORCHER_MODE_TYPE = buildTorcherMode().build();
        HEALER_MODE_TYPE = buildHealerMode().build();
    }

    public static ModeType.Builder<FencerMode> buildFencerMode() {
        return ModeType.<FencerMode>builder((type, maid) -> new FencerMode(type, "Fencer", maid, 1.0f))
                .addItemMatcher(ItemMatchers.clazz(SwordItem.class), ItemMatcher.Priority.LOWER)
                .addItemMatcher(ItemMatchers.clazz(AxeItem.class), ItemMatcher.Priority.LOWER)
                .addItemMatcher(ItemMatchers.tag(LMTags.Items.FENCER_MODE), ItemMatcher.Priority.HIGHER);
    }

    public static ModeType.Builder<ArcherMode> buildArcherMode() {
        return ModeType.<ArcherMode>builder((type, maid) -> new ArcherMode(type, "Archer", maid))
                .addItemMatcher(ItemMatchers.clazz(IRangedWeapon.class), ItemMatcher.Priority.LOWER)
                .addItemMatcher(ItemMatchers.tag(LMTags.Items.ARCHER_MODE), ItemMatcher.Priority.HIGHER);
    }

    public static ModeType.Builder<CookingMode> buildCookingMode() {
        return ModeType.<CookingMode>builder((type, maid) -> new CookingMode(type, "Cooking", maid))
                .addItemMatcher(ItemMatchers.tag(LMTags.Items.COOKING_MODE), ItemMatcher.Priority.HIGHER);
    }

    public static ModeType.Builder<RipperMode> buildRipperMode() {
        return ModeType.<RipperMode>builder((type, maid) -> new RipperMode(type, "Ripper", maid, 8F))
                .addItemMatcher(ItemMatchers.clazz(ShearsItem.class), ItemMatcher.Priority.LOWER)
                .addItemMatcher(ItemMatchers.tag(LMTags.Items.RIPPER_MODE), ItemMatcher.Priority.HIGHER);
    }

    public static ModeType.Builder<TorcherMode> buildTorcherMode() {
        return ModeType.<TorcherMode>builder((type, maid) -> new TorcherMode(type, "Torcher", maid, 12F))
                .addItemMatcher(stack -> stack.getItem() instanceof BlockItem
                        && 9 < ((BlockItem) stack.getItem()).getBlock().defaultBlockState().getLightEmission(),
                        ItemMatcher.Priority.LOWER)
                .addItemMatcher(ItemMatchers.tag(LMTags.Items.TORCHER_MODE), ItemMatcher.Priority.HIGHER);
    }

    public static ModeType.Builder<HealerMode> buildHealerMode() {
        return ModeType.<HealerMode>builder((type, maid) -> new HealerMode(type, "Healer", maid))
                .addItemMatcher(stack -> stack.get(DataComponents.FOOD) != null, ItemMatcher.Priority.LOWER)
                .addItemMatcher(stack -> {
                    var contents = stack.get(DataComponents.POTION_CONTENTS);
                    return contents != null && contents.potion().isPresent();
                }, ItemMatcher.Priority.LOWER)
                .addItemMatcher(ItemMatchers.tag(LMTags.Items.HEALER_MODE), ItemMatcher.Priority.HIGHER);
    }

    public static void init() {
        register("fencer", FENCER_MODE_TYPE);
        register("archer", ARCHER_MODE_TYPE);
        register("cooking", COOKING_MODE_TYPE);
        register("ripper", RIPPER_MODE_TYPE);
        register("torcher", TORCHER_MODE_TYPE);
        register("healer", HEALER_MODE_TYPE);
    }

    private static void register(String id, ModeType<?> modeType) {
        ModeManager.INSTANCE.register(ResourceLocation.fromNamespaceAndPath(MODID, id), modeType);
    }

}
