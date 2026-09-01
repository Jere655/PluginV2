package fr.openmc.core.features.earth.models;

import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;
import lombok.Getter;
import java.util.UUID;

@Getter
@DatabaseTable(tableName = "earth_country_offices")
public class DBEarthCountryOffice {
    @DatabaseField(id = true, columnName = "country_id") private String countryId;
    @DatabaseField(canBeNull = false, columnName = "officeholder_uuid") private UUID officeholderId;
    @DatabaseField(canBeNull = false, columnName = "elected_at") private long electedAt;
    @DatabaseField(canBeNull = false, columnName = "vote_count") private long voteCount;
    DBEarthCountryOffice() { }
    public DBEarthCountryOffice(String countryId, UUID officeholderId, long electedAt, long voteCount) {
        this.countryId = countryId; this.officeholderId = officeholderId; this.electedAt = electedAt; this.voteCount = voteCount;
    }
}
