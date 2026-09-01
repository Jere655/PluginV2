package fr.openmc.core.features.earth.models;
import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;
import lombok.Getter;
@Getter @DatabaseTable(tableName="earth_diplomatic_relations")
public class DBEarthDiplomaticRelation {
 @DatabaseField(id=true) private String id; @DatabaseField(canBeNull=false,columnName="country_a") private String countryA; @DatabaseField(canBeNull=false,columnName="country_b") private String countryB; @DatabaseField(canBeNull=false) private String status; @DatabaseField(canBeNull=false,columnName="updated_at") private long updatedAt;
 DBEarthDiplomaticRelation(){}
 public DBEarthDiplomaticRelation(String countryA,String countryB,EarthDiplomaticStatus status,long updatedAt){this.countryA=countryA;this.countryB=countryB;this.id=idFor(countryA,countryB);this.status=status.name();this.updatedAt=updatedAt;}
 public EarthDiplomaticStatus getStatusType(){return EarthDiplomaticStatus.valueOf(status);}
 public void setStatus(EarthDiplomaticStatus status,long updatedAt){this.status=status.name();this.updatedAt=updatedAt;}
 public static String idFor(String a,String b){return a.compareToIgnoreCase(b)<=0?a.toLowerCase(java.util.Locale.ROOT)+":"+b.toLowerCase(java.util.Locale.ROOT):b.toLowerCase(java.util.Locale.ROOT)+":"+a.toLowerCase(java.util.Locale.ROOT);}
}
