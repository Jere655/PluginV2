package fr.openmc.core.hooks.craftengine;

import java.io.File;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

/**
 * Convertit les propriétés ItemsAdder modernes qui n'étaient pas prises en charge
 * par le convertisseur historique : équipements 1.21.2+ et consommables 1.21.4+.
 */
public final class ItemsAdderModernContentConverter {
    private final String namespace;
    private final File namespaceDir;
    private final ConversionReport report;
    private final Map<String, Object> equipments = new LinkedHashMap<>();

    public ItemsAdderModernContentConverter(String namespace, File namespaceDir, ConversionReport report) {
        this.namespace = namespace;
        this.namespaceDir = namespaceDir;
        this.report = report;
    }

    public Map<String, Object> getEquipments() {
        return equipments;
    }

    /**
     * Retire uniquement les champs que ce convertisseur prend en charge afin que
     * le convertisseur historique ne les signale pas à tort comme non supportés.
     */
    public Map<String, Object> legacyCompatibleContent(Map<String, Object> content) {
        Map<String, Object> sanitized = new LinkedHashMap<>(content);
        sanitized.remove("equipments");

        Map<String, Object> items = asSection(content.get("items"));
        if (!items.isEmpty()) {
            Map<String, Object> sanitizedItems = new LinkedHashMap<>();
            for (Map.Entry<String, Object> entry : items.entrySet()) {
                Map<String, Object> definition = new LinkedHashMap<>(asSection(entry.getValue()));
                definition.remove("equipment");
                definition.remove("consumable");
                sanitizedItems.put(entry.getKey(), definition);
            }
            sanitized.put("items", sanitizedItems);
        }

        return sanitized;
    }

    public void read(String fileName, Map<String, Object> content, Map<String, Object> convertedItems) {
        readEquipments(fileName, asSection(content.get("equipments")));
        readItemProperties(asSection(content.get("items")), convertedItems);
    }

    private void readEquipments(String fileName, Map<String, Object> section) {
        for (Map.Entry<String, Object> entry : section.entrySet()) {
            String fullId = qualifyId(entry.getKey());
            Map<String, Object> definition = asSection(entry.getValue());
            if (definition.isEmpty()) continue;

            String type = String.valueOf(definition.getOrDefault("type", "armor"));
            if (!"armor".equalsIgnoreCase(type)) {
                report.unsupported(fullId, "type d'équipement ItemsAdder non converti : " + type);
                continue;
            }

            Map<String, Object> equipment = new LinkedHashMap<>();
            equipment.put("type", "component");

            Object layer1 = definition.get("layer_1");
            if (layer1 instanceof String path && !path.isBlank()) {
                equipment.put("humanoid", qualifyTexture(path));
                reportMissingTexture(fullId, path);
            }

            Object layer2 = definition.get("layer_2");
            if (layer2 instanceof String path && !path.isBlank()) {
                equipment.put("humanoid_leggings", qualifyTexture(path));
                reportMissingTexture(fullId, path);
            }

            if (equipment.size() == 1) {
                report.unsupported(namespace + "/" + fileName + " [equipments." + entry.getKey() + "]",
                        "aucune couche layer_1/layer_2 exploitable");
                continue;
            }

            equipments.put(fullId, equipment);
            report.getEquipmentIDs().add(fullId);
        }
    }

    private void readItemProperties(Map<String, Object> section, Map<String, Object> convertedItems) {
        for (Map.Entry<String, Object> entry : section.entrySet()) {
            String fullId = qualifyId(entry.getKey());
            Map<String, Object> definition = asSection(entry.getValue());
            Map<String, Object> item = mutableSection(convertedItems.get(fullId));
            if (item.isEmpty()) continue;

            Map<String, Object> equipment = asSection(definition.get("equipment"));
            if (!equipment.isEmpty()) {
                applyEquipment(fullId, definition, equipment, item);
            }

            Map<String, Object> consumable = asSection(definition.get("consumable"));
            if (!consumable.isEmpty()) {
                applyConsumable(fullId, consumable, item);
            }

            convertedItems.put(fullId, item);
        }
    }

    private void applyEquipment(String fullId, Map<String, Object> itemDefinition,
                                Map<String, Object> source, Map<String, Object> item) {
        String slot = inferSlot(itemDefinition, source);
        Object equipmentId = source.get("id");

        if (equipmentId instanceof String id && !id.isBlank()) {
            Map<String, Object> settings = mutableSection(item.get("settings"));
            Map<String, Object> target = new LinkedHashMap<>();
            target.put("asset_id", qualifyId(id));
            if (slot != null) target.put("slot", slot);
            settings.put("equipment", target);
            item.put("settings", settings);
        } else if (slot != null) {
            // Cas typique des casques 3D ItemsAdder : écrase le composant vanilla
            // sans asset_id afin que le modèle de l'item reste visible sur la tête.
            Map<String, Object> data = mutableSection(item.get("data"));
            data.put("equippable", Map.of("slot", slot));
            item.put("data", data);
        } else {
            report.unsupported(fullId, "equipment sans id et slot impossible à déterminer");
            return;
        }

        report.getEquipmentItemIDs().add(fullId);
    }

    private void applyConsumable(String fullId, Map<String, Object> source, Map<String, Object> item) {
        Number nutrition = source.get("nutrition") instanceof Number value ? value : null;
        Number saturation = source.get("saturation") instanceof Number value ? value : null;

        if (nutrition == null && saturation == null) {
            report.unsupported(fullId, "consumable sans nutrition/saturation");
            return;
        }

        Map<String, Object> data = mutableSection(item.get("data"));
        Map<String, Object> food = new LinkedHashMap<>();
        food.put("nutrition", nutrition == null ? 0 : nutrition.intValue());
        food.put("saturation", saturation == null ? 0.0 : saturation.doubleValue());
        food.put("can_always_eat", false);
        data.put("food", food);

        Map<String, Object> components = mutableSection(data.get("components"));
        // La présence du composant vanilla rend l'item consommable sur 1.21.4+.
        // Les valeurs par défaut de Minecraft/CraftEngine fournissent l'animation et la durée.
        components.put("minecraft:consumable", new LinkedHashMap<>());
        data.put("components", components);
        item.put("data", data);

        report.getConsumableIDs().add(fullId);
    }

    private String inferSlot(Map<String, Object> definition, Map<String, Object> equipment) {
        Object explicit = equipment.get("slot");
        if (explicit instanceof String slot && !slot.isBlank()) return normalizeSlot(slot);

        Map<String, Object> modifiers = asSection(equipment.get("slot_attribute_modifiers"));
        for (String key : modifiers.keySet()) {
            String normalized = normalizeSlot(key);
            if (normalized != null) return normalized;
        }

        Map<String, Object> resource = asSection(definition.get("resource"));
        String material = String.valueOf(resource.getOrDefault("material", "")).toLowerCase(Locale.ROOT);
        if (material.endsWith("_helmet") || material.equals("carved_pumpkin") || material.equals("player_head")) return "head";
        if (material.endsWith("_chestplate") || material.equals("elytra")) return "chest";
        if (material.endsWith("_leggings")) return "legs";
        if (material.endsWith("_boots")) return "feet";
        return null;
    }

    private String normalizeSlot(String slot) {
        return switch (slot.toLowerCase(Locale.ROOT)) {
            case "head", "helmet" -> "head";
            case "chest", "chestplate" -> "chest";
            case "legs", "leggings" -> "legs";
            case "feet", "boots" -> "feet";
            case "body" -> "body";
            case "saddle" -> "saddle";
            case "mainhand", "main_hand" -> "mainhand";
            case "offhand", "off_hand" -> "offhand";
            default -> null;
        };
    }

    private void reportMissingTexture(String fullId, String path) {
        if (!hasTexture(path)) report.missingAsset(fullId, "textures/" + stripExtension(path) + ".png");
    }

    private boolean hasTexture(String path) {
        if (path.contains(":")) return true;
        String texture = stripExtension(path) + ".png";
        return new File(namespaceDir, "textures/" + texture).isFile()
                || new File(namespaceDir, "resourcepack/assets/" + namespace + "/textures/" + texture).isFile()
                || new File(namespaceDir, "resourcepack/" + namespace + "/textures/" + texture).isFile();
    }

    private String qualifyTexture(String path) {
        return path.contains(":") ? stripExtension(path) : namespace + ":" + stripExtension(path);
    }

    private String qualifyId(String id) {
        return id.contains(":") ? id : namespace + ":" + id;
    }

    private String stripExtension(String path) {
        return path.endsWith(".png") ? path.substring(0, path.length() - 4) : path;
    }

    private Map<String, Object> mutableSection(Object value) {
        return new LinkedHashMap<>(asSection(value));
    }

    private Map<String, Object> asSection(Object value) {
        if (!(value instanceof Map<?, ?> map)) return Map.of();
        Map<String, Object> section = new LinkedHashMap<>();
        map.forEach((key, entry) -> section.put(String.valueOf(key), entry));
        return section;
    }
}
