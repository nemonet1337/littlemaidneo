package work.nemonet.littlemaidneo.network;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.OwnableEntity;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import work.nemonet.littlemaidneo.entity.LittleMaidEntity;
import work.nemonet.littlemaidneo.entity.util.TameableUtil;

import java.util.function.BiConsumer;

public class PayloadHandlers {

    /**
     * 指定されたIDのエンティティが LittleMaidEntity かつ、送信者がその所有者（テイム済み）である場合にのみ処理を実行する。
     */
    public static void onOwnedMaid(IPayloadContext context, int entityId, BiConsumer<ServerPlayer, LittleMaidEntity> handler) {
        context.enqueueWork(() -> {
            ServerPlayer player = (ServerPlayer) context.player();
            Entity entity = player.level().getEntity(entityId);
            if (entity instanceof LittleMaidEntity maid) {
                // 所有者チェック
                if (TameableUtil.getTameOwnerUuid(maid)
                        .filter(ownerId -> ownerId.equals(player.getUUID()))
                        .isPresent()) {
                    handler.accept(player, maid);
                }
            }
        });
    }

    /**
     * 指定された型 E のエンティティが存在する場合に、送信者情報と共に処理を実行する。
     * 所有者チェック等の詳細な条件は呼び出し元のラムダで行う。
     */
    public static <E extends Entity> void resolveEntity(IPayloadContext context, int entityId, Class<E> entityClass, BiConsumer<ServerPlayer, E> handler) {
        context.enqueueWork(() -> {
            ServerPlayer player = (ServerPlayer) context.player();
            Entity entity = player.level().getEntity(entityId);
            if (entityClass.isInstance(entity)) {
                handler.accept(player, entityClass.cast(entity));
            }
        });
    }

    /**
     * テイム可能エンティティについて「送信者が所有者でない（拒否すべき）」とき true。
     * Ownable でないエンティティは常に false（許可）。
     * 所有者が未設定の Ownable も true（拒否）— 未所有のメイドさんを他人が操作できないようにする。
     */
    public static boolean isNotOwner(ServerPlayer player, Entity entity) {
        if (!(entity instanceof OwnableEntity ownable)) {
            return false;
        }
        return TameableUtil.getTameOwnerUuid(ownable)
                .filter(ownerId -> ownerId.equals(player.getUUID()))
                .isEmpty();
    }
}
