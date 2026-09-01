package fr.openmc.core.features.earth.models;

import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;
import lombok.Getter;
import java.util.UUID;

@Getter
@DatabaseTable(tableName = "earth_election_votes")
public class DBEarthElectionVote {
    @DatabaseField(id = true) private String id;
    @DatabaseField(canBeNull = false, columnName = "country_id") private String countryId;
    @DatabaseField(canBeNull = false, columnName = "voter_uuid") private UUID voterId;
    @DatabaseField(canBeNull = false, columnName = "candidate_uuid") private UUID candidateId;
    @DatabaseField(canBeNull = false, columnName = "cast_at") private long castAt;
    DBEarthElectionVote() { }
    public DBEarthElectionVote(String countryId, UUID voterId, UUID candidateId, long castAt) {
        this.countryId = countryId; this.voterId = voterId; this.candidateId = candidateId; this.id = idFor(countryId, voterId); this.castAt = castAt;
    }
    public static String idFor(String countryId, UUID voterId) { return countryId.toLowerCase(java.util.Locale.ROOT) + ":" + voterId; }
}
