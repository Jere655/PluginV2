package fr.openmc.core.hooks.craftengine;

import fr.openmc.core.bootstrap.integration.OMCLogger;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.yaml.snakeyaml.Yaml;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

public class EweniteTexturePathRegressionTest {

    @Test
    public void eweniteUsesAtlasSafeTextureAndPngExists() throws IOException {
        Path tmp = Files.createTempDirectory("omc-ewenite-test");
        Path pluginsDir = tmp.resolve("plugins");
        Files.createDirectories(pluginsDir);
        Path contentsDir = pluginsDir.resolve("ItemsAdder/contents");
        copyDirectory(new File("src/main/resources/contents"), contentsDir.toFile());

        Logger logger = LoggerFactory.getLogger("ConversionAudit");
        OMCLogger.setRuntimeLogger(logger);

        File reportFile = tmp.resolve("craftengine-conversion-report.txt").toFile();
        ConversionReport report = CraftEnginePackGenerator.generate(pluginsDir.toFile(), reportFile);

        File packDir = new File(pluginsDir.toFile(), "CraftEngine/resources/openmc");
        File configFile = new File(packDir, "configuration/omc_dream.yml");
        assertTrue(configFile.isFile(), "Generated config omc_dream.yml must exist");

        Yaml yaml = new Yaml();
        Map<String, Object> config;
        try (var reader = Files.newBufferedReader(configFile.toPath(), StandardCharsets.UTF_8)) {
            config = yaml.load(reader);
        }

        @SuppressWarnings("unchecked")
        Map<String, Object> items = (Map<String, Object>) config.get("items");
        assertNotNull(items, "items section must exist");
        assertTrue(items.containsKey("omc_dream:ewenite"), "ewenite item must be present");

        @SuppressWarnings("unchecked")
        Map<String, Object> ewenite = (Map<String, Object>) items.get("omc_dream:ewenite");

        Object texture = ewenite.get("texture");
        assertNotNull(texture, "ewenite must have a texture reference");
        assertEquals("omc_dream:item/others/ewenite", texture,
                "ewenite texture must point to atlas-safe path");

        File png = new File(packDir, "resourcepack/assets/omc_dream/textures/item/others/ewenite.png");
        assertTrue(png.isFile(), "Relocated ewenite PNG must exist at " + png.getAbsolutePath());

        Map<String, Object> data = (Map<String, Object>) ewenite.get("data");
        assertNotNull(data, "ewenite must have data section");

        @SuppressWarnings("unchecked")
        Map<String, Object> nbt = (Map<String, Object>) data.get("nbt");
        assertNotNull(nbt, "ewenite data must preserve nbt");

        @SuppressWarnings("unchecked")
        Map<String, Object> publicBukkitValues = (Map<String, Object>) nbt.get("PublicBukkitValues");
        assertNotNull(publicBukkitValues, "PublicBukkitValues must be preserved");
        assertEquals("omc_dream:ewenite", publicBukkitValues.get("openmc:to_dream_item"),
                "Dream marker must remain intact");
    }

    @Test
    public void similarNonStandardItemTexturesAreRelocated() throws IOException {
        Path tmp = Files.createTempDirectory("omc-relocate-test");
        Path pluginsDir = tmp.resolve("plugins");
        Files.createDirectories(pluginsDir);
        Path contentsDir = pluginsDir.resolve("ItemsAdder/contents");
        copyDirectory(new File("src/main/resources/contents"), contentsDir.toFile());

        Logger logger = LoggerFactory.getLogger("ConversionAudit");
        OMCLogger.setRuntimeLogger(logger);

        File reportFile = tmp.resolve("craftengine-conversion-report.txt").toFile();
        ConversionReport report = CraftEnginePackGenerator.generate(pluginsDir.toFile(), reportFile);

        File packDir = new File(pluginsDir.toFile(), "CraftEngine/resources/openmc");
        File configFile = new File(packDir, "configuration/omc_dream.yml");

        Yaml yaml = new Yaml();
        Map<String, Object> config;
        try (var reader = Files.newBufferedReader(configFile.toPath(), StandardCharsets.UTF_8)) {
            config = yaml.load(reader);
        }

        @SuppressWarnings("unchecked")
        Map<String, Object> items = (Map<String, Object>) config.get("items");

        String[][] checks = {
                {"omc_dream:soul", "omc_dream:item/others/soul", "others/soul.png"},
                {"omc_dream:cloud_key", "omc_dream:item/others/cloud_key", "others/cloud_key.png"},
                {"omc_dream:somnifere", "omc_dream:item/others/somnifere", "others/somnifere.png"},
                {"omc_dream:crystallized_pickaxe", "omc_dream:item/tools/crystalized_pickaxe", "tools/crystalized_pickaxe.png"},
        };

        for (String[] check : checks) {
            String itemId = check[0];
            String expectedTexture = check[1];
            String expectedPngRel = check[2];

            assertTrue(items.containsKey(itemId), itemId + " must exist");

            @SuppressWarnings("unchecked")
            Map<String, Object> item = (Map<String, Object>) items.get(itemId);
            Object texture = item.get("texture");
            assertNotNull(texture, itemId + " must have texture");
            assertEquals(expectedTexture, texture, itemId + " texture reference");

            File png = new File(packDir, "resourcepack/assets/omc_dream/textures/item/" + expectedPngRel);
            assertTrue(png.isFile(), "Relocated PNG for " + itemId + " must exist at " + png.getAbsolutePath());
        }
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
                    Files.copy(path, dest, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                }
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });
    }
}
