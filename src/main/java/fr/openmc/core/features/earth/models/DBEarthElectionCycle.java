package fr.openmc.core.features.earth.models;

import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;
import lombok.Getter;

/** Country election clock retained across plugin reloads and server restarts. */
@Getter
@DatabaseTable(tableName = "earth_election_cycles")
public class DBEarthElectionCycle {
    @DatabaseField(id = true, columnName = "country_id")
    private String countryId;
    @DatabaseField(canBeNull = false)
    private String phase;
    @DatabaseField(canBeNull = false)
    private long phaseStartedAt;
    @DatabaseField(canBeNull = false)
    private long phaseEndsAt;
    @DatabaseField(canBeNull = false)
    private long mandateEndsAt;

    DBEarthElectionCycle() { }

    public DBEarthElectionCycle(String countryId, EarthElectionPhase phase, long phaseStartedAt, long phaseEndsAt, long mandateEndsAt) {
        this.countryId = countryId;
        moveTo(phase, phaseStartedAt, phaseEndsAt, mandateEndsAt);
    }

    public EarthElectionPhase getPhaseType() { return EarthElectionPhase.valueOf(phase); }
    public boolean phaseHasEnded(long now) { return now >= phaseEndsAt; }

    public void moveTo(EarthElectionPhase phase, long startedAt, long endsAt, long mandateEndsAt) {
        this.phase = phase.name();
        this.phaseStartedAt = startedAt;
        this.phaseEndsAt = endsAt;
        this.mandateEndsAt = mandateEndsAt;
    }
}
