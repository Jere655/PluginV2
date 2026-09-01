package fr.openmc.core.features.earth.models;

import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;
import lombok.Getter;

import java.util.Locale;
import java.util.UUID;

/** Player-founded political party scoped to one active country. */
@Getter
@DatabaseTable(tableName = "earth_political_parties")
public class DBEarthPoliticalParty {
    @DatabaseField(id = true)
    private String id;
    @DatabaseField(canBeNull = false, columnName = "country_id")
    private String countryId;
    @DatabaseField(canBeNull = false)
    private String name;
    @DatabaseField(canBeNull = false)
    private String platform;
    @DatabaseField(canBeNull = false, columnName = "founder_uuid")
    private UUID founderId;
    @DatabaseField(canBeNull = false, columnName = "created_at")
    private long createdAt;
    @DatabaseField(canBeNull = false)
    private boolean active;

    DBEarthPoliticalParty() { }

    public DBEarthPoliticalParty(String countryId, String name, String platform, UUID founderId, long now) {
        this.countryId = countryId.toLowerCase(Locale.ROOT);
        this.name = name;
        this.platform = platform;
        this.founderId = founderId;
        this.createdAt = now;
        this.active = true;
        this.id = idFor(countryId, name);
    }

    public static String idFor(String countryId, String name) {
        String slug = name.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]+", "-")
                .replaceAll("^-+|-+$", "");
        return countryId.toLowerCase(Locale.ROOT) + ":" + slug;
    }
}
