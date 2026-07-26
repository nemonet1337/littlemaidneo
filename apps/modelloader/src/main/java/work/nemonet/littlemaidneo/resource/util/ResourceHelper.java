package work.nemonet.littlemaidneo.resource.util;

import net.minecraft.resources.Identifier;
import work.nemonet.littlemaidneo.common.LMNLib;

import java.nio.file.Path;
import java.util.Optional;

public class ResourceHelper {

    protected static final String[] defNames = {
            "mob_littlemaid0.png", "mob_littlemaid1.png",
            "mob_littlemaid2.png", "mob_littlemaid3.png",
            "mob_littlemaid4.png", "mob_littlemaid5.png",
            "mob_littlemaid6.png", "mob_littlemaid7.png",
            "mob_littlemaid8.png", "mob_littlemaid9.png",
            "mob_littlemaida.png", "mob_littlemaidb.png",
            "mob_littlemaidc.png", "mob_littlemaidd.png",
            "mob_littlemaide.png", "mob_littlemaidf.png",
            "mob_littlemaidw.png",
            "mob_littlemaid_a00.png", "mob_littlemaid_a01.png"
    };
    private static final int OLD_WILD = 0x10;
    private static final int OLD_ARMOR_1 = 0x11;
    private static final int OLD_ARMOR_2 = 0x12;

    /**
     * ファイルセパレータを正規化する。アーカイブ内パスは既に '/' 区切りのため変更しない。
     * （命名結果は従来と不変。{@code replace("\\","/")} の共通化のみ）
     */
    private static String normalizePath(String path, boolean isArchive) {
        return isArchive ? path : path.replace("\\", "/");
    }

    /** Identifier 用にパス文字列を小文字化し許可文字以外を '-' へ置換する（命名結果は従来と不変）。 */
    private static String sanitize(String value) {
        return value.toLowerCase().replaceAll("[^a-z0-9/._\\-]", "-");
    }

    public static String getFileName(String path, boolean isArchive) {
        String name = normalizePath(path, isArchive);
        int lastSplitter = name.lastIndexOf("/");
        if (lastSplitter == -1) return name;
        return name.substring(lastSplitter + 1);
    }

    public static Optional<String> getTexturePackName(String path, boolean isArchive) {
        String name = normalizePath(path, isArchive);
        if (path.contains("/littlemaid/") || path.contains("littleMaid")) {
            int lmFolderIndex = path.lastIndexOf("/littlemaid/");
            if (lmFolderIndex == -1) lmFolderIndex = path.lastIndexOf("/littleMaid/");
            if (lmFolderIndex == -1) return getParentFolderName(path, isArchive);
            name = name.substring(lmFolderIndex + "/littlemaid/".length());
            int lastSplitter = name.lastIndexOf("/");
            if (lastSplitter == -1) return getParentFolderName(path, isArchive);
            return Optional.of(name.substring(0, lastSplitter).replace("/", "."));
        }
        return getParentFolderName(path, isArchive);
    }

    public static String getModelName(String textureName) {
        int lastSplitter = textureName.lastIndexOf("_");
        if (lastSplitter == -1) return "default";
        return textureName.substring(lastSplitter + 1);
    }

    public static Optional<String> getParentFolderName(String path, boolean isArchive) {
        String name = normalizePath(path, isArchive);
        int lastSplitter = name.lastIndexOf("/");
        if (lastSplitter == -1) return Optional.empty();
        name = name.substring(0, lastSplitter);
        lastSplitter = name.lastIndexOf("/");
        if (lastSplitter != -1) name = name.substring(lastSplitter + 1);
        return Optional.of(name);
    }

    public static int getIndex(String path) {
        int index = -1;
        for (int i = 0; i < defNames.length; i++) {
            if (path.endsWith(defNames[i])) {
                index = i;
                break;
            }
        }
        if (index == -1) {
            String name = path.toLowerCase();
            int lastDot = name.lastIndexOf(".");
            if (lastDot == -1) return -1;
            name = name.substring(0, lastDot);
            int lastSplitter = name.lastIndexOf("_");
            if (lastSplitter == -1) return -1;
            name = name.substring(lastSplitter + 1);
            try {
                index = Integer.decode("0x" + name);
            } catch (Exception e) {
                return -1;
            }
        }
        if (index == OLD_ARMOR_1) index = TextureIndexes.ARMOR_1_DAMAGED.getIndexMin();
        if (index == OLD_ARMOR_2) index = TextureIndexes.ARMOR_2_DAMAGED.getIndexMin();
        if (index == OLD_WILD) index = TextureIndexes.COLOR_WILD.getIndexMin() + TextureColors.BROWN.getIndex();
        return index;
    }

    public static String removeNameLastIndex(String fileName) {
        String removed = fileName;
        while (true) {
            if (removed.isEmpty()) break;
            int subLength = removed.length() - 1;
            String temp = removed.substring(subLength);
            try {
                Integer.valueOf(temp);
            } catch (NumberFormatException e) {
                break;
            }
            removed = removed.substring(0, subLength);
        }
        return removed;
    }

    public static String removeExtension(String fileName) {
        int lastSplitter = fileName.lastIndexOf(".");
        if (lastSplitter == -1) return fileName;
        return fileName.substring(0, lastSplitter);
    }

    public static Optional<String> getFirstParentName(String path, Path homePath, boolean isArchive) {
        if (isArchive) {
            String zipName = homePath.getFileName().toString();
            return Optional.of(zipName.substring(0, zipName.lastIndexOf(".")));
        } else {
            // フォルダ形式: OS セパレータを '/' に正規化してから先頭セグメントをパック名とする
            // （旧実装は indexOf("\\") 固定で Linux/macOS では常に empty になっていた）
            String name = normalizePath(path, false);
            if (name.startsWith("/")) {
                name = name.substring(1);
            }
            int firstSplitter = name.indexOf('/');
            if (firstSplitter == -1) return Optional.empty();
            return Optional.of(name.substring(0, firstSplitter));
        }
    }

    public static Identifier getLocation(String packName, String fileName) {
        return Identifier.fromNamespaceAndPath(LMNLib.MODID, sanitize(packName) + "/" + sanitize(fileName));
    }

    public static Identifier getLocation(String prefix, String packName, String fileName) {
        return Identifier.fromNamespaceAndPath(LMNLib.MODID,
                prefix + "/" + sanitize(packName) + "/" + sanitize(fileName));
    }
}
