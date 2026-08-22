package fr.openmc.api.datapacks.injectors;

import fr.openmc.api.datapacks.DatapackInjector;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class PackMetadataInjector implements DatapackInjector {
    private static final int PACK_FORMAT = 81; // ** format de datapack de Minecraft 1.21.7

    @Override
    public void inject(File rootFile) {
        Path root = rootFile.toPath();
        try {
            Path metaDataFile = root.resolve("pack.mcmeta");
            Files.writeString(metaDataFile, packMcMeta());
        } catch (IOException e) {
            throw new IllegalStateException("Cannot write pack mcmeta file", e);
        }
    }

    private String packMcMeta() {
        return String.format("""
                {
                  "pack": {
                    "description": "OMC datapack injected from plugin",
                    "pack_format": %d,
                    "supported_formats": {
                      "min_inclusive": %d,
                      "max_inclusive": %d
                    }
                  }
                }
                """, PACK_FORMAT, PACK_FORMAT, PACK_FORMAT);
    }
}
