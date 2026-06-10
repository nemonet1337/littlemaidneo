package work.nemonet.littlemaidneo.data;

import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.data.recipes.*;
import net.minecraft.world.item.Items;
import work.nemonet.littlemaidneo.setup.ModRegistration;
import work.nemonet.littlemaidneo.tags.LMTags;

import java.util.concurrent.CompletableFuture;

public class LMRecipeProvider extends RecipeProvider {
    public LMRecipeProvider(HolderLookup.Provider registries, RecipeOutput output) {
        super(registries, output);
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

    public static class Runner extends RecipeProvider.Runner {
        public Runner(PackOutput output, CompletableFuture<HolderLookup.Provider> registries) {
            super(output, registries);
        }

        @Override
        protected RecipeProvider createRecipeProvider(HolderLookup.Provider registries, RecipeOutput output) {
            return new LMRecipeProvider(registries, output);
        }

        @Override
        public String getName() {
            return "Recipes";
        }
    }
}
