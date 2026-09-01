package fr.openmc.core.features.earth.models;

import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;
import lombok.Getter;

/** Records the highest non-destructive Earth schema migration applied to a database. */
@Getter
@DatabaseTable(tableName = "earth_schema_version")
public class DBEarthSchemaVersion {
    public static final String ID = "earth";

    @DatabaseField(id = true)
    private String id;
    @DatabaseField(canBeNull = false)
    private int version;
    @DatabaseField(canBeNull = false)
    private long updatedAt;

    DBEarthSchemaVersion() { }

    public DBEarthSchemaVersion(int version, long updatedAt) {
        this.id = ID;
        this.version = version;
        this.updatedAt = updatedAt;
    }

    public void advanceTo(int version, long updatedAt) {
        this.version = version;
        this.updatedAt = updatedAt;
    }
}
