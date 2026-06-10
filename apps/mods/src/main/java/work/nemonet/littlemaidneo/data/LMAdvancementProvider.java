package work.nemonet.littlemaidneo.data;

import net.minecraft.advancements.Advancement;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.advancements.AdvancementType;
import net.minecraft.advancements.AdvancementRequirements;
import net.minecraft.advancements.criterion.InventoryChangeTrigger;
import net.minecraft.advancements.criterion.ItemPredicate;
import net.minecraft.advancements.criterion.RecipeUnlockedTrigger;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.HolderGetter;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.PackOutput;
import net.minecraft.data.advancements.AdvancementSubProvider;
import net.minecraft.data.advancements.AdvancementProvider;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import work.nemonet.littlemaidneo.LittleMaidNeo;
import work.nemonet.littlemaidneo.advancement.criterion.ContractMaidCriterion;
import work.nemonet.littlemaidneo.advancement.criterion.LMNCriteria;
import work.nemonet.littlemaidneo.advancement.criterion.ResurrectMaidCriterion;
import work.nemonet.littlemaidneo.tags.LMTags;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

public class LMAdvancementProvider {
    public static AdvancementProvider create(PackOutput output, CompletableFuture<HolderLookup.Provider> lookupProvider) {
        return new AdvancementProvider(output, lookupProvider, List.of(new SubProvider()));
    }

    public static class SubProvider implements AdvancementSubProvider {
        @Override
        public void generate(HolderLookup.Provider registries, Consumer<AdvancementHolder> saver) {
            HolderGetter<Item> itemLookup = registries.lookupOrThrow(Registries.ITEM);

            AdvancementHolder contractMaid = Advancement.Builder.advancement()
                    .parent(Identifier.parse("minecraft:husbandry/root"))
                    .display(
                            Items.CAKE,
                            Component.translatable("advancements.husbandry.contract_maid.title"),
                            Component.translatable("advancements.husbandry.contract_maid.description"),
                            null,
                            AdvancementType.TASK,
                            true,
                            true,
                            false
                    )
                    .addCriterion("contracted_maid", LMNCriteria.CONTRACT_MAID.createCriterion(new ContractMaidCriterion.TriggerInstance(Optional.empty(), Optional.empty())))
                    .save(saver, LittleMaidNeo.MODID + ":husbandry/contract_maid");

            AdvancementHolder resurrectMaid = Advancement.Builder.advancement()
                    .parent(contractMaid)
                    .display(
                            Items.CAKE,
                            Component.translatable("advancements.husbandry.resurrect_maid.title"),
                            Component.translatable("advancements.husbandry.resurrect_maid.description"),
                            null,
                            AdvancementType.TASK,
                            true,
                            true,
                            false
                    )
                    .addCriterion("resurrected_maid", LMNCriteria.RESURRECT_MAID.createCriterion(new ResurrectMaidCriterion.TriggerInstance(Optional.empty(), Optional.empty())))
                    .save(saver, LittleMaidNeo.MODID + ":husbandry/resurrect_maid");

            Advancement.Builder.advancement()
                    .parent(Identifier.parse("minecraft:recipes/root"))
                    .addCriterion("sugar", InventoryChangeTrigger.TriggerInstance.hasItems(
                            ItemPredicate.Builder.item().of(itemLookup, LMTags.Items.MAIDS_SALARY).build()
                    ))
                    .addCriterion("cake", InventoryChangeTrigger.TriggerInstance.hasItems(
                            ItemPredicate.Builder.item().of(itemLookup, LMTags.Items.MAIDS_EMPLOYABLE).build()
                    ))
                    .addCriterion("gold_ingot", InventoryChangeTrigger.TriggerInstance.hasItems(
                            Items.GOLD_INGOT
                    ))
                    .addCriterion("egg", InventoryChangeTrigger.TriggerInstance.hasItems(
                            Items.EGG
                    ))
                    .addCriterion("has_the_recipe", RecipeUnlockedTrigger.unlocked(
                            ResourceKey.create(Registries.RECIPE, Identifier.fromNamespaceAndPath(LittleMaidNeo.MODID, "little_maid_spawn_egg"))
                    ))
                    .requirements(AdvancementRequirements.Strategy.OR)
                    .rewards(net.minecraft.advancements.AdvancementRewards.Builder.recipe(
                            ResourceKey.create(Registries.RECIPE, Identifier.fromNamespaceAndPath(LittleMaidNeo.MODID, "little_maid_spawn_egg"))
                    ))
                    .save(saver, LittleMaidNeo.MODID + ":recipes/little_maid_spawn_egg");

            Advancement.Builder.advancement()
                    .parent(Identifier.parse("minecraft:recipes/root"))
                    .addCriterion("sugar", InventoryChangeTrigger.TriggerInstance.hasItems(
                            ItemPredicate.Builder.item().of(itemLookup, Items.SUGAR).build()
                    ))
                    .addCriterion("barrel", InventoryChangeTrigger.TriggerInstance.hasItems(
                            Items.BARREL
                    ))
                    .addCriterion("has_the_recipe", RecipeUnlockedTrigger.unlocked(
                            ResourceKey.create(Registries.RECIPE, Identifier.fromNamespaceAndPath(LittleMaidNeo.MODID, "salary_box"))
                    ))
                    .requirements(AdvancementRequirements.Strategy.OR)
                    .rewards(net.minecraft.advancements.AdvancementRewards.Builder.recipe(
                            ResourceKey.create(Registries.RECIPE, Identifier.fromNamespaceAndPath(LittleMaidNeo.MODID, "salary_box"))
                    ))
                    .save(saver, LittleMaidNeo.MODID + ":recipes/salary_box");
        }
    }
}
