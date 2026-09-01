package fr.openmc.core.features.earth.models;

import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;
import lombok.Getter;

import java.util.Set;

/**
 * Persistent aggregate conditions for one region. This deliberately models
 * people statistically: it is a bounded input/output surface for gameplay,
 * not a physical NPC population.
 */
@Getter
@DatabaseTable(tableName = "earth_regional_indicators")
public class DBEarthRegionalIndicators {
    @DatabaseField(id = true, columnName = "region_id")
    private String regionId;
    @DatabaseField(canBeNull = false, defaultValue = "0.02")
    private double inflation;
    @DatabaseField(canBeNull = false, defaultValue = "0.25")
    private double crime;
    @DatabaseField(canBeNull = false, defaultValue = "0.65")
    private double health;
    @DatabaseField(canBeNull = false, defaultValue = "0.55")
    private double education;
    @DatabaseField(canBeNull = false, defaultValue = "0.20")
    private double pollution;
    @DatabaseField(canBeNull = false, defaultValue = "0.50")
    private double infrastructure;
    @DatabaseField(canBeNull = false, defaultValue = "0.65")
    private double energy;
    @DatabaseField(canBeNull = false, defaultValue = "0.70")
    private double foodSecurity;
    @DatabaseField(canBeNull = false, defaultValue = "0")
    private double publicDebt;
    @DatabaseField(canBeNull = false, defaultValue = "0.55")
    private double approval;
    @DatabaseField(canBeNull = false)
    private long lastUpdatedAt;

    DBEarthRegionalIndicators() { }

    public DBEarthRegionalIndicators(String regionId, long now) {
        this.regionId = regionId;
        this.inflation = 0.02D;
        this.crime = 0.25D;
        this.health = 0.65D;
        this.education = 0.55D;
        this.pollution = 0.20D;
        this.infrastructure = 0.50D;
        this.energy = 0.65D;
        this.foodSecurity = 0.70D;
        this.publicDebt = 0D;
        this.approval = 0.55D;
        this.lastUpdatedAt = now;
    }

    /** Applies one bounded causal cycle after fiscal policy, law and events. */
    public void simulate(DBEarthRegionState state, DBEarthCountryPolicy policy, Set<EarthLawType> laws,
                         Set<EarthRegionalEventType> events, int activeActors, long now) {
        double social = policy == null ? 0.40D : policy.getSocialServices();
        double investment = policy == null ? 0.25D : policy.getInvestment();
        double infrastructureBudget = policy == null ? 0.35D : policy.getInfrastructure();
        double activity = Math.min(1D, Math.max(0D, activeActors / 100D)) * state.getProsperity();
        boolean green = laws != null && laws.contains(EarthLawType.GREEN_INFRASTRUCTURE);
        boolean safety = laws != null && laws.contains(EarthLawType.PUBLIC_SAFETY);

        pollution = clamp(pollution + activity * 0.012D - infrastructureBudget * 0.010D - (green ? 0.018D : 0D));
        infrastructure = clamp(infrastructure + infrastructureBudget * 0.018D + investment * 0.005D);
        education = clamp(education + social * 0.010D + investment * 0.008D - crime * 0.004D);
        foodSecurity = clamp(foodSecurity + state.getProsperity() * 0.008D + investment * 0.004D - inflation * 0.030D);
        energy = clamp(energy + infrastructureBudget * 0.008D - activity * 0.006D);
        health = clamp(health + social * 0.014D + foodSecurity * 0.006D - pollution * 0.018D - crime * 0.005D);
        crime = clamp(crime + (1D - state.getEmployment()) * 0.020D + (1D - state.getSatisfaction()) * 0.012D
                - (safety ? 0.020D : 0D) - social * 0.008D);
        inflation = clamp(inflation + activity * 0.004D - foodSecurity * 0.003D - energy * 0.002D, 0D, 0.35D);
        publicDebt = Math.max(0D, publicDebt + Math.max(0D, 250D - state.getTreasury()) * 0.002D
                - Math.max(0D, state.getTreasury() - 1_000D) * 0.001D);

        applyEvents(events);
        approval = clamp(state.getSatisfaction() * 0.42D + health * 0.20D + infrastructure * 0.12D
                + education * 0.10D + foodSecurity * 0.10D - crime * 0.16D - pollution * 0.10D
                - Math.min(0.15D, inflation * 0.6D) - Math.min(0.12D, publicDebt / 100_000D));
        lastUpdatedAt = now;
    }

    private void applyEvents(Set<EarthRegionalEventType> events) {
        if (events == null || events.isEmpty()) return;
        if (events.contains(EarthRegionalEventType.RECESSION)) inflation = clamp(inflation + 0.012D, 0D, 0.35D);
        if (events.contains(EarthRegionalEventType.POLLUTION_CRISIS)) pollution = clamp(pollution + 0.045D);
        if (events.contains(EarthRegionalEventType.STRIKE)) infrastructure = clamp(infrastructure - 0.035D);
        if (events.contains(EarthRegionalEventType.EPIDEMIC)) health = clamp(health - 0.050D);
        if (events.contains(EarthRegionalEventType.FOOD_SHORTAGE)) foodSecurity = clamp(foodSecurity - 0.055D);
        if (events.contains(EarthRegionalEventType.ENERGY_SHORTAGE)) energy = clamp(energy - 0.055D);
        if (events.contains(EarthRegionalEventType.MIGRATION_WAVE)) education = clamp(education + 0.008D);
    }

    public double productivityMultiplier() {
        return clamp(0.65D + health * 0.15D + education * 0.10D + infrastructure * 0.10D
                + energy * 0.08D - pollution * 0.12D - crime * 0.08D, 0.45D, 1.25D);
    }

    /** Poor infrastructure and high pollution make paid travel less efficient. */
    public double transportCostMultiplier() {
        return clamp(1D + (1D - infrastructure) * 0.20D + pollution * 0.10D, 1D, 1.30D);
    }

    /**
     * Routes are physical-economic infrastructure: rail provides the cleanest
     * mobility benefit while air has the largest pollution cost. Effects stay
     * bounded and feed the normal societal feedback path in the same cycle.
     */
    public void applyTransportNetwork(int roadRoutes, int railRoutes, int airRoutes, int waterRoutes) {
        int roads = Math.max(0, roadRoutes);
        int rails = Math.max(0, railRoutes);
        int air = Math.max(0, airRoutes);
        int water = Math.max(0, waterRoutes);
        int total = roads + rails + air + water;
        if (total == 0) return;
        infrastructure = clamp(infrastructure + Math.min(0.025D, total * 0.0025D + rails * 0.0015D));
        pollution = clamp(pollution + roads * 0.0015D + air * 0.004D + water * 0.001D - rails * 0.0015D);
        energy = clamp(energy - air * 0.0015D - roads * 0.0005D + rails * 0.0005D);
    }

    /**
     * An installed cabinet gives the corresponding public system modest,
     * bounded operational attention every cycle. These are intentionally
     * smaller than budget and law effects: appointments improve delivery but
     * cannot substitute for funding or policy.
     */
    public void applyGovernmentPortfolios(Set<EarthGovernmentPortfolio> portfolios) {
        if (portfolios == null || portfolios.isEmpty()) return;
        for (EarthGovernmentPortfolio portfolio : portfolios) {
            switch (portfolio) {
                case FINANCE -> {
                    inflation = clamp(inflation - 0.0015D, 0D, 0.35D);
                    publicDebt = Math.max(0D, publicDebt - 1.5D);
                }
                case HEALTH -> health = clamp(health + 0.003D);
                case EDUCATION -> education = clamp(education + 0.003D);
                case INFRASTRUCTURE -> infrastructure = clamp(infrastructure + 0.003D);
                case ENVIRONMENT -> pollution = clamp(pollution - 0.003D);
                case PUBLIC_SAFETY -> crime = clamp(crime - 0.003D);
                case LABOUR -> approval = clamp(approval + 0.001D);
                case FOREIGN_AFFAIRS -> foodSecurity = clamp(foodSecurity + 0.001D);
            }
        }
    }

    private static double clamp(double value) { return clamp(value, 0D, 1D); }
    private static double clamp(double value, double min, double max) { return Math.max(min, Math.min(max, value)); }
}
