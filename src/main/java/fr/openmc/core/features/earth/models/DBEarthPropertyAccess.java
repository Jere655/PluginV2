package fr.openmc.core.features.earth.models;

import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;
import lombok.Getter;

import java.util.UUID;

/** Additive lease and access policy, separated from immutable location records. */
@Getter
@DatabaseTable(tableName = "earth_property_access")
public class DBEarthPropertyAccess {
    @DatabaseField(id = true, columnName = "property_uuid")
    private UUID propertyId;
    @DatabaseField(canBeNull = false)
    private String accessMode;
    @DatabaseField(columnName = "tenant_uuid")
    private UUID tenantId;
    @DatabaseField(canBeNull = false)
    private double leasePrice;
    @DatabaseField(canBeNull = false)
    private long leaseEndsAt;
    @DatabaseField(canBeNull = false)
    private long updatedAt;

    DBEarthPropertyAccess() { }

    public DBEarthPropertyAccess(UUID propertyId, EarthPropertyAccessMode accessMode, long now) {
        this.propertyId = propertyId;
        this.accessMode = accessMode.name();
        this.updatedAt = now;
    }

    public EarthPropertyAccessMode getAccessModeType() { return EarthPropertyAccessMode.valueOf(accessMode); }
    public boolean hasActiveLease(long now) { return tenantId != null && now < leaseEndsAt; }
    public boolean canAccess(UUID playerId, UUID ownerId, long now) {
        return playerId.equals(ownerId) || hasActiveLease(now) && playerId.equals(tenantId)
                || getAccessModeType() == EarthPropertyAccessMode.PUBLIC || getAccessModeType() == EarthPropertyAccessMode.VISITABLE
                || getAccessModeType() == EarthPropertyAccessMode.BUSINESS;
    }

    public void setAccessMode(EarthPropertyAccessMode accessMode, long now) { this.accessMode = accessMode.name(); this.updatedAt = now; }
    public void setLeaseOffer(double price, long now) { this.leasePrice = Math.max(0D, price); this.tenantId = null; this.leaseEndsAt = 0L; this.updatedAt = now; }
    public void startLease(UUID tenantId, long endsAt, long now) { this.tenantId = tenantId; this.leaseEndsAt = endsAt; this.updatedAt = now; }
    public void clearLease(long now) { this.tenantId = null; this.leaseEndsAt = 0L; this.updatedAt = now; }
}
