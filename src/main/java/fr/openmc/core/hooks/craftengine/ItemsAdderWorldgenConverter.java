package fr.openmc.core.hooks.craftengine;

import java.io.File;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Convertit les génurateurs d'items au monde ItemsAdder (worlds_populators) en placed_features CraftEngine.
 * Chaque génrateur devient une feature de type minecraft:ore avec configuration de placement.
 */
public final class ItemsAdderWorldgenConverter {
    private final String namespace;
    private final ConversionReport report;
    private final Map<String, Object> placedFeatures = new LinkedHashMap<>();

    public ItemsAdderWorldgenConverter(String namespace, ConversionReport report) {
        this.namespace = namespace;
        this.report = report;
    }

    public Map<String, Object> getPlacedFeatures() {
        return placedFeatures;
    }

    public Map<String, Object> legacyCompatibleContent(Map<String, Object> content) {
        Map<String, Object> sanitized = new LinkedHashMap<>(content);
        sanitized.remove("worlds_populators");
        return sanitized;
    }

    public void read(String fileName, Map<String, Object> content) {
        Map<String, Object> section = asSection(content.get("worlds_populators"));
        for (Map.Entry<String, Object> entry : section.entrySet()) {
            String id = entry.getKey();
            Map<String, Object> definition = asSection(entry.getValue());
            if (definition.isEmpty()) continue;

            convertPopulator(fileName, id, definition);
        }
    }

    private void convertPopulator(String fileName, String id, Map<String, Object> definition) {
        String fullId = namespace + ":" + id;

        if (!(definition.get("block") instanceof String block)) {
            report.unsupported(fullId, "world_populator sans bloc cible");
            return;
        }

        int veinBlocks = definition.get("vein_blocks") instanceof Number number ? number.intValue() : 4;
        int chunkVeins = definition.get("chunk_veins") instanceof Number number ? number.intValue() : 1;
        double chunkChance = definition.get("chunk_chance") instanceof Number number ? number.doubleValue() : 100.0;
        int minHeight = definition.get("min_height") instanceof Number number ? number.intValue() : 0;
        int maxHeight = definition.get("max_height") instanceof Number number ? number.intValue() : 64;

        List<String> replaceableBlocks = stringList(definition.get("replaceable_blocks"));
        if (replaceableBlocks.isEmpty()) {
            report.unsupported(fullId, "world_populator sans blocs remplaçables");
            return;
        }

        Map<String, Object> feature = new LinkedHashMap<>();
        feature.put("feature", oreFeature(fullId, block, veinBlocks, replaceableBlocks));
        feature.put("placement", orePlacement(minHeight, maxHeight, chunkVeins));

        reportUnsupportedPopulatorFields(fullId, fileName, definition);

        placedFeatures.put(fullId, feature);
        report.getWorldgenIDs().add(fullId);
    }

    private Map<String, Object> oreFeature(String fullId, String block, int veinBlocks, List<String> replaceableBlocks) {
        Map<String, Object> feature = new LinkedHashMap<>();
        feature.put("type", "minecraft:ore");

        Map<String, Object> config = new LinkedHashMap<>();
        config.put("size", veinBlocks);
        config.put("discard_chance_on_air_exposure", 0.0);

        List<Map<String, Object>> targets = new java.util.ArrayList<>();
        for (String replaceable : replaceableBlocks) {
            targets.add(oreTarget(fullId, block, replaceable));
        }
        config.put("targets", targets);

        feature.put("config", config);
        return feature;
    }

    private Map<String, Object> oreTarget(String fullId, String block, String replaceable) {
        Map<String, Object> target = new LinkedHashMap<>();

        Map<String, Object> state = new LinkedHashMap<>();
        state.put("Name", toItemId(block));
        target.put("state", state);

        Map<String, Object> oreTarget = new LinkedHashMap<>();
        String tag = replaceableTag(replaceable);
        if (tag != null) {
            oreTarget.put("predicate_type", "minecraft:tag_match");
            oreTarget.put("tag", tag);
        } else {
            oreTarget.put("block", toItemId(replaceable));
        }
        target.put("target", oreTarget);

        return target;
    }

    private Map<String, Object> orePlacement(int minHeight, int maxHeight, int chunkVeins) {
        List<Map<String, Object>> placement = new java.util.ArrayList<>();

        placement.add(Map.of("type", "minecraft:count", "count", chunkVeins));
        placement.add(Map.of("type", "minecraft:in_square"));

        Map<String, Object> heightRange = new LinkedHashMap<>();
        heightRange.put("type", "minecraft:uniform");
        heightRange.put("min_inclusive", Map.of("absolute", minHeight));
        heightRange.put("max_inclusive", Map.of("absolute", maxHeight));

        Map<String, Object> heightPlacement = new LinkedHashMap<>();
        heightPlacement.put("type", "minecraft:height_range");
        heightPlacement.put("height", heightRange);
        placement.add(heightPlacement);

        return Map.of("placement", placement);
    }

    private String replaceableTag(String replaceable) {
        String upper = replaceable.toUpperCase(Locale.ROOT);
        return switch (upper) {
            case "STONE" -> "minecraft:stone_ore_replaceables";
            case "DEEPSLATE" -> "minecraft:deepslate_ore_replaceables";
            case "NETHERRACK" -> "minecraft:base_stone_nether";
            case "GRANITE", "DIORITE", "ANDESITE" -> null;
            default -> null;
        };
    }

    private void reportUnsupportedPopulatorFields(String fullId, String fileName, Map<String, Object> definition) {
        for (String key : definition.keySet()) {
            if (!List.of("block", "worlds", "replaceable_blocks", "chunk_chance",
                    "max_height", "min_height", "vein_blocks", "chunk_veins").contains(key)) {
                report.unsupported(fullId, "propriété worlds_populators ItemsAdder non convertie : " + key);
            }
        }
    }

    private String toItemId(String id) {
        String value = id.trim().toLowerCase(Locale.ROOT);
        if (value.contains(":")) return value;
        return "minecraft:" + value;
    }

    private List<String> stringList(Object value) {
        if (!(value instanceof List<?> list)) return List.of();
        List<String> result = new java.util.ArrayList<>();
        for (Object element : list) {
            if (element != null) result.add(String.valueOf(element));
        }
        return result;
    }

    private Map<String, Object> asSection(Object value) {
        if (!(value instanceof Map<?, ?> map)) return Map.of();
        Map<String, Object> section = new LinkedHashMap<>();
        map.forEach((key, entry) -> section.put(String.valueOf(key), entry));
        return section;
    }
}
