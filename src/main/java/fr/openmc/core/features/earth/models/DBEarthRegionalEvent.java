package fr.openmc.core.features.earth.models;

import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;
import lombok.Getter;
import java.util.UUID;

@Getter
@DatabaseTable(tableName = "earth_regional_events")
public class DBEarthRegionalEvent {
    @DatabaseField(id = true, columnName = "event_uuid") private UUID eventId;
    @DatabaseField(canBeNull = false, columnName = "region_id") private String regionId;
    @DatabaseField(canBeNull = false) private String eventType;
    @DatabaseField(canBeNull = false, columnName = "starts_at") private long startsAt;
    @DatabaseField(canBeNull = false, columnName = "ends_at") private long endsAt;
    @DatabaseField(columnName = "created_by") private UUID createdBy;
    DBEarthRegionalEvent() { }
    public DBEarthRegionalEvent(String regionId, EarthRegionalEventType eventType, long startsAt, long endsAt, UUID createdBy) {
        this.eventId = UUID.randomUUID(); this.regionId = regionId; this.eventType = eventType.name(); this.startsAt = startsAt; this.endsAt = endsAt; this.createdBy = createdBy;
    }
    public EarthRegionalEventType getType() { return EarthRegionalEventType.valueOf(eventType); }
    public boolean isActive(long now) { return now >= startsAt && now < endsAt; }
}
