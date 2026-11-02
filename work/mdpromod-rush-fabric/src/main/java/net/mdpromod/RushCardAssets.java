package net.mdpromod;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 * Reads Rush card art directly from MDPro3 .ypk archives.
 *
 * <p>The official expansion packs are large image containers. Keeping them
 * external avoids inflating the mod jar by several hundred megabytes.
 */
final class RushCardAssets {
    private static final String PROPERTY = "mdpromod.rushExpansions";
    private static final Map<Path, ZipFile> OPEN_ARCHIVES = new HashMap<>();
    private static List<Path> archives = List.of();
    private static boolean warnedMissing;

    private RushCardAssets() {
    }

    static synchronized BufferedImage read(int code) {
        BufferedImage extracted = readExtracted(code);
        if (extracted != null) return extracted;
        for (Path archivePath : candidates()) {
            try {
                ZipFile archive = OPEN_ARCHIVES.computeIfAbsent(archivePath, RushCardAssets::open);
                for (String extension : List.of("jpg", "jpeg", "png")) {
                    ZipEntry entry = archive.getEntry("pics/" + code + "." + extension);
                    if (entry == null) continue;
                    try (InputStream input = archive.getInputStream(entry)) {
                        BufferedImage image = ImageIO.read(input);
                        if (image != null) return image;
                    }
                }
            } catch (Exception ex) {
                ModLog.warn("读取 Rush 卡图失败 {}：{}", archivePath, ex.toString());
            }
        }
        if (!warnedMissing) {
            warnedMissing = true;
            ModLog.warn("没有找到 Rush 扩展包；卡图会暂时显示卡背。"
                    + "可用 -D{}=<目录> 指定包含 .ypk 的目录", PROPERTY);
        }
        return null;
    }

    private static BufferedImage readExtracted(int code) {
        Path root = ModPaths.bundleDir("rush-art");
        if (root == null) return null;
        for (String extension : List.of("png", "jpg", "jpeg")) {
            Path path = root.resolve(code + "." + extension);
            if (!Files.isRegularFile(path)) continue;
            try {
                return ImageIO.read(path.toFile());
            } catch (Exception ex) {
                ModLog.warn("读取 Rush 卡图失败 {}：{}", path, ex.toString());
            }
        }
        return null;
    }

    private static ZipFile open(Path path) {
        try {
            return new ZipFile(path.toFile());
        } catch (Exception ex) {
            throw new IllegalStateException("无法打开 " + path + ": " + ex.getMessage(), ex);
        }
    }

    private static List<Path> candidates() {
        if (!archives.isEmpty() && archives.stream().allMatch(Files::isRegularFile)) {
            return archives;
        }
        List<Path> result = new ArrayList<>();
        addPath(result, pathProperty());
        Path config = ModPaths.configDir().resolve("rush-expansions");
        addPath(result, config);
        Path data = ModPaths.dataRoot();
        if (data != null) addPath(result, data.resolve("rush-expansions"));
        addPath(result, Path.of("D:\\Game\\MDPro3\\Expansions"));
        archives = result.stream()
                .filter(Files::isRegularFile)
                .sorted(Comparator.comparingInt(RushCardAssets::priority)
                        .thenComparing(path -> path.getFileName().toString()))
                .toList();
        return archives;
    }

    private static Path pathProperty() {
        String value = System.getProperty(PROPERTY);
        return value == null || value.isBlank() ? null : Path.of(value);
    }

    private static void addPath(List<Path> result, Path path) {
        if (path == null || !Files.exists(path)) return;
        if (Files.isDirectory(path)) {
            try (var files = Files.list(path)) {
                files.filter(Files::isRegularFile)
                        .filter(file -> file.getFileName().toString()
                                .toLowerCase(java.util.Locale.ROOT).endsWith(".ypk"))
                        .forEach(file -> result.add(file.toAbsolutePath().normalize()));
            } catch (Exception ex) {
                ModLog.warn("扫描 Rush 扩展包目录失败 {}：{}", path, ex.toString());
            }
        } else if (path.getFileName().toString().toLowerCase(java.util.Locale.ROOT).endsWith(".ypk")) {
            result.add(path.toAbsolutePath().normalize());
        }
    }

    private static int priority(Path path) {
        String name = path.getFileName().toString();
        if (name.contains("正式")) return 0;
        if (name.contains("先行")) return 1;
        return 2;
    }
}
