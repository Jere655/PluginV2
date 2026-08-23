package fr.openmc.core.hooks.craftengine;

import java.io.File;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Convertit les tables de loot ItemsAdder (loots.mobs, loots.blocks, loots.fishing, etc.)
 * en loot_sources CraftEngine.
 */
public final class ItemsAdderLootConverter {
    private final String namespace;
    private final ConversionReport report;
    private final Map<String, Object> lootSources = new LinkedHashMap<>();

    public ItemsAdderLootConverter(String namespace, ConversionReport report) {
        this.namespace = namespace;
        this.report = report;
    }

    public Map<String, Object> getLootSources() {
        return lootSources;
    }

    public Map<String, Object> legacyCompatibleContent(Map<String, Object> content) {
        Map<String, Object> sanitized = new LinkedHashMap<>(content);
        sanitized.remove("loots");
        return sanitized;
    }

    public void read(String fileName, Map<String, Object> content) {
        Map<String, Object> section = asSection(content.get("loots"));
        for (Map.Entry<String, Object> entry : section.entrySet()) {
            String category = entry.getKey();
            Map<String, Object> byTarget = asSection(entry.getValue());
            if (byTarget.isEmpty()) continue;

            switch (category) {
                case "mobs" -> readMobLoots(fileName, byTarget);
                case "blocks" -> readBlockLoots(fileName, byTarget);
                case "fishing" -> readFishingLoots(fileName, byTarget);
                case "chests" -> readChestLoots(fileName, byTarget);
                default -> report.unsupported(namespace + "/" + fileName + " [loots." + category + "]",
                        "catégorie de loot ItemsAdder non convertie : " + category);
            }
        }
    }

    private void readMobLoots(String fileName, Map<String, Object> mobs) {
        for (Map.Entry<String, Object> entry : mobs.entrySet()) {
            String targetId = entry.getKey();
            Map<String, Object> definition = asSection(entry.getValue());
            if (definition.isEmpty()) continue;

            String fullId = namespace + ":mob_" + targetId;
            String mobType = mobType(definition.get("type"));

            Map<String, Object> lootSource = new LinkedHashMap<>();
            lootSource.put("type", "entity_death");
            lootSource.put("target", mobType);

            Map<String, Object> nbt = asSection(definition.get("nbt"));
            if (!nbt.isEmpty()) {
                report.unsupported(fullId, "NBT ItemsAdder non convertible en condition CraftEngine : " + nbt);
            }

            Map<String, Object> items = asSection(definition.get("items"));
            if (items.isEmpty()) {
                report.unsupported(fullId, "loot mob sans items exploitables");
                continue;
            }

            lootSource.put("loot", mobLoot(fullId, items));

            lootSources.put(fullId, lootSource);
            report.getLootSourceIDs().add(fullId);
        }
    }

    private void readBlockLoots(String fileName, Map<String, Object> blocks) {
        for (Map.Entry<String, Object> entry : blocks.entrySet()) {
            String targetId = entry.getKey();
            Map<String, Object> definition = asSection(entry.getValue());
            if (definition.isEmpty()) continue;

            String fullId = namespace + ":block_" + targetId;

            Map<String, Object> lootSource = new LinkedHashMap<>();
            lootSource.put("type", "block_break");
            lootSource.put("target", toItemId(targetId));

            Map<String, Object> items = asSection(definition.get("items"));
            if (items.isEmpty()) {
                report.unsupported(fullId, "loot bloc sans items exploitables");
                continue;
            }

            lootSource.put("loot", blockLoot(fullId, items));

            lootSources.put(fullId, lootSource);
            report.getLootSourceIDs().add(fullId);
        }
    }

    private void readFishingLoots(String fileName, Map<String, Object> fishing) {
        for (Map.Entry<String, Object> entry : fishing.entrySet()) {
            String targetId = entry.getKey();
            Map<String, Object> definition = asSection(entry.getValue());
            if (definition.isEmpty()) continue;

            String fullId = namespace + ":fishing_" + targetId;

            Map<String, Object> lootSource = new LinkedHashMap<>();
            lootSource.put("type", "fishing");

            Map<String, Object> items = asSection(definition.get("items"));
            if (items.isEmpty()) {
                report.unsupported(fullId, "loot pêche sans items exploitables");
                continue;
            }

            lootSource.put("loot", fishingLoot(fullId, items));

            lootSources.put(fullId, lootSource);
            report.getLootSourceIDs().add(fullId);
        }
    }

    private void readChestLoots(String fileName, Map<String, Object> chests) {
        for (Map.Entry<String, Object> entry : chests.entrySet()) {
            String targetId = entry.getKey();
            Map<String, Object> definition = asSection(entry.getValue());
            if (definition.isEmpty()) continue;

            String fullId = namespace + ":chest_" + targetId;

            Map<String, Object> lootSource = new LinkedHashMap<>();
            lootSource.put("type", "container");
            lootSource.put("target", toItemId(targetId));

            Map<String, Object> items = asSection(definition.get("items"));
            if (items.isEmpty()) {
                report.unsupported(fullId, "loot coffre sans items exploitables");
                continue;
            }

            lootSource.put("loot", chestLoot(fullId, items));

            lootSources.put(fullId, lootSource);
            report.getLootSourceIDs().add(fullId);
        }
    }

    private Map<String, Object> mobLoot(String fullId, Map<String, Object> items) {
        List<Map<String, Object>> entries = new java.util.ArrayList<>();
        for (Map.Entry<String, Object> entry : items.entrySet()) {
            Map<String, Object> itemDef = asSection(entry.getValue());
            Map<String, Object> lootEntry = lootEntry(fullId, entry.getKey(), itemDef);
            if (lootEntry != null) entries.add(lootEntry);
        }
        return Map.of("pools", List.of(Map.of("rolls", 1, "entries", entries)));
    }

    private Map<String, Object> blockLoot(String fullId, Map<String, Object> items) {
        List<Map<String, Object>> entries = new java.util.ArrayList<>();
        for (Map.Entry<String, Object> entry : items.entrySet()) {
            Map<String, Object> itemDef = asSection(entry.getValue());
            Map<String, Object> lootEntry = lootEntry(fullId, entry.getKey(), itemDef);
            if (lootEntry != null) entries.add(lootEntry);
        }
        return Map.of("pools", List.of(Map.of("rolls", 1, "entries", entries)));
    }

    private Map<String, Object> fishingLoot(String fullId, Map<String, Object> items) {
        List<Map<String, Object>> entries = new java.util.ArrayList<>();
        for (Map.Entry<String, Object> entry : items.entrySet()) {
            Map<String, Object> itemDef = asSection(entry.getValue());
            Map<String, Object> lootEntry = lootEntry(fullId, entry.getKey(), itemDef);
            if (lootEntry != null) entries.add(lootEntry);
        }
        return Map.of("pools", List.of(Map.of("rolls", 1, "entries", entries)));
    }

    private Map<String, Object> chestLoot(String fullId, Map<String, Object> items) {
        List<Map<String, Object>> entries = new java.util.ArrayList<>();
        for (Map.Entry<String, Object> entry : items.entrySet()) {
            Map<String, Object> itemDef = asSection(entry.getValue());
            Map<String, Object> lootEntry = lootEntry(fullId, entry.getKey(), itemDef);
            if (lootEntry != null) entries.add(lootEntry);
        }
        return Map.of("pools", List.of(Map.of("rolls", 1, "entries", entries)));
    }

    private Map<String, Object> lootEntry(String fullId, String itemId, Map<String, Object> definition) {
        if (!(definition.get("item") instanceof String item)) {
            report.unsupported(fullId, "entrée de loot sans item : " + itemId);
            return null;
        }

        int min = definition.get("min_amount") instanceof Number number ? number.intValue() : 1;
        int max = definition.get("max_amount") instanceof Number number ? number.intValue() : min;
        double chance = definition.get("chance") instanceof Number number ? number.doubleValue() : 100.0;

        Map<String, Object> entry = new LinkedHashMap<>();
        entry.put("type", "item");
        entry.put("item", toItemId(item));

        if (chance < 100.0 && chance > 0.0) {
            entry.put("conditions", List.of(Map.of("type", "random", "value", chance / 100.0)));
        }

        if (min != 1 || max != 1) {
            entry.put("functions", List.of(Map.of(
                    "type", "set_count",
                    "add", false,
                    "count", min + "~" + max
            )));
        }

        return entry;
    }

    private String mobType(Object type) {
        if (!(type instanceof String value) || value.isBlank()) return "minecraft:pig";
        String upper = value.toUpperCase(Locale.ROOT);
        return switch (upper) {
            case "ZOMBIE" -> "minecraft:zombie";
            case "SKELETON" -> "minecraft:skeleton";
            case "CREEPER" -> "minecraft:creeper";
            case "SPIDER" -> "minecraft:spider";
            case "ENDERMAN" -> "minecraft:enderman";
            case "WITCH" -> "minecraft:witch";
            case "VILLAGER" -> "minecraft:villager";
            case "COW" -> "minecraft:cow";
            case "PIG" -> "minecraft:pig";
            case "SHEEP" -> "minecraft:sheep";
            case "CHICKEN" -> "minecraft:chicken";
            case "BLAZE" -> "minecraft:blaze";
            case "WITHER_SKELETON" -> "minecraft:wither_skeleton";
            case "GHAST" -> "minecraft:ghast";
            case "SLIME" -> "minecraft:slime";
            case "MAGMA_CUBE" -> "minecraft:magma_cube";
            case "PIGLIN" -> "minecraft:piglin";
            case "PIGLIN_BRUTE" -> "minecraft:piglin_brute";
            case "ZOMBIFIED_PIGLIN" -> "minecraft:zombified_piglin";
            case "HOGLIN" -> "minecraft:hoglin";
            case "ZOGLIN" -> "minecraft:zoglin";
            case "WARDEN" -> "minecraft:warden";
            default -> "minecraft:" + value.toLowerCase(Locale.ROOT);
        };
    }

    private String toItemId(String id) {
        String value = id.trim().toLowerCase(Locale.ROOT);
        if (value.contains(":")) return value;
        return "minecraft:" + value;
    }

    private Map<String, Object> asSection(Object value) {
        if (!(value instanceof Map<?, ?> map)) return Map.of();
        Map<String, Object> section = new LinkedHashMap<>();
        map.forEach((key, entry) -> section.put(String.valueOf(key), entry));
        return section;
    }
}
