package fr.openmc.core.features.earth.models;

import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;
import lombok.Getter;

/** Pending commerce between deterministic regional simulation cycles. */
@Getter
@DatabaseTable(tableName = "earth_regional_ledgers")
public class DBEarthRegionalLedger {
    @DatabaseField(id = true, columnName = "region_id")
    private String regionId;
    @DatabaseField(canBeNull = false)
    private double pendingTurnover;
    @DatabaseField(canBeNull = false)
    private double pendingTax;
    @DatabaseField(canBeNull = false)
    private long pendingSales;

    DBEarthRegionalLedger() {
        // ORMLite
    }

    public DBEarthRegionalLedger(String regionId) {
        this.regionId = regionId;
    }

    public void recordSale(double revenue, double tax) {
        pendingTurnover += revenue;
        pendingTax += tax;
        pendingSales++;
    }

    public CommerceCycle consume() {
        CommerceCycle cycle = new CommerceCycle(pendingTurnover, pendingTax, pendingSales);
        pendingTurnover = 0D;
        pendingTax = 0D;
        pendingSales = 0L;
        return cycle;
    }

    public record CommerceCycle(double turnover, double tax, long sales) { }
}
