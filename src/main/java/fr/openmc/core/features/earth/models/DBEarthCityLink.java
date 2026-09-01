package fr.openmc.core.features.earth.models;

import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;
import lombok.Getter;

import java.util.UUID;

@Getter
@DatabaseTable(tableName = "earth_city_links")
public class DBEarthCityLink {
    @DatabaseField(id = true, columnName = "city_uuid")
    private UUID cityId;
    @DatabaseField(canBeNull = false, columnName = "region_id")
    private String regionId;

    DBEarthCityLink() {
        // ORMLite
    }

    public DBEarthCityLink(UUID cityId, String regionId) {
        this.cityId = cityId;
        this.regionId = regionId;
    }
}
