package fr.openmc.api.datapacks.builders;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import lombok.Getter;

import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/**
 * Exemple simple d'un biome:
 * {
 *   "attributes": {},
 *   "carvers": [],
 *   "creature_spawn_probability": 0.03,
 *   "downfall": 0,
 *   "effects": {
 *     "foliage_color": "#9e814d",
 *     "grass_color": "#90814d",
 *     "water_color": "#3f76e4",
 *     "dry_foliage_color": "",
 *     "grass_color_modifier": "none"
 *   },
 *   "features": [],
 *   "has_precipitation": false,
 *   "spawn_costs": {},
 *   "spawners": {},
 *   "temperature": 2
 * }
 */
public final class BiomeBuilder {
    private static final int DEFAULT_FOG_COLOR = 12638463;
    private static final int DEFAULT_WATER_COLOR = 4159204;
    private static final int DEFAULT_WATER_FOG_COLOR = 329011;
    private static final int DEFAULT_SKY_COLOR = 7907327;

    /**
     * Les clés d'attributs d'environnement (format 26.2) traduites vers les effects d'un biome 1.21.7
     */
    private static final Map<String, String> ATTRIBUTE_TO_EFFECT = Map.of(
            "visual/fog_color", "fog_color",
            "visual/water_color", "water_color",
            "visual/water_fog_color", "water_fog_color",
            "visual/sky_color", "sky_color",
            "visual/grass_color", "grass_color",
            "visual/foliage_color", "foliage_color",
            "visual/dry_foliage_color", "dry_foliage_color"
    );

    private static final List<String> COLOR_EFFECTS = List.of(
            "fog_color", "water_color", "water_fog_color", "sky_color",
            "grass_color", "foliage_color", "dry_foliage_color"
    );

    private JsonObject attributes = new JsonObject();
    private final JsonArray carvers = new JsonArray();
    @Getter
    private final JsonObject effects = new JsonObject();
    private final JsonArray features = new JsonArray();
    private final JsonObject spawnCosts = new JsonObject();
    private final JsonObject spawners = new JsonObject();
    private String temperatureModifier = "none";
    private Double creatureSpawnProbability = 0.03;
    private Float downfall = 0.5f;
    private Float temperatures = 0.5f;
    private Boolean hasPrecipitation = true;

    public BiomeBuilder attributes(EnvironnementAttributeBuilder builder) {
        this.attributes = builder.getOutputData();
        return this;
    }

    public BiomeBuilder carver(String id) {
        this.carvers.add(id);
        return this;
    }

    public BiomeBuilder features(JsonElement id) {
        this.features.add(id);
        return this;
    }

    public BiomeBuilder temperatureModifier(String id) {
        this.temperatureModifier =id;
        return this;
    }

    public BiomeBuilder creatureSpawnProbability(Double value) {
        this.creatureSpawnProbability=value;
        return this;
    }

    public BiomeBuilder downfall(Float value) {
        this.downfall=value;
        return this;
    }

    public BiomeBuilder effects(Consumer<JsonObject> builder) {
        JsonObject obj = new JsonObject();
        builder.accept(obj);
        for (var entry : obj.entrySet()) {
            this.effects.add(entry.getKey(), entry.getValue());
        }
        return this;
    }

    public BiomeBuilder waterColor(String color) {
        this.effects.addProperty("water_color", color);
        return this;
    }

    public BiomeBuilder grassColor(String color) {
        this.effects.addProperty("grass_color", color);
        return this;
    }

    public BiomeBuilder foliageColor(String color) {
        this.effects.addProperty("foliage_color", color);
        return this;
    }

    public BiomeBuilder dryFoliageColor(String color) {
        this.effects.addProperty("dry_foliage_color", color);
        return this;
    }

    public BiomeBuilder waterColor(Integer color) {
        this.effects.addProperty("water_color", color);
        return this;
    }

    public BiomeBuilder grassColor(Integer color) {
        this.effects.addProperty("grass_color", color);
        return this;
    }

    public BiomeBuilder foliageColor(Integer color) {
        this.effects.addProperty("foliage_color", color);
        return this;
    }

    public BiomeBuilder dryFoliageColor(Integer color) {
        this.effects.addProperty("dry_foliage_color", color);
        return this;
    }

    /**
     * Set la grass color modifier
     * @param id none, dark_forest, swamp
     * @return le builder
     */
    public BiomeBuilder grassColorModifier(String id) {
        this.effects.addProperty("grass_color_modifier", id);
        return this;
    }

    public BiomeBuilder spawnCosts(Consumer<JsonObject> builder) {
        JsonObject obj = new JsonObject();
        builder.accept(obj);
        for (var entry : obj.entrySet()) {
            this.spawnCosts.add(entry.getKey(), entry.getValue());
        }
        return this;
    }

    public BiomeBuilder spawnCosts(JsonObject spawnCosts) {
        for (var entry : spawnCosts.entrySet()) {
            this.spawnCosts.add(entry.getKey(), entry.getValue());
        }
        return this;
    }

    public BiomeBuilder spawners(JsonObject spawners) {
        for (var entry : spawners.entrySet()) {
            this.spawners.add(entry.getKey(), entry.getValue());
        }
        return this;
    }

    public BiomeBuilder temperatures(Float value) {
        this.temperatures=value;
        return this;
    }

    public BiomeBuilder hasPrecipitation(Boolean bool) {
        this.hasPrecipitation=bool;
        return this;
    }

    public JsonObject toJson() {
        JsonObject json = new JsonObject();
        if (temperatureModifier != null) json.addProperty("temperature_modifier", temperatureModifier);
        if (creatureSpawnProbability != null) json.addProperty("creature_spawn_probability", creatureSpawnProbability);
        if (carvers != null) json.add("carvers", carvers);
        if (downfall != null) json.addProperty("downfall", downfall);
        json.add("effects", buildEffects());
        if (features != null) json.add("features", features);
        if (hasPrecipitation != null) json.addProperty("has_precipitation", hasPrecipitation);
        if (spawnCosts != null) json.add("spawn_costs", spawnCosts);
        if (spawners != null) json.add("spawners", spawners);
        if (temperatures != null) json.addProperty("temperature", temperatures);

        return json;
    }

    /**
     * Construit les effects au format 1.21.7 : les couleurs obligatoires sont toujours présentes
     * et les attributs d'environnement (format 26.2) sont traduits en effects équivalents
     */
    private JsonObject buildEffects() {
        JsonObject json = new JsonObject();
        json.addProperty("fog_color", DEFAULT_FOG_COLOR);
        json.addProperty("water_color", DEFAULT_WATER_COLOR);
        json.addProperty("water_fog_color", DEFAULT_WATER_FOG_COLOR);
        json.addProperty("sky_color", DEFAULT_SKY_COLOR);

        for (var entry : effects.entrySet()) {
            json.add(entry.getKey(), entry.getValue());
        }

        if (attributes != null) {
            for (var entry : attributes.entrySet()) {
                String key = entry.getKey().replace("minecraft:", "");
                String effect = ATTRIBUTE_TO_EFFECT.get(key);
                if (effect != null) {
                    json.add(effect, entry.getValue());
                } else if (key.equals("visual/ambient_particles")) {
                    JsonObject particle = toParticleEffect(entry.getValue());
                    if (particle != null) json.add("particle", particle);
                } else if (key.equals("audio/ambient_sounds")) {
                    addAmbientSounds(json, entry.getValue());
                }
            }
        }

        for (String color : COLOR_EFFECTS) {
            if (json.has(color)) json.addProperty(color, toColorInt(json.get(color)));
        }

        return json;
    }

    /**
     * Traduit la liste d'ambient_particles du format 26.2 vers l'unique particule d'un biome 1.21.7
     */
    private static JsonObject toParticleEffect(JsonElement value) {
        if (!value.isJsonArray() || value.getAsJsonArray().isEmpty()) return null;
        JsonObject first = value.getAsJsonArray().get(0).getAsJsonObject();
        if (!first.has("particle")) return null;

        JsonObject particle = new JsonObject();
        particle.add("options", first.get("particle"));
        particle.addProperty("probability", first.has("probability") ? first.get("probability").getAsFloat() : 0.01f);
        return particle;
    }

    /**
     * Traduit audio/ambient_sounds (26.2) vers ambient_sound / mood_sound / additions_sound (1.21.7)
     */
    private static void addAmbientSounds(JsonObject effects, JsonElement value) {
        if (!value.isJsonObject()) return;
        JsonObject sounds = value.getAsJsonObject();

        if (sounds.has("loop")) effects.add("ambient_sound", sounds.get("loop"));
        if (sounds.has("mood")) effects.add("mood_sound", sounds.get("mood"));
        if (sounds.has("additions")) effects.add("additions_sound", sounds.get("additions"));
    }

    /**
     * Les couleurs d'un biome sont des entiers en 1.21.7, on accepte quand même les hexadécimaux (#RRGGBB / #AARRGGBB)
     */
    private static int toColorInt(JsonElement value) {
        JsonPrimitive primitive = value.getAsJsonPrimitive();
        if (primitive.isNumber()) return primitive.getAsInt();
        return (int) (Long.parseLong(primitive.getAsString().replace("#", ""), 16) & 0xFFFFFF);
    }
}
