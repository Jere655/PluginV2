package fr.openmc.core.features.earth.models;

import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;
import lombok.Getter;

import java.util.UUID;

/** Business record backed by an existing OpenMC player shop. */
@Getter
@DatabaseTable(tableName = "earth_businesses")
public class DBEarthBusiness {
    @DatabaseField(id = true, columnName = "shop_uuid")
    private UUID shopId;
    @DatabaseField(canBeNull = false, columnName = "owner_uuid")
    private UUID ownerId;
    @DatabaseField(canBeNull = false, columnName = "property_uuid")
    private UUID propertyId;
    @DatabaseField(canBeNull = false, columnName = "region_id")
    private String regionId;
    @DatabaseField(canBeNull = false)
    private String industry;
    @DatabaseField(canBeNull = false)
    private double lifetimeTurnover;
    @DatabaseField(canBeNull = false)
    private double lifetimeTax;
    @DatabaseField(canBeNull = false)
    private long saleCount;
    @DatabaseField(canBeNull = false, columnName = "last_activity_at")
    private long lastActivityAt;

    DBEarthBusiness() {
        // ORMLite
    }

    public DBEarthBusiness(UUID shopId, UUID ownerId, UUID propertyId, String regionId, String industry, long createdAt) {
        this.shopId = shopId;
        this.ownerId = ownerId;
        this.propertyId = propertyId;
        this.regionId = regionId;
        this.industry = industry;
        this.lastActivityAt = createdAt;
    }

    public void recordSale(double revenue, double tax, long occurredAt) {
        lifetimeTurnover += revenue;
        lifetimeTax += tax;
        saleCount++;
        lastActivityAt = occurredAt;
    }
}
