package fr.openmc.core.hooks.craftengine;

import lombok.Getter;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

/**
 * Résultat de la conversion des contenus ItemsAdder d'OpenMC vers un pack CraftEngine.
 * Sert aussi de référence pour la validation faite une fois CraftEngine chargé.
 */
public class ConversionReport {
    @Getter
    private final Set<String> itemIDs = new TreeSet<>();
    @Getter
    private final Set<String> blockIDs = new TreeSet<>();
    @Getter
    private final Set<String> furnitureIDs = new TreeSet<>();
    @Getter
    private final Set<String> imageIDs = new TreeSet<>();
    @Getter
    private final Set<String> recipeIDs = new TreeSet<>();
    @Getter
    private final Set<String> equipmentIDs = new TreeSet<>();
    @Getter
    private final Set<String> equipmentItemIDs = new TreeSet<>();
    @Getter
    private final Set<String> consumableIDs = new TreeSet<>();
    @Getter
    private final Set<String> lootSourceIDs = new TreeSet<>();
    @Getter
    private final Set<String> worldgenIDs = new TreeSet<>();
    @Getter
    private final Set<String> soundIDs = new TreeSet<>();

    private final List<String> unsupported = new ArrayList<>();
    private final List<String> missingAssets = new ArrayList<>();

    public void unsupported(String id, String reason) {
        unsupported.add(id + " → " + reason);
    }

    public void missingAsset(String id, String path) {
        missingAssets.add(id + " → " + path);
    }

    public List<String> getUnsupported() {
        return Collections.unmodifiableList(unsupported);
    }

    public List<String> getMissingAssets() {
        return Collections.unmodifiableList(missingAssets);
    }

    public String summary() {
        return "items=%d, blocs=%d, furniture=%d, images=%d, recettes=%d, équipements=%d, items équipés=%d, consommables=%d, loots=%d, worldgen=%d, sons=%d, non supportés=%d, assets manquants=%d"
                .formatted(itemIDs.size(), blockIDs.size(), furnitureIDs.size(), imageIDs.size(),
                        recipeIDs.size(), equipmentIDs.size(), equipmentItemIDs.size(), consumableIDs.size(),
                        lootSourceIDs.size(), worldgenIDs.size(), soundIDs.size(),
                        unsupported.size(), missingAssets.size());
    }

    public void write(File destination) throws IOException {
        StringBuilder builder = new StringBuilder();
        builder.append("# Rapport de conversion ItemsAdder → CraftEngine (OpenMC)\n");
        builder.append("# Généré le ").append(LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)).append("\n\n");
        builder.append("items convertis       : ").append(itemIDs.size()).append('\n');
        builder.append("blocs convertis       : ").append(blockIDs.size()).append('\n');
        builder.append("furniture convertis   : ").append(furnitureIDs.size()).append('\n');
        builder.append("images converties     : ").append(imageIDs.size()).append('\n');
        builder.append("recettes converties   : ").append(recipeIDs.size()).append('\n');
        builder.append("équipements convertis : ").append(equipmentIDs.size()).append('\n');
        builder.append("items équipés         : ").append(equipmentItemIDs.size()).append('\n');
        builder.append("consommables convertis: ").append(consumableIDs.size()).append('\n');
        builder.append("loots convertis       : ").append(lootSourceIDs.size()).append('\n');
        builder.append("worldgen convertis    : ").append(worldgenIDs.size()).append('\n');
        builder.append("sons convertis        : ").append(soundIDs.size()).append('\n');

        appendSection(builder, "Entrées non supportées (à porter manuellement)", unsupported);
        appendSection(builder, "Textures / modèles manquants", missingAssets);
        appendSection(builder, "Équipements", new ArrayList<>(equipmentIDs));
        appendSection(builder, "Items équipés", new ArrayList<>(equipmentItemIDs));
        appendSection(builder, "Consommables", new ArrayList<>(consumableIDs));
        appendSection(builder, "Loots", new ArrayList<>(lootSourceIDs));
        appendSection(builder, "Worldgen", new ArrayList<>(worldgenIDs));
        appendSection(builder, "Sons", new ArrayList<>(soundIDs));
        appendSection(builder, "Blocs", new ArrayList<>(blockIDs));
        appendSection(builder, "Furniture", new ArrayList<>(furnitureIDs));
        appendSection(builder, "Items", new ArrayList<>(itemIDs));

        if (destination.getParentFile() != null) {
            Files.createDirectories(destination.getParentFile().toPath());
        }
        Files.writeString(destination.toPath(), builder.toString(), StandardCharsets.UTF_8);
    }

    private void appendSection(StringBuilder builder, String title, List<String> lines) {
        builder.append('\n').append("== ").append(title).append(" (").append(lines.size()).append(") ==\n");
        for (String line : lines) {
            builder.append("  - ").append(line).append('\n');
        }
    }
}
