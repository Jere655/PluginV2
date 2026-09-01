package fr.openmc.core.features.earth.models;
import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;
import lombok.Getter;
import java.util.UUID;
@Getter @DatabaseTable(tableName = "earth_travel_passes")
public class DBEarthTravelPass {
 @DatabaseField(id=true,columnName="player_uuid") private UUID playerId;
 @DatabaseField(canBeNull=false,columnName="route_uuid") private UUID routeId;
 @DatabaseField(canBeNull=false,columnName="destination_region") private String destinationRegion;
 @DatabaseField(canBeNull=false,columnName="expires_at") private long expiresAt;
 DBEarthTravelPass() {}
 public DBEarthTravelPass(UUID playerId,UUID routeId,String destinationRegion,long expiresAt){this.playerId=playerId;this.routeId=routeId;this.destinationRegion=destinationRegion;this.expiresAt=expiresAt;}
 public boolean isActive(long now){return now<expiresAt;}
}
