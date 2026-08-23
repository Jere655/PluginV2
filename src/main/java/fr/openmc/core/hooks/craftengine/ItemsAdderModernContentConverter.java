package fr.openmc.core.hooks.craftengine;

import java.io.File;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Convertit les propriétés ItemsAdder modernes qui n'étaient pas prises en charge
 * par le convertisseur historique : équipements, consommables, attributs,
 * enchantements et recettes de cuisson.
 */
public final class ItemsAdderModernContentConverter {
    private final String namespace;
    private final File namespaceDir;
    private final ConversionReport report;
    private final Map<String, Object> equipments = new LinkedHashMap<>();
    private final Map<String, Object> recipes = new LinkedHashMap<>();
    private final LinkedHashMap<String, Map<String, Object>> legacyArmorRenderings = new LinkedHashMap<>();

    public ItemsAdderModernContentConverter(String namespace, File namespaceDir, ConversionReport report) {
        this.namespace = namespace;
        this.namespaceDir = namespaceDir;
        this.report = report;
    }

    public Map<String, Object> getEquipments() {
        return equipments;
    }

    public Map<String, Object> getRecipes() {
        return recipes;
    }

    /**
     * Retire uniquement les champs que ce convertisseur prend en charge afin que
     * le convertisseur historique ne les signale pas à tort comme non supportés.
     */
    public Map<String, Object> legacyCompatibleContent(Map<String, Object> content) {
        Map<String, Object> sanitized = new LinkedHashMap<>(content);
        sanitized.remove("equipments");
        sanitized.remove("legacy_armor_renderings");

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

        Map<String, Object> sourceRecipes = asSection(content.get("recipes"));
        if (!sourceRecipes.isEmpty()) {
            Map<String, Object> legacyRecipes = new LinkedHashMap<>(sourceRecipes);
            legacyRecipes.remove("cooking");
            legacyRecipes.remove("campfire_cooking");
            sanitized.put("recipes", legacyRecipes);
        }

        return sanitized;
    }

    public void read(String fileName, Map<String, Object> content, Map<String, Object> convertedItems) {
        legacyArmorRenderings.clear();
        Map<String, Object> shaders = asSection(content.get("legacy_armor_renderings"));
        if (!shaders.isEmpty()) {
            for (Map.Entry<String, Object> entry : shaders.entrySet()) {
                legacyArmorRenderings.put(qualifyId(entry.getKey()), asSection(entry.getValue()));
            }
        }
        readEquipments(fileName, asSection(content.get("equipments")));
        readItemProperties(asSection(content.get("items")), convertedItems);
        readRecipes(fileName, asSection(content.get("recipes")));
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

            Object attributes = definition.get("attribute_modifiers");
            if (attributes instanceof Map<?, ?>) {
                applyAttributeModifiers(fullId, asSection(attributes), item);
            }

            Object enchants = definition.get("enchants");
            if (enchants instanceof List<?> list && !list.isEmpty()) {
                applyEnchantments(fullId, list, item);
            }

            convertedItems.put(fullId, item);
        }
    }

    @SuppressWarnings("unchecked")
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

            Object legacyRenderingId = source.get("legacy_armor_rendering_id");
            if (legacyRenderingId instanceof String shaderId && !shaderId.isBlank()) {
                Map<String, Object> shader = legacyArmorRenderings.get(qualifyId(shaderId));
                if (shader != null && shader.get("color") instanceof String color && !color.isBlank()) {
                    Map<String, Object> equipmentEntry = (Map<String, Object>) equipments.get(qualifyId(id));
                    if (equipmentEntry != null) {
                        Map<String, Object> dyeable = new LinkedHashMap<>();
                        dyeable.put("color_when_undyed", "#" + color);
                        equipmentEntry.put("dyeable", dyeable);
                    }
                }
            }
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

    private void applyAttributeModifiers(String fullId, Map<String, Object> source, Map<String, Object> item) {
        List<Map<String, Object>> converted = new ArrayList<>();
        for (Map.Entry<String, Object> slotEntry : source.entrySet()) {
            String slot = normalizeAttributeSlot(slotEntry.getKey());
            if (slot == null) {
                report.unsupported(fullId, "slot attribute_modifiers inconnu : " + slotEntry.getKey());
                continue;
            }
            Map<String, Object> modifiers = asSection(slotEntry.getValue());
            for (Map.Entry<String, Object> modifier : modifiers.entrySet()) {
                if (!(modifier.getValue() instanceof Number amount)) {
                    report.unsupported(fullId, "valeur attribute_modifiers non numérique : " + modifier.getKey());
                    continue;
                }
                String type = normalizeAttributeType(modifier.getKey());
                if (type == null) {
                    report.unsupported(fullId, "attribute_modifiers inconnu : " + modifier.getKey());
                    continue;
                }
                Map<String, Object> target = new LinkedHashMap<>();
                target.put("type", type);
                target.put("slot", slot);
                target.put("id", namespace + ":" + sanitizeId(fullId.substring(fullId.indexOf(':') + 1) + "_" + modifier.getKey()));
                target.put("amount", amount.doubleValue());
                target.put("operation", "add_value");
                converted.add(target);
            }
        }
        if (!converted.isEmpty()) {
            Map<String, Object> data = mutableSection(item.get("data"));
            data.put("attribute_modifiers", converted);
            item.put("data", data);
        }
    }

    private void applyEnchantments(String fullId, List<?> source, Map<String, Object> item) {
        Map<String, Object> enchantments = new LinkedHashMap<>();
        for (Object raw : source) {
            if (!(raw instanceof String value) || value.isBlank()) continue;
            int split = value.lastIndexOf(':');
            if (split <= 0 || split == value.length() - 1) {
                report.unsupported(fullId, "enchant invalide : " + value);
                continue;
            }
            String enchantment = value.substring(0, split).toLowerCase(Locale.ROOT);
            try {
                int level = Integer.parseInt(value.substring(split + 1));
                enchantments.put(enchantment, level);
            } catch (NumberFormatException e) {
                report.unsupported(fullId, "niveau d'enchant invalide : " + value);
            }
        }
        if (!enchantments.isEmpty()) {
            Map<String, Object> data = mutableSection(item.get("data"));
            data.put("enchantments", Map.of("merge", true, "enchantments", enchantments));
            item.put("data", data);
        }
    }

    private void readRecipes(String fileName, Map<String, Object> section) {
        readCookingRecipes(fileName, asSection(section.get("cooking")));
        readCampfireRecipes(fileName, asSection(section.get("campfire_cooking")));
    }

    private void readCookingRecipes(String fileName, Map<String, Object> section) {
        for (Map.Entry<String, Object> entry : section.entrySet()) {
            String baseId = qualifyId(entry.getKey());
            Map<String, Object> definition = asSection(entry.getValue());
            String ingredient = cookingIngredient(definition);
            Map<String, Object> result = cookingResult(definition);
            if (ingredient == null || result == null) {
                report.unsupported(baseId, "recette cooking incomplète dans " + fileName);
                continue;
            }

            List<String> machines = stringList(definition.get("machines"));
            if (machines.isEmpty()) machines = List.of("FURNACE");

            for (String machine : machines) {
                String type;
                String suffix;
                int defaultTime;
                switch (machine.toUpperCase(Locale.ROOT)) {
                    case "FURNACE" -> { type = "smelting"; suffix = "furnace"; defaultTime = 200; }
                    case "SMOKER" -> { type = "smoking"; suffix = "smoker"; defaultTime = 100; }
                    case "BLAST_FURNACE", "BLASTING" -> { type = "blasting"; suffix = "blasting"; defaultTime = 100; }
                    default -> {
                        report.unsupported(baseId, "machine de cuisson ItemsAdder non convertie : " + machine);
                        continue;
                    }
                }

                String id = baseId + "_" + suffix;
                Map<String, Object> recipe = cookingRecipe(type, ingredient, result, definition, defaultTime);
                recipes.put(id, recipe);
                report.getRecipeIDs().add(id);
            }
        }
    }

    private void readCampfireRecipes(String fileName, Map<String, Object> section) {
        for (Map.Entry<String, Object> entry : section.entrySet()) {
            String baseId = qualifyId(entry.getKey());
            Map<String, Object> definition = asSection(entry.getValue());
            String ingredient = cookingIngredient(definition);
            Map<String, Object> result = cookingResult(definition);
            if (ingredient == null || result == null) {
                report.unsupported(baseId, "recette campfire_cooking incomplète dans " + fileName);
                continue;
            }

            String id = baseId + "_campfire";
            recipes.put(id, cookingRecipe("campfire_cooking", ingredient, result, definition, 600));
            report.getRecipeIDs().add(id);
        }
    }

    private Map<String, Object> cookingRecipe(String type, String ingredient, Map<String, Object> result,
                                              Map<String, Object> definition, int defaultTime) {
        Map<String, Object> recipe = new LinkedHashMap<>();
        recipe.put("type", type);
        recipe.put("ingredients", List.of(ingredient));
        recipe.put("result", result);
        recipe.put("time", definition.get("cook_time") instanceof Number n ? n.intValue() : defaultTime);
        recipe.put("exp", definition.get("exp") instanceof Number n ? n.doubleValue() : 0.0);
        return recipe;
    }

    private String cookingIngredient(Map<String, Object> definition) {
        Object raw = definition.get("ingredient");
        if (raw instanceof String value) return toItemId(value);
        Map<String, Object> ingredient = asSection(raw);
        Object item = ingredient.get("item");
        if (!(item instanceof String value) || value.isBlank()) return null;
        if (ingredient.get("amount") instanceof Number n && n.intValue() != 1) {
            report.unsupported(qualifyId("recipe_ingredient"), "quantité de cuisson > 1 ignorée : " + n.intValue());
        }
        return toItemId(value);
    }

    private Map<String, Object> cookingResult(Map<String, Object> definition) {
        Map<String, Object> source = asSection(definition.get("result"));
        Object item = source.get("item");
        if (!(item instanceof String value) || value.isBlank()) return null;
        int count = source.get("amount") instanceof Number n ? n.intValue() : 1;
        return Map.of("id", toItemId(value), "count", count);
    }

    private List<String> stringList(Object value) {
        if (!(value instanceof List<?> list)) return List.of();
        List<String> result = new ArrayList<>();
        for (Object element : list) result.add(String.valueOf(element));
        return result;
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

    private String normalizeAttributeSlot(String slot) {
        return switch (slot.toLowerCase(Locale.ROOT)) {
            case "mainhand", "main_hand" -> "mainhand";
            case "offhand", "off_hand" -> "offhand";
            case "hand" -> "hand";
            case "head", "helmet" -> "head";
            case "chest", "chestplate" -> "chest";
            case "legs", "leggings" -> "legs";
            case "feet", "boots" -> "feet";
            case "armor" -> "armor";
            case "body" -> "body";
            case "saddle" -> "saddle";
            case "any" -> "any";
            default -> null;
        };
    }

    private String normalizeAttributeType(String type) {
        return switch (type.toLowerCase(Locale.ROOT).replace("_", "")) {
            case "attackdamage" -> "minecraft:attack_damage";
            case "attackspeed" -> "minecraft:attack_speed";
            case "attackknockback" -> "minecraft:attack_knockback";
            case "armor" -> "minecraft:armor";
            case "armortoughness" -> "minecraft:armor_toughness";
            case "knockbackresistance" -> "minecraft:knockback_resistance";
            case "luck" -> "minecraft:luck";
            case "maxhealth" -> "minecraft:max_health";
            case "movementspeed" -> "minecraft:movement_speed";
            case "flyingspeed" -> "minecraft:flying_speed";
            case "followrange" -> "minecraft:follow_range";
            case "jumpstrength" -> "minecraft:jump_strength";
            case "scale" -> "minecraft:scale";
            case "stepheight" -> "minecraft:step_height";
            default -> null;
        };
    }

    private String sanitizeId(String value) {
        return value.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9_./-]", "_");
    }

    private String toItemId(String value) {
        String id = value.trim();
        if (id.contains(":")) return id.toLowerCase(Locale.ROOT);
        return "minecraft:" + id.toLowerCase(Locale.ROOT);
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
