package de.cuteghg.fre.pack;

import de.cuteghg.fre.FlagRanksEmotesPlugin;
import de.cuteghg.fre.config.ConfigManager;
import de.cuteghg.fre.config.EmoteDefinition;
import de.cuteghg.fre.rank.RankDefinition;
import de.cuteghg.fre.util.CodepointAllocator;
import de.cuteghg.fre.util.HashUtil;
import org.bukkit.entity.Player;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public final class ResourcePackService {
    private final FlagRanksEmotesPlugin plugin;
    private final ConfigManager configManager;
    private final Executor executor;

    private volatile PackBuildResult lastBuild = PackBuildResult.empty();

    public ResourcePackService(FlagRanksEmotesPlugin plugin, ConfigManager configManager, Executor executor) {
        this.plugin = plugin;
        this.configManager = configManager;
        this.executor = executor;
    }

    public CompletableFuture<PackBuildResult> buildPack() {
        return CompletableFuture.supplyAsync(this::buildPackSync, executor);
    }

    public PackBuildResult buildPackSync() {
        try {
            configManager.getRanksDirectory().mkdirs();
            configManager.getFlagsDirectory().mkdirs();
            configManager.getEmotesDirectory().mkdirs();
            configManager.getGeneratedDirectory().mkdirs();

            Path buildDir = configManager.getGeneratedDirectory().toPath().resolve("pack-build");
            deleteDirectory(buildDir);
            Files.createDirectories(buildDir);

            String namespace = configManager.getPackNamespace();
            Path assets = buildDir.resolve("assets").resolve(namespace);
            Path fontDir = assets.resolve("font");
            Path texturesRanks = assets.resolve("textures/ranks");
            Path texturesFlags = assets.resolve("textures/flags");
            Path texturesEmotes = assets.resolve("textures/emotes");
            Files.createDirectories(fontDir);
            Files.createDirectories(texturesRanks);
            Files.createDirectories(texturesFlags);
            Files.createDirectories(texturesEmotes);

            Map<String, Integer> rankCodes = CodepointAllocator.allocate(
                    configManager.getRanks().stream().map(r -> r.icon().toLowerCase(Locale.ROOT)).distinct().sorted().toList(),
                    CodepointAllocator.RANK_BASE
            );
            Map<String, Integer> flagCodes = CodepointAllocator.allocate(loadFlagKeys(), CodepointAllocator.FLAG_BASE);
            Map<String, Integer> emoteCodes = CodepointAllocator.allocate(loadEmoteKeys(), CodepointAllocator.EMOTE_BASE);

            copyRankAssets(texturesRanks, rankCodes.keySet());
            copyFlagAssets(texturesFlags, flagCodes.keySet());
            copyEmoteAssets(texturesEmotes, emoteCodes.keySet());
            writePackMeta(buildDir);
            writeFontJson(fontDir.resolve("default.json"), namespace, rankCodes, flagCodes, emoteCodes);

            Path zipPath = configManager.getPackZipFile().toPath();
            zipDirectoryDeterministic(buildDir, zipPath);
            String sha1 = HashUtil.sha1Hex(zipPath);

            PackBuildResult result = new PackBuildResult(sha1, zipPath.toFile(),
                    toGlyphMap(rankCodes), toGlyphMap(flagCodes), toGlyphMap(emoteCodes));
            this.lastBuild = result;
            return result;
        } catch (Exception e) {
            throw new IllegalStateException("Could not build resource pack", e);
        }
    }

    private List<String> loadFlagKeys() {
        List<String> keys = new ArrayList<>();
        File[] files = configManager.getFlagsDirectory().listFiles((dir, name) -> name.toLowerCase(Locale.ROOT).endsWith(".png"));
        if (files != null) {
            for (File file : files) {
                String key = file.getName().substring(0, file.getName().length() - 4).toLowerCase(Locale.ROOT);
                if (key.matches("[a-z0-9_-]+")) {
                    keys.add(key);
                }
            }
        }
        if (!keys.contains("unknown")) {
            keys.add("unknown");
        }
        return keys.stream().distinct().sorted().toList();
    }

    private List<String> loadEmoteKeys() {
        return configManager.getEmotesByAlias().values().stream().map(EmoteDefinition::key)
                .map(k -> k.toLowerCase(Locale.ROOT)).distinct().sorted().toList();
    }

    private void copyRankAssets(Path texturesRanks, Iterable<String> keys) throws IOException {
        for (String key : keys) {
            Path target = texturesRanks.resolve(key + ".png");
            File source = new File(configManager.getRanksDirectory(), key + ".png");
            copyOrGenerate(source.toPath(), target, "R");
        }
    }

    private void copyFlagAssets(Path texturesFlags, Iterable<String> keys) throws IOException {
        for (String key : keys) {
            Path target = texturesFlags.resolve(key + ".png");
            File source = new File(configManager.getFlagsDirectory(), key + ".png");
            copyOrGenerate(source.toPath(), target, "F");
        }
    }

    private void copyEmoteAssets(Path texturesEmotes, Iterable<String> keys) throws IOException {
        for (String key : keys) {
            Path target = texturesEmotes.resolve(key + ".png");
            EmoteDefinition emote = configManager.getEmotesByAlias().values().stream()
                    .filter(e -> e.key().equalsIgnoreCase(key)).findFirst().orElse(null);
            if (emote != null) {
                Path safePath = resolveSafe(configManager.getEmotesDirectory().toPath(), emote.file());
                copyOrGenerate(safePath, target, "E");
            } else {
                copyOrGenerate(Path.of(""), target, "E");
            }
        }
    }

    private void copyOrGenerate(Path source, Path target, String label) throws IOException {
        if (source != null && !source.toString().isBlank() && Files.exists(source) && source.toString().toLowerCase(Locale.ROOT).endsWith(".png")) {
            Files.copy(source, target, StandardCopyOption.REPLACE_EXISTING);
            return;
        }

        BufferedImage image = new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = image.createGraphics();
        g.setColor(new Color(35, 35, 35, 220));
        g.fillRect(0, 0, 16, 16);
        g.setColor(new Color(255, 255, 255, 230));
        g.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 9));
        g.drawString(label, 4, 11);
        g.dispose();
        ImageIO.write(image, "png", target.toFile());
    }

    private void writePackMeta(Path buildDir) throws IOException {
        String json = """
                {
                  "pack": {
                    "pack_format": %d,
                    "description": "%s"
                  }
                }
                """.formatted(configManager.getPackFormat(), escapeJson(configManager.getPackDescription()));
        Files.writeString(buildDir.resolve("pack.mcmeta"), json, StandardCharsets.UTF_8);
    }

    private void writeFontJson(Path path, String namespace,
                               Map<String, Integer> rankCodes,
                               Map<String, Integer> flagCodes,
                               Map<String, Integer> emoteCodes) throws IOException {
        List<String> providers = new ArrayList<>();
        appendProviders(providers, namespace, "ranks", rankCodes);
        appendProviders(providers, namespace, "flags", flagCodes);
        appendProviders(providers, namespace, "emotes", emoteCodes);
        String json = "{\n  \"providers\": [\n" + String.join(",\n", providers) + "\n  ]\n}";
        Files.writeString(path, json, StandardCharsets.UTF_8);
    }

    private void appendProviders(List<String> providers, String namespace, String category, Map<String, Integer> map) {
        map.entrySet().stream().sorted(Map.Entry.comparingByKey()).forEach(entry -> {
            String value = "    {\n" +
                    "      \"type\": \"bitmap\",\n" +
                    "      \"file\": \"" + namespace + ":" + category + "/" + entry.getKey() + ".png\",\n" +
                    "      \"ascent\": 8,\n" +
                    "      \"height\": 8,\n" +
                    "      \"chars\": [\"" + escapeJson(CodepointAllocator.toChar(entry.getValue())) + "\"]\n" +
                    "    }";
            providers.add(value);
        });
    }

    private Path resolveSafe(Path base, String input) {
        Path resolved = base.resolve(input).normalize();
        if (!resolved.startsWith(base)) {
            throw new IllegalArgumentException("Invalid path traversal attempt: " + input);
        }
        return resolved;
    }

    private String escapeJson(String text) {
        return text.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    private Map<String, String> toGlyphMap(Map<String, Integer> codes) {
        Map<String, String> out = new LinkedHashMap<>();
        codes.forEach((key, value) -> out.put(key, CodepointAllocator.toChar(value)));
        return out;
    }

    private void zipDirectoryDeterministic(Path sourceDir, Path zipFile) throws IOException {
        List<Path> files = new ArrayList<>();
        Files.walkFileTree(sourceDir, new SimpleFileVisitor<>() {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
                files.add(file);
                return FileVisitResult.CONTINUE;
            }
        });
        files.sort(Comparator.comparing(path -> sourceDir.relativize(path).toString()));

        try (ZipOutputStream zos = new ZipOutputStream(Files.newOutputStream(zipFile))) {
            for (Path file : files) {
                String entryName = sourceDir.relativize(file).toString().replace('\\', '/');
                ZipEntry entry = new ZipEntry(entryName);
                entry.setTime(Instant.parse("2020-01-01T00:00:00Z").toEpochMilli());
                zos.putNextEntry(entry);
                Files.copy(file, zos);
                zos.closeEntry();
            }
        }
    }

    private void deleteDirectory(Path path) throws IOException {
        if (!Files.exists(path)) {
            return;
        }
        Files.walkFileTree(path, new SimpleFileVisitor<>() {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                Files.delete(file);
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
                Files.delete(dir);
                return FileVisitResult.CONTINUE;
            }
        });
    }

    public Optional<String> getRankGlyph(String rankKey) {
        return Optional.ofNullable(lastBuild.rankGlyphs().get(rankKey.toLowerCase(Locale.ROOT)));
    }

    public Optional<String> getFlagGlyph(String countryCode) {
        String normalized = countryCode.toLowerCase(Locale.ROOT);
        String glyph = lastBuild.flagGlyphs().get(normalized);
        if (glyph == null) {
            glyph = lastBuild.flagGlyphs().get("unknown");
        }
        return Optional.ofNullable(glyph);
    }

    public Map<String, String> getEmoteGlyphs() {
        Map<String, String> map = new LinkedHashMap<>();
        configManager.getEmotesByAlias().forEach((alias, emote) -> {
            String glyph = lastBuild.emoteGlyphs().get(emote.key().toLowerCase(Locale.ROOT));
            if (glyph != null) {
                map.put(alias, glyph);
            }
        });
        return map;
    }

    public void sendConfiguredPack(Player player) {
        String url = configManager.requirePackUrl();
        if (url == null || url.isBlank() || !url.startsWith("https://")) {
            player.sendMessage("§cConfigured pack URL is missing or not HTTPS.");
            return;
        }

        String hash = configManager.getPackSha1().isBlank() ? lastBuild.sha1() : configManager.getPackSha1();
        byte[] hashBytes = HashUtil.sha1BytesFromHex(hash);
        player.setResourcePack(url, hashBytes, configManager.getPackPrompt(), configManager.isPackRequired());
    }

    public PackBuildResult getLastBuild() {
        return lastBuild;
    }

    public record PackBuildResult(String sha1, File zipFile, Map<String, String> rankGlyphs, Map<String, String> flagGlyphs,
                                  Map<String, String> emoteGlyphs) {
        static PackBuildResult empty() {
            return new PackBuildResult("", null, Map.of(), Map.of(), Map.of());
        }
    }
}
