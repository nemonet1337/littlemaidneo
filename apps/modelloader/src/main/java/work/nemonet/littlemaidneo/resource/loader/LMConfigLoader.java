package work.nemonet.littlemaidneo.resource.loader;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import work.nemonet.littlemaidneo.config.LMNModelConfig;
import work.nemonet.littlemaidneo.resource.manager.LMConfigManager;
import work.nemonet.littlemaidneo.resource.util.ResourceHelper;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.Charset;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Stream;

public class LMConfigLoader implements LMLoader {
    private static final Logger LOGGER = LogManager.getLogger();
    private static final Charset MS932 = Charset.forName("MS932");
    private final LMConfigManager configManager;

    public LMConfigLoader(LMConfigManager configManager) {
        this.configManager = configManager;
    }

    @Override
    public boolean canLoad(String path, Path folderPath, InputStream inputStream, boolean isArchive) {
        return path.endsWith(".cfg") && ResourceHelper.getFirstParentName(path, folderPath, isArchive).isPresent();
    }

    @Override
    public Runnable parse(String path, Path folderPath, InputStream inputStream, boolean isArchive) {
        // ストリームの読み取りは parse 内（並列フェーズ・ストリーム生存中）で完了させる。
        Map<String, String> settings = new HashMap<>();
        getTextStream(inputStream).forEach(s -> addSettings(settings, s));
        String packName = ResourceHelper.getFirstParentName(path, folderPath, isArchive).orElse("");
        String parentName = ResourceHelper.getParentFolderName(path, isArchive).orElse("");
        String fileName = ResourceHelper.removeExtension(ResourceHelper.getFileName(path, isArchive));
        return () -> {
            configManager.addConfig(packName, parentName, fileName, settings);
            if (LMNModelConfig.isDebugMode())
                LOGGER.debug("Loaded Config : {}.{}.{} : Total {}", packName, parentName, fileName, settings.size());
        };
    }

    public void addSettings(Map<String, String> settings, String text) {
        int firstComment = text.indexOf('#');
        if (firstComment != -1) text = text.substring(0, firstComment);
        int firstSplitter = text.indexOf('=');
        if (firstSplitter == -1) return;
        String firstText = text.substring(0, firstSplitter);
        String secondText = text.substring(firstSplitter + 1);
        settings.put(firstText.toLowerCase(), secondText.toLowerCase());
    }

    /**
     * .cfg をテキスト行ストリームにする。
     * 旧パックの SJIS コメント向けに、UTF-8 として不正なバイト列なら MS932 にフォールバックする。
     * キー/値は ASCII 前提のため、保護コア B のキー解決挙動は変えない。
     */
    public Stream<String> getTextStream(InputStream inputStream) {
        try {
            byte[] bytes = inputStream.readAllBytes();
            Charset charset = detectCharset(bytes);
            return new BufferedReader(new InputStreamReader(new ByteArrayInputStream(bytes), charset)).lines();
        } catch (Exception e) {
            LOGGER.warn("cfg の読み込みに失敗しました。空として扱います。", e);
            return Stream.empty();
        }
    }

    private static Charset detectCharset(byte[] bytes) {
        try {
            StandardCharsets.UTF_8.newDecoder()
                    .onMalformedInput(CodingErrorAction.REPORT)
                    .onUnmappableCharacter(CodingErrorAction.REPORT)
                    .decode(ByteBuffer.wrap(bytes));
            return StandardCharsets.UTF_8;
        } catch (CharacterCodingException e) {
            return MS932;
        }
    }
}
