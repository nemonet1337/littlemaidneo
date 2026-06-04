package work.nemonet.littlemaidneo.resource.loader;

import org.jetbrains.annotations.Nullable;

import java.io.InputStream;
import java.nio.file.Path;

public interface LMLoader {

    boolean canLoad(String path, Path folderPath, InputStream inputStream, boolean isArchive);

    /**
     * 重い解析・構築（バイトコード変換・インスタンス化・テキスト解析など）をワーカースレッドで実行し、
     * 共有 Manager への登録だけを行う {@link Runnable}（登録アクション）を返す。
     *
     * <p>登録アクションは {@code LMFileLoader} が単一スレッドで収集順（=従来の走査順）に実行するため、
     * {@code parse} 自身は共有状態（各 Manager の Map など）を変更してはならない。登録対象が無い場合は
     * {@code null} を返してよい。
     *
     * <p>注意: 返した {@code Runnable} は {@code inputStream} がクローズされた後に実行され得るため、
     * ストリームからの読み取りは必ず {@code parse} 内で完了させること。
     */
    @Nullable
    Runnable parse(String path, Path folderPath, InputStream inputStream, boolean isArchive);

    /**
     * 解析と登録を即時に行う逐次実行用 API。{@code parse} の結果をその場で実行する。
     * 並列ロードを行わない呼び出し元との後方互換のために残す。
     */
    default void load(String path, Path folderPath, InputStream inputStream, boolean isArchive) {
        Runnable registration = parse(path, folderPath, inputStream, isArchive);
        if (registration != null) registration.run();
    }
}
