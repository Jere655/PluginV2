package fr.openmc.core.hooks.craftengine;

import java.io.File;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Convertit les cultures ItemsAdder en blocs CraftEngine avec le comportement crop_block.
 * Chaque culture devient un bloc multi-états (age 0..max_age) avec le comportement de croissance
 * et un loot récolte.
 */
public final class ItemsAdderCropConverter {
    private static final double DEFAULT_GROW_SPEED = 0.125;
    private static final int DEFAULT_MAX_LIGHT = 15;
    private static final int DEFAULT_MIN_LIGHT = 9;

    private final String namespace;
    private final File namespaceDir;
    private final ConversionReport report;
    private final Map<String, Object> crops = new LinkedHashMap<>();

    public ItemsAdderCropConverter(String namespace, File namespaceDir, ConversionReport report) {
        this.namespace = namespace;
        this.namespaceDir = namespaceDir;
        this.report = report;
    }

    public Map<String, Object> getCrops() {
        return crops;
    }

    public Map<String, Object> legacyCompatibleContent(Map<String, Object> content) {
        Map<String, Object> sanitized = new LinkedHashMap<>(content);
        sanitized.remove("crops");
        return sanitized;
    }

    public void read(String fileName, Map<String, Object> content) {
        Map<String, Object> section = asSection(content.get("crops"));
        for (Map.Entry<String, Object> entry : section.entrySet()) {
            String id = entry.getKey();
            Map<String, Object> definition = asSection(entry.getValue());
            if (definition.isEmpty()) continue;

            convertCrop(fileName, id, definition);
        }
    }

    private void convertCrop(String fileName, String id, Map<String, Object> definition) {
        String fullId = namespace + ":" + id;

        Object maxAge = definition.get("max_age");
        int maxAgeValue = maxAge instanceof Number number ? number.intValue() : 3;

        Object minLight = definition.get("min_light");
        int minLightValue = minLight instanceof Number number ? number.intValue() : DEFAULT_MIN_LIGHT;

        Map<String, Object> itemDrop = asSection(definition.get("item_drop"));
        Map<String, Object> modelsByTextures = asSection(definition.get("models_by_textures"));

        Map<String, Object> block = new LinkedHashMap<>();

        block.put("state", cropStates(id, modelsByTextures, maxAgeValue));
        block.put("behavior", cropBehavior(definition, minLightValue, maxLightValue(definition)));
        block.put("settings", cropSettings(fullId));

        Map<String, Object> loot = cropLoot(fullId, itemDrop, maxAgeValue);
        if (loot != null) {
            block.put("loot", loot);
        }

        reportUnsupportedCropFields(fullId, fileName, definition);

        crops.put(fullId, block);
        report.getBlockIDs().add(fullId);
    }

    private int maxLightValue(Map<String, Object> definition) {
        Object maxLight = definition.get("max_light");
        return maxLight instanceof Number number ? number.intValue() : DEFAULT_MAX_LIGHT;
    }

    private Map<String, Object> cropStates(String id, Map<String, Object> modelsByTextures, int maxAge) {
        Map<String, Object> properties = new LinkedHashMap<>();
        Map<String, Object> age = new LinkedHashMap<>();
        age.put("type", "int");
        age.put("default", "0");
        age.put("range", "0~" + maxAge);
        properties.put("age", age);

        Map<String, Object> appearances = new LinkedHashMap<>();
        String texturesPrefix = modelsByTextures.get("textures_prefix") instanceof String prefix ? prefix : "blocks/crops/" + id + "/" + id + "_";
        String textureKey = modelsByTextures.get("model_texture_key") instanceof String key ? key : "layer0";

        for (int stage = 0; stage <= maxAge; stage++) {
            String stageName = "age" + stage;
            String texture = stripExtension(texturesPrefix + stage + ".png");

            Map<String, Object> appearance = new LinkedHashMap<>();
            appearance.put("auto_state", "solid");
            appearance.put("model", Map.of("texture", namespace + ":" + texture));
            appearances.put(stageName, appearance);
        }

        Map<String, Object> states = new LinkedHashMap<>();
        states.put("properties", properties);
        states.put("appearances", appearances);

        return states;
    }

    private Map<String, Object> cropBehavior(Map<String, Object> definition, int minLight, int maxLight) {
        Map<String, Object> behavior = new LinkedHashMap<>();
        behavior.put("type", "crop_block");

        Object growSpeed = definition.get("grow_speed");
        behavior.put("grow_speed", growSpeed instanceof Number number ? number.doubleValue() : DEFAULT_GROW_SPEED);

        behavior.put("light_requirement", minLight);
        behavior.put("max_light_requirement", maxLight);

        Object boneMeal = definition.get("is_bone_meal_target");
        if (boneMeal instanceof Boolean boneMealTarget) {
            behavior.put("is_bone_meal_target", boneMealTarget);
        }

        Object boneMealAge = definition.get("bone_meal_age_bonus");
        if (boneMealAge instanceof Number number) {
            behavior.put("bone_meal_age_bonus", Map.of(
                    "type", "uniform",
                    "min", 1,
                    "max", number.intValue()
            ));
        }

        return behavior;
    }

    private Map<String, Object> cropSettings(String fullId) {
        Map<String, Object> settings = new LinkedHashMap<>();
        settings.put("item", fullId);
        return settings;
    }

    private Map<String, Object> cropLoot(String fullId, Map<String, Object> itemDrop, int maxAge) {
        if (itemDrop.isEmpty()) {
            return Map.of("pools", List.of(Map.of("rolls", 1, "entries", List.of(Map.of(
                    "type", "item",
                    "item", fullId
            )))));
        }

        if (!(itemDrop.get("item") instanceof String item)) {
            report.unsupported(fullId, "culture sans item_drop.item exploitable");
            return null;
        }

        int min = itemDrop.get("min_amount") instanceof Number number ? number.intValue() : 1;
        int max = itemDrop.get("max_amount") instanceof Number number ? number.intValue() : min;
        double chance = itemDrop.get("chance") instanceof Number number ? number.doubleValue() : 100.0;

        Map<String, Object> entry = new LinkedHashMap<>();
        entry.put("type", "item");
        entry.put("item", toItemId(item));

        if (chance < 100.0) {
            entry.put("conditions", List.of(Map.of("type", "random", "value", chance / 100.0)));
        }

        if (min != 1 || max != 1) {
            entry.put("functions", List.of(Map.of(
                    "type", "set_count",
                    "add", false,
                    "count", min + "~" + max
            )));
        }

        return Map.of("pools", List.of(Map.of("rolls", 1, "entries", List.of(entry))));
    }

    private void reportUnsupportedCropFields(String fullId, String fileName, Map<String, Object> definition) {
        for (String key : definition.keySet()) {
            if (!List.of("models_by_textures", "max_age", "min_light", "max_light", "seed",
                    "bottom_block", "item_drop", "grow_speed", "is_bone_meal_target",
                    "bone_meal_age_bonus", "spawn_light_requirement", "max_spawn_light_requirement").contains(key)) {
                report.unsupported(fullId, "propriété culture ItemsAdder non convertie : " + key);
            }
        }
    }

    private String toItemId(String id) {
        String value = id.trim().toLowerCase(Locale.ROOT);
        if (value.contains(":")) return value;
        return "minecraft:" + value;
    }

    private String stripExtension(String path) {
        return path.endsWith(".png") ? path.substring(0, path.length() - ".png".length()) : path;
    }

    private Map<String, Object> asSection(Object value) {
        if (!(value instanceof Map<?, ?> map)) return Map.of();
        Map<String, Object> section = new LinkedHashMap<>();
        map.forEach((key, entry) -> section.put(String.valueOf(key), entry));
        return section;
    }
}
