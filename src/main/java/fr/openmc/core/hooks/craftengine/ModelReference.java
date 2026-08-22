package fr.openmc.core.hooks.craftengine;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Référence de rendu partagée entre l'item CraftEngine et, le cas échéant, son bloc.
 *
 * @param path       le chemin du modèle, ou null si l'item utilise directement une texture
 * @param texture    la texture de l'item, ou null si un modèle est utilisé
 * @param generation la génération de modèle à écrire, ou null si le modèle existe déjà dans le pack
 */
public record ModelReference(String path, String texture, Map<String, Object> generation) {
    public static ModelReference none() {
        return new ModelReference(null, null, null);
    }

    public static ModelReference model(String path) {
        return new ModelReference(path, null, null);
    }

    public static ModelReference texture(String texture) {
        return new ModelReference(null, texture, null);
    }

    /**
     * @param path     le chemin du modèle à générer
     * @param parent   le modèle parent vanilla
     * @param textures les textures du modèle généré
     * @return une référence qui fera générer le modèle par CraftEngine
     */
    public static ModelReference generated(String path, String parent, Map<String, Object> textures) {
        Map<String, Object> generation = new LinkedHashMap<>();
        generation.put("parent", parent);
        generation.put("textures", textures);

        return new ModelReference(path, null, generation);
    }

    /**
     * Applique la référence à la configuration d'un item CraftEngine.
     *
     * @param item l'item en cours de construction
     */
    public void applyToItem(Map<String, Object> item) {
        if (texture != null) {
            item.put("texture", texture);
            return;
        }
        if (path == null) return;

        if (generation == null) {
            item.put("model", path);
            return;
        }

        Map<String, Object> model = new LinkedHashMap<>();
        model.put("type", "minecraft:model");
        model.put("path", path);
        model.put("generation", generation);
        item.put("model", model);
    }

    /**
     * Applique la référence à l'état d'un bloc CraftEngine.
     * Le modèle est déjà généré par l'item, l'état ne fait donc que le référencer.
     *
     * @param state l'état de bloc en cours de construction
     */
    public void applyToBlockState(Map<String, Object> state) {
        if (texture != null) {
            state.put("texture", texture);
            return;
        }
        if (path == null) return;

        state.put("model", Map.of("path", path));
    }
}
