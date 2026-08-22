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

    private final List<String> unsupported = new ArrayList<>();
    private final List<String> missingAssets = new ArrayList<>();

    /**
     * Note un contenu ItemsAdder qui n'a pas d'équivalent CraftEngine.
     *
     * @param id     l'identifiant concerné (namespace:id ou fichier)
     * @param reason la raison lisible
     */
    public void unsupported(String id, String reason) {
        unsupported.add(id + " → " + reason);
    }

    /**
     * Note une texture ou un modèle référencé par un contenu mais absent des ressources.
     *
     * @param id   l'identifiant du contenu
     * @param path le chemin manquant
     */
    public void missingAsset(String id, String path) {
        missingAssets.add(id + " → " + path);
    }

    public List<String> getUnsupported() {
        return Collections.unmodifiableList(unsupported);
    }

    public List<String> getMissingAssets() {
        return Collections.unmodifiableList(missingAssets);
    }

    /**
     * @return un résumé sur une ligne des contenus convertis
     */
    public String summary() {
        return "items=%d, blocs=%d, furniture=%d, images=%d, recettes=%d, non supportés=%d, assets manquants=%d"
                .formatted(itemIDs.size(), blockIDs.size(), furnitureIDs.size(), imageIDs.size(),
                        recipeIDs.size(), unsupported.size(), missingAssets.size());
    }

    /**
     * Écrit le rapport de migration lisible à côté de la configuration du plugin.
     *
     * @param destination le fichier à écrire
     * @throws IOException si le fichier ne peut pas être écrit
     */
    public void write(File destination) throws IOException {
        StringBuilder builder = new StringBuilder();
        builder.append("# Rapport de conversion ItemsAdder → CraftEngine (OpenMC)\n");
        builder.append("# Généré le ").append(LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)).append("\n\n");
        builder.append("items convertis      : ").append(itemIDs.size()).append('\n');
        builder.append("blocs convertis      : ").append(blockIDs.size()).append('\n');
        builder.append("furniture convertis  : ").append(furnitureIDs.size()).append('\n');
        builder.append("images converties    : ").append(imageIDs.size()).append('\n');
        builder.append("recettes converties  : ").append(recipeIDs.size()).append('\n');

        appendSection(builder, "Entrées non supportées (à porter manuellement)", unsupported);
        appendSection(builder, "Textures / modèles manquants", missingAssets);
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
