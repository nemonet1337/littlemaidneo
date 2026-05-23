package work.nemonet.littlemaidneo.util;

import net.minecraft.world.item.crafting.AbstractCookingRecipe;
import net.minecraft.world.item.crafting.RecipeType;

/**
 * Mixin Accessor
 */
public interface AbstractFurnaceAccessor {

    RecipeType<? extends AbstractCookingRecipe> getRecipeType_LM();

    boolean isBurningFire_LM();

}
