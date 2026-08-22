package fr.openmc.core.hooks.craftengine;

import java.io.File;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Convertit les propriétés ItemsAdder modernes qui n'étaient pas prises en charge
 * par le convertisseur historique : équipements, consommables, attributs et enchantements.
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
                definition.remove("attribute_modifiers");
                definition.remove("enchants");
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

            Map<String, Object> attributes = asSection(definition.get("attribute_modifiers"));
            if (!attributes.isEmpty()) {
                applyAttributeModifiers(fullId, attributes, item);
            }

            if (definition.containsKey("enchants")) {
                applyEnchantments(fullId, definition.get("enchants"), item);
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
        components.put("minecraft:consumable", new LinkedHashMap<>());
        data.put("components", components);
        item.put("data", data);

        report.getConsumableIDs().add(fullId);
    }

    /**
     * ItemsAdder stocke les attributs sous forme slot -> nom -> valeur, par exemple
     * mainhand.attackDamage=4. CraftEngine accepte une liste de modifiers avec type,
     * slot, id déterministe et amount.
     */
    private void applyAttributeModifiers(String fullId, Map<String, Object> source, Map<String, Object> item) {
        List<Map<String, Object>> converted = new ArrayList<>();

        for (Map.Entry<String, Object> slotEntry : source.entrySet()) {
            String slot = normalizeSlot(slotEntry.getKey());
            if (slot == null) {
                report.unsupported(fullId, "slot d'attribut ItemsAdder inconnu : " + slotEntry.getKey());
                continue;
            }

            Map<String, Object> modifiers = asSection(slotEntry.getValue());
            for (Map.Entry<String, Object> modifierEntry : modifiers.entrySet()) {
                if (!(modifierEntry.getValue() instanceof Number amount)) {
                    report.unsupported(fullId, "valeur d'attribut non numérique : " + modifierEntry.getKey());
                    continue;
                }

                String attribute = normalizeAttribute(modifierEntry.getKey());
                Map<String, Object> modifier = new LinkedHashMap<>();
                modifier.put("type", attribute);
                modifier.put("slot", slot);
                modifier.put("id", modifierId(fullId, slot, attribute));
                modifier.put("amount", amount.doubleValue());
                converted.add(modifier);
            }
        }

        if (converted.isEmpty()) return;

        Map<String, Object> data = mutableSection(item.get("data"));
        data.put("attribute_modifiers", converted);
        item.put("data", data);
    }

    /**
     * Convertit les formes ItemsAdder les plus courantes :
     * - liste : [minecraft:sharpness:10]
     * - map : {minecraft:sharpness: 10}
     */
    private void applyEnchantments(String fullId, Object source, Map<String, Object> item) {
        Map<String, Object> enchantments = new LinkedHashMap<>();

        if (source instanceof List<?> list) {
            for (Object raw : list) {
                parseEnchantment(fullId, String.valueOf(raw), enchantments);
            }
        } else if (source instanceof Map<?, ?> map) {
            for (Map.Entry<?, ?> entry : map.entrySet()) {
                if (entry.getValue() instanceof Number level) {
                    enchantments.put(normalizeEnchantmentId(String.valueOf(entry.getKey())), level.intValue());
                } else {
                    report.unsupported(fullId, "niveau d'enchantement non numérique : " + entry.getKey());
                }
            }
        } else if (source != null) {
            parseEnchantment(fullId, String.valueOf(source), enchantments);
        }

        if (enchantments.isEmpty()) return;

        Map<String, Object> data = mutableSection(item.get("data"));
        data.put("enchantments", enchantments);
        item.put("data", data);
    }

    private void parseEnchantment(String fullId, String raw, Map<String, Object> enchantments) {
        int separator = raw.lastIndexOf(':');
        if (separator <= 0 || separator == raw.length() - 1) {
            report.unsupported(fullId, "format d'enchantement ItemsAdder non reconnu : " + raw);
            return;
        }

        String id = raw.substring(0, separator);
        String levelText = raw.substring(separator + 1);
        try {
            int level = Integer.parseInt(levelText);
            enchantments.put(normalizeEnchantmentId(id), level);
        } catch (NumberFormatException exception) {
            report.unsupported(fullId, "niveau d'enchantement invalide : " + raw);
        }
    }

    private String normalizeAttribute(String attribute) {
        if (attribute.contains(":")) return attribute.toLowerCase(Locale.ROOT);

        StringBuilder normalized = new StringBuilder("minecraft:");
        for (int i = 0; i < attribute.length(); i++) {
            char current = attribute.charAt(i);
            if (Character.isUpperCase(current)) {
                normalized.append('_').append(Character.toLowerCase(current));
            } else {
                normalized.append(Character.toLowerCase(current));
            }
        }
        return normalized.toString();
    }

    private String normalizeEnchantmentId(String id) {
        String normalized = id.toLowerCase(Locale.ROOT);
        return normalized.contains(":") ? normalized : "minecraft:" + normalized;
    }

    private String modifierId(String fullId, String slot, String attribute) {
        int separator = fullId.indexOf(':');
        String itemNamespace = separator >= 0 ? fullId.substring(0, separator) : namespace;
        String itemPath = separator >= 0 ? fullId.substring(separator + 1) : fullId;
        String attributePath = attribute.contains(":") ? attribute.substring(attribute.indexOf(':') + 1) : attribute;
        return itemNamespace + ":" + sanitizePath(itemPath + "_" + slot + "_" + attributePath);
    }

    private String sanitizePath(String value) {
        return value.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9_./-]", "_");
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
            case "any" -> "any";
            case "hand" -> "hand";
            case "head", "helmet" -> "head";
            case "chest", "chestplate" -> "chest";
            case "legs", "leggings" -> "legs";
            case "feet", "boots" -> "feet";
            case "armor" -> "armor";
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
