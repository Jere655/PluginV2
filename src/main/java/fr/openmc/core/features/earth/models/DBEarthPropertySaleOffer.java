package fr.openmc.core.features.earth.models;

import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;
import lombok.Getter;

import java.util.UUID;

/** A seller-created, restart-safe offer; only an explicit buyer acceptance can settle it. */
@Getter
@DatabaseTable(tableName = "earth_property_sale_offers")
public class DBEarthPropertySaleOffer {
    @DatabaseField(id = true, columnName = "property_uuid")
    private UUID propertyId;
    @DatabaseField(canBeNull = false, columnName = "seller_uuid")
    private UUID sellerId;
    @DatabaseField(canBeNull = false)
    private double price;
    @DatabaseField(canBeNull = false, columnName = "created_at")
    private long createdAt;
    @DatabaseField(canBeNull = false, columnName = "expires_at")
    private long expiresAt;

    DBEarthPropertySaleOffer() { }

    public DBEarthPropertySaleOffer(UUID propertyId, UUID sellerId, double price, long createdAt, long expiresAt) {
        this.propertyId = propertyId;
        this.sellerId = sellerId;
        this.price = Math.max(0D, price);
        this.createdAt = createdAt;
        this.expiresAt = expiresAt;
    }

    public boolean isActive(long now) { return now < expiresAt; }
}
