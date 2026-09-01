package fr.openmc.core.features.earth.models;

import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;
import lombok.Getter;

/** Persistent accelerated calendar kept separate from real-time election clocks. */
@Getter
@DatabaseTable(tableName = "earth_simulation_clock")
public class DBEarthSimulationClock {
    public static final String GLOBAL_ID = "global";

    @DatabaseField(id = true)
    private String id;
    @DatabaseField(canBeNull = false)
    private double simulatedMonths;
    @DatabaseField(canBeNull = false)
    private long lastAdvancedAt;

    DBEarthSimulationClock() { }

    public DBEarthSimulationClock(long now) {
        this.id = GLOBAL_ID;
        this.lastAdvancedAt = now;
    }

    public void advance(long now, long realDaySeconds, double monthsPerRealDay) {
        long elapsed = Math.max(0L, now - lastAdvancedAt);
        simulatedMonths += elapsed / 1000D / Math.max(1D, realDaySeconds) * Math.max(0D, monthsPerRealDay);
        lastAdvancedAt = now;
    }

    /** Reset uptime baseline without applying server downtime to the virtual calendar. */
    public void resume(long now) { lastAdvancedAt = now; }
}
