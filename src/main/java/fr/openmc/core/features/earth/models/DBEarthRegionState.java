package fr.openmc.core.features.earth.models;

import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;
import lombok.Getter;

import java.util.Set;

@Getter
@DatabaseTable(tableName = "earth_region_states")
public class DBEarthRegionState {
    @DatabaseField(id = true, columnName = "region_id")
    private String regionId;
    @DatabaseField(canBeNull = false)
    private double treasury;
    @DatabaseField(canBeNull = false)
    private double prosperity;
    @DatabaseField(canBeNull = false)
    private double employment;
    @DatabaseField(canBeNull = false)
    private double satisfaction;
    @DatabaseField(canBeNull = false)
    private long lastSimulationAt;

    DBEarthRegionState() {
        // ORMLite
    }

    public DBEarthRegionState(String regionId, double treasury, double prosperity, double employment, double satisfaction, long lastSimulationAt) {
        this.regionId = regionId;
        this.treasury = treasury;
        this.prosperity = prosperity;
        this.employment = employment;
        this.satisfaction = satisfaction;
        this.lastSimulationAt = lastSimulationAt;
    }

    public void simulate(double taxRate, int activeCitizens, long now) {
        double services = Math.max(1D, treasury / 1000D);
        double economicActivity = Math.max(1D, activeCitizens) * prosperity * employment;
        treasury = Math.max(0D, treasury + (economicActivity * taxRate) - services);
        prosperity = clamp(prosperity + (employment - 0.55D) * 0.025D - taxRate * 0.01D, 0D, 1D);
        employment = clamp(employment + (prosperity - 0.5D) * 0.02D, 0.15D, 0.98D);
        satisfaction = clamp((prosperity * 0.55D) + (employment * 0.35D) + Math.min(0.1D, treasury / 100000D) - taxRate * 0.15D, 0D, 1D);
        lastSimulationAt = now;
    }

    public void creditTreasury(double amount) {
        if (amount > 0D) treasury += amount;
    }

    /** Applies a small bounded public-service budget before the normal regional cycle. */
    public void applyPublicBudget(DBEarthCountryPolicy policy) {
        if (policy == null || treasury <= 0D) return;
        double budget = treasury * 0.02D;
        treasury -= budget;
        prosperity = clamp(prosperity + policy.getInfrastructure() * 0.012D + policy.getInvestment() * 0.010D, 0D, 1D);
        employment = clamp(employment + policy.getInvestment() * 0.012D, 0.15D, 0.98D);
        satisfaction = clamp(satisfaction + policy.getSocialServices() * 0.018D + policy.getInfrastructure() * 0.004D, 0D, 1D);
    }

    /** Applies active national law effects after budget allocation. */
    public void applyLaws(Set<EarthLawType> laws) {
        if (laws == null || laws.isEmpty()) return;
        if (laws.contains(EarthLawType.PUBLIC_SAFETY)) satisfaction = clamp(satisfaction + 0.008D, 0D, 1D);
        if (laws.contains(EarthLawType.GREEN_INFRASTRUCTURE)) {
            prosperity = clamp(prosperity + 0.006D, 0D, 1D);
            satisfaction = clamp(satisfaction + 0.004D, 0D, 1D);
        }
        if (laws.contains(EarthLawType.WORKER_PROTECTION)) satisfaction = clamp(satisfaction + 0.005D, 0D, 1D);
    }

    /** Small bounded benefit of resident civic participation. */
    public void applyCivicContribution() {
        satisfaction = clamp(satisfaction + 0.006D, 0D, 1D);
    }

    public void applyDemocraticMandate() {
        satisfaction = clamp(satisfaction + 0.002D, 0D, 1D);
    }

    public void applyEvents(Set<EarthRegionalEventType> events) {
        if (events == null || events.isEmpty()) return;
        if (events.contains(EarthRegionalEventType.HARVEST_FESTIVAL)) prosperity = clamp(prosperity + 0.008D, 0D, 1D);
        if (events.contains(EarthRegionalEventType.CIVIC_CELEBRATION)) satisfaction = clamp(satisfaction + 0.010D, 0D, 1D);
        if (events.contains(EarthRegionalEventType.INFRASTRUCTURE_DISRUPTION)) {
            prosperity = clamp(prosperity - 0.010D, 0D, 1D);
            employment = clamp(employment - 0.008D, 0.15D, 0.98D);
        }
    }

    public void applyTradeClimate(double multiplier) { prosperity = clamp(prosperity + (multiplier - 1D) * 0.02D, 0D, 1D); }

    /**
     * Feed the wider social conditions back into the next regional cycle.
     * This makes pollution, crime, health and infrastructure gameplay inputs,
     * rather than a decorative dashboard.
     */
    public void applySocietalConditions(DBEarthRegionalIndicators indicators) {
        if (indicators == null) return;
        prosperity = clamp(prosperity + (indicators.getInfrastructure() - 0.5D) * 0.012D
                + (indicators.getEducation() - 0.5D) * 0.006D - indicators.getPollution() * 0.010D
                - indicators.getInflation() * 0.020D, 0D, 1D);
        employment = clamp(employment + (indicators.getInfrastructure() - 0.5D) * 0.008D
                + (indicators.getEnergy() - 0.5D) * 0.006D - indicators.getCrime() * 0.008D, 0.15D, 0.98D);
        satisfaction = clamp(satisfaction + (indicators.getHealth() - 0.5D) * 0.014D
                + (indicators.getFoodSecurity() - 0.5D) * 0.010D - indicators.getCrime() * 0.012D
                - indicators.getPollution() * 0.009D + (indicators.getApproval() - 0.5D) * 0.006D, 0D, 1D);
    }

    private static double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }
}
