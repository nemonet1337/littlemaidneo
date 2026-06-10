package work.nemonet.littlemaidneo.client.util;

import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.Level;
import work.nemonet.littlemaidneo.client.screen.ModelSelectScreen;
import work.nemonet.littlemaidneo.entity.MultiModelEntity;
import work.nemonet.littlemaidneo.entity.LittleMaidEntity;
import work.nemonet.littlemaidneo.entity.util.TameableUtil;

import java.util.Optional;

public class ClientScreenHelper {
    public static void openModelSelectScreen(Level level, MultiModelEntity entity) {
        Minecraft.getInstance().setScreen(new ModelSelectScreen(
                Component.translatable("screen.littlemaidneo.model_select"), level, entity));
    }

    public static boolean shouldShowOwnerName(LittleMaidEntity maid) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null && TameableUtil.hasTameOwner(maid) && !TameableUtil.isTameOwner(maid, mc.player)) {
            if (mc.hitResult instanceof net.minecraft.world.phys.EntityHitResult hit && hit.getEntity() == maid) {
                return true;
            }
        }
        return false;
    }

    public static Optional<String> getOwnerNameForClient(LittleMaidEntity maid) {
        return TameableUtil.getTameOwnerUuid(maid).flatMap(uuid -> {
            Minecraft mc = Minecraft.getInstance();
            if (mc.player != null && !mc.player.getUUID().equals(uuid)) {
                var connection = mc.getConnection();
                if (connection != null) {
                    var info = connection.getPlayerInfo(uuid);
                    if (info != null) {
                        return Optional.of(info.getProfile().name());
                    }
                }
                var owner = maid.getOwner();
                if (owner != null) {
                    return Optional.of(owner.getName().getString());
                }
                return Optional.of("???");
            }
            return Optional.empty();
        });
    }
    public static void drawScrollingText(net.minecraft.client.gui.GuiGraphicsExtractor context, net.minecraft.client.gui.Font textRenderer, String text,
            int x, int y, int availableWidth, int color, boolean shadow) {
        drawScrollingText(context, textRenderer, Component.nullToEmpty(text), x, y, availableWidth, color, shadow);
    }

    public static void drawScrollingText(net.minecraft.client.gui.GuiGraphicsExtractor context, net.minecraft.client.gui.Font textRenderer, Component text,
            int x, int y, int availableWidth, int color, boolean shadow) {
        int textWidth = textRenderer.width(text);
        if (textWidth <= availableWidth) {
            context.text(textRenderer, text, x, y, color, shadow);
        } else {
            // 長すぎるテキストをスクロール表示
            double seconds = net.minecraft.util.Util.getMillis() / 1000.0;
            double scrollSpeed = 20.0;
            int displayWidth = availableWidth - 8;
            int scrollDistance = textWidth - displayWidth;
            double cycleTime = (scrollDistance + displayWidth) / scrollSpeed;
            double cyclePosition = (seconds % cycleTime) / cycleTime;

            int scrollOffset;
            if (cyclePosition < 0.8) {
                scrollOffset = (int) (cyclePosition / 0.8 * scrollDistance);
            } else {
                scrollOffset = scrollDistance;
            }

            context.enableScissor(x, y, x + displayWidth, y + textRenderer.lineHeight);
            context.text(textRenderer, text, x - scrollOffset, y, color, shadow);
            context.disableScissor();
        }
    }
}
