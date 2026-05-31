package work.nemonet.littlemaidneo.resource.holder;

import work.nemonet.littlemaidneo.resource.util.ResourceHelper;

import java.util.Map;
import java.util.Optional;

/**
 * サウンド/テクスチャ設定パックの 1 エントリを表す不変 DTO。
 * <p>
 * record コンポーネント:
 * <ul>
 *   <li>{@code packName}   — パックのルートフォルダ名</li>
 *   <li>{@code parentName} — サブフォルダ名（なければ空文字）</li>
 *   <li>{@code fileName}   — 拡張子なしファイル名</li>
 *   <li>{@code settings}   — ".cfg" から読んだ key=value マップ</li>
 * </ul>
 */
public record ConfigHolder(
        String packName,
        String parentName,
        String fileName,
        Map<String, String> settings
) {
    /** "{packName}.{parentName}.{fileName}" 形式の一意キー */
    public String getName() {
        return packName + "." + parentName + "." + fileName;
    }

    /** キーに対応する設定値を返す。 */
    public Optional<String> getParameter(String parameterName) {
        return Optional.ofNullable(settings.get(parameterName));
    }

    /**
     * サウンドファイル名の正規化済みキーを返す。
     * <p>
     * "{packName}:{path}" 形式ならそのまま返す。
     * そうでなければ ".cfg" の命名規則に従って "{packName}.{normalized}" を返す。
     */
    public Optional<String> getSoundFileName(String soundName) {
        Optional<String> optional = getParameter(soundName);
        if (optional.filter(s -> s.contains(":")).isPresent()) return optional;
        return optional
                .map(ResourceHelper::removeNameLastIndex)
                .map(fn -> {
                    int firstSplitter = fn.indexOf(".");
                    if (firstSplitter == -1) return "." + fn;
                    int lastSplitter = fn.lastIndexOf(".");
                    if (firstSplitter == lastSplitter) return fn;
                    int secondLastSplitter = fn.substring(0, lastSplitter).lastIndexOf(".");
                    return fn.substring(secondLastSplitter + 1);
                })
                .map(fn -> (packName + "." + fn).toLowerCase());
    }
}
