package net.sistr.littlemaidrebirth.setup;

import dev.architectury.registry.level.biome.BiomeModifications;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.MobSpawnSettings;
import net.sistr.littlemaidrebirth.LMRBMod;
import net.sistr.littlemaidrebirth.api.mode.Modes;

import java.util.List;

public class ModSetup {

    public static void init() {
        // ネットワーク登録は各プラットフォームのイベントハンドラで行う
        // Fabric: ModInitializer.onInitialize()
        // NeoForge: Architecturyが自動的に行う

        if (LMRBMod.getConfig().spawn.canNaturalSpawn) {
            registerSpawnSettingLM();
        }

        Modes.init();
    }

    private static void registerSpawnSettingLM() {
        // TODO メイドさんのスポーン設定容易化
        var spawnBiomeTags = LMRBMod.getConfig().spawn.maidSpawnBiomeTags
                .stream()
                .map(ResourceLocation::tryParse)
                .filter(java.util.Objects::nonNull)
                .map(id -> TagKey.create(Registries.BIOME, id))
                .toList();
        var spawnExcludeBiomeTags = LMRBMod.getConfig().spawn.maidSpawnExcludeBiomeTags
                .stream()
                .map(ResourceLocation::tryParse)
                .filter(java.util.Objects::nonNull)
                .map(id -> TagKey.create(Registries.BIOME, id))
                .toList();
        BiomeModifications.addProperties((context) -> canSpawnBiome(context, spawnBiomeTags, spawnExcludeBiomeTags),
                (context, mutable) -> mutable.getSpawnProperties()
                        .addSpawn(Registration.LITTLE_MAID_MOB.get().getCategory(),
                                new MobSpawnSettings.SpawnerData(Registration.LITTLE_MAID_MOB.get(),
                                        LMRBMod.getConfig().spawn.spawnWeight,
                                        LMRBMod.getConfig().spawn.minSpawnGroupSize,
                                        LMRBMod.getConfig().spawn.maxSpawnGroupSize)));
    }

    private static boolean canSpawnBiome(BiomeModifications.BiomeContext context,
            List<TagKey<Biome>> spawnBiomeTags,
            List<TagKey<Biome>> spawnExcludeBiomeTags) {
        for (TagKey<Biome> biomeTag : spawnBiomeTags) {
            if (context.hasTag(biomeTag)) {
                for (TagKey<Biome> excludeBiomeTag : spawnExcludeBiomeTags) {
                    if (context.hasTag(excludeBiomeTag)) {
                        return false;
                    }
                }
                return true;
            }
        }
        return false;
    }

}
