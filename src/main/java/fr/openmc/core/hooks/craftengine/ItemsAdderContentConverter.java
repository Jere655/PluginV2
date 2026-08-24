package fr.openmc.core.hooks.craftengine;

import lombok.Getter;

import java.io.File;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Traduit les définitions ItemsAdder d'un namespace OpenMC en configuration CraftEngine.
 * Les identifiants {@code namespace:id} sont conservés tels quels pour que le code OpenMC
 * et la couche de compatibilité ItemsAdder continuent de les résoudre.
 */
public class ItemsAdderContentConverter {
    private static final Set<String> IGNORED_SECTIONS = Set.of("info", "minecraft_lang_overwrite");
    private static final Set<String> UNSUPPORTED_SECTIONS = Set.of(
            "categories", "equipments", "emotes", "entities",
            "scripts", "legacy_armor_renderings"
    );
    private static final Set<String> UNSUPPORTED_ITEM_FIELDS = Set.of(
            "equipment", "consumable", "attribute_modifiers", "components_nbt_file",
            "blocked_enchants", "enchants"
    );

    private final String namespace;
    private final File namespaceDir;
    private final ConversionReport report;

    @Getter
    private final Map<String, Object> items = new LinkedHashMap<>();
    @Getter
    private final Map<String, Object> blocks = new LinkedHashMap<>();
    @Getter
    private final Map<String, Object> images = new LinkedHashMap<>();
    @Getter
    private final Map<String, Object> recipes = new LinkedHashMap<>();

    public ItemsAdderContentConverter(String namespace, File namespaceDir, ConversionReport report) {
        this.namespace = namespace;
        this.namespaceDir = namespaceDir;
        this.report = report;
    }

    /**
     * Convertit un fichier de contenu ItemsAdder.
     *
     * @param fileName le nom du fichier (pour le rapport)
     * @param content  le contenu YAML déjà parsé
     */
    public void read(String fileName, Map<String, Object> content) {
        for (Map.Entry<String, Object> entry : content.entrySet()) {
            String section = entry.getKey();

            if (IGNORED_SECTIONS.contains(section)) continue;

            switch (section) {
                case "items" -> readItems(asSection(entry.getValue()));
                case "font_images" -> readFontImages(asSection(entry.getValue()));
                case "recipes" -> readRecipes(asSection(entry.getValue()));
                case "entities" -> report.unsupported(namespace + "/" + fileName + " [" + section + "]",
                        "entité ItemsAdder sans équivalent CraftEngine, aucun usage Java OpenMC détecté");
                case "scripts" -> report.unsupported(namespace + "/" + fileName + " [" + section + "]",
                        "script ItemsAdder sans équivalent CraftEngine, aucun usage Java OpenMC détecté");
                default -> {
                    if (UNSUPPORTED_SECTIONS.contains(section)) {
                        report.unsupported(namespace + "/" + fileName + " [" + section + "]",
                                "section ItemsAdder sans équivalent CraftEngine, à porter à la main");
                    } else {
                        report.unsupported(namespace + "/" + fileName + " [" + section + "]", "section inconnue");
                    }
                }
            }
        }
    }

    private void readItems(Map<String, Object> section) {
        for (Map.Entry<String, Object> entry : section.entrySet()) {
            Map<String, Object> definition = asSection(entry.getValue());
            if (definition.isEmpty()) continue;

            convertItem(entry.getKey(), definition);
        }
    }

    private void convertItem(String id, Map<String, Object> definition) {
        String fullId = namespace + ":" + id;

        Map<String, Object> resource = asSection(definition.get("resource"));
        Map<String, Object> graphics = asSection(definition.get("graphics"));
        Map<String, Object> behaviours = asSection(definition.get("behaviours"));
        Map<String, Object> specific = asSection(definition.get("specific_properties"));

        Map<String, Object> blockDefinition = asSection(specific.get("block"));
        if (blockDefinition.isEmpty()) blockDefinition = asSection(behaviours.get("block"));

        Map<String, Object> furnitureDefinition = asSection(behaviours.get("furniture"));
        if (furnitureDefinition.isEmpty()) furnitureDefinition = asSection(behaviours.get("furniture_sit"));

        Map<String, Object> item = new LinkedHashMap<>();
        item.put("material", material(resource));

        Map<String, Object> data = itemData(definition);
        if (!data.isEmpty()) item.put("data", data);

        ModelReference model = resolveModel(fullId, id, resource, graphics, !blockDefinition.isEmpty(), definition);
        model.applyToItem(item);

        if (!blockDefinition.isEmpty()) {
            convertBlock(fullId, blockDefinition, definition, model);
            item.put("behavior", Map.of("type", "block_item", "block", fullId));
        } else if (!furnitureDefinition.isEmpty()) {
            item.put("behavior", furnitureBehavior(fullId, furnitureDefinition));
            report.getFurnitureIDs().add(fullId);
        }

        reportUnsupportedFields(fullId, definition);

        items.put(fullId, item);
        report.getItemIDs().add(fullId);
    }

    private Map<String, Object> itemData(Map<String, Object> definition) {
        Map<String, Object> data = new LinkedHashMap<>();

        Object displayName = definition.get("display_name");
        if (displayName instanceof String name && !name.isBlank()) {
            data.put("item_name", "<!i>" + name);
        }

        Object lore = definition.get("lore");
        if (lore instanceof List<?> lines && !lines.isEmpty()) {
            List<String> converted = new ArrayList<>();
            for (Object line : lines) {
                converted.add("<!i>" + line);
            }
            data.put("lore", converted);
        }

        Map<String, Object> durability = asSection(definition.get("durability"));
        if (durability.get("max_durability") instanceof Number maxDurability) {
            data.put("max_damage", maxDurability.intValue());
        }

        if (Boolean.TRUE.equals(definition.get("glint"))) {
            data.put("enchantment_glint_override", true);
        }

        return data;
    }

    private void convertBlock(String fullId, Map<String, Object> blockDefinition,
                              Map<String, Object> itemDefinition, ModelReference model) {
        Map<String, Object> block = new LinkedHashMap<>();

        block.put("settings", blockSettings(fullId, blockDefinition));
        block.put("state", blockState(blockDefinition, model));

        Map<String, Object> loot = blockLoot(fullId, blockDefinition, itemDefinition);
        if (loot != null) block.put("loot", loot);

        Map<String, Object> experience = blockExperience(itemDefinition);
        if (experience != null) block.put("behavior", experience);

        blocks.put(fullId, block);
        report.getBlockIDs().add(fullId);
    }

    private Map<String, Object> blockSettings(String fullId, Map<String, Object> blockDefinition) {
        Map<String, Object> settings = new LinkedHashMap<>();

        double hardness = blockDefinition.get("hardness") instanceof Number value ? value.doubleValue() : 1.0;
        settings.put("hardness", hardness);
        settings.put("resistance", Boolean.TRUE.equals(blockDefinition.get("no_explosion")) ? 3600000.0 : hardness);
        settings.put("item", fullId);

        if (blockDefinition.get("light_level") instanceof Number light) {
            settings.put("luminance", light.intValue());
        }

        List<String> correctTools = new ArrayList<>();
        if (blockDefinition.get("break_tools_whitelist") instanceof List<?> tools) {
            for (Object tool : tools) {
                correctTools.add(toItemId(String.valueOf(tool)));
            }
        }
        if (!correctTools.isEmpty()) settings.put("correct_tools", correctTools);

        Map<String, Object> sounds = blockSounds(asSection(blockDefinition.get("sound")));
        if (!sounds.isEmpty()) settings.put("sounds", sounds);

        return settings;
    }

    private Map<String, Object> blockSounds(Map<String, Object> soundDefinition) {
        Map<String, Object> sounds = new LinkedHashMap<>();

        String breakSound = soundName(soundDefinition.get("break"));
        String placeSound = soundName(soundDefinition.get("place"));

        if (breakSound != null) {
            sounds.put("break", breakSound);
            sounds.put("hit", breakSound);
        }
        if (placeSound != null) sounds.put("place", placeSound);

        return sounds;
    }

    private Map<String, Object> blockState(Map<String, Object> blockDefinition, ModelReference model) {
        Map<String, Object> state = new LinkedHashMap<>();

        Object placedType = asSection(blockDefinition.get("placed_model")).get("type");
        state.put("auto_state", "REAL_WIRE".equals(placedType) ? "tripwire" : "solid");

        model.applyToBlockState(state);

        return state;
    }

    private Map<String, Object> blockLoot(String fullId, Map<String, Object> blockDefinition,
                                          Map<String, Object> itemDefinition) {
        Map<String, Object> drop = asSection(asSection(asSection(asSection(itemDefinition.get("events"))
                .get("placed_block")).get("break")).get("drop_item"));

        if (!drop.isEmpty() && drop.get("item") instanceof String dropped) {
            int min = drop.get("min_amount") instanceof Number value ? value.intValue() : 1;
            int max = drop.get("max_amount") instanceof Number value ? value.intValue() : min;

            return lootOf(toItemId(dropped), min, max);
        }

        if (Boolean.FALSE.equals(blockDefinition.get("drop_when_mined"))) return null;

        return lootOf(fullId, 1, 1);
    }

    private Map<String, Object> lootOf(String item, int min, int max) {
        Map<String, Object> entry = new LinkedHashMap<>();
        entry.put("type", "item");
        entry.put("item", item);

        if (min != 1 || max != 1) {
            entry.put("functions", List.of(Map.of("type", "set_count", "add", false, "count", min + "~" + max)));
        }

        return Map.of("pools", List.of(Map.of("rolls", 1, "entries", List.of(entry))));
    }

    private Map<String, Object> blockExperience(Map<String, Object> itemDefinition) {
        Map<String, Object> experience = asSection(asSection(asSection(asSection(itemDefinition.get("events"))
                .get("placed_block")).get("break")).get("drop_exp"));

        if (experience.isEmpty()) return null;

        int chance = experience.get("chance") instanceof Number value ? value.intValue() : 0;
        if (chance <= 0) return null;

        int min = experience.get("min_amount") instanceof Number value ? value.intValue() : 1;
        int max = experience.get("max_amount") instanceof Number value ? value.intValue() : min;

        return Map.of("type", "drop_experience_block", "amount", min + "~" + max);
    }

    private Map<String, Object> furnitureBehavior(String fullId, Map<String, Object> furnitureDefinition) {
        Map<String, Object> placeableOn = asSection(furnitureDefinition.get("placeable_on"));
        boolean fixedRotation = Boolean.TRUE.equals(furnitureDefinition.get("fixed_rotation"));

        Map<String, Object> rules = new LinkedHashMap<>();
        if (!Boolean.FALSE.equals(placeableOn.get("floor"))) {
            rules.put("ground", Map.of("rotation", fixedRotation ? "four" : "any", "alignment", "center"));
        }
        if (Boolean.TRUE.equals(placeableOn.get("walls"))) {
            rules.put("wall", Map.of("rotation", "four", "alignment", "center"));
        }
        if (Boolean.TRUE.equals(placeableOn.get("ceiling"))) {
            rules.put("ceiling", Map.of("rotation", "four", "alignment", "center"));
        }
        if (rules.isEmpty()) {
            rules.put("ground", Map.of("rotation", "four", "alignment", "center"));
        }

        Map<String, Object> settings = new LinkedHashMap<>();
        settings.put("item", fullId);
        settings.put("hit_times", Boolean.FALSE.equals(furnitureDefinition.get("breakable")) ? 100 : 3);

        Map<String, Object> sounds = blockSounds(asSection(furnitureDefinition.get("sound")));
        if (!sounds.isEmpty()) settings.put("sounds", sounds);

        Map<String, Object> variants = new LinkedHashMap<>();
        for (String variant : rules.keySet()) {
            variants.put(variant, furnitureVariant(fullId, furnitureDefinition));
        }

        Map<String, Object> furniture = new LinkedHashMap<>();
        furniture.put("settings", settings);
        furniture.put("variants", variants);
        furniture.put("loot", lootOf(fullId, 1, 1));

        Map<String, Object> behavior = new LinkedHashMap<>();
        behavior.put("type", "furniture_item");
        behavior.put("rules", rules);
        behavior.put("furniture", furniture);

        return behavior;
    }

    private Map<String, Object> furnitureVariant(String fullId, Map<String, Object> furnitureDefinition) {
        Map<String, Object> transformation = asSection(furnitureDefinition.get("display_transformation"));
        Map<String, Object> hitbox = asSection(furnitureDefinition.get("hitbox"));

        Map<String, Object> element = new LinkedHashMap<>();
        element.put("item", fullId);
        element.put("display_transform", "none");
        element.put("billboard", "fixed");
        element.put("position", "0,0,0");
        element.put("translation", vector(asSection(transformation.get("translation")), "0,0.5,0"));
        element.put("scale", vector(asSection(transformation.get("scale")), "1,1,1"));

        double width = hitbox.get("width") instanceof Number value ? value.doubleValue() : 1.0;
        double height = hitbox.get("height") instanceof Number value ? value.doubleValue() : 1.0;

        Map<String, Object> hitboxEntry = new LinkedHashMap<>();
        hitboxEntry.put("type", "interaction");
        hitboxEntry.put("position", "0,0,0");
        hitboxEntry.put("width", width);
        hitboxEntry.put("height", height);
        hitboxEntry.put("interactive", true);
        hitboxEntry.put("blocks_building", Boolean.TRUE.equals(furnitureDefinition.get("solid")));
        hitboxEntry.put("can_use_item_on", true);

        Map<String, Object> variant = new LinkedHashMap<>();
        variant.put("elements", List.of(element));
        variant.put("hitboxes", List.of(hitboxEntry));

        return variant;
    }

    private void readFontImages(Map<String, Object> section) {
        for (Map.Entry<String, Object> entry : section.entrySet()) {
            Map<String, Object> definition = asSection(entry.getValue());
            if (definition.isEmpty()) continue;

            String id = namespace + ":" + entry.getKey();
            String path = String.valueOf(definition.get("path"));

            if (!hasTexture(path)) {
                report.missingAsset(id, "textures/" + path);
                continue;
            }

            int ascent = definition.get("y_position") instanceof Number value ? value.intValue() : 7;
            int scale = definition.get("scale_ratio") instanceof Number value ? value.intValue() : 8;

            Map<String, Object> image = new LinkedHashMap<>();
            // * CraftEngine refuse une hauteur inférieure à l'ascent, contrairement à ItemsAdder
            image.put("height", Math.max(scale, ascent));
            image.put("ascent", ascent);
            image.put("font", "minecraft:default");
            image.put("file", namespace + ":" + path);

            images.put(id, image);
            report.getImageIDs().add(id);
        }
    }

    private void readRecipes(Map<String, Object> section) {
        for (Map.Entry<String, Object> typeEntry : section.entrySet()) {
            String type = typeEntry.getKey();
            Map<String, Object> byId = asSection(typeEntry.getValue());

            for (Map.Entry<String, Object> recipeEntry : byId.entrySet()) {
                Map<String, Object> definition = asSection(recipeEntry.getValue());
                String id = namespace + ":" + recipeEntry.getKey();

                if (!"crafting_table".equals(type)) {
                    report.unsupported(id, "type de recette ItemsAdder non converti : " + type);
                    continue;
                }
                if (Boolean.FALSE.equals(definition.get("enabled"))) continue;

                Map<String, Object> recipe = craftingRecipe(definition);
                if (recipe == null) {
                    report.unsupported(id, "recette d'établi incomplète");
                    continue;
                }

                recipes.put(id, recipe);
                report.getRecipeIDs().add(id);
            }
        }
    }

    private Map<String, Object> craftingRecipe(Map<String, Object> definition) {
        Map<String, Object> result = asSection(definition.get("result"));
        if (!(result.get("item") instanceof String resultItem)) return null;

        Map<String, Object> ingredients = asSection(definition.get("ingredients"));
        if (ingredients.isEmpty()) return null;

        Map<String, Object> converted = new LinkedHashMap<>();
        for (Map.Entry<String, Object> ingredient : ingredients.entrySet()) {
            converted.put(ingredient.getKey(), toItemId(String.valueOf(ingredient.getValue())));
        }

        Map<String, Object> recipe = new LinkedHashMap<>();
        int count = result.get("amount") instanceof Number amount ? amount.intValue() : 1;

        if (Boolean.TRUE.equals(definition.get("shapeless"))) {
            recipe.put("type", "shapeless");
            recipe.put("ingredients", new ArrayList<>(converted.values()));
        } else {
            if (!(definition.get("pattern") instanceof List<?> pattern) || pattern.isEmpty()) return null;
            recipe.put("type", "shaped");
            recipe.put("pattern", normalizePattern(pattern, converted.keySet()));
            recipe.put("ingredients", converted);
        }

        recipe.put("result", Map.of("id", toItemId(resultItem), "count", count));
        return recipe;
    }

    /**
     * ItemsAdder utilise des lettres arbitraires (souvent X) pour les cases vides ;
     * CraftEngine attend un espace pour toute case sans ingrédient.
     */
    private List<String> normalizePattern(List<?> pattern, Set<String> ingredientKeys) {
        List<String> rows = new ArrayList<>();

        for (Object row : pattern) {
            StringBuilder normalized = new StringBuilder();
            for (char slot : String.valueOf(row).toCharArray()) {
                normalized.append(ingredientKeys.contains(String.valueOf(slot)) ? slot : ' ');
            }
            rows.add(normalized.toString());
        }

        return rows;
    }

    private ModelReference resolveModel(String fullId, String id, Map<String, Object> resource,
                                        Map<String, Object> graphics, boolean isBlock,
                                        Map<String, Object> definition) {
        Map<String, Object> faces = asSection(graphics.get("textures"));
        if (!faces.isEmpty()) {
            return cubeModel(fullId, id, faces);
        }

        Object singleTexture = graphics.get("texture");
        if (singleTexture instanceof String texture && !texture.isBlank()) {
            if (!isQualified(texture) && !hasTexture(texture)) report.missingAsset(fullId, "textures/" + texture);
            return ModelReference.texture(qualify(stripExtension(texture)));
        }

        Object modelPath = resource.get("model_path");
        if (modelPath instanceof String path && !path.isBlank()) {
            if (!isQualified(path) && !hasModel(path)) report.missingAsset(fullId, "models/" + path + ".json");
            return ModelReference.model(qualify(path));
        }

        List<String> textures = textureList(resource);
        if (!textures.isEmpty()) {
            for (String texture : textures) {
                if (!isQualified(texture) && !hasTexture(texture)) report.missingAsset(fullId, "textures/" + texture);
            }

            if (textures.size() == 1) {
                return ModelReference.texture(qualify(stripExtension(textures.getFirst())));
            }

            Map<String, Object> layers = new LinkedHashMap<>();
            for (int index = 0; index < textures.size(); index++) {
                layers.put("layer" + index, qualify(stripExtension(textures.get(index))));
            }
            return ModelReference.generated(namespace + ":item/" + id, "minecraft:item/generated", layers);
        }

        Object resourceTexture = resource.get("texture");
        if (resourceTexture instanceof String texture && !texture.isBlank()) {
            if (!isQualified(texture) && !hasTexture(texture)) report.missingAsset(fullId, "textures/" + texture);
            return ModelReference.texture(qualify(stripExtension(texture)));
        }

        if (!isBlock) {
            boolean hasEquipment = definition.containsKey("equipment");
            String material = materialName(resource, definition);
            boolean hasHeadNbt = "player_head".equalsIgnoreCase(material)
                    || definition.containsKey("components_nbt_file");
            if (!hasEquipment && !hasHeadNbt) {
                report.missingAsset(fullId, "aucune texture ni modèle défini");
            }
        }
        return ModelReference.none();
    }

    private ModelReference cubeModel(String fullId, String id, Map<String, Object> faces) {
        Map<String, Object> textures = new LinkedHashMap<>();

        for (String face : List.of("north", "south", "east", "west", "up", "down")) {
            Object texture = faces.get(face);
            if (!(texture instanceof String path)) continue;

            if (!isQualified(path) && !hasTexture(path)) report.missingAsset(fullId, "textures/" + path);
            textures.put(face, qualify(stripExtension(path)));
        }

        if (textures.isEmpty()) return ModelReference.none();

        textures.put("particle", textures.values().iterator().next());
        return ModelReference.generated(namespace + ":block/" + id, "minecraft:block/cube", textures);
    }

    private void reportUnsupportedFields(String fullId, Map<String, Object> definition) {
        for (String field : UNSUPPORTED_ITEM_FIELDS) {
            if (definition.containsKey(field)) {
                report.unsupported(fullId, "propriété ItemsAdder non convertie : " + field);
            }
        }
    }

    private List<String> textureList(Map<String, Object> resource) {
        if (!(resource.get("textures") instanceof List<?> textures)) return List.of();

        List<String> paths = new ArrayList<>();
        for (Object texture : textures) {
            paths.add(String.valueOf(texture));
        }
        return paths;
    }

    private String material(Map<String, Object> resource) {
        Object material = resource.get("material");
        if (material instanceof String name && !name.isBlank()) {
            return name.toLowerCase(Locale.ROOT).replace("minecraft:", "");
        }
        return "paper";
    }

    private String materialName(Map<String, Object> resource, Map<String, Object> definition) {
        Object material = resource.get("material");
        if (material == null || (material instanceof String s && s.isBlank())) {
            material = definition.get("material");
        }
        if (material instanceof String name && !name.isBlank()) {
            return name.toLowerCase(Locale.ROOT).replace("minecraft:", "");
        }
        if (material instanceof Map<?, ?> map) {
            Object inner = map.get("dream_item_material");
            if (!(inner instanceof String s) || s.isBlank()) {
                inner = recoverDreamMaterial(map);
            }
            if (inner instanceof String s && !s.isBlank()) {
                return s.toLowerCase(Locale.ROOT).replace("minecraft:", "");
            }
        }
        return "";
    }

    private static String recoverDreamMaterial(Map<?, ?> map) {
        for (Object key : map.keySet()) {
            String k = String.valueOf(key);
            int idx = k.indexOf(':');
            if (idx > 0 && k.substring(0, idx).trim().equalsIgnoreCase("dream_item_material")) {
                return k.substring(idx + 1).trim();
            }
        }
        return null;
    }

    private String soundName(Object soundDefinition) {
        if (soundDefinition instanceof String name && !name.isBlank()) return SoundKeyMapper.toKey(name);

        Object name = asSection(soundDefinition).get("name");
        if (name instanceof String value && !value.isBlank()) return SoundKeyMapper.toKey(value);

        return null;
    }

    private String vector(Map<String, Object> section, String fallback) {
        if (section.isEmpty()) return fallback;

        return "%s,%s,%s".formatted(number(section.get("x")), number(section.get("y")), number(section.get("z")));
    }

    private String number(Object value) {
        return value instanceof Number number ? String.valueOf(number.doubleValue()) : "0";
    }

    private String toItemId(String id) {
        if (id.contains(":")) return id;
        return "minecraft:" + id.toLowerCase(Locale.ROOT);
    }

    /**
     * @param path un chemin ItemsAdder, éventuellement déjà namespacé (minecraft:item/stick)
     * @return le chemin préfixé par le namespace du contenu s'il ne l'était pas déjà
     */
    private String qualify(String path) {
        return isQualified(path) ? path : namespace + ":" + path;
    }

    private boolean isQualified(String path) {
        return path.contains(":");
    }

    private String stripExtension(String path) {
        return path.endsWith(".png") ? path.substring(0, path.length() - ".png".length()) : path;
    }

    private boolean hasTexture(String path) {
        String texture = stripExtension(path) + ".png";
        return new File(namespaceDir, "textures/" + texture).isFile()
                || new File(namespaceDir, "resourcepack/assets/" + namespace + "/textures/" + texture).isFile()
                || new File(namespaceDir, "resourcepack/" + namespace + "/textures/" + texture).isFile();
    }

    private boolean hasModel(String path) {
        String model = path.endsWith(".json") ? path : path + ".json";
        return new File(namespaceDir, "models/" + model).isFile()
                || new File(namespaceDir, "resourcepack/assets/" + namespace + "/models/" + model).isFile()
                || new File(namespaceDir, "resourcepack/" + namespace + "/models/" + model).isFile();
    }

    /**
     * Normalise une section YAML : les clés ItemsAdder peuvent être numériques (font images, patterns).
     */
    private Map<String, Object> asSection(Object value) {
        if (!(value instanceof Map<?, ?> map)) return Map.of();

        Map<String, Object> section = new LinkedHashMap<>();
        map.forEach((key, entry) -> section.put(String.valueOf(key), entry));

        return section;
    }
}
