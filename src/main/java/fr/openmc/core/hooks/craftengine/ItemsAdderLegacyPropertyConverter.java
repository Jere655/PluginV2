package fr.openmc.core.hooks.craftengine;

import org.yaml.snakeyaml.Yaml;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Convertit quelques propriétés ItemsAdder qui reposent encore sur des formats
 * historiques mais qui ont un équivalent sûr dans CraftEngine/OpenMC.
 *
 * <p>Actuellement :
 * <ul>
 *     <li>{@code components_nbt_file}: fusion des data-components JSON/YAML dans l'item CraftEngine.</li>
 *     <li>{@code nbt}: conservation des PublicBukkitValues simples dans le custom_data CraftEngine.</li>
 *     <li>{@code blocked_enchants: [ALL]}: suppression du composant vanilla enchantable.</li>
 *     <li>{@code blocked_enchants} spécifiques: marqueur PDC OpenMC, appliqué au runtime par BlockedEnchantListener.</li>
 * </ul>
 */
public final class ItemsAdderLegacyPropertyConverter {
    private static final Pattern PUBLIC_BUKKIT_VALUES = Pattern.compile(
            "[\"']?PublicBukkitValues[\"']?\\s*:\\s*\\{(.*?)\\}", Pattern.DOTALL);
    private static final Pattern STRING_ENTRY = Pattern.compile(
            "[\\\"']?([a-zA-Z0-9_.:-]+)[\\\"']?\\s*:\\s*[\\\"']([^\\\"']*)[\\\"']");

    private final String namespace;
    private final File namespaceDir;
    private final ConversionReport report;

    public ItemsAdderLegacyPropertyConverter(String namespace, File namespaceDir, ConversionReport report) {
        this.namespace = namespace;
        this.namespaceDir = namespaceDir;
        this.report = report;
    }

    /**
     * Retire uniquement les propriétés traitées ici avant de passer la définition
     * au convertisseur historique, afin d'éviter de faux warnings "non converti".
     */
    public Map<String, Object> legacyCompatibleContent(Map<String, Object> content) {
        Map<String, Object> sanitized = new LinkedHashMap<>(content);
        Map<String, Object> items = asSection(content.get("items"));
        if (items.isEmpty()) return sanitized;

        Map<String, Object> sanitizedItems = new LinkedHashMap<>();
        for (Map.Entry<String, Object> entry : items.entrySet()) {
            Map<String, Object> definition = new LinkedHashMap<>(asSection(entry.getValue()));
            definition.remove("components_nbt_file");
            definition.remove("blocked_enchants");
            definition.remove("nbt");
            sanitizedItems.put(entry.getKey(), definition);
        }
        sanitized.put("items", sanitizedItems);
        return sanitized;
    }

    public void read(String fileName, Map<String, Object> content, Map<String, Object> convertedItems) {
        Map<String, Object> items = asSection(content.get("items"));
        for (Map.Entry<String, Object> entry : items.entrySet()) {
            String fullId = qualifyId(entry.getKey());
            Map<String, Object> definition = asSection(entry.getValue());
            Map<String, Object> item = mutableSection(convertedItems.get(fullId));
            if (item.isEmpty()) continue;

            Object componentsFile = definition.get("components_nbt_file");
            if (componentsFile instanceof String path && !path.isBlank()) {
                applyComponentsFile(fileName, fullId, path, item);
            }

            Object directNbt = definition.get("nbt");
            if (directNbt instanceof String snbt && !snbt.isBlank()) {
                applyDirectNbt(fullId, snbt, item);
            }

            Object blocked = definition.get("blocked_enchants");
            if (blocked instanceof List<?> list && !list.isEmpty()) {
                applyBlockedEnchants(fullId, list, item);
            }

            convertedItems.put(fullId, item);
        }
    }

    private void applyComponentsFile(String sourceFile, String fullId, String relativePath, Map<String, Object> item) {
        try {
            File root = namespaceDir.getCanonicalFile();
            File source = new File(namespaceDir, relativePath).getCanonicalFile();
            if (!source.toPath().startsWith(root.toPath())) {
                report.unsupported(fullId, "components_nbt_file hors du namespace : " + relativePath);
                return;
            }
            if (!source.isFile()) {
                report.missingAsset(fullId, relativePath);
                return;
            }

            Object loaded = new Yaml().load(Files.readString(source.toPath(), StandardCharsets.UTF_8));
            Map<String, Object> rootData = asSection(loaded);
            Map<String, Object> sourceComponents = asSection(rootData.get("components"));
            if (sourceComponents.isEmpty()) {
                report.unsupported(fullId, "components_nbt_file sans section components : " + relativePath);
                return;
            }

            Map<String, Object> data = mutableSection(item.get("data"));
            Map<String, Object> components = mutableSection(data.get("components"));

            for (Map.Entry<String, Object> component : sourceComponents.entrySet()) {
                String key = normalizeComponentKey(component.getKey());
                if ("minecraft:food".equals(key)) {
                    mergeFoodComponent(fullId, component.getValue(), data);
                    continue;
                }

                Object value = component.getValue();
                if (value instanceof Map<?, ?>) {
                    Map<String, Object> merged = mutableSection(components.get(key));
                    merged.putAll(asSection(value));
                    components.put(key, merged);
                } else {
                    components.put(key, value);
                }
            }

            if (!components.isEmpty()) data.put("components", components);
            item.put("data", data);

            for (String extra : rootData.keySet()) {
                if (!"components".equals(extra)) {
                    report.unsupported(fullId,
                            "clé components_nbt_file non convertie (" + sourceFile + "): " + extra);
                }
            }
        } catch (Exception e) {
            report.unsupported(fullId,
                    "components_nbt_file illisible " + relativePath + " : " + e.getMessage());
        }
    }

    private void mergeFoodComponent(String fullId, Object value, Map<String, Object> data) {
        Map<String, Object> sourceFood = asSection(value);
        if (sourceFood.isEmpty()) {
            report.unsupported(fullId, "composant food vide dans components_nbt_file");
            return;
        }

        Map<String, Object> food = mutableSection(data.get("food"));
        Object nutrition = sourceFood.get("nutrition");
        if (nutrition instanceof Number number) food.put("nutrition", number.intValue());

        Object saturation = sourceFood.get("saturation");
        if (saturation instanceof Number number) food.put("saturation", number.doubleValue());

        Object alwaysEat = sourceFood.get("can_always_eat");
        if (alwaysEat instanceof Boolean bool) food.put("can_always_eat", bool);

        // CraftEngine attend une définition food complète. Les anciens fichiers
        // ItemsAdder peuvent ne préciser que can_always_eat.
        food.putIfAbsent("nutrition", 0);
        food.putIfAbsent("saturation", 0.0);
        food.putIfAbsent("can_always_eat", false);
        data.put("food", food);

        for (String key : sourceFood.keySet()) {
            if (!List.of("nutrition", "saturation", "can_always_eat").contains(key)) {
                report.unsupported(fullId, "propriété food non convertie : " + key);
            }
        }
    }

    /**
     * Le contenu OpenMC utilise actuellement le NBT historique uniquement pour
     * PublicBukkitValues, par exemple :
     * {PublicBukkitValues:{"openmc:item_id": "ewenite"}}.
     * On traduit uniquement ce sous-ensemble connu au lieu de prétendre parser
     * arbitrairement tout le SNBT ItemsAdder.
     */
    private void applyDirectNbt(String fullId, String snbt, Map<String, Object> item) {
        Matcher sectionMatcher = PUBLIC_BUKKIT_VALUES.matcher(snbt);
        if (!sectionMatcher.find()) {
            report.unsupported(fullId, "nbt direct hors du sous-ensemble PublicBukkitValues supporté");
            return;
        }

        Map<String, Object> values = new LinkedHashMap<>();
        Matcher entryMatcher = STRING_ENTRY.matcher(sectionMatcher.group(1));
        while (entryMatcher.find()) {
            values.put(entryMatcher.group(1), entryMatcher.group(2));
        }

        if (values.isEmpty()) {
            report.unsupported(fullId, "nbt PublicBukkitValues sans valeur chaîne convertible");
            return;
        }

        mergePublicBukkitValues(item, values);

        String stripped = snbt.replace(sectionMatcher.group(), "").replaceAll("[{}\\s,]", "");
        if (!stripped.isEmpty()) {
            report.unsupported(fullId, "nbt direct partiellement converti : données hors PublicBukkitValues restantes");
        }
    }

    private void applyBlockedEnchants(String fullId, List<?> blocked, Map<String, Object> item) {
        List<String> values = new ArrayList<>();
        for (Object value : blocked) {
            if (value != null) values.add(String.valueOf(value).trim().toUpperCase(Locale.ROOT));
        }

        boolean blockAll = values.stream().anyMatch("ALL"::equalsIgnoreCase);
        if (blockAll) {
            Map<String, Object> data = mutableSection(item.get("data"));
            List<String> removed = stringList(data.get("remove_components"));
            if (!removed.contains("minecraft:enchantable")) removed.add("minecraft:enchantable");
            data.put("remove_components", removed);
            item.put("data", data);
        }

        List<String> specific = values.stream()
                .filter(value -> !"ALL".equalsIgnoreCase(value))
                .distinct()
                .toList();
        if (!specific.isEmpty()) {
            // CraftEngine n'a pas de blacklist d'enchantements par item. On garde
            // donc la règle dans le PDC Bukkit puis BlockedEnchantListener
            // l'applique à la table d'enchantement et à l'enclume.
            mergePublicBukkitValues(item,
                    Map.of("openmc:blocked_enchants", String.join(",", specific)));
        }
    }

    private void mergePublicBukkitValues(Map<String, Object> item, Map<String, ?> additions) {
        Map<String, Object> data = mutableSection(item.get("data"));
        Map<String, Object> nbt = mutableSection(data.get("nbt"));
        Map<String, Object> publicValues = mutableSection(nbt.get("PublicBukkitValues"));
        publicValues.putAll(additions);
        nbt.put("PublicBukkitValues", publicValues);
        data.put("nbt", nbt);
        item.put("data", data);
    }

    private String normalizeComponentKey(String key) {
        String normalized = key.toLowerCase(Locale.ROOT);
        return normalized.contains(":") ? normalized : "minecraft:" + normalized;
    }

    private String qualifyId(String id) {
        return id.contains(":") ? id : namespace + ":" + id;
    }

    private List<String> stringList(Object value) {
        List<String> result = new ArrayList<>();
        if (value instanceof List<?> list) {
            for (Object element : list) result.add(String.valueOf(element));
        }
        return result;
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
