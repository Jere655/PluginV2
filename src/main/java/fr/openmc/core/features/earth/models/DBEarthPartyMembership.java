package fr.openmc.core.features.earth.models;

import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;
import lombok.Getter;

import java.util.UUID;

/** One player's current party affiliation, changed only by explicit membership actions. */
@Getter
@DatabaseTable(tableName = "earth_party_memberships")
public class DBEarthPartyMembership {
    @DatabaseField(id = true, columnName = "player_uuid")
    private UUID playerId;
    @DatabaseField(canBeNull = false, columnName = "party_id")
    private String partyId;
    @DatabaseField(canBeNull = false, columnName = "joined_at")
    private long joinedAt;

    DBEarthPartyMembership() { }

    public DBEarthPartyMembership(UUID playerId, String partyId, long joinedAt) {
        this.playerId = playerId;
        this.partyId = partyId;
        this.joinedAt = joinedAt;
    }
}
