package fr.openmc.core.features.earth.models;

import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;
import lombok.Getter;
import java.util.UUID;

@Getter
@DatabaseTable(tableName = "earth_election_candidates")
public class DBEarthElectionCandidate {
    @DatabaseField(id = true) private String id;
    @DatabaseField(canBeNull = false, columnName = "country_id") private String countryId;
    @DatabaseField(canBeNull = false, columnName = "candidate_uuid") private UUID candidateId;
    @DatabaseField(columnName = "party_id") private String partyId;
    @DatabaseField(canBeNull = false, columnName = "nominated_at") private long nominatedAt;
    DBEarthElectionCandidate() { }
    public DBEarthElectionCandidate(String countryId, UUID candidateId, long nominatedAt) {
        this(countryId, candidateId, null, nominatedAt);
    }
    public DBEarthElectionCandidate(String countryId, UUID candidateId, String partyId, long nominatedAt) {
        this.countryId = countryId; this.candidateId = candidateId; this.partyId = partyId; this.id = idFor(countryId, candidateId); this.nominatedAt = nominatedAt;
    }
    public static String idFor(String countryId, UUID candidateId) { return countryId.toLowerCase(java.util.Locale.ROOT) + ":" + candidateId; }
}
