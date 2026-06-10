package work.nemonet.littlemaidneo.common;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * LittleMaidNeo (LMN) の全モジュール共通の基盤定数。
 * メインクラス {@code LittleMaidNeo}（mods モジュール）へ依存できない
 * common / modelloader モジュールからは、必ずこちらを参照する。
 */
public final class LMNLib {
    public static final String MODID = "littlemaidneo";
    public static final Logger LOGGER = LogManager.getLogger(MODID);

    private LMNLib() {
    }
}
