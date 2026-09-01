package fr.openmc.core.features.earth.models;

import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;
import lombok.Getter;

import java.util.UUID;

/** Civic standing independent from country selection, kept separate for safe migrations. */
@Getter
@DatabaseTable(tableName = "earth_citizen_status")
public class DBEarthCitizenStatus {
    @DatabaseField(id = true, columnName = "player_uuid")
    private UUID playerId;
    @DatabaseField(canBeNull = false)
    private int reputation;
    @DatabaseField(canBeNull = false, columnName = "civic_actions")
    private long civicActions;
    @DatabaseField(canBeNull = false, columnName = "last_civic_at")
    private long lastCivicAt;

    DBEarthCitizenStatus() {
        // ORMLite
    }

    public DBEarthCitizenStatus(UUID playerId) {
        this.playerId = playerId;
    }

    public boolean canVolunteer(long now, long cooldownMillis) {
        return now - lastCivicAt >= cooldownMillis;
    }

    public void recordVolunteer(long now) {
        reputation = Math.min(10_000, reputation + 5);
        civicActions++;
        lastCivicAt = now;
    }

    public void recordVote(long now) {
        reputation = Math.min(10_000, reputation + 2);
        civicActions++;
        lastCivicAt = now;
    }

    public double productivityMultiplier() {
        return 1D + Math.min(0.15D, reputation / 10_000D);
    }
}
