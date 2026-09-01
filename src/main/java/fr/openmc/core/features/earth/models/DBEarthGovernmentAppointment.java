package fr.openmc.core.features.earth.models;

import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;
import lombok.Getter;

import java.util.Locale;
import java.util.UUID;

/** A durable, country-scoped assignment for one government portfolio. */
@Getter
@DatabaseTable(tableName = "earth_government_appointments")
public class DBEarthGovernmentAppointment {
    @DatabaseField(id = true, columnName = "appointment_id")
    private String id;
    @DatabaseField(canBeNull = false, columnName = "country_id")
    private String countryId;
    @DatabaseField(canBeNull = false)
    private String portfolio;
    @DatabaseField(canBeNull = false, columnName = "minister_uuid")
    private UUID ministerId;
    @DatabaseField(canBeNull = false, columnName = "appointed_by_uuid")
    private UUID appointedById;
    @DatabaseField(canBeNull = false, columnName = "appointed_at")
    private long appointedAt;

    DBEarthGovernmentAppointment() { }

    public DBEarthGovernmentAppointment(String countryId, EarthGovernmentPortfolio portfolio, UUID ministerId,
                                        UUID appointedById, long appointedAt) {
        this.countryId = countryId.toLowerCase(Locale.ROOT);
        this.portfolio = portfolio.name();
        this.id = this.countryId + ":" + this.portfolio.toLowerCase(Locale.ROOT);
        this.ministerId = ministerId;
        this.appointedById = appointedById;
        this.appointedAt = appointedAt;
    }

    public EarthGovernmentPortfolio getPortfolioType() {
        return EarthGovernmentPortfolio.valueOf(portfolio);
    }
}
