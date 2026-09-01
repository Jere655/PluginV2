package fr.openmc.core.features.earth.models;
import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;
import lombok.Getter;
import java.util.UUID;
@Getter @DatabaseTable(tableName = "earth_transport_routes")
public class DBEarthTransportRoute {
 @DatabaseField(id=true,columnName="route_uuid") private UUID routeId;
 @DatabaseField(canBeNull=false,columnName="from_region") private String fromRegion;
 @DatabaseField(canBeNull=false,columnName="to_region") private String toRegion;
 @DatabaseField(canBeNull=false) private String mode;
 @DatabaseField(canBeNull=false) private double fare;
 @DatabaseField(canBeNull=false,columnName="pass_minutes") private long passMinutes;
 DBEarthTransportRoute() {}
 public DBEarthTransportRoute(String fromRegion,String toRegion,EarthTransportMode mode,double fare,long passMinutes){this.routeId=UUID.randomUUID();this.fromRegion=fromRegion;this.toRegion=toRegion;this.mode=mode.name();this.fare=fare;this.passMinutes=passMinutes;}
 public EarthTransportMode getModeType(){return EarthTransportMode.valueOf(mode);}
}
