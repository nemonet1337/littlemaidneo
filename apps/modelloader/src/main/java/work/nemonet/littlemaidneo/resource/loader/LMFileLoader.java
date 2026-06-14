package work.nemonet.littlemaidneo.resource.loader;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.nio.charset.MalformedInputException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipException;
import java.util.zip.ZipInputStream;

public class LMFileLoader {
    public static final LMFileLoader INSTANCE = new LMFileLoader();
    private static final Logger LOGGER = LogManager.getLogger();
    private final ArrayList<LMLoader> loaders = new ArrayList<>();
    private final ArrayList<Path> folderPaths = new ArrayList<>();

    public void addLoader(LMLoader loader) {
        loaders.add(loader);
    }

    public void addLoadFolderPath(Path path) {
        if (Files.notExists(path)) {
            try {
                Files.createDirectory(path);
            } catch (Exception e) {
                e.printStackTrace();
                return;
            }
        }
        folderPaths.add(path);
    }

    public ArrayList<Path> getFolderPaths() {
        return folderPaths;
    }

    /**
     * 外部パックのロードを 3 フェーズで実行する。
     * <ol>
     *   <li>収集: 全フォルダを走査しトップレベルファイルを安定順で列挙（単一スレッド）。</li>
     *   <li>解析: 各ファイルを仮想スレッドで並列にパース・構築し、登録アクション({@link Runnable})を得る。
     *       この間 Manager（共有 Map）には触れない。アーカイブは 1 タスク内でエントリを逐次処理する。</li>
     *   <li>登録: 収集順（=従来の走査順）に登録アクションを単一スレッドで実行する。決定性とスレッド安全を両立。</li>
     * </ol>
     */
    public void load() {
        long start = System.nanoTime();
        LOGGER.debug("Loading start");
        List<FileUnit> units = collectUnits();
        List<Runnable> registrations = parseInParallel(units);
        registrations.forEach(this::runRegistration);
        long end = System.nanoTime();
        LOGGER.debug("Loading end : {}ms", (end - start) / (1000D * 1000D));
    }

    /** 並列タスクの単位（トップレベルの通常ファイル or アーカイブ）。 */
    private record FileUnit(Path folderPath, Path path, boolean isArchive) {}

    private List<FileUnit> collectUnits() {
        List<FileUnit> units = new ArrayList<>();
        folderPaths.forEach(folderPath -> {
            try {
                if (Files.notExists(folderPath)) Files.createDirectory(folderPath);
                try (Stream<Path> stream = Files.walk(folderPath)) {
                    stream.filter(path -> !Files.isDirectory(path))
                            .forEach(path -> units.add(new FileUnit(folderPath, path, isArchive(path))));
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
        return units;
    }

    private List<Runnable> parseInParallel(List<FileUnit> units) {
        List<Future<List<Runnable>>> futures = new ArrayList<>(units.size());
        try (ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor()) {
            for (FileUnit unit : units) {
                Callable<List<Runnable>> task = () -> parseUnit(unit);
                futures.add(executor.submit(task));
            }
        } // close() が全タスクの完了を待機する
        // futures は投入順（=収集順）。get() は完了済みのため即時返る。
        List<Runnable> registrations = new ArrayList<>();
        for (Future<List<Runnable>> future : futures) {
            try {
                registrations.addAll(future.get());
            } catch (Exception e) {
                LOGGER.error("ロードタスクが失敗しました。", e);
            }
        }
        return registrations;
    }

    private List<Runnable> parseUnit(FileUnit unit) {
        if (unit.isArchive()) {
            return parseArchiveWithRetry(unit.folderPath(), unit.path());
        }
        return parseFile(unit.folderPath(), unit.path());
    }

    private List<Runnable> parseFile(Path folderPath, Path path) {
        List<Runnable> registrations = new ArrayList<>();
        String relPath = path.toString().replace(folderPath.toString(), "");
        try (InputStream inputStream = Files.newInputStream(path)) {
            collectParses(relPath, folderPath, inputStream, false, registrations);
        } catch (Exception e) {
            LOGGER.error("Error! : {} : {}", e.getMessage(), path);
        }
        return registrations;
    }

    private List<Runnable> parseArchiveWithRetry(Path folderPath, Path path) {
        List<Runnable> registrations = parseArchive(folderPath, path, StandardCharsets.UTF_8);
        if (registrations == null && System.getProperty("os.name").toLowerCase().contains("win")) {
            LOGGER.info("MS932でリトライします。 : {}", path);
            registrations = parseArchive(folderPath, path, Charset.forName("MS932"));
            if (registrations != null) {
                LOGGER.info("読み込みに成功。");
            } else {
                LOGGER.error("読み込みに失敗。");
            }
        }
        return registrations != null ? registrations : List.of();
    }

    /** アーカイブ内エントリを 1 ストリームで逐次解析する（ZIP 同士は呼び出し元で並列）。失敗時は {@code null}。 */
    @Nullable
    private List<Runnable> parseArchive(Path folderPath, Path path, Charset charset) {
        List<Runnable> registrations = new ArrayList<>();
        try (ZipInputStream zipStream = new ZipInputStream(Files.newInputStream(path), charset)) {
            ZipEntry entry;
            while ((entry = zipStream.getNextEntry()) != null) {
                collectParses(entry.getName(), path, zipStream, true, registrations);
            }
        } catch (ZipException e) {
            LOGGER.error("Zipの読み込み中にエラーが発生。 : {}", path);
            return null;
        } catch (IllegalArgumentException e) {
            if (e.getCause() instanceof MalformedInputException) {
                LOGGER.error("Zip内のファイル名に日本語などが入っている可能性があります。 : {}", path);
            } else {
                LOGGER.error("不明なエラーによりZipが読み込めません。 : {}", path);
            }
            return null;
        } catch (Exception e) {
            LOGGER.error("不明なエラーによりZipが読み込めません。 : {}", path);
            return null;
        }
        return registrations;
    }

    /**
     * 対象を読み込めるローダの {@code parse} を実行し、登録アクションを集める。
     * {@code canLoad} は相互排他のため 1 ファイルにつき高々 1 ローダが解析する。
     * 1 ローダの解析失敗が他ファイル・他ローダを止めないよう個別に握りつぶす（従来の applyLoaders の例外無保護を是正）。
     */
    private void collectParses(String name, Path basePath, InputStream stream, boolean isArchive,
                               List<Runnable> registrations) {
        for (LMLoader loader : loaders) {
            if (!loader.canLoad(name, basePath, stream, isArchive)) continue;
            try {
                Runnable registration = loader.parse(name, basePath, stream, isArchive);
                if (registration != null) registrations.add(registration);
            } catch (Exception e) {
                LOGGER.error("ローダの解析に失敗しました : {}", name, e);
            }
        }
    }

    private void runRegistration(Runnable registration) {
        try {
            registration.run();
        } catch (Exception e) {
            LOGGER.error("登録処理に失敗しました。", e);
        }
    }

    public boolean isArchive(Path path) {
        return path.toString().endsWith("zip") || path.toString().endsWith("jar");
    }
}
