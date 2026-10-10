package work.nemonet.littlemaidneo.data;

import net.minecraft.advancements.Advancement;
import net.minecraft.data.recipes.*;
import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Recipe;
import work.nemonet.littlemaidneo.setup.ModRegistration;
import work.nemonet.littlemaidneo.tags.LMTags;

public class LMRecipeProvider extends RecipeProvider {
    public LMRecipeProvider(BootstrapContext<Recipe<?>> recipeOutput, BootstrapContext<Advancement> advancementOutput) {
        super(recipeOutput, advancementOutput);
    }

    @Override
    protected void buildRecipes() {
        shaped(RecipeCategory.MISC, ModRegistration.LITTLE_MAID_SPAWN_EGG_ITEM.get())
                .pattern("SGS")
                .pattern("CEC")
                .pattern("SGS")
                .define('S', LMTags.Items.MAIDS_SALARY)
                .define('C', LMTags.Items.MAIDS_EMPLOYABLE)
                .define('G', Items.GOLD_INGOT)
                .define('E', Items.EGG)
                .unlockedBy("sugar", has(LMTags.Items.MAIDS_SALARY))
                .unlockedBy("cake", has(LMTags.Items.MAIDS_EMPLOYABLE))
                .unlockedBy("gold_ingot", has(Items.GOLD_INGOT))
                .unlockedBy("egg", has(Items.EGG))
                .save(this.output);

        shaped(RecipeCategory.MISC, ModRegistration.SALARY_BOX_BLOCK_ITEM.get())
                .pattern("SSS")
                .pattern("SBS")
                .pattern("SSS")
                .define('S', LMTags.Items.MAIDS_SALARY)
                .define('B', Items.BARREL)
                .unlockedBy("sugar", has(LMTags.Items.MAIDS_SALARY))
                .unlockedBy("barrel", has(Items.BARREL))
                .save(this.output);
    }
}
