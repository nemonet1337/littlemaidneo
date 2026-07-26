package work.nemonet.littlemaidneo.event;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.ItemTags;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.OwnableEntity;
import net.minecraft.world.item.FireChargeItem;
import net.minecraft.world.item.FlintAndSteelItem;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.CandleCakeBlock;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.level.storage.TagValueOutput;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.entity.player.PlayerWakeUpEvent;
import net.neoforged.neoforge.event.entity.player.UseItemOnBlockEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import work.nemonet.littlemaidneo.entity.LittleMaidEntity;
import work.nemonet.littlemaidneo.entity.compound.SoundPlayable;
import work.nemonet.littlemaidneo.entity.util.TameableUtil;
import work.nemonet.littlemaidneo.resource.util.LMSounds;
import work.nemonet.littlemaidneo.setup.ModRegistration;
import work.nemonet.littlemaidneo.tags.LMTags;
import work.nemonet.littlemaidneo.world.WorldMaidSoulState;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Stream;

/**
 * プレイヤー関連のイベント（リスポーン、睡眠、起床、セーブ、ティック、および復活儀式）を処理するイベントハンドラ。
 * MixinServerPlayerEntity および MixinCandleCakeBlock からの脱 Mixin 移植先。
 */
public final class PlayerEventHandler {
    private static final Map<UUID, Boolean> sleepingPlayers = new ConcurrentHashMap<>();

    private PlayerEventHandler() {}

    public static void register(net.neoforged.bus.api.IEventBus eventBus) {
        eventBus.addListener(PlayerEventHandler::onPlayerClone);
        eventBus.addListener(PlayerEventHandler::onPlayerWakeUp);
        eventBus.addListener(PlayerEventHandler::onPlayerTick);
        eventBus.addListener(PlayerEventHandler::onPlayerSave);
        eventBus.addListener(PlayerEventHandler::onUseItemOnBlock);
        eventBus.addListener(PlayerEventHandler::onPlayerLoggedOut);
    }

    private static void onPlayerClone(PlayerEvent.Clone event) {
        if (event.getEntity() instanceof ServerPlayer newPlayer && event.getOriginal() instanceof ServerPlayer oldPlayer) {
            // ターゲットタグ
            var thisTarget = newPlayer.getData(ModRegistration.TARGET_TAG_ATTACHMENT.get());
            var oldTarget = oldPlayer.getData(ModRegistration.TARGET_TAG_ATTACHMENT.get());
            thisTarget.getTargetTagsSync().syncFrom(oldTarget.getTargetTagsSync());

            // メイドさん管理
            migrateWorldMaidSoulState(newPlayer);
            var thisMaid = newPlayer.getData(ModRegistration.MAID_MANAGER_ATTACHMENT.get());
            var oldMaid = oldPlayer.getData(ModRegistration.MAID_MANAGER_ATTACHMENT.get());
            thisMaid.checkMaidUnload();
            TagValueOutput output = TagValueOutput.createWithContext(ProblemReporter.DISCARDING, newPlayer.registryAccess());
            oldMaid.writeMaidManager(output);
            thisMaid.readMaidManager(TagValueInput.create(ProblemReporter.DISCARDING, newPlayer.registryAccess(), output.buildResult()));
        }
    }

    private static void onPlayerWakeUp(PlayerWakeUpEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            if (!event.wakeImmediately() && !event.updateLevel()) {
                getAroundTamedSoundPlayable(player)
                        .forEach(s -> s.play(LMSounds.GOOD_MORNING));
            }
        }
    }

    private static void onPlayerTick(PlayerTickEvent.Post event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            boolean isSleeping = player.isSleeping();
            boolean wasSleeping = sleepingPlayers.getOrDefault(player.getUUID(), false);

            if (isSleeping && !wasSleeping) {
                // 睡眠に入った瞬間におやすみボイスを流す
                getAroundTamedSoundPlayable(player)
                        .forEach(e -> e.play(LMSounds.GOOD_NIGHT));
            }
            sleepingPlayers.put(player.getUUID(), isSleeping);

            if (player.getRandom().nextInt(20) == 0) {
                player.getData(ModRegistration.MAID_MANAGER_ATTACHMENT.get()).checkMaidUnload();
            }
        }
    }

    private static void onPlayerSave(PlayerEvent.SaveToFile event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            player.getData(ModRegistration.MAID_MANAGER_ATTACHMENT.get()).checkMaidUnload();
        }
    }

    private static void onPlayerLoggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            sleepingPlayers.remove(player.getUUID());
            // ログアウト時に entityId を落としておき、再ログイン後の stale ID を防ぐ
            player.getData(ModRegistration.MAID_MANAGER_ATTACHMENT.get()).checkMaidUnload();
        }
    }

    private static void onUseItemOnBlock(UseItemOnBlockEvent event) {
        var context = event.getUseOnContext();
        var world = context.getLevel();
        var pos = context.getClickedPos();
        var player = event.getPlayer();
        var stack = context.getItemInHand();
        var state = world.getBlockState(pos);

        if (state.getBlock() instanceof CandleCakeBlock &&
                (stack.getItem() instanceof FlintAndSteelItem
                        || stack.getItem() instanceof FireChargeItem
                        || stack.is(ItemTags.CREEPER_IGNITERS))
                && CandleCakeBlock.canLight(state)
                && getAroundAlterComponentBlocks(world, pos) >= 4
                && world instanceof ServerLevel serverWorld) {
            if (LittleMaidEntity.resurrectionMaid(serverWorld, pos, player)) {
                event.setCancellationResult(InteractionResult.SUCCESS);
                event.setCanceled(true);
            }
        }
    }

    private static int getAroundAlterComponentBlocks(Level world, BlockPos center) {
        int num = 0;
        for (int i = 0; i < 9; i++) {
            if (i == 4) {
                continue;
            }
            var blockState = world.getBlockState(center.offset((i % 3) - 1, 0, (i / 3) - 1));
            if (blockState.is(LMTags.Blocks.MAID_ALTER_COMPONENT_BLOCKS)) {
                num++;
            }
        }
        return num;
    }

    private static Stream<SoundPlayable> getAroundTamedSoundPlayable(ServerPlayer player) {
        return player.level().getEntities(player, player.getBoundingBox().inflate(8),
                        e -> e instanceof OwnableEntity tameable
                                && TameableUtil.getTameOwnerUuid(tameable)
                                .filter(id -> id.equals(player.getUUID()))
                                .isPresent() && e instanceof SoundPlayable
                ).stream()
                .map(e -> (SoundPlayable) e)
                .filter(s -> !(s instanceof LivingEntity)
                        || (((LivingEntity) s).getMainHandItem().getItem() == Items.CLOCK
                        || ((LivingEntity) s).getOffhandItem().getItem() == Items.CLOCK)
                );
    }

    private static void migrateWorldMaidSoulState(ServerPlayer player) {
        WorldMaidSoulState worldMaidSoulState = WorldMaidSoulState.getWorldMaidSoulState(player.level());
        var attachment = player.getData(ModRegistration.MAID_MANAGER_ATTACHMENT.get());
        worldMaidSoulState.get(player.getUUID())
                .forEach(attachment::registerMaid);
        worldMaidSoulState.remove(player.getUUID());
    }
}
