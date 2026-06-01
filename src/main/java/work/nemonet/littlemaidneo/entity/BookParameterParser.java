package work.nemonet.littlemaidneo.entity;

import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import work.nemonet.littlemaidneo.entity.util.MovingMode;
import work.nemonet.littlemaidneo.entity.util.TameableUtil;

/**
 * 書き込み可能な本のテキスト（{@code key=value} 形式・{@code #} 始まりはコメント）から
 * メイドさんのパラメータを適用する（R-3）。
 *
 * <p>旧 {@code LittleMaidEntity.applyParametersFromBook}/{@code applyParameter} から抽出。挙動は不変。
 */
public final class BookParameterParser {

    private BookParameterParser() {
    }

    public static void apply(LittleMaidEntity maid, ItemStack stack, Player player) {
        var content = stack.get(DataComponents.WRITABLE_BOOK_CONTENT);
        if (content == null) return;

        for (var page : content.pages()) {
            String text = page.raw();
            for (String line : text.split("\\r?\\n")) {
                line = line.trim();
                if (line.isEmpty() || line.startsWith("#")) continue;
                int eqIdx = line.indexOf('=');
                if (eqIdx != -1) {
                    String key = line.substring(0, eqIdx).trim().toLowerCase();
                    String value = line.substring(eqIdx + 1).trim();
                    applyParameter(maid, key, value);
                }
            }
        }
    }

    private static void applyParameter(LittleMaidEntity maid, String key, String value) {
        switch (key) {
            case "name" -> {
                maid.setCustomName(Component.literal(value));
                maid.setCustomNameVisible(true);
            }
            case "moving" -> {
                try {
                    MovingMode mode = MovingMode.valueOf(value.toUpperCase());
                    maid.setMovingMode(mode);
                    if (mode == MovingMode.FREEDOM) {
                        maid.setFreedomPos(maid.blockPosition());
                    }
                } catch (IllegalArgumentException e) {
                    // 無効値は無視
                }
            }
            case "bloodsuck" -> maid.setBloodSuck(Boolean.parseBoolean(value));
            case "wait" -> {
                boolean wait = Boolean.parseBoolean(value);
                TameableUtil.setWait(maid, wait);
                maid.setOrderedToSit(wait);
            }
        }
    }
}
