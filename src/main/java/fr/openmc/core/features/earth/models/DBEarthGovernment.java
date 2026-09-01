package fr.openmc.core.features.earth.models;

import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;
import lombok.Getter;

import java.util.UUID;

/** Country-level government state installed by the persisted election process. */
@Getter
@DatabaseTable(tableName = "earth_governments")
public class DBEarthGovernment {
    @DatabaseField(id = true, columnName = "country_id")
    private String countryId;
    @DatabaseField(canBeNull = false, columnName = "government_id")
    private String governmentId;
    @DatabaseField(canBeNull = false, columnName = "government_type")
    private String governmentType;
    @DatabaseField(columnName = "leader_uuid")
    private UUID leaderId;
    @DatabaseField(columnName = "ruling_party_id")
    private String rulingPartyId;
    @DatabaseField(canBeNull = false, columnName = "mandate_ends_at")
    private long mandateEndsAt;
    @DatabaseField(canBeNull = false, columnName = "transitioned_at")
    private long transitionedAt;

    DBEarthGovernment() { }

    public DBEarthGovernment(String countryId, String governmentId, String governmentType, long now) {
        this.countryId = countryId;
        this.governmentId = governmentId;
        this.governmentType = governmentType;
        this.transitionedAt = now;
    }

    public void installLeader(UUID leaderId, long mandateEndsAt, long now) {
        installLeader(leaderId, null, mandateEndsAt, now);
    }

    public void installLeader(UUID leaderId, String rulingPartyId, long mandateEndsAt, long now) {
        this.leaderId = leaderId;
        this.rulingPartyId = rulingPartyId;
        this.mandateEndsAt = mandateEndsAt;
        this.transitionedAt = now;
    }

    public void vacateLeadership(long now) {
        this.leaderId = null;
        this.rulingPartyId = null;
        this.mandateEndsAt = 0L;
        this.transitionedAt = now;
    }

    public void applyConfiguredIdentity(String governmentId, String governmentType) {
        this.governmentId = governmentId;
        this.governmentType = governmentType;
    }
}
