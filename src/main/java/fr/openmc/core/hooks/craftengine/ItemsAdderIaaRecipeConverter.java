package fr.openmc.core.hooks.craftengine;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Convertit les recettes ItemsAdderAdditions {@code iaa_crafting_table} vers les
 * recettes CraftEngine. CraftEngine 26.x sait imposer un {@code count} sur un
 * ingrédient, ce qui permet de conserver les recettes OpenMC à grosses quantités.
 */
public final class ItemsAdderIaaRecipeConverter {
    private final String namespace;
    private final ConversionReport report;
    private final Map<String, Object> recipes = new LinkedHashMap<>();

    public ItemsAdderIaaRecipeConverter(String namespace, ConversionReport report) {
        this.namespace = namespace;
        this.report = report;
    }

    public Map<String, Object> getRecipes() {
        return recipes;
    }

    /** Retire iaa_crafting_table du convertisseur standard pour éviter un faux warning. */
    public Map<String, Object> legacyCompatibleContent(Map<String, Object> content) {
        Map<String, Object> sanitized = new LinkedHashMap<>(content);
        Map<String, Object> sourceRecipes = asSection(content.get("recipes"));
        if (sourceRecipes.isEmpty()) return sanitized;

        Map<String, Object> remaining = new LinkedHashMap<>(sourceRecipes);
        remaining.remove("iaa_crafting_table");
        sanitized.put("recipes", remaining);
        return sanitized;
    }

    public void read(String fileName, Map<String, Object> content) {
        Map<String, Object> recipesSection = asSection(content.get("recipes"));
        Map<String, Object> iaa = asSection(recipesSection.get("iaa_crafting_table"));

        for (Map.Entry<String, Object> entry : iaa.entrySet()) {
            String baseId = qualifyId(entry.getKey());
            Map<String, Object> definition = asSection(entry.getValue());
            if (definition.isEmpty() || Boolean.FALSE.equals(definition.get("enabled"))) continue;

            Map<String, Object> result = result(definition);
            if (result == null) {
                report.unsupported(baseId, "recette iaa_crafting_table sans résultat valide dans " + fileName);
                continue;
            }

            if (Boolean.TRUE.equals(definition.get("shapeless"))) {
                Map<String, Object> converted = shapeless(definition, result, baseId);
                if (converted != null) {
                    recipes.put(baseId, converted);
                    report.getRecipeIDs().add(baseId);
                }
                continue;
            }

            List<List<String>> patterns = patterns(definition);
            if (patterns.isEmpty()) {
                report.unsupported(baseId, "recette iaa_crafting_table sans pattern exploitable");
                continue;
            }

            for (int index = 0; index < patterns.size(); index++) {
                String id = index == 0 ? baseId : baseId + "_pattern_" + (index + 1);
                Map<String, Object> converted = shaped(definition, patterns.get(index), result, baseId);
                if (converted != null) {
                    recipes.put(id, converted);
                    report.getRecipeIDs().add(id);
                }
            }
        }
    }

    private Map<String, Object> shapeless(Map<String, Object> definition,
                                           Map<String, Object> result,
                                           String recipeId) {
        if (!(definition.get("ingredients") instanceof List<?> source) || source.isEmpty()) {
            report.unsupported(recipeId, "recette iaa shapeless sans liste d'ingrédients");
            return null;
        }

        List<Object> ingredients = new ArrayList<>();
        for (Object raw : source) {
            Map<String, Object> ingredient = asSection(raw);
            Object item = ingredient.get("item");
            if (!(item instanceof String itemId) || itemId.isBlank()) {
                report.unsupported(recipeId, "ingrédient iaa shapeless sans item");
                return null;
            }
            int amount = positiveAmount(ingredient.get("amount"), 1);
            ingredients.add(ingredient(toItemId(itemId), amount));
        }

        Map<String, Object> recipe = new LinkedHashMap<>();
        recipe.put("type", "shapeless");
        recipe.put("ingredients", ingredients);
        recipe.put("result", result);
        return recipe;
    }

    private Map<String, Object> shaped(Map<String, Object> definition,
                                       List<String> pattern,
                                       Map<String, Object> result,
                                       String recipeId) {
        Map<String, Object> sourceIngredients = asSection(definition.get("ingredients"));
        if (sourceIngredients.isEmpty()) {
            report.unsupported(recipeId, "recette iaa shaped sans ingrédients");
            return null;
        }

        Map<String, Integer> occurrences = new LinkedHashMap<>();
        for (String row : pattern) {
            for (char symbol : row.toCharArray()) {
                String key = String.valueOf(symbol);
                if (sourceIngredients.containsKey(key)) occurrences.merge(key, 1, Integer::sum);
            }
        }

        Map<String, Object> ingredients = new LinkedHashMap<>();
        for (Map.Entry<String, Object> entry : sourceIngredients.entrySet()) {
            String symbol = entry.getKey();
            int slots = occurrences.getOrDefault(symbol, 0);
            if (slots == 0) continue;

            Map<String, Object> source = asSection(entry.getValue());
            Object item = source.get("item");
            if (!(item instanceof String itemId) || itemId.isBlank()) {
                report.unsupported(recipeId, "ingrédient iaa shaped " + symbol + " sans item");
                return null;
            }

            int totalAmount = positiveAmount(source.get("amount"), slots);
            if (totalAmount % slots != 0) {
                report.unsupported(recipeId,
                        "quantité iaa " + totalAmount + " non divisible entre " + slots + " cases pour " + symbol);
                return null;
            }

            int perSlot = totalAmount / slots;
            if (perSlot > 64) {
                report.unsupported(recipeId,
                        "quantité iaa par case supérieure à 64 pour " + symbol + " : " + perSlot);
                return null;
            }
            ingredients.put(symbol, ingredient(toItemId(itemId), perSlot));
        }

        if (ingredients.isEmpty()) {
            report.unsupported(recipeId, "aucun ingrédient iaa shaped utilisé dans le pattern");
            return null;
        }

        Map<String, Object> recipe = new LinkedHashMap<>();
        recipe.put("type", "shaped");
        recipe.put("pattern", normalizePattern(pattern, ingredients));
        recipe.put("ingredients", ingredients);
        recipe.put("result", result);
        return recipe;
    }

    private List<List<String>> patterns(Map<String, Object> definition) {
        List<List<String>> result = new ArrayList<>();
        for (int index = 1; index <= 9; index++) {
            String key = index == 1 ? "pattern" : "pattern_" + index;
            Object value = definition.get(key);
            if (!(value instanceof List<?> rows) || rows.isEmpty()) continue;

            List<String> pattern = new ArrayList<>();
            for (Object row : rows) pattern.add(String.valueOf(row));
            result.add(pattern);
        }
        return result;
    }

    private List<String> normalizePattern(List<String> pattern, Map<String, Object> ingredients) {
        List<String> normalized = new ArrayList<>();
        for (String row : pattern) {
            StringBuilder builder = new StringBuilder();
            for (char symbol : row.toCharArray()) {
                builder.append(ingredients.containsKey(String.valueOf(symbol)) ? symbol : ' ');
            }
            normalized.add(builder.toString());
        }
        return normalized;
    }

    private Object ingredient(String itemId, int count) {
        if (count <= 1) return itemId;
        Map<String, Object> ingredient = new LinkedHashMap<>();
        ingredient.put("items", List.of(itemId));
        ingredient.put("count", count);
        return ingredient;
    }

    private Map<String, Object> result(Map<String, Object> definition) {
        Map<String, Object> source = asSection(definition.get("result"));
        Object item = source.get("item");
        if (!(item instanceof String itemId) || itemId.isBlank()) return null;

        int count = positiveAmount(source.get("amount"), 1);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("id", toItemId(itemId));
        result.put("count", count);
        return result;
    }

    private int positiveAmount(Object value, int fallback) {
        if (value instanceof Number number && number.intValue() > 0) return number.intValue();
        return fallback;
    }

    private String qualifyId(String id) {
        return id.contains(":") ? id : namespace + ":" + id;
    }

    private String toItemId(String value) {
        String id = value.trim();
        if (id.contains(":")) return id.toLowerCase(Locale.ROOT);
        return "minecraft:" + id.toLowerCase(Locale.ROOT);
    }

    private Map<String, Object> asSection(Object value) {
        if (!(value instanceof Map<?, ?> map)) return Map.of();
        Map<String, Object> section = new LinkedHashMap<>();
        map.forEach((key, entry) -> section.put(String.valueOf(key), entry));
        return section;
    }
}
