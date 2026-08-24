package fr.openmc.core.hooks.craftengine;

import fr.openmc.core.bootstrap.integration.OMCLogger;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;
import java.util.regex.Pattern;

public class ConversionReportAuditTest {

    @Test
    public void runRealConversionAndAnalyze() throws IOException {
        Logger logger = LoggerFactory.getLogger("ConversionAudit");
        OMCLogger.setRuntimeLogger(logger);

        Path tmp = Files.createTempDirectory("omc-conversion-audit");
        Path pluginsDir = tmp.resolve("plugins");
        Files.createDirectories(pluginsDir);
        Path contentsDir = pluginsDir.resolve("ItemsAdder/contents");
        File resources = new File("src/main/resources/contents");
        copyDirectory(resources, contentsDir.toFile());

        File reportFile = tmp.resolve("craftengine-conversion-report.txt").toFile();
        ConversionReport report = CraftEnginePackGenerator.generate(pluginsDir.toFile(), reportFile);

        File packDir = new File(pluginsDir.toFile(), "CraftEngine/resources/openmc");
        File configDir = new File(packDir, "configuration");

        StringBuilder out = new StringBuilder();
        out.append("=== AUDIT REPORT ===\n");
        out.append("summary: ").append(report.summary()).append("\n");
        out.append("--- unsupported (").append(report.getUnsupported().size()).append(") ---\n");
        for (String u : report.getUnsupported()) out.append(u).append("\n");
        out.append("--- missing assets (").append(report.getMissingAssets().size()).append(") ---\n");
        for (String m : report.getMissingAssets()) out.append(m).append("\n");

        Map<String, Integer> unsupportedByReason = new HashMap<>();
        for (String u : report.getUnsupported()) {
            String reason = u.substring(u.indexOf("→") + 2).trim();
            unsupportedByReason.merge(reason, 1, Integer::sum);
        }
        out.append("--- unsupported reasons ---\n");
        for (Map.Entry<String, Integer> e : unsupportedByReason.entrySet())
            out.append(e.getValue()).append("x ").append(e.getKey()).append("\n");

        List<String> configs = new ArrayList<>();
        if (configDir.isDirectory()) {
            File[] files = configDir.listFiles((d, n) -> n.endsWith(".yml"));
            if (files != null) {
                java.util.Arrays.sort(files, Comparator.comparing(File::getName));
                for (File f : files) configs.add(f.getName());
            }
        }
        out.append("--- namespaces with config (").append(configs.size()).append(") ---\n");
        for (String c : configs) out.append(c).append("\n");

        // Duplicate ID detection within each config file and cross-namespace collisions
        TreeSet<String> allIds = new TreeSet<>();
        Map<String, List<String>> idSources = new HashMap<>();
        for (String name : configs) {
            File f = new File(configDir, name);
            Map<String, Object> yaml = readYaml(f);
            for (String topKey : yaml.keySet()) {
                Object section = yaml.get(topKey);
                if (!(section instanceof Map<?, ?> map)) continue;
                for (Object rawKey : map.keySet()) {
                    String id = String.valueOf(rawKey);
                    String location = name + " : " + topKey + "." + id;
                    idSources.computeIfAbsent(id, k -> new ArrayList<>()).add(location);
                }
            }
        }
        out.append("--- duplicate IDs across namespaces ---\n");
        boolean dup = false;
        for (Map.Entry<String, List<String>> e : idSources.entrySet()) {
            if (e.getValue().size() > 1) {
                dup = true;
                out.append("DUPLICATE ").append(e.getKey()).append(" in: ").append(e.getValue()).append("\n");
            }
        }
        if (!dup) out.append("(none)\n");

        // Recipe vs item ID collisions (same id used as both recipe and item)
        out.append("--- recipe vs item collisions ---\n");
        TreeSet<String> recipeSet = new TreeSet<>(report.getRecipeIDs());
        TreeSet<String> itemSet = new TreeSet<>(report.getItemIDs());
        TreeSet<String> blockSet = new TreeSet<>(report.getBlockIDs());
        TreeSet<String> furnitureSet = new TreeSet<>(report.getFurnitureIDs());
        TreeSet<String> equipSet = new TreeSet<>(report.getEquipmentIDs());
        TreeSet<String> equipItemSet = new TreeSet<>(report.getEquipmentItemIDs());
        for (String id : recipeSet) {
            boolean collides = itemSet.contains(id) || blockSet.contains(id) || furnitureSet.contains(id)
                    || equipSet.contains(id) || equipItemSet.contains(id);
            // recipes commonly share id with their result item (e.g. craft recipe omc_blocks:aywenite_block -> item aywenite_block). Flag only if ambiguous.
            if (collides) {
                int owners = (itemSet.contains(id)?1:0)+(blockSet.contains(id)?1:0)+(furnitureSet.contains(id)?1:0)
                        +(equipSet.contains(id)?1:0)+(equipItemSet.contains(id)?1:0);
                if (owners > 1) out.append("COLLISION ").append(id).append(" owned by ").append(owners).append(" non-recipe kinds\n");
            }
        }
        TreeSet<String> recipeDup = new TreeSet<>();
        for (String id : report.getRecipeIDs()) if (!recipeDup.add(id)) out.append("RECIPE-DUP ").append(id).append("\n");
        out.append("(done)\n");

        // Write full config dump
        out.append("=== GENERATED CONFIG DUMP ===\n");
        if (configDir.isDirectory()) {
            File[] files = configDir.listFiles((d, n) -> n.endsWith(".yml"));
            if (files != null) {
                java.util.Arrays.sort(files, Comparator.comparing(File::getName));
                for (File f : files) {
                    out.append("### FILE: ").append(f.getName()).append(" ###\n");
                    out.append(Files.readString(f.toPath(), StandardCharsets.UTF_8)).append("\n");
                }
            }
        }

        Path dump = tmp.resolve("audit-dump.txt");
        Files.writeString(dump, out, StandardCharsets.UTF_8);
        System.out.println("DUMP_FILE=" + dump.toAbsolutePath());
        System.out.println("SUMMARY=" + report.summary());
        System.out.println("UNSUPPORTED_COUNT=" + report.getUnsupported().size());
        System.out.println("MISSING_COUNT=" + report.getMissingAssets().size());
    }

    private static Map<String, Object> readYaml(File file) throws IOException {
        org.yaml.snakeyaml.Yaml yaml = new org.yaml.snakeyaml.Yaml();
        try (var reader = Files.newBufferedReader(file.toPath(), StandardCharsets.UTF_8)) {
            Object loaded = yaml.load(reader);
            if (loaded instanceof Map<?, ?> map) {
                Map<String, Object> content = new LinkedHashMap<>();
                map.forEach((k, v) -> content.put(String.valueOf(k), v));
                return content;
            }
        }
        return Map.of();
    }

    private static void copyDirectory(File source, File target) throws IOException {
        if (!source.exists()) return;
        Files.walk(source.toPath()).forEach(path -> {
            Path relative = source.toPath().relativize(path);
            Path dest = target.toPath().resolve(relative);
            try {
                if (Files.isDirectory(path)) {
                    Files.createDirectories(dest);
                } else {
                    Files.copy(path, dest, StandardCopyOption.REPLACE_EXISTING);
                }
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });
    }
}
