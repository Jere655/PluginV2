package fr.openmc.core.hooks.craftengine;

import fr.openmc.core.bootstrap.integration.OMCLogger;
import lombok.Getter;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.Yaml;

import java.io.File;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Stream;

/**
 * Génère un pack CraftEngine à partir des contenus ItemsAdder d'OpenMC.
 * <p>
 * Le pack est régénéré à chaque démarrage à partir de {@code plugins/ItemsAdder/contents},
 * ce qui rend la conversion déterministe et évite les doublons.
 */
public final class CraftEnginePackGenerator {
    private static final String PACK_NAME = "openmc";
    private static final String CONTENTS_PATH = "ItemsAdder/contents";
    private static final String PACK_PATH = "CraftEngine/resources/" + PACK_NAME;

    @Getter
    private static ConversionReport lastReport;

    private CraftEnginePackGenerator() {
    }

    public static ConversionReport generate(File pluginsDir, File reportFile) {
        File contentsDir = new File(pluginsDir, CONTENTS_PATH);

        if (!contentsDir.isDirectory()) {
            OMCLogger.warn("Contenus ItemsAdder introuvables ({}), pack CraftEngine non généré",
                    contentsDir.getAbsolutePath());
            return null;
        }

        File packDir = new File(pluginsDir, PACK_PATH);
        ConversionReport report = new ConversionReport();

        try {
            deleteRecursively(packDir.toPath());
            Files.createDirectories(packDir.toPath());
            writePackMetadata(packDir);

            File[] namespaces = contentsDir.listFiles(File::isDirectory);
            if (namespaces == null) return report;

            List<File> sorted = new ArrayList<>(List.of(namespaces));
            sorted.sort(Comparator.comparing(File::getName));

            for (File namespaceDir : sorted) {
                convertNamespace(namespaceDir, packDir, report);
            }

            report.write(reportFile);
        } catch (Exception e) {
            OMCLogger.error("Erreur lors de la génération du pack CraftEngine", e);
            return report;
        }

        lastReport = report;
        OMCLogger.successFormatted("Pack CraftEngine généré depuis les contenus ItemsAdder : {}", report.summary());

        for (String unsupported : report.getUnsupported()) {
            OMCLogger.warn("Contenu ItemsAdder non converti : {}", unsupported);
        }
        for (String missing : report.getMissingAssets()) {
            OMCLogger.warn("Asset ItemsAdder manquant : {}", missing);
        }

        return report;
    }

    private static void convertNamespace(File namespaceDir, File packDir, ConversionReport report) throws IOException {
        String namespace = namespaceDir.getName();
        ItemsAdderContentConverter converter = new ItemsAdderContentConverter(namespace, namespaceDir, report);
        ItemsAdderModernContentConverter modernConverter =
                new ItemsAdderModernContentConverter(namespace, namespaceDir, report);
        ItemsAdderLegacyPropertyConverter legacyPropertyConverter =
                new ItemsAdderLegacyPropertyConverter(namespace, namespaceDir, report);
        ItemsAdderIaaRecipeConverter iaaRecipeConverter =
                new ItemsAdderIaaRecipeConverter(namespace, report);
        ItemsAdderCategoryConverter categoryConverter =
                new ItemsAdderCategoryConverter(namespace, report);
        ItemsAdderCropConverter cropConverter =
                new ItemsAdderCropConverter(namespace, namespaceDir, report);
        ItemsAdderLootConverter lootConverter =
                new ItemsAdderLootConverter(namespace, report);
        ItemsAdderWorldgenConverter worldgenConverter =
                new ItemsAdderWorldgenConverter(namespace, report);
        ItemsAdderSoundConverter soundConverter =
                new ItemsAdderSoundConverter(namespace, namespaceDir, report);

        List<File> configs = new ArrayList<>();
        collectFiles(namespaceDir, ".yml", configs);
        configs.sort(Comparator.comparing(File::getAbsolutePath));

        for (File config : configs) {
            Map<String, Object> content = readYaml(config);
            if (content.isEmpty()) continue;

            String relative = namespaceDir.toPath().relativize(config.toPath()).toString();
            try {
                Map<String, Object> legacyCompatible = legacyPropertyConverter.legacyCompatibleContent(content);
                Map<String, Object> modernCompatible = modernConverter.legacyCompatibleContent(legacyCompatible);
                Map<String, Object> iaaCompatible = iaaRecipeConverter.legacyCompatibleContent(modernCompatible);
                Map<String, Object> categoryCompatible = categoryConverter.legacyCompatibleContent(iaaCompatible);
                Map<String, Object> cropCompatible = cropConverter.legacyCompatibleContent(categoryCompatible);
                Map<String, Object> lootCompatible = lootConverter.legacyCompatibleContent(cropCompatible);
                Map<String, Object> worldgenCompatible = worldgenConverter.legacyCompatibleContent(lootCompatible);
                Map<String, Object> soundCompatible = soundConverter.legacyCompatibleContent(worldgenCompatible);
                converter.read(relative, soundCompatible);
                modernConverter.read(relative, content, converter.getItems());
                legacyPropertyConverter.read(relative, content, converter.getItems());
                iaaRecipeConverter.read(relative, content);
                categoryConverter.read(relative, content);
                cropConverter.read(relative, content);
                lootConverter.read(relative, content);
                worldgenConverter.read(relative, content);
                soundConverter.read(relative, content);
            } catch (Exception e) {
                report.unsupported(namespace + "/" + relative, "erreur de conversion : " + e);
            }
        }

        Set<String> allItemIds = new TreeSet<>();
        allItemIds.addAll(report.getItemIDs());
        allItemIds.addAll(report.getBlockIDs());
        allItemIds.addAll(report.getFurnitureIDs());
        allItemIds.addAll(report.getEquipmentIDs());
        allItemIds.addAll(report.getEquipmentItemIDs());
        allItemIds.addAll(report.getConsumableIDs());
        categoryConverter.expandWildcards(allItemIds);

        copyAssets(namespaceDir, namespace, packDir);
        writeConfiguration(packDir, namespace, converter, modernConverter, iaaRecipeConverter, categoryConverter,
                cropConverter, lootConverter, worldgenConverter, soundConverter);
    }

    private static void writeConfiguration(File packDir, String namespace, ItemsAdderContentConverter converter,
                                           ItemsAdderModernContentConverter modernConverter,
                                           ItemsAdderIaaRecipeConverter iaaRecipeConverter,
                                           ItemsAdderCategoryConverter categoryConverter,
                                           ItemsAdderCropConverter cropConverter,
                                           ItemsAdderLootConverter lootConverter,
                                           ItemsAdderWorldgenConverter worldgenConverter,
                                           ItemsAdderSoundConverter soundConverter) throws IOException {
        Map<String, Object> configuration = new LinkedHashMap<>();

        if (!converter.getItems().isEmpty()) configuration.put("items", converter.getItems());
        if (!modernConverter.getEquipments().isEmpty()) configuration.put("equipments", modernConverter.getEquipments());
        if (!converter.getImages().isEmpty()) configuration.put("images", converter.getImages());
        if (!categoryConverter.getCategories().isEmpty()) configuration.put("categories", categoryConverter.getCategories());

        Map<String, Object> allBlocks = new LinkedHashMap<>(converter.getBlocks());
        allBlocks.putAll(cropConverter.getCrops());
        if (!allBlocks.isEmpty()) configuration.put("blocks", allBlocks);

        Map<String, Object> allRecipes = new LinkedHashMap<>(converter.getRecipes());
        allRecipes.putAll(modernConverter.getRecipes());
        allRecipes.putAll(iaaRecipeConverter.getRecipes());
        if (!allRecipes.isEmpty()) configuration.put("recipes", allRecipes);

        if (!lootConverter.getLootSources().isEmpty()) configuration.put("loot_sources", lootConverter.getLootSources());
        if (!worldgenConverter.getPlacedFeatures().isEmpty()) configuration.put("placed_features", worldgenConverter.getPlacedFeatures());
        if (!soundConverter.getSounds().isEmpty()) configuration.put("sounds", soundConverter.getSounds());

        if (configuration.isEmpty()) return;

        File target = new File(packDir, "configuration/" + namespace + ".yml");
        Files.createDirectories(target.getParentFile().toPath());

        try (Writer writer = Files.newBufferedWriter(target.toPath(), StandardCharsets.UTF_8)) {
            writer.write("# Généré automatiquement par OpenMC depuis plugins/ItemsAdder/contents/" + namespace + "\n");
            writer.write("# Ne pas éditer à la main : le fichier est réécrit à chaque démarrage.\n");
            yaml().dump(configuration, writer);
        }
    }

    private static void copyAssets(File namespaceDir, String namespace, File packDir) throws IOException {
        Path assetsDir = packDir.toPath().resolve("resourcepack/assets");

        for (String folder : List.of("textures", "models", "sounds")) {
            File source = new File(namespaceDir, folder);
            if (source.isDirectory()) {
                copyDirectory(source.toPath(), assetsDir.resolve(namespace).resolve(folder));
            }
        }

        File resourcepack = new File(namespaceDir, "resourcepack");
        if (!resourcepack.isDirectory()) return;

        File standardAssets = new File(resourcepack, "assets");
        if (standardAssets.isDirectory()) {
            copyDirectory(standardAssets.toPath(), assetsDir);
        }

        File[] legacyNamespaces = resourcepack.listFiles(file -> file.isDirectory() && !file.getName().equals("assets"));
        if (legacyNamespaces == null) return;

        for (File legacyNamespace : legacyNamespaces) {
            copyDirectory(legacyNamespace.toPath(), assetsDir.resolve(legacyNamespace.getName()));
        }
    }

    private static void writePackMetadata(File packDir) throws IOException {
        String metadata = """
                author: OpenMC
                version: 1.0
                description: Contenus OpenMC convertis depuis ItemsAdder
                namespace: %s
                """.formatted(PACK_NAME);

        Files.writeString(packDir.toPath().resolve("pack.yml"), metadata, StandardCharsets.UTF_8);
    }

    private static Map<String, Object> readYaml(File file) {
        try (Reader reader = Files.newBufferedReader(file.toPath(), StandardCharsets.UTF_8)) {
            Object loaded = new Yaml().load(reader);
            if (loaded instanceof Map<?, ?> map) {
                Map<String, Object> content = new LinkedHashMap<>();
                map.forEach((key, value) -> content.put(String.valueOf(key), value));
                return content;
            }
        } catch (Exception e) {
            OMCLogger.warn("Contenu ItemsAdder illisible ({}) : {}", file.getName(), e.getMessage());
        }
        return Map.of();
    }

    private static Yaml yaml() {
        DumperOptions options = new DumperOptions();
        options.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);
        options.setPrettyFlow(true);
        options.setIndent(2);

        return new Yaml(options);
    }

    private static void collectFiles(File directory, String extension, List<File> found) {
        File[] children = directory.listFiles();
        if (children == null) return;

        for (File child : children) {
            if (child.isDirectory()) {
                collectFiles(child, extension, found);
            } else if (child.getName().endsWith(extension)) {
                found.add(child);
            }
        }
    }

    private static void copyDirectory(Path source, Path target) throws IOException {
        try (Stream<Path> paths = Files.walk(source)) {
            for (Path path : paths.toList()) {
                Path destination = target.resolve(source.relativize(path).toString());

                if (Files.isDirectory(path)) {
                    Files.createDirectories(destination);
                    continue;
                }

                Files.createDirectories(destination.getParent());
                Files.copy(path, destination, StandardCopyOption.REPLACE_EXISTING);
            }
        }
    }

    private static void deleteRecursively(Path path) throws IOException {
        if (!Files.exists(path)) return;

        try (Stream<Path> paths = Files.walk(path)) {
            for (Path child : paths.sorted(Comparator.reverseOrder()).toList()) {
                Files.deleteIfExists(child);
            }
        }
    }
}
