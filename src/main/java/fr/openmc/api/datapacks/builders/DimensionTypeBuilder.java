package fr.openmc.api.datapacks.builders;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import fr.openmc.api.datapacks.injectors.TimelinesInjector;
import lombok.Getter;

/**
 * Exemple simple d'un dimension type :
 * {
 *   "attributes": {},
 *   "ambient_light": 0,
 *   "coordinate_scale": 1,
 *   "default_clock": "minecraft:overworld",
 *   "has_ceiling": false,
 *   "has_ender_dragon_fight": false,
 *   "has_skylight": true,
 *   "has_fixed_time": true,
 *   "skybox": "overworld",
 *   "cardinal_light": "default",
 *   "height": 384,
 *   "infiniburn": "#minecraft:infiniburn_overworld",
 *   "logical_height": 384,
 *   "min_y": -64,
 *   "monster_spawn_block_light_limit": 0,
 *   "monster_spawn_light_level": {
 *     "type": "minecraft:uniform",
 *     "max_inclusive": 7,
 *     "min_inclusive": 0
 *   },
 *   "timelines": "#minecraft:in_overworld"
 * }
 */
public final class DimensionTypeBuilder {
    /**
     * 1.21.7 has no DimensionType.Skybox. These names map to the {@code effects} field
     * (overworld / nether / end sky and fog).
     */
    public enum Skybox {
        OVERWORLD("minecraft:overworld"),
        NETHER("minecraft:the_nether"),
        END("minecraft:the_end"),
        NONE("minecraft:the_nether");

        private final String effects;

        Skybox(String effects) {
            this.effects = effects;
        }

        public String getSerializedName() {
            return name().toLowerCase();
        }

        public String getEffects() {
            return effects;
        }
    }

    @Getter
    private EnvironnementAttributeBuilder attributesBuilder;
    private JsonObject attributes;
    private Double ambientLight = 0.0;
    private Double coordinateScale = 1.0;
    private String defaultClock = "overworld";
    private Boolean hasCeiling = false;
    private Boolean hasEnderDragonFlight = false;
    private Boolean hasSkylight = true;
    private Boolean hasFixedTime = false;
    private String skybox = "overworld";
    private String cardinalLight = "default";
    private Integer height = 384;
    private String infiniburn = "#infiniburn_overworld";
    private Integer logicalHeight = 384;
    private Integer minY = -64;
    private Integer monsterSpawnBlockLightLimit = 0;
    private JsonElement monsterSpawnLightLevel = new JsonPrimitive(0);
    private String timelines = "#minecraft:in_overworld";

    public DimensionTypeBuilder attributesBuilder(EnvironnementAttributeBuilder builder) {
        this.attributesBuilder = builder;
        this.attributes = builder.getOutputData();
        return this;
    }

    public DimensionTypeBuilder ambientLight(double ambientLight) {
        this.ambientLight = ambientLight;
        return this;
    }

    public DimensionTypeBuilder coordinateScale(double coordinateScale) {
        this.coordinateScale = coordinateScale;
        return this;
    }

    public DimensionTypeBuilder defaultClock(String keyOfClock) {
        this.defaultClock = keyOfClock;
        return this;
    }

    public DimensionTypeBuilder hasCeiling(boolean hasCeiling) {
        this.hasCeiling = hasCeiling;
        return this;
    }

    public DimensionTypeBuilder hasEnderDragonFlight(boolean hasEnderDragonFlight) {
        this.hasEnderDragonFlight = hasEnderDragonFlight;
        return this;
    }

    public DimensionTypeBuilder hasSkylight(boolean hasSkylight) {
        this.hasSkylight = hasSkylight;
        return this;
    }

    public DimensionTypeBuilder hasFixedTime(boolean hasFixedTime) {
        this.hasFixedTime = hasFixedTime;
        return this;
    }

    public DimensionTypeBuilder skybox(String skybox) {
        this.skybox = skybox;
        return this;
    }

    public DimensionTypeBuilder skybox(Skybox skybox) {
        return skybox(skybox.getSerializedName());
    }

    public DimensionTypeBuilder cardinalLight(String cardinalLight) {
        this.cardinalLight = cardinalLight;
        return this;
    }

    public DimensionTypeBuilder height(int height) {
        this.height = height;
        return this;
    }

    public DimensionTypeBuilder infiniburn(String infiniburn) {
        this.infiniburn = infiniburn;
        return this;
    }

    public DimensionTypeBuilder logicalHeight(int logicalHeight) {
        this.logicalHeight = logicalHeight;
        return this;
    }

    public DimensionTypeBuilder minY(int minY) {
        this.minY = minY;
        return this;
    }

    public DimensionTypeBuilder monsterSpawnBlockLightLimit(int limit) {
        this.monsterSpawnBlockLightLimit = limit;
        return this;
    }

    public DimensionTypeBuilder monsterSpawnLightLevelUniform(int minInclusive, int maxInclusive) {
        JsonObject uniform = new JsonObject();
        uniform.addProperty("type", "minecraft:uniform");
        uniform.addProperty("min_inclusive", minInclusive);
        uniform.addProperty("max_inclusive", maxInclusive);
        this.monsterSpawnLightLevel = uniform;
        return this;
    }

    public DimensionTypeBuilder monsterSpawnLightLevel(int level) {
        this.monsterSpawnLightLevel = new JsonPrimitive(level);
        return this;
    }

    public DimensionTypeBuilder monsterSpawnLightLevel(JsonElement monsterSpawnLightLevel) {
        this.monsterSpawnLightLevel = monsterSpawnLightLevel;
        return this;
    }

    public DimensionTypeBuilder timelines(String timelines) {
        this.timelines = timelines;
        return this;
    }

    public DimensionTypeBuilder timelines(TimelinesInjector injector) {
        this.timelines = injector.getNamespace() + ":" + injector.getId();
        return this;
    }

    public JsonObject toJson() {
        JsonObject json = new JsonObject();
        // 1.21.7 dimension_type codec. 26.2-only fields (skybox, attributes, timelines,
        // default_clock, cardinal_light) are mapped or omitted so the datapack loads.
        json.addProperty("ultrawarm", false);
        json.addProperty("natural", true);
        json.addProperty("piglin_safe", false);
        json.addProperty("respawn_anchor_works", false);
        json.addProperty("bed_works", true);
        json.addProperty("has_raids", true);
        if (logicalHeight != null) json.addProperty("logical_height", logicalHeight);
        if (minY != null) json.addProperty("min_y", minY);
        if (height != null) json.addProperty("height", height);
        if (coordinateScale != null) json.addProperty("coordinate_scale", coordinateScale);
        if (ambientLight != null) json.addProperty("ambient_light", ambientLight);
        if (hasSkylight != null) json.addProperty("has_skylight", hasSkylight);
        if (hasCeiling != null) json.addProperty("has_ceiling", hasCeiling);
        json.addProperty("infiniburn", normalizeInfiniburn(infiniburn));
        json.addProperty("effects", effectsFromSkybox(skybox));
        if (monsterSpawnLightLevel != null) json.add("monster_spawn_light_level", monsterSpawnLightLevel);
        if (monsterSpawnBlockLightLimit != null) json.addProperty("monster_spawn_block_light_limit", monsterSpawnBlockLightLimit);
        if (Boolean.TRUE.equals(hasFixedTime)) {
            json.addProperty("fixed_time", 18000);
        }
        return json;
    }

    private static String normalizeInfiniburn(String infiniburn) {
        if (infiniburn == null || infiniburn.isBlank()) {
            return "#minecraft:infiniburn_overworld";
        }
        if (infiniburn.startsWith("#minecraft:")) {
            return infiniburn;
        }
        if (infiniburn.startsWith("#")) {
            return "#minecraft:" + infiniburn.substring(1);
        }
        return infiniburn;
    }

    private static String effectsFromSkybox(String skybox) {
        if (skybox == null) {
            return Skybox.OVERWORLD.getEffects();
        }
        return switch (skybox.toLowerCase()) {
            case "end", "the_end", "minecraft:the_end" -> Skybox.END.getEffects();
            case "nether", "the_nether", "minecraft:the_nether", "none" -> Skybox.NETHER.getEffects();
            default -> Skybox.OVERWORLD.getEffects();
        };
    }

    private JsonObject toOverridenEnvironnementAttribute(JsonElement value) {
        JsonObject obj = new JsonObject();
        obj.addProperty("modifier", "override");
        obj.add("argument", value);
        return obj;
    }
}
