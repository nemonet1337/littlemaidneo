package work.nemonet.littlemaidneo.data;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import net.minecraft.data.CachedOutput;
import net.minecraft.data.DataProvider;
import net.minecraft.data.PackOutput;
import work.nemonet.littlemaidneo.LittleMaidNeo;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public class LMModelProvider implements DataProvider {
    private final PackOutput packOutput;

    public LMModelProvider(PackOutput packOutput) {
        this.packOutput = packOutput;
    }

    @Override
    public CompletableFuture<?> run(CachedOutput cache) {
        Path assetsDir = this.packOutput.getOutputFolder().resolve("assets").resolve(LittleMaidNeo.MODID);
        List<CompletableFuture<?>> futures = new ArrayList<>();

        // 1. blockstates/salary_box.json
        futures.add(save(cache, assetsDir.resolve("blockstates").resolve("salary_box.json"),
                """
                {
                  "variants": {
                    "facing=down,open=false": {
                      "model": "littlemaidneo:block/salary_box",
                      "x": 180
                    },
                    "facing=down,open=true": {
                      "model": "littlemaidneo:block/salary_box_open",
                      "x": 180
                    },
                    "facing=east,open=false": {
                      "model": "littlemaidneo:block/salary_box",
                      "x": 90,
                      "y": 90
                    },
                    "facing=east,open=true": {
                      "model": "littlemaidneo:block/salary_box_open",
                      "x": 90,
                      "y": 90
                    },
                    "facing=north,open=false": {
                      "model": "littlemaidneo:block/salary_box",
                      "x": 90
                    },
                    "facing=north,open=true": {
                      "model": "littlemaidneo:block/salary_box_open",
                      "x": 90
                    },
                    "facing=south,open=false": {
                      "model": "littlemaidneo:block/salary_box",
                      "x": 90,
                      "y": 180
                    },
                    "facing=south,open=true": {
                      "model": "littlemaidneo:block/salary_box_open",
                      "x": 90,
                      "y": 180
                    },
                    "facing=up,open=false": {
                      "model": "littlemaidneo:block/salary_box"
                    },
                    "facing=up,open=true": {
                      "model": "littlemaidneo:block/salary_box_open"
                    },
                    "facing=west,open=false": {
                      "model": "littlemaidneo:block/salary_box",
                      "x": 90,
                      "y": 270
                    },
                    "facing=west,open=true": {
                      "model": "littlemaidneo:block/salary_box_open",
                      "x": 90,
                      "y": 270
                    }
                  }
                }
                """));

        // 2. models/block/salary_box.json
        futures.add(save(cache, assetsDir.resolve("models").resolve("block").resolve("salary_box.json"),
                """
                {
                  "parent": "block/cube_bottom_top",
                  "textures": {
                    "bottom": "minecraft:block/barrel_bottom",
                    "side": "minecraft:block/barrel_side",
                    "top": "minecraft:block/barrel_top"
                  }
                }
                """));

        // 3. models/block/salary_box_open.json
        futures.add(save(cache, assetsDir.resolve("models").resolve("block").resolve("salary_box_open.json"),
                """
                {
                  "parent": "block/cube_bottom_top",
                  "textures": {
                    "bottom": "minecraft:block/barrel_bottom",
                    "side": "minecraft:block/barrel_side",
                    "top": "minecraft:block/barrel_top_open"
                  }
                }
                """));

        // 5. models/item/salary_box.json
        futures.add(save(cache, assetsDir.resolve("models").resolve("item").resolve("salary_box.json"),
                """
                {
                  "parent": "littlemaidneo:block/salary_box"
                }
                """));

        // 5b. models/item/little_maid_spawn_egg.json（旧手書き assets から DataGen へ移行）
        futures.add(save(cache, assetsDir.resolve("models").resolve("item").resolve("little_maid_spawn_egg.json"),
                """
                {
                  "parent": "minecraft:item/generated",
                  "textures": {
                    "layer0": "littlemaidneo:item/spawn_egg",
                    "layer1": "littlemaidneo:item/spawn_egg_overlay"
                  }
                }
                """));

        // 6. items/little_maid_spawn_egg.json
        futures.add(save(cache, assetsDir.resolve("items").resolve("little_maid_spawn_egg.json"),
                """
                {
                  "model": {
                    "type": "minecraft:model",
                    "model": "littlemaidneo:item/little_maid_spawn_egg",
                    "tints": [
                      {
                        "type": "minecraft:constant",
                        "value": -1
                      },
                      {
                        "type": "minecraft:constant",
                        "value": -8339456
                      }
                    ]
                  }
                }
                """));

        // 7. items/salary_box.json
        futures.add(save(cache, assetsDir.resolve("items").resolve("salary_box.json"),
                """
                {
                  "model": {
                    "type": "minecraft:model",
                    "model": "littlemaidneo:item/salary_box"
                  }
                }
                """));

        return CompletableFuture.allOf(futures.toArray(CompletableFuture[]::new));
    }

    private CompletableFuture<?> save(CachedOutput cache, Path path, String jsonContent) {
        JsonElement json = JsonParser.parseString(jsonContent);
        return DataProvider.saveStable(cache, json, path);
    }

    @Override
    public String getName() {
        return "Model Definitions - " + LittleMaidNeo.MODID;
    }
}
