package fr.openmc.core.hooks.craftengine;

import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class PlacedFeatureSchemaTest {

    private static final List<String> EXPECTED = List.of(
            "omc_dream:deepslate_ewenite_ore",
            "omc_populator:deepslate_aywenite_ore",
            "omc_populator:aywenite_ore"
    );

    @Test
    public void placementIsFlatListOfModifiers() throws IOException {
        File configDir = generateTempPack();
        assertTrue(configDir.isDirectory(), "configuration directory must exist");

        List<String> foundIds = new ArrayList<>();
        for (File config : configDir.listFiles((d, n) -> n.endsWith(".yml"))) {
            Map<String, Object> loaded = readYaml(config);
            Object placedFeatures = loaded.get("placed_features");
            if (!(placedFeatures instanceof Map<?, ?> pf)) continue;
            for (Map.Entry<?, ?> entry : pf.entrySet()) {
                String id = String.valueOf(entry.getKey());
                foundIds.add(id);
                assertNotNull(entry.getValue(), "placed feature " + id + " missing structure");

                @SuppressWarnings("unchecked")
                Map<String, Object> feature = (Map<String, Object>) entry.getValue();

                Object placement = feature.get("placement");
                assertNotNull(placement, "placed feature " + id + " has no placement key");
                assertTrue(placement instanceof List,
                        "placement for " + id + " must be a list, was: " + placement.getClass());

                @SuppressWarnings("unchecked")
                List<Object> modifiers = (List<Object>) placement;
                assertFalse(modifiers.isEmpty(), "placement for " + id + " is empty");
                for (Object modifier : modifiers) {
                    assertTrue(modifier instanceof Map,
                            "each placement modifier for " + id + " must be a map");
                    Map<?, ?> map = (Map<?, ?>) modifier;
                    assertTrue(map.containsKey("type"),
                            "placement modifier for " + id + " missing 'type': " + map);
                }
            }
        }

        java.util.Set<String> found = new java.util.TreeSet<>(foundIds);
        for (String expected : EXPECTED) {
            assertTrue(found.contains(expected), "missing expected placed_feature: " + expected);
        }
        assertEquals(EXPECTED.size(), found.size(),
                "expected exactly the 3 placed features, got: " + found);
    }

    private static File generateTempPack() throws IOException {
        Path tmp = Files.createTempDirectory("omc-placed-feature-test");
        Path pluginsDir = tmp.resolve("plugins");
        Files.createDirectories(pluginsDir);
        Path contentsDir = pluginsDir.resolve("ItemsAdder/contents");
        File resources = new File("src/main/resources/contents");
        copyDirectory(resources, contentsDir.toFile());

        File reportFile = tmp.resolve("report.txt").toFile();
        CraftEnginePackGenerator.generate(pluginsDir.toFile(), reportFile);

        File packDir = new File(pluginsDir.toFile(), "CraftEngine/resources/openmc");
        File configDir = new File(packDir, "configuration");
        assertTrue(configDir.isDirectory(), "configuration dir must exist after generation");
        return configDir;
    }

    private static Map<String, Object> readYaml(File file) throws IOException {
        org.yaml.snakeyaml.Yaml yaml = new org.yaml.snakeyaml.Yaml();
        try (var reader = Files.newBufferedReader(file.toPath(), StandardCharsets.UTF_8)) {
            Object loaded = yaml.load(reader);
            if (loaded instanceof Map<?, ?> map) {
                Map<String, Object> content = new java.util.LinkedHashMap<>();
                for (Map.Entry<?, ?> e : map.entrySet()) {
                    Object value = e.getValue();
                    if (value instanceof Map<?, ?> inner) {
                        Map<String, Object> typed = new java.util.LinkedHashMap<>();
                        for (Map.Entry<?, ?> ie : inner.entrySet()) {
                            typed.put(String.valueOf(ie.getKey()), ie.getValue());
                        }
                        value = typed;
                    }
                    content.put(String.valueOf(e.getKey()), value);
                }
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
