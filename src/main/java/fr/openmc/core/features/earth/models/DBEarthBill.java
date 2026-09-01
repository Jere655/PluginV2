package fr.openmc.core.features.earth.models;

import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;
import lombok.Getter;

import java.util.UUID;

/** Auditable country bill that may become one active simulation law when passed. */
@Getter
@DatabaseTable(tableName = "earth_bills")
public class DBEarthBill {
    @DatabaseField(id = true, columnName = "bill_uuid")
    private UUID billId;
    @DatabaseField(canBeNull = false, columnName = "country_id")
    private String countryId;
    @DatabaseField(canBeNull = false, columnName = "law_type")
    private String lawType;
    @DatabaseField(canBeNull = false, columnName = "author_uuid")
    private UUID authorId;
    @DatabaseField(canBeNull = false)
    private String status;
    @DatabaseField(canBeNull = false, columnName = "proposed_at")
    private long proposedAt;
    @DatabaseField(canBeNull = false, columnName = "resolved_at")
    private long resolvedAt;

    DBEarthBill() { }

    public DBEarthBill(String countryId, EarthLawType lawType, UUID authorId, long now) {
        this.billId = UUID.randomUUID();
        this.countryId = countryId;
        this.lawType = lawType.name();
        this.authorId = authorId;
        this.status = EarthBillStatus.PROPOSED.name();
        this.proposedAt = now;
    }

    public EarthLawType getLawType() { return EarthLawType.valueOf(lawType); }
    public EarthBillStatus getStatusType() { return EarthBillStatus.valueOf(status); }
    public boolean isOpen() { return getStatusType() == EarthBillStatus.PROPOSED; }
    public void resolve(EarthBillStatus status, long now) { this.status = status.name(); this.resolvedAt = now; }
}
