package work.nemonet.littlemaidneo.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import work.nemonet.littlemaidneo.config.LMNConfig;
import work.nemonet.littlemaidneo.resource.loader.LMFileLoader;
import work.nemonet.littlemaidneo.resource.manager.LMModelManager;
import work.nemonet.littlemaidneo.resource.manager.LMConfigManager;
import work.nemonet.littlemaidneo.entity.LittleMaidEntity;
import work.nemonet.littlemaidneo.entity.util.TameableUtil;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.entity.Entity;
import net.minecraft.server.level.ServerPlayer;
import java.util.List;
import java.util.Collection;

public class LMCommands {

    public static void register(RegisterCommandsEvent event) {
        CommandDispatcher<CommandSourceStack> dispatcher = event.getDispatcher();

        dispatcher.register(
            Commands.literal("littlemaidneo")
                .then(Commands.literal("reload")
                    .requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
                    .executes(LMCommands::executeReload)
                )
                .then(Commands.literal("models")
                    .then(Commands.literal("list")
                        .executes(LMCommands::executeModelsList)
                    )
                )
                .then(Commands.literal("maid")
                    .then(Commands.literal("count")
                        .executes(LMCommands::executeMaidCount)
                    )
                    .then(Commands.literal("tp")
                        .requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
                        .executes(LMCommands::executeMaidTp)
                    )
                    .then(Commands.literal("dismiss")
                        .requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
                        .executes(LMCommands::executeMaidDismiss)
                    )
                )
                .then(Commands.literal("config")
                    .then(Commands.literal("bake")
                        .requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
                        .executes(LMCommands::executeConfigBake)
                    )
                )
                .then(Commands.literal("debug")
                    .then(Commands.literal("dump")
                        .requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
                        .executes(LMCommands::executeDebugDump)
                    )
                )
        );

        // エイリアス /lmn も登録
        dispatcher.register(
            Commands.literal("lmn")
                .then(Commands.literal("reload")
                    .requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
                    .executes(LMCommands::executeReload)
                )
                .then(Commands.literal("models")
                    .then(Commands.literal("list")
                        .executes(LMCommands::executeModelsList)
                    )
                )
                .then(Commands.literal("maid")
                    .then(Commands.literal("count")
                        .executes(LMCommands::executeMaidCount)
                    )
                    .then(Commands.literal("tp")
                        .requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
                        .executes(LMCommands::executeMaidTp)
                    )
                    .then(Commands.literal("dismiss")
                        .requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
                        .executes(LMCommands::executeMaidDismiss)
                    )
                )
                .then(Commands.literal("config")
                    .then(Commands.literal("bake")
                        .requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
                        .executes(LMCommands::executeConfigBake)
                    )
                )
                .then(Commands.literal("debug")
                    .then(Commands.literal("dump")
                        .requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
                        .executes(LMCommands::executeDebugDump)
                    )
                )
        );
    }

    private static int executeReload(CommandContext<CommandSourceStack> context) {
        context.getSource().sendSystemMessage(Component.translatable("commands.littlemaidneo.reload.start"));
        try {
            LMFileLoader.INSTANCE.load();
            context.getSource().sendSystemMessage(Component.translatable("commands.littlemaidneo.reload.success"));
            return 1;
        } catch (Exception e) {
            context.getSource().sendFailure(Component.translatable("commands.littlemaidneo.reload.failure", e.getMessage()));
            return 0;
        }
    }

    private static int executeModelsList(CommandContext<CommandSourceStack> context) {
        Collection<String> modelNames = LMModelManager.INSTANCE.getModelNames();
        context.getSource().sendSystemMessage(Component.translatable("commands.littlemaidneo.models.list.count", modelNames.size()));
        for (String name : modelNames) {
            context.getSource().sendSystemMessage(Component.literal(" - " + name));
        }
        return modelNames.size();
    }

    private static int executeMaidCount(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        Entity executor = source.getEntity();
        if (executor == null) {
            source.sendFailure(Component.translatable("commands.littlemaidneo.executor.null"));
            return 0;
        }

        AABB box = executor.getBoundingBox().inflate(64.0);
        List<LittleMaidEntity> maids = source.getLevel().getEntitiesOfClass(LittleMaidEntity.class, box);
        source.sendSystemMessage(Component.translatable("commands.littlemaidneo.maid.count.nearby", maids.size(), 64));
        return maids.size();
    }

    private static int executeMaidTp(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        Entity executor = source.getEntity();
        if (!(executor instanceof ServerPlayer player)) {
            source.sendFailure(Component.translatable("commands.littlemaidneo.executor.not_player"));
            return 0;
        }

        AABB box = player.getBoundingBox().inflate(128.0);
        List<LittleMaidEntity> maids = player.level().getEntitiesOfClass(LittleMaidEntity.class, box);
        int tpCount = 0;
        for (LittleMaidEntity maid : maids) {
            if (maid.isTame() && TameableUtil.getTameOwnerUuid(maid).filter(uuid -> uuid.equals(player.getUUID())).isPresent()) {
                maid.teleportTo(player.getX(), player.getY(), player.getZ());
                tpCount++;
            }
        }
        source.sendSystemMessage(Component.translatable("commands.littlemaidneo.maid.tp.success", tpCount));
        return tpCount;
    }

    private static int executeMaidDismiss(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        Entity executor = source.getEntity();
        if (!(executor instanceof ServerPlayer player)) {
            source.sendFailure(Component.translatable("commands.littlemaidneo.executor.not_player"));
            return 0;
        }

        AABB box = player.getBoundingBox().inflate(16.0);
        List<LittleMaidEntity> maids = player.level().getEntitiesOfClass(LittleMaidEntity.class, box);
        int dismissCount = 0;
        for (LittleMaidEntity maid : maids) {
            if (maid.isTame() && TameableUtil.getTameOwnerUuid(maid).filter(uuid -> uuid.equals(player.getUUID())).isPresent()) {
                maid.setOwnerReference(null);
                maid.setTame(false, false);
                dismissCount++;
            }
        }
        source.sendSystemMessage(Component.translatable("commands.littlemaidneo.maid.dismiss.success", dismissCount));
        return dismissCount;
    }

    private static int executeDebugDump(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        Collection<String> modelNames = LMModelManager.INSTANCE.getModelNames();
        int soundPacksCount = LMConfigManager.INSTANCE.getAllConfig().size();

        source.sendSystemMessage(Component.literal("=== LittleMaidNeo Debug Dump ==="));
        source.sendSystemMessage(Component.literal("Models Loaded: " + modelNames.size()));
        source.sendSystemMessage(Component.literal("Sound Packs Loaded: " + soundPacksCount));
        return 1;
    }

    /**
     * サーバー/共通コンフィグを再 bake し、メモリ上の設定値を TOML と揃える。
     * SERVER 型コンフィグは NeoForge がクライアントへ同期する。
     */
    private static int executeConfigBake(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        try {
            // SERVER コンフィグをメモリへ再適用。クライアント同期は NeoForge の SERVER config 配信に委ねる
            LMNConfig.bake();
            source.sendSystemMessage(Component.translatable("commands.littlemaidneo.config.bake.success"));
            return 1;
        } catch (Exception e) {
            source.sendFailure(Component.translatable("commands.littlemaidneo.config.bake.failure", e.getMessage()));
            return 0;
        }
    }
}
