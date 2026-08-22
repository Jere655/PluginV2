package fr.openmc.core.hooks.craftengine;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Convertit les catégories du menu ItemsAdder vers le navigateur d'items CraftEngine.
 */
public final class ItemsAdderCategoryConverter {
    private final String namespace;
    private final ConversionReport report;
    private final Map<String, Object> categories = new LinkedHashMap<>();

    public ItemsAdderCategoryConverter(String namespace, ConversionReport report) {
        this.namespace = namespace;
        this.report = report;
    }

    public Map<String, Object> getCategories() {
        return categories;
    }

    /** Retire la section prise en charge pour éviter un faux warning du convertisseur historique. */
    public Map<String, Object> legacyCompatibleContent(Map<String, Object> content) {
        Map<String, Object> sanitized = new LinkedHashMap<>(content);
        sanitized.remove("categories");
        return sanitized;
    }

    public void read(String fileName, Map<String, Object> content) {
        Map<String, Object> section = asSection(content.get("categories"));
        for (Map.Entry<String, Object> entry : section.entrySet()) {
            String fullId = qualifyId(entry.getKey());
            Map<String, Object> source = asSection(entry.getValue());
            if (source.isEmpty() || Boolean.FALSE.equals(source.get("enabled"))) continue;

            Map<String, Object> category = new LinkedHashMap<>();

            Object name = source.get("name");
            if (name instanceof String text && !text.isBlank()) {
                category.put("name", "<!i>" + text);
            }

            Object icon = source.get("icon");
            if (icon instanceof String id && !id.isBlank()) {
                category.put("icon", toItemId(id));
            }

            Object permission = source.get("permission");
            if (permission instanceof String node && !node.isBlank()) {
                category.put("conditions", List.of(Map.of(
                        "type", "permission",
                        "permission", node
                )));
            }

            List<String> items = stringList(source.get("items"));
            if (!items.isEmpty()) {
                List<String> converted = new ArrayList<>();
                for (String item : items) {
                    if (item.contains("*") || looksLikeRegex(item)) {
                        report.unsupported(fullId,
                                "entrée de catégorie ItemsAdder dynamique non convertie (" + fileName + "): " + item);
                        continue;
                    }
                    converted.add(toItemId(item));
                }
                if (!converted.isEmpty()) category.put("list", converted);
            }

            // Ces propriétés ne contrôlent que l'ancien GUI ItemsAdder et n'ont pas
            // d'équivalent utile dans le menu CraftEngine.
            for (String key : source.keySet()) {
                if (!List.of("enabled", "icon", "name", "permission", "items", "skip_if_already",
                        "font_image", "title_position_pixels").contains(key)) {
                    report.unsupported(fullId, "propriété de catégorie ItemsAdder non convertie : " + key);
                }
            }

            categories.put(fullId, category);
        }
    }

    private boolean looksLikeRegex(String value) {
        return value.contains("(.*)") || value.contains("\\:") || value.startsWith("^") || value.endsWith("$");
    }

    private String qualifyId(String id) {
        return id.contains(":") ? id : namespace + ":" + id;
    }

    private String toItemId(String id) {
        String value = id.trim().toLowerCase(Locale.ROOT);
        if (value.contains(":")) return value;
        return namespace + ":" + value;
    }

    private List<String> stringList(Object value) {
        if (!(value instanceof List<?> list)) return List.of();
        List<String> result = new ArrayList<>();
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
