package fr.openmc.core.features.earth;

import com.j256.ormlite.dao.Dao;
import com.j256.ormlite.dao.DaoManager;
import com.j256.ormlite.support.ConnectionSource;
import fr.openmc.core.OMCPlugin;
import fr.openmc.core.bootstrap.features.DisableFeatureException;
import fr.openmc.core.bootstrap.features.Feature;
import fr.openmc.core.bootstrap.features.types.HasCommands;
import fr.openmc.core.bootstrap.features.types.HasDatabase;
import fr.openmc.core.bootstrap.features.types.HasListeners;
import fr.openmc.core.bootstrap.listeners.ListenerFactory;
import fr.openmc.core.features.earth.commands.EarthCommands;
import fr.openmc.core.features.earth.listeners.EarthCitizenshipListener;
import fr.openmc.core.features.earth.integrations.EarthMapAdapter;
import fr.openmc.core.features.earth.integrations.EarthNpcAdapter;
import fr.openmc.core.features.earth.models.*;
import fr.openmc.core.features.city.City;
import fr.openmc.core.features.city.CityManager;
import fr.openmc.core.features.shops.models.Shop;
import fr.openmc.core.features.shops.managers.ShopManager;
import fr.openmc.core.features.economy.EconomyManager;
import fr.openmc.core.features.homes.models.Home;
import fr.openmc.core.features.homes.HomesManager;
import lombok.Getter;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitTask;

import java.sql.SQLException;
import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Persistent Earth layer. It deliberately links to existing OpenMC cities and
 * balances instead of owning claims or player money itself.
 */
public class EarthManager extends Feature implements HasDatabase, HasCommands, HasListeners {
    @Getter
    private static EarthManager instance;

    private final Map<String, DBEarthCountry> countries = new ConcurrentHashMap<>();
    private final Map<String, DBEarthCountryStatistics> countryStatistics = new ConcurrentHashMap<>();
    private final Map<String, DBEarthRegion> regions = new ConcurrentHashMap<>();
    private final Map<UUID, DBEarthCityLink> cityLinks = new ConcurrentHashMap<>();
    private final Map<UUID, DBEarthCitizenship> citizenships = new ConcurrentHashMap<>();
    private final Map<String, DBEarthRegionState> regionStates = new ConcurrentHashMap<>();
    private final Map<String, DBEarthRegionalIndicators> regionalIndicators = new ConcurrentHashMap<>();
    private final Map<String, DBEarthCountryPolicy> countryPolicies = new ConcurrentHashMap<>();
    private final Map<UUID, DBEarthProperty> properties = new ConcurrentHashMap<>();
    private final Map<UUID, DBEarthPropertyAccess> propertyAccess = new ConcurrentHashMap<>();
    private final Map<UUID, DBEarthPropertySaleOffer> propertySaleOffers = new ConcurrentHashMap<>();
    private final Map<UUID, DBEarthBusiness> businesses = new ConcurrentHashMap<>();
    private final Map<String, DBEarthRegionalLedger> regionalLedgers = new ConcurrentHashMap<>();
    private final Map<UUID, DBEarthJob> jobs = new ConcurrentHashMap<>();
    private final Map<String, Set<EarthLawType>> lawsByCountry = new ConcurrentHashMap<>();
    private final Map<String, DBEarthLaw> lawRecords = new ConcurrentHashMap<>();
    private final Map<UUID, DBEarthCitizenStatus> citizenStatuses = new ConcurrentHashMap<>();
    private final Map<String, Map<UUID, DBEarthElectionCandidate>> candidatesByCountry = new ConcurrentHashMap<>();
    private final Map<String, Map<UUID, DBEarthElectionVote>> votesByCountry = new ConcurrentHashMap<>();
    private final Map<String, DBEarthCountryOffice> offices = new ConcurrentHashMap<>();
    private final Map<String, DBEarthGovernment> governments = new ConcurrentHashMap<>();
    private final Map<String, Map<EarthGovernmentPortfolio, DBEarthGovernmentAppointment>> appointmentsByCountry = new ConcurrentHashMap<>();
    private final Map<String, DBEarthPoliticalParty> politicalParties = new ConcurrentHashMap<>();
    private final Map<UUID, DBEarthPartyMembership> partyMemberships = new ConcurrentHashMap<>();
    private final Map<UUID, DBEarthBill> bills = new ConcurrentHashMap<>();
    private final Map<String, DBEarthElectionCycle> electionCycles = new ConcurrentHashMap<>();
    private final Map<UUID, DBEarthRegionalEvent> regionalEvents = new ConcurrentHashMap<>();
    private final Map<UUID, DBEarthTransportRoute> transportRoutes = new ConcurrentHashMap<>();
    private final Map<UUID, DBEarthTravelPass> travelPasses = new ConcurrentHashMap<>();
    private final Map<String, DBEarthDemographics> demographics = new ConcurrentHashMap<>();
    private final Map<String, DBEarthRegionWaypoint> waypoints = new ConcurrentHashMap<>();
    private final Map<String, DBEarthDiplomaticRelation> diplomaticRelations = new ConcurrentHashMap<>();
    /** Serializes async ORM writes so an older queued mutation cannot overtake a newer one. */
    private final Queue<SqlOperation> persistenceQueue = new ConcurrentLinkedQueue<>();
    private final AtomicBoolean persistenceDraining = new AtomicBoolean();

    private Dao<DBEarthCountry, String> countriesDao;
    private Dao<DBEarthCountryStatistics, String> countryStatisticsDao;
    private Dao<DBEarthRegion, String> regionsDao;
    private Dao<DBEarthCityLink, UUID> cityLinksDao;
    private Dao<DBEarthCitizenship, UUID> citizenshipsDao;
    private Dao<DBEarthRegionState, String> regionStatesDao;
    private Dao<DBEarthRegionalIndicators, String> regionalIndicatorsDao;
    private Dao<DBEarthCountryPolicy, String> countryPoliciesDao;
    private Dao<DBEarthProperty, UUID> propertiesDao;
    private Dao<DBEarthPropertyAccess, UUID> propertyAccessDao;
    private Dao<DBEarthPropertySaleOffer, UUID> propertySaleOffersDao;
    private Dao<DBEarthBusiness, UUID> businessesDao;
    private Dao<DBEarthRegionalLedger, String> regionalLedgersDao;
    private Dao<DBEarthJob, UUID> jobsDao;
    private Dao<DBEarthLaw, String> lawsDao;
    private Dao<DBEarthCitizenStatus, UUID> citizenStatusesDao;
    private Dao<DBEarthElectionCandidate, String> candidatesDao;
    private Dao<DBEarthElectionVote, String> votesDao;
    private Dao<DBEarthCountryOffice, String> officesDao;
    private Dao<DBEarthGovernment, String> governmentsDao;
    private Dao<DBEarthGovernmentAppointment, String> governmentAppointmentsDao;
    private Dao<DBEarthPoliticalParty, String> politicalPartiesDao;
    private Dao<DBEarthPartyMembership, UUID> partyMembershipsDao;
    private Dao<DBEarthBill, UUID> billsDao;
    private Dao<DBEarthElectionCycle, String> electionCyclesDao;
    private Dao<DBEarthRegionalEvent, UUID> regionalEventsDao;
    private Dao<DBEarthTransportRoute, UUID> transportRoutesDao;
    private Dao<DBEarthTravelPass, UUID> travelPassesDao;
    private Dao<DBEarthDemographics, String> demographicsDao;
    private Dao<DBEarthRegionWaypoint, String> waypointsDao;
    private Dao<DBEarthDiplomaticRelation, String> diplomaticRelationsDao;
    private Dao<DBEarthSimulationClock, String> simulationClockDao;
    private boolean earthEnabled;
    @Getter
    private double earthScaleMetersPerBlock;
    private String defaultCountryId;
    private long simulationPeriodTicks;
    private long workCooldownMillis;
    private long civicCooldownMillis;
    private long electionCandidacyMillis;
    private long electionCampaignMillis;
    private long electionVotingMillis;
    private long electionMandateMillis;
    private long simulationRealDaySeconds;
    private double simulatedMonthsPerRealDay;
    private DBEarthSimulationClock simulationClock;
    private volatile EarthMapAdapter mapAdapter = overlays -> { };
    private volatile EarthNpcAdapter npcAdapter = (player, region, population, conditions) -> { };
    private BukkitTask simulationTask;

    @Override
    public void initDB(ConnectionSource connectionSource) throws SQLException {
        EarthSchemaManager.migrate(connectionSource);
        countriesDao = DaoManager.createDao(connectionSource, DBEarthCountry.class);
        countryStatisticsDao = DaoManager.createDao(connectionSource, DBEarthCountryStatistics.class);
        regionsDao = DaoManager.createDao(connectionSource, DBEarthRegion.class);
        cityLinksDao = DaoManager.createDao(connectionSource, DBEarthCityLink.class);
        citizenshipsDao = DaoManager.createDao(connectionSource, DBEarthCitizenship.class);
        regionStatesDao = DaoManager.createDao(connectionSource, DBEarthRegionState.class);
        regionalIndicatorsDao = DaoManager.createDao(connectionSource, DBEarthRegionalIndicators.class);
        countryPoliciesDao = DaoManager.createDao(connectionSource, DBEarthCountryPolicy.class);
        propertiesDao = DaoManager.createDao(connectionSource, DBEarthProperty.class);
        propertyAccessDao = DaoManager.createDao(connectionSource, DBEarthPropertyAccess.class);
        propertySaleOffersDao = DaoManager.createDao(connectionSource, DBEarthPropertySaleOffer.class);
        businessesDao = DaoManager.createDao(connectionSource, DBEarthBusiness.class);
        regionalLedgersDao = DaoManager.createDao(connectionSource, DBEarthRegionalLedger.class);
        jobsDao = DaoManager.createDao(connectionSource, DBEarthJob.class);
        lawsDao = DaoManager.createDao(connectionSource, DBEarthLaw.class);
        citizenStatusesDao = DaoManager.createDao(connectionSource, DBEarthCitizenStatus.class);
        candidatesDao = DaoManager.createDao(connectionSource, DBEarthElectionCandidate.class);
        votesDao = DaoManager.createDao(connectionSource, DBEarthElectionVote.class);
        officesDao = DaoManager.createDao(connectionSource, DBEarthCountryOffice.class);
        governmentsDao = DaoManager.createDao(connectionSource, DBEarthGovernment.class);
        governmentAppointmentsDao = DaoManager.createDao(connectionSource, DBEarthGovernmentAppointment.class);
        politicalPartiesDao = DaoManager.createDao(connectionSource, DBEarthPoliticalParty.class);
        partyMembershipsDao = DaoManager.createDao(connectionSource, DBEarthPartyMembership.class);
        billsDao = DaoManager.createDao(connectionSource, DBEarthBill.class);
        electionCyclesDao = DaoManager.createDao(connectionSource, DBEarthElectionCycle.class);
        regionalEventsDao = DaoManager.createDao(connectionSource, DBEarthRegionalEvent.class);
        transportRoutesDao = DaoManager.createDao(connectionSource, DBEarthTransportRoute.class);
        travelPassesDao = DaoManager.createDao(connectionSource, DBEarthTravelPass.class);
        demographicsDao = DaoManager.createDao(connectionSource, DBEarthDemographics.class);
        waypointsDao = DaoManager.createDao(connectionSource, DBEarthRegionWaypoint.class);
        diplomaticRelationsDao = DaoManager.createDao(connectionSource, DBEarthDiplomaticRelation.class);
        simulationClockDao = DaoManager.createDao(connectionSource, DBEarthSimulationClock.class);
    }

    @Override
    public void init() {
        readConfiguration();
        if (!earthEnabled) {
            throw new DisableFeatureException("Earth simulation is disabled by earth.enabled");
        }
        instance = this;
        loadState();
        simulationClock.resume(System.currentTimeMillis());
        bootstrapConfiguredGeography();
        simulationTask = Bukkit.getScheduler().runTaskTimer(OMCPlugin.getInstance(), this::simulateOnce,
                simulationPeriodTicks, simulationPeriodTicks);
    }

    @Override
    public void save() {
        if (simulationTask != null) simulationTask.cancel();
        persistAll();
    }

    @Override
    public Set<Object> getCommands() {
        return Set.of(new EarthCommands());
    }

    @Override
    public Set<ListenerFactory> getListeners() {
        return Set.of(EarthCitizenshipListener::new);
    }

    private void readConfiguration() {
        earthEnabled = OMCPlugin.getConfigs().getBoolean("earth.enabled", true);
        earthScaleMetersPerBlock = Math.max(0.001D, OMCPlugin.getConfigs().getDouble("earth.scale", 1_000D));
        defaultCountryId = OMCPlugin.getConfigs().getString("earth.default-country", "canada").toLowerCase(Locale.ROOT);
        simulationPeriodTicks = Math.max(20L, OMCPlugin.getConfigs().getLong("earth.simulation-period-ticks", 1200L));
        workCooldownMillis = Math.max(1_000L, OMCPlugin.getConfigs().getLong("earth.work-cooldown-seconds", 60L) * 1_000L);
        civicCooldownMillis = Math.max(1_000L, OMCPlugin.getConfigs().getLong("earth.civic-cooldown-seconds", 300L) * 1_000L);
        electionCandidacyMillis = Math.max(60_000L, OMCPlugin.getConfigs().getLong("earth.elections.candidacy-seconds", 172_800L) * 1_000L);
        electionCampaignMillis = Math.max(60_000L, OMCPlugin.getConfigs().getLong("earth.elections.campaign-seconds", 172_800L) * 1_000L);
        electionVotingMillis = Math.max(60_000L, OMCPlugin.getConfigs().getLong("earth.elections.voting-seconds", 86_400L) * 1_000L);
        electionMandateMillis = Math.max(60_000L, OMCPlugin.getConfigs().getLong("earth.elections.mandate-seconds", 604_800L) * 1_000L);
        simulationRealDaySeconds = Math.max(1L, OMCPlugin.getConfigs().getLong("earth.simulation.real-day-seconds", 86_400L));
        simulatedMonthsPerRealDay = Math.max(0D, OMCPlugin.getConfigs().getDouble("earth.simulation.simulated-months-per-real-day", 6D));
    }

    private void loadState() {
        try {
            countriesDao.queryForAll().forEach(country -> countries.put(country.getId(), country));
            countryStatisticsDao.queryForAll().forEach(statistics -> countryStatistics.put(statistics.getCountryId(), statistics));
            regionsDao.queryForAll().forEach(region -> regions.put(region.getId(), region));
            cityLinksDao.queryForAll().forEach(link -> cityLinks.put(link.getCityId(), link));
            citizenshipsDao.queryForAll().forEach(citizenship -> citizenships.put(citizenship.getPlayerId(), citizenship));
            regionStatesDao.queryForAll().forEach(state -> regionStates.put(state.getRegionId(), state));
            regionalIndicatorsDao.queryForAll().forEach(indicators -> regionalIndicators.put(indicators.getRegionId(), indicators));
            countryPoliciesDao.queryForAll().forEach(policy -> countryPolicies.put(policy.getCountryId(), policy));
            propertiesDao.queryForAll().forEach(property -> properties.put(property.getPropertyId(), property));
            propertyAccessDao.queryForAll().forEach(access -> propertyAccess.put(access.getPropertyId(), access));
            propertySaleOffersDao.queryForAll().forEach(offer -> propertySaleOffers.put(offer.getPropertyId(), offer));
            businessesDao.queryForAll().forEach(business -> businesses.put(business.getShopId(), business));
            regionalLedgersDao.queryForAll().forEach(ledger -> regionalLedgers.put(ledger.getRegionId(), ledger));
            jobsDao.queryForAll().forEach(job -> jobs.put(job.getPlayerId(), job));
            lawsDao.queryForAll().forEach(law -> {
                lawsByCountry.computeIfAbsent(law.getCountryId(), ignored -> ConcurrentHashMap.newKeySet()).add(law.getType());
                lawRecords.put(law.getId(), law);
            });
            citizenStatusesDao.queryForAll().forEach(status -> citizenStatuses.put(status.getPlayerId(), status));
            candidatesDao.queryForAll().forEach(candidate -> candidatesByCountry.computeIfAbsent(candidate.getCountryId(), ignored -> new ConcurrentHashMap<>()).put(candidate.getCandidateId(), candidate));
            votesDao.queryForAll().forEach(vote -> votesByCountry.computeIfAbsent(vote.getCountryId(), ignored -> new ConcurrentHashMap<>()).put(vote.getVoterId(), vote));
            officesDao.queryForAll().forEach(office -> offices.put(office.getCountryId(), office));
            governmentsDao.queryForAll().forEach(government -> governments.put(government.getCountryId(), government));
            governmentAppointmentsDao.queryForAll().forEach(appointment -> appointmentsByCountry
                    .computeIfAbsent(appointment.getCountryId(), ignored -> new ConcurrentHashMap<>())
                    .put(appointment.getPortfolioType(), appointment));
            politicalPartiesDao.queryForAll().forEach(party -> politicalParties.put(party.getId(), party));
            partyMembershipsDao.queryForAll().forEach(membership -> partyMemberships.put(membership.getPlayerId(), membership));
            billsDao.queryForAll().forEach(bill -> bills.put(bill.getBillId(), bill));
            electionCyclesDao.queryForAll().forEach(cycle -> electionCycles.put(cycle.getCountryId(), cycle));
            regionalEventsDao.queryForAll().forEach(event -> regionalEvents.put(event.getEventId(), event));
            transportRoutesDao.queryForAll().forEach(route -> transportRoutes.put(route.getRouteId(), route));
            travelPassesDao.queryForAll().forEach(pass -> travelPasses.put(pass.getPlayerId(), pass));
            demographicsDao.queryForAll().forEach(demographic -> demographics.put(demographic.getRegionId(), demographic));
            waypointsDao.queryForAll().forEach(waypoint -> waypoints.put(waypoint.getRegionId(), waypoint));
            diplomaticRelationsDao.queryForAll().forEach(relation -> diplomaticRelations.put(relation.getId(), relation));
            simulationClock = Optional.ofNullable(simulationClockDao.queryForId(DBEarthSimulationClock.GLOBAL_ID))
                    .orElseGet(() -> new DBEarthSimulationClock(System.currentTimeMillis()));
        } catch (SQLException exception) {
            throw new IllegalStateException("Unable to load Earth simulation state", exception);
        }
    }

    private void bootstrapConfiguredGeography() {
        ConfigurationSection countriesSection = OMCPlugin.getConfigs().getConfigurationSection("earth.countries");
        if (countriesSection == null) {
            registerCountry(new DBEarthCountry("canada", "Canada", "Ottawa", "CAD", "federal_democracy", 0.12D));
            registerPolicy(new DBEarthCountryPolicy("canada", 0.40D, 0.35D, 0.25D, System.currentTimeMillis()));
            registerRegion(new DBEarthRegion("canada-ontario", "canada", "Ontario", -5000, 5000, -5000, 5000));
            return;
        }

        for (String countryId : countriesSection.getKeys(false)) {
            ConfigurationSection country = countriesSection.getConfigurationSection(countryId);
            if (country == null) continue;
            String normalizedCountryId = countryId.toLowerCase(Locale.ROOT);
            long now = System.currentTimeMillis();
            DBEarthCountry earthCountry = new DBEarthCountry(normalizedCountryId,
                    country.getString("code", countryId),
                    country.getString("name", countryId),
                    country.getString("capital", "Unknown"),
                    country.getString("currency", "CR"),
                    country.getString("government", "republic"),
                    country.getString("government-id", normalizedCountryId + "-government"),
                    country.getDouble("tax-rate", 0.12D), country.getBoolean("active", true), now);
            registerCountry(earthCountry);
            if (!earthCountry.isActive()) continue;
            registerPolicy(new DBEarthCountryPolicy(normalizedCountryId,
                    country.getDouble("budget.social-services", 0.40D),
                    country.getDouble("budget.infrastructure", 0.35D),
                    country.getDouble("budget.investment", 0.25D), System.currentTimeMillis()));
            ConfigurationSection configuredRegions = country.getConfigurationSection("regions");
            if (configuredRegions == null) continue;
            for (String regionId : configuredRegions.getKeys(false)) {
                ConfigurationSection region = configuredRegions.getConfigurationSection(regionId);
                if (region == null) continue;
                String id = normalizedCountryId + "-" + regionId.toLowerCase(Locale.ROOT);
                registerRegion(new DBEarthRegion(id, normalizedCountryId, region.getString("name", regionId),
                        region.getInt("min-chunk-x", -30_000_000), region.getInt("max-chunk-x", 30_000_000),
                        region.getInt("min-chunk-z", -30_000_000), region.getInt("max-chunk-z", 30_000_000)));
            }
        }
    }

    private void registerCountry(DBEarthCountry country) {
        DBEarthCountry persistedCountry = countries.putIfAbsent(country.getId(), country);
        DBEarthCountry effectiveCountry = persistedCountry == null ? country : persistedCountry;
        if (persistedCountry != null) effectiveCountry.applyConfiguredMetadata(country, System.currentTimeMillis());
        countryStatistics.computeIfAbsent(country.getId(), id -> new DBEarthCountryStatistics(id, System.currentTimeMillis()));
        ensureElectionCycle(country.getId());
        ensureGovernment(effectiveCountry);
        try {
            countriesDao.createOrUpdate(effectiveCountry);
            countryStatisticsDao.createIfNotExists(countryStatistics.get(country.getId()));
        } catch (SQLException exception) {
            throw new IllegalStateException("Unable to persist Earth country " + country.getId(), exception);
        }
    }

    private void ensureElectionCycle(String countryId) {
        String country = countryId.toLowerCase(Locale.ROOT);
        long now = System.currentTimeMillis();
        DBEarthElectionCycle cycle = electionCycles.computeIfAbsent(country,
                id -> new DBEarthElectionCycle(id, EarthElectionPhase.CANDIDACY, now, now + electionCandidacyMillis, 0L));
        try {
            electionCyclesDao.createIfNotExists(cycle);
        } catch (SQLException exception) {
            throw new IllegalStateException("Unable to persist Earth election cycle " + country, exception);
        }
    }

    private void ensureGovernment(DBEarthCountry country) {
        String countryId = country.getId();
        DBEarthGovernment government = governments.computeIfAbsent(countryId, id -> new DBEarthGovernment(id,
                country.getGovernmentId().isBlank() ? id + "-government" : country.getGovernmentId(), country.getGovernmentType(),
                System.currentTimeMillis()));
        government.applyConfiguredIdentity(country.getGovernmentId().isBlank() ? countryId + "-government" : country.getGovernmentId(),
                country.getGovernmentType());
        try {
            governmentsDao.createOrUpdate(government);
        } catch (SQLException exception) {
            throw new IllegalStateException("Unable to persist Earth government " + countryId, exception);
        }
    }

    private void registerRegion(DBEarthRegion region) {
        if (!region.hasValidBounds()) {
            throw new IllegalArgumentException("Earth region " + region.getId() + " has invalid chunk bounds");
        }
        DBEarthRegion existing = regions.get(region.getId());
        if (existing == null) {
            regions.values().stream()
                    .filter(other -> isCountryActive(other.getCountryId()) && region.overlaps(other))
                    .findFirst()
                    .ifPresent(other -> {
                        throw new IllegalArgumentException("Earth region " + region.getId() + " overlaps configured region " + other.getId());
                    });
        }
        regions.putIfAbsent(region.getId(), region);
        regionStates.computeIfAbsent(region.getId(), id -> new DBEarthRegionState(id, 1_000D, 0.50D, 0.55D, 0.55D, System.currentTimeMillis()));
        regionalIndicators.computeIfAbsent(region.getId(), id -> new DBEarthRegionalIndicators(id, System.currentTimeMillis()));
        regionalLedgers.computeIfAbsent(region.getId(), DBEarthRegionalLedger::new);
        demographics.computeIfAbsent(region.getId(), id -> new DBEarthDemographics(id, 50, 30, 0.5D, System.currentTimeMillis()));
        try {
            regionsDao.createIfNotExists(region);
            regionStatesDao.createIfNotExists(regionStates.get(region.getId()));
            regionalIndicatorsDao.createIfNotExists(regionalIndicators.get(region.getId()));
            regionalLedgersDao.createIfNotExists(regionalLedgers.get(region.getId()));
            demographicsDao.createIfNotExists(demographics.get(region.getId()));
        } catch (SQLException exception) {
            throw new IllegalStateException("Unable to persist Earth region " + region.getId(), exception);
        }
    }

    private void registerPolicy(DBEarthCountryPolicy policy) {
        countryPolicies.putIfAbsent(policy.getCountryId(), policy);
        try {
            countryPoliciesDao.createIfNotExists(policy);
        } catch (SQLException exception) {
            throw new IllegalStateException("Unable to persist Earth country policy " + policy.getCountryId(), exception);
        }
    }

    public Optional<DBEarthCountry> getCountry(String countryId) {
        return Optional.ofNullable(countries.get(countryId.toLowerCase(Locale.ROOT)));
    }

    private boolean isCountryActive(String countryId) {
        DBEarthCountry country = countries.get(countryId.toLowerCase(Locale.ROOT));
        return country != null && country.isActive();
    }

    public Optional<DBEarthCountryStatistics> getCountryStatistics(String countryId) {
        return Optional.ofNullable(countryStatistics.get(countryId.toLowerCase(Locale.ROOT)));
    }

    public Optional<DBEarthCountryPolicy> getCountryPolicy(String countryId) {
        return Optional.ofNullable(countryPolicies.get(countryId.toLowerCase(Locale.ROOT)));
    }

    public boolean configureCountryPolicy(String countryId, double socialServices, double infrastructure, double investment) {
        String normalizedId = countryId.toLowerCase(Locale.ROOT);
        if (!isCountryActive(normalizedId) || socialServices < 0D || infrastructure < 0D || investment < 0D
                || socialServices + infrastructure + investment <= 0D) return false;
        DBEarthCountryPolicy policy = countryPolicies.computeIfAbsent(normalizedId,
                id -> new DBEarthCountryPolicy(id, socialServices, infrastructure, investment, System.currentTimeMillis()));
        policy.setAllocations(socialServices, infrastructure, investment, System.currentTimeMillis());
        persistAsync(() -> countryPoliciesDao.createOrUpdate(policy));
        return true;
    }

    /**
     * A state-aware one-cycle estimate for a proposed public budget. It is
     * intentionally an estimate: current regional conditions influence every
     * returned effect and the real simulation still applies events and trade.
     */
    public Optional<PolicyImpactEstimate> previewCountryPolicy(String countryId, double socialServices, double infrastructure, double investment) {
        String country = countryId.toLowerCase(Locale.ROOT);
        if (!isCountryActive(country) || socialServices < 0D || infrastructure < 0D || investment < 0D
                || socialServices + infrastructure + investment <= 0D) return Optional.empty();
        double total = socialServices + infrastructure + investment;
        double social = socialServices / total;
        double infra = infrastructure / total;
        double invest = investment / total;
        DBEarthCountryPolicy current = countryPolicies.getOrDefault(country, new DBEarthCountryPolicy(country, .40D, .35D, .25D, 0L));
        double socialDelta = social - current.getSocialServices();
        double infraDelta = infra - current.getInfrastructure();
        double investmentDelta = invest - current.getInvestment();
        List<DBEarthRegion> countryRegions = regions.values().stream().filter(region -> region.getCountryId().equals(country)).toList();
        double pollution = countryRegions.stream().map(region -> regionalIndicators.get(region.getId())).filter(Objects::nonNull)
                .mapToDouble(DBEarthRegionalIndicators::getPollution).average().orElse(.20D);
        double crime = countryRegions.stream().map(region -> regionalIndicators.get(region.getId())).filter(Objects::nonNull)
                .mapToDouble(DBEarthRegionalIndicators::getCrime).average().orElse(.25D);
        return Optional.of(new PolicyImpactEstimate(
                socialDelta * .14D - pollution * .015D,
                infraDelta * .18D,
                investmentDelta * .16D + infraDelta * .05D,
                -infraDelta * .11D - investmentDelta * .04D,
                socialDelta * .10D + infraDelta * .04D - crime * .008D,
                -(Math.abs(socialDelta) + Math.abs(infraDelta) + Math.abs(investmentDelta)) * .006D));
    }

    public Set<EarthLawType> getActiveLaws(String countryId) {
        return Set.copyOf(lawsByCountry.getOrDefault(countryId.toLowerCase(Locale.ROOT), Set.of()));
    }

    public boolean enactLaw(String countryId, EarthLawType lawType, UUID enactedBy) {
        String normalizedId = countryId.toLowerCase(Locale.ROOT);
        if (!isCountryActive(normalizedId)) return false;
        Set<EarthLawType> laws = lawsByCountry.computeIfAbsent(normalizedId, ignored -> ConcurrentHashMap.newKeySet());
        if (!laws.add(lawType)) return false;
        DBEarthLaw law = new DBEarthLaw(normalizedId, lawType, enactedBy, System.currentTimeMillis());
        lawRecords.put(law.getId(), law);
        persistAsync(() -> lawsDao.createOrUpdate(law));
        return true;
    }

    public boolean repealLaw(String countryId, EarthLawType lawType) {
        String normalizedId = countryId.toLowerCase(Locale.ROOT);
        Set<EarthLawType> laws = lawsByCountry.get(normalizedId);
        if (laws == null || !laws.remove(lawType)) return false;
        String id = DBEarthLaw.idFor(normalizedId, lawType);
        lawRecords.remove(id);
        persistAsync(() -> lawsDao.deleteById(id));
        return true;
    }

    public Optional<DBEarthRegion> getRegion(String regionId) {
        return Optional.ofNullable(regions.get(regionId.toLowerCase(Locale.ROOT)));
    }

    public Optional<DBEarthRegionalIndicators> getRegionalIndicators(String regionId) {
        return Optional.ofNullable(regionalIndicators.get(regionId.toLowerCase(Locale.ROOT)));
    }

    public Optional<DBEarthRegion> findRegion(int chunkX, int chunkZ) {
        return regions.values().stream().filter(region -> isCountryActive(region.getCountryId())
                && region.containsChunk(chunkX, chunkZ)).findFirst();
    }

    public Optional<DBEarthCitizenship> getCitizenship(UUID playerId) {
        return Optional.ofNullable(citizenships.get(playerId));
    }

    public void grantDefaultCitizenship(UUID playerId) {
        if (!citizenships.containsKey(playerId)) {
            String countryId = isCountryActive(defaultCountryId) ? defaultCountryId
                    : countries.values().stream().filter(DBEarthCountry::isActive).map(DBEarthCountry::getId).findFirst().orElse(null);
            if (countryId == null) return;
            DBEarthCitizenship citizenship = new DBEarthCitizenship(playerId, countryId, null, System.currentTimeMillis());
            citizenships.put(playerId, citizenship);
            persistAsync(() -> citizenshipsDao.createOrUpdate(citizenship));
        }
        ensureCitizenStatus(playerId);
    }

    public boolean chooseCitizenship(UUID playerId, String countryId) {
        String normalizedId = countryId.toLowerCase(Locale.ROOT);
        if (!isCountryActive(normalizedId)) return false;
        DBEarthCitizenship citizenship = new DBEarthCitizenship(playerId, normalizedId, null, System.currentTimeMillis());
        citizenships.put(playerId, citizenship);
        ensureCitizenStatus(playerId);
        persistAsync(() -> citizenshipsDao.createOrUpdate(citizenship));
        return true;
    }

    public Optional<DBEarthRegionState> getRegionState(String regionId) {
        return Optional.ofNullable(regionStates.get(regionId));
    }

    /** Links an existing OpenMC city to one Earth region without altering its claims or members. */
    public boolean linkCityToRegion(UUID cityId, String regionId) {
        String normalizedId = regionId.toLowerCase(Locale.ROOT);
        if (!regions.containsKey(normalizedId)) return false;
        DBEarthCityLink link = new DBEarthCityLink(cityId, normalizedId);
        cityLinks.put(cityId, link);
        persistAsync(() -> cityLinksDao.createOrUpdate(link));
        return true;
    }

    public Optional<DBEarthRegion> getLinkedCityRegion(UUID cityId) {
        DBEarthCityLink link = cityLinks.get(cityId);
        return link == null ? Optional.empty() : getRegion(link.getRegionId());
    }

    /**
     * Converts a completed OpenMC shop sale into an Earth commercial property,
     * business record and pending public tax. No player balance is charged here:
     * the existing shop transaction remains the monetary authority.
     */
    public void recordShopSale(Shop shop, double saleValue) {
        if (shop == null || saleValue <= 0D) return;
        findRegion(Math.floorDiv(shop.getX(), 16), Math.floorDiv(shop.getZ(), 16)).ifPresent(region -> {
            long now = System.currentTimeMillis();
            UUID shopId = shop.getShopUUID();
            City city = CityManager.getCityFromChunk(Math.floorDiv(shop.getX(), 16), Math.floorDiv(shop.getZ(), 16));
            DBEarthProperty property = properties.computeIfAbsent(shopId, id -> new DBEarthProperty(
                    id, shop.getOwnerUUID(), city == null ? null : city.getUniqueId(), region.getId(), "world", Math.floorDiv(shop.getX(), 16),
                    Math.floorDiv(shop.getZ(), 16), EarthPropertyType.COMMERCIAL, Math.max(100D, saleValue * 20D), now));
            DBEarthPropertyAccess access = ensurePropertyAccess(property, now);
            DBEarthBusiness business = businesses.computeIfAbsent(shopId, id -> new DBEarthBusiness(
                    id, shop.getOwnerUUID(), property.getPropertyId(), region.getId(), "retail", now));
            DBEarthCountry country = countries.get(region.getCountryId());
            double tax = saleValue * (country == null ? 0D : country.getTaxRate() * taxMultiplier(country.getId()));
            business.recordSale(saleValue, tax, now);
            regionalLedgers.computeIfAbsent(region.getId(), DBEarthRegionalLedger::new).recordSale(saleValue, tax);
            persistAsync(() -> {
                propertiesDao.createOrUpdate(property);
                propertyAccessDao.createOrUpdate(access);
                businessesDao.createOrUpdate(business);
                regionalLedgersDao.createOrUpdate(regionalLedgers.get(region.getId()));
            });
        });
    }

    public Optional<DBEarthBusiness> getBusiness(UUID shopId) {
        return Optional.ofNullable(businesses.get(shopId));
    }

    public Optional<DBEarthProperty> getProperty(UUID propertyId) {
        return Optional.ofNullable(properties.get(propertyId));
    }

    public Optional<DBEarthPropertyAccess> getPropertyAccess(UUID propertyId) {
        return Optional.ofNullable(propertyAccess.get(propertyId));
    }

    public List<DBEarthProperty> getOwnedProperties(UUID ownerId) {
        return properties.values().stream().filter(property -> property.getOwnerId().equals(ownerId)).toList();
    }

    public boolean configurePropertyAccess(UUID ownerId, UUID propertyId, EarthPropertyAccessMode mode) {
        DBEarthProperty property = properties.get(propertyId);
        if (property == null || !property.getOwnerId().equals(ownerId)) return false;
        long now = System.currentTimeMillis();
        DBEarthPropertyAccess access = ensurePropertyAccess(property, now);
        access.setAccessMode(mode, now);
        persistAsync(() -> propertyAccessDao.createOrUpdate(access));
        return true;
    }

    public boolean offerPropertyLease(UUID ownerId, UUID propertyId, double leasePrice) {
        DBEarthProperty property = properties.get(propertyId);
        if (property == null || !property.getOwnerId().equals(ownerId) || leasePrice < 0D) return false;
        long now = System.currentTimeMillis();
        DBEarthPropertyAccess access = ensurePropertyAccess(property, now);
        access.setLeaseOffer(leasePrice, now);
        persistAsync(() -> propertyAccessDao.createOrUpdate(access));
        return true;
    }

    /** Charges a one-time configured rent and records a restart-safe lease; claims are never transferred. */
    public boolean rentProperty(UUID tenantId, UUID propertyId, long durationMinutes) {
        DBEarthProperty property = properties.get(propertyId);
        if (property == null || property.getOwnerId().equals(tenantId) || durationMinutes < 1L || durationMinutes > 43_200L) return false;
        long now = System.currentTimeMillis();
        DBEarthPropertyAccess access = ensurePropertyAccess(property, now);
        if (access.hasActiveLease(now) || !EconomyManager.withdrawBalance(tenantId, access.getLeasePrice(), "Earth property lease")) return false;
        EconomyManager.addBalance(property.getOwnerId(), access.getLeasePrice(), "Earth property lease");
        access.startLease(tenantId, now + durationMinutes * 60_000L, now);
        persistAsync(() -> propertyAccessDao.createOrUpdate(access));
        return true;
    }

    public Optional<DBEarthPropertySaleOffer> getPropertySaleOffer(UUID propertyId) {
        DBEarthPropertySaleOffer offer = propertySaleOffers.get(propertyId);
        if (offer == null || !offer.isActive(System.currentTimeMillis())) return Optional.empty();
        return Optional.of(offer);
    }

    /** Seller initiates a time-bounded public offer; acceptance is a separate buyer action. */
    public Optional<DBEarthPropertySaleOffer> offerPropertySale(UUID sellerId, UUID propertyId, double price, long durationMinutes) {
        DBEarthProperty property = properties.get(propertyId);
        if (property == null || !property.getOwnerId().equals(sellerId) || price < 0D
                || durationMinutes < 1L || durationMinutes > 43_200L) return Optional.empty();
        long now = System.currentTimeMillis();
        DBEarthPropertySaleOffer offer = new DBEarthPropertySaleOffer(propertyId, sellerId, price, now,
                now + durationMinutes * 60_000L);
        propertySaleOffers.put(propertyId, offer);
        persistAsync(() -> propertySaleOffersDao.createOrUpdate(offer));
        return Optional.of(offer);
    }

    /** Sellers may withdraw their own unaccepted offer at any time. */
    public boolean withdrawPropertySale(UUID sellerId, UUID propertyId) {
        DBEarthPropertySaleOffer offer = propertySaleOffers.get(propertyId);
        if (offer == null || !sellerId.equals(offer.getSellerId()) || !propertySaleOffers.remove(propertyId, offer)) return false;
        persistAsync(() -> propertySaleOffersDao.delete(offer));
        return true;
    }

    /**
     * Settles a seller-authored offer only after a buyer invokes it. Existing
     * Home/Shop owner records are updated through their authoritative managers
     * before money moves; a payment failure compensates the owner transition.
     */
    public synchronized boolean acceptPropertySale(UUID buyerId, UUID propertyId) {
        DBEarthPropertySaleOffer offer = propertySaleOffers.get(propertyId);
        DBEarthProperty property = properties.get(propertyId);
        long now = System.currentTimeMillis();
        if (offer == null || property == null || !offer.isActive(now) || buyerId.equals(offer.getSellerId())
                || !property.getOwnerId().equals(offer.getSellerId()) || EconomyManager.getBalance(buyerId) < offer.getPrice()) {
            discardExpiredPropertySaleOffer(propertyId, offer, now);
            return false;
        }
        if (!transferAuthoritativeProperty(property, offer.getSellerId(), buyerId)) return false;
        if (!EconomyManager.transferBalance(buyerId, offer.getSellerId(), offer.getPrice(), "Earth property sale")) {
            transferAuthoritativeProperty(property, buyerId, offer.getSellerId());
            return false;
        }
        property.transferTo(buyerId, offer.getPrice());
        DBEarthPropertyAccess access = ensurePropertyAccess(property, now);
        access.clearLease(now);
        propertySaleOffers.remove(propertyId, offer);
        persistAsync(() -> {
            propertiesDao.createOrUpdate(property);
            propertyAccessDao.createOrUpdate(access);
            propertySaleOffersDao.delete(offer);
        });
        return true;
    }

    private boolean transferAuthoritativeProperty(DBEarthProperty property, UUID sellerId, UUID buyerId) {
        return switch (property.getType()) {
            case RESIDENTIAL -> HomesManager.homes.stream().filter(home -> home.getUniqueId().equals(property.getPropertyId()))
                    .findFirst().map(home -> HomesManager.transferOwnership(home, sellerId, buyerId)).orElse(false);
            case COMMERCIAL -> Optional.ofNullable(ShopManager.getShopByUUID(property.getPropertyId()))
                    .map(shop -> ShopManager.transferOwnership(shop, sellerId, buyerId)).orElse(false);
            default -> true; // Earth-owned civic/land records have no parent authority to coordinate.
        };
    }

    private void discardExpiredPropertySaleOffer(UUID propertyId, DBEarthPropertySaleOffer offer, long now) {
        if (offer == null || offer.isActive(now)) return;
        propertySaleOffers.remove(propertyId, offer);
        persistAsync(() -> propertySaleOffersDao.delete(offer));
    }

    private DBEarthPropertyAccess ensurePropertyAccess(DBEarthProperty property, long now) {
        return propertyAccess.computeIfAbsent(property.getPropertyId(), id -> new DBEarthPropertyAccess(id,
                switch (property.getType()) {
                    case COMMERCIAL -> EarthPropertyAccessMode.BUSINESS;
                    case CIVIC -> EarthPropertyAccessMode.GOVERNMENT;
                    default -> EarthPropertyAccessMode.PRIVATE;
                }, now));
    }

    /** Registers an existing OpenMC home as a residential Earth property without changing home behavior. */
    public void registerResidentialHome(Home home) {
        if (home == null || home.getLocation() == null || home.getLocation().getWorld() == null) return;
        int chunkX = Math.floorDiv(home.getLocation().getBlockX(), 16);
        int chunkZ = Math.floorDiv(home.getLocation().getBlockZ(), 16);
        findRegion(chunkX, chunkZ).ifPresent(region -> {
            City city = CityManager.getCityFromChunk(chunkX, chunkZ);
            DBEarthProperty property = properties.computeIfAbsent(home.getUniqueId(), id -> new DBEarthProperty(
                    id, home.getOwner(), city == null ? null : city.getUniqueId(), region.getId(), home.getLocation().getWorld().getName(),
                    chunkX, chunkZ, EarthPropertyType.RESIDENTIAL, 250D, System.currentTimeMillis()));
            DBEarthPropertyAccess access = ensurePropertyAccess(property, System.currentTimeMillis());
            persistAsync(() -> { propertiesDao.createOrUpdate(property); propertyAccessDao.createOrUpdate(access); });
        });
    }

    public Optional<DBEarthRegionalLedger> getRegionalLedger(String regionId) {
        return Optional.ofNullable(regionalLedgers.get(regionId.toLowerCase(Locale.ROOT)));
    }

    public Optional<DBEarthCitizenStatus> getCitizenStatus(UUID playerId) {
        return Optional.ofNullable(citizenStatuses.get(playerId));
    }

    /** Volunteer work is local civic participation, not a money faucet. */
    public boolean volunteer(UUID playerId, String regionId) {
        DBEarthCitizenStatus status = ensureCitizenStatus(playerId);
        if (!regions.containsKey(regionId.toLowerCase(Locale.ROOT))) return false;
        long now = System.currentTimeMillis();
        if (!status.canVolunteer(now, civicCooldownMillis)) return false;
        status.recordVolunteer(now);
        DBEarthRegionState state = regionStates.get(regionId.toLowerCase(Locale.ROOT));
        if (state != null) state.applyCivicContribution();
        persistAsync(() -> {
            citizenStatusesDao.createOrUpdate(status);
            if (state != null) regionStatesDao.createOrUpdate(state);
        });
        return true;
    }

    public Optional<DBEarthCountryOffice> getOffice(String countryId) { return Optional.ofNullable(offices.get(countryId.toLowerCase(Locale.ROOT))); }

    public Optional<DBEarthGovernment> getGovernment(String countryId) {
        return Optional.ofNullable(governments.get(countryId.toLowerCase(Locale.ROOT)));
    }

    /** Returns the current cabinet, indexed by portfolio, for a country. */
    public Map<EarthGovernmentPortfolio, DBEarthGovernmentAppointment> getGovernmentAppointments(String countryId) {
        return Map.copyOf(appointmentsByCountry.getOrDefault(countryId.toLowerCase(Locale.ROOT), Map.of()));
    }

    /**
     * Replaces one country portfolio. The elected leader appoints only citizens
     * of that same active country; no command-level assumption is relied on.
     */
    public Optional<DBEarthGovernmentAppointment> appointMinister(UUID leaderId, String countryId,
                                                                    EarthGovernmentPortfolio portfolio, UUID ministerId) {
        String country = countryId.toLowerCase(Locale.ROOT);
        if (!isCountryActive(country) || !isGovernmentLeader(leaderId, country)
                || !isCitizenOf(ministerId, country)) return Optional.empty();
        DBEarthGovernmentAppointment appointment = new DBEarthGovernmentAppointment(country, portfolio, ministerId,
                leaderId, System.currentTimeMillis());
        appointmentsByCountry.computeIfAbsent(country, ignored -> new ConcurrentHashMap<>()).put(portfolio, appointment);
        persistAsync(() -> governmentAppointmentsDao.createOrUpdate(appointment));
        return Optional.of(appointment);
    }

    private Set<EarthGovernmentPortfolio> activePortfolios(String countryId) {
        return Set.copyOf(appointmentsByCountry.getOrDefault(countryId, Map.of()).keySet());
    }

    private void clearGovernmentAppointments(String countryId) {
        Map<EarthGovernmentPortfolio, DBEarthGovernmentAppointment> appointments = appointmentsByCountry.remove(countryId.toLowerCase(Locale.ROOT));
        if (appointments == null || appointments.isEmpty()) return;
        Collection<DBEarthGovernmentAppointment> removed = new ArrayList<>(appointments.values());
        persistAsync(() -> {
            for (DBEarthGovernmentAppointment appointment : removed) governmentAppointmentsDao.delete(appointment);
        });
    }

    /** True only for the currently installed country leader; command code handles the administrator bypass. */
    public boolean isGovernmentLeader(UUID playerId, String countryId) {
        return getGovernment(countryId).map(government -> playerId.equals(government.getLeaderId())).orElse(false);
    }

    public Optional<DBEarthPoliticalParty> getPoliticalParty(String partyId) {
        return Optional.ofNullable(politicalParties.get(partyId));
    }

    public Optional<DBEarthPartyMembership> getPartyMembership(UUID playerId) {
        return Optional.ofNullable(partyMemberships.get(playerId));
    }

    public Optional<DBEarthPoliticalParty> createPoliticalParty(UUID founderId, String countryId, String name, String platform) {
        String country = countryId.toLowerCase(Locale.ROOT);
        if (!isCountryActive(country) || !isCitizenOf(founderId, country) || name == null || name.isBlank() || name.length() > 40
                || platform == null || platform.isBlank() || platform.length() > 160) return Optional.empty();
        DBEarthPoliticalParty party = new DBEarthPoliticalParty(country, name.trim(), platform.trim(), founderId, System.currentTimeMillis());
        if (party.getId().endsWith(":")) return Optional.empty();
        if (politicalParties.putIfAbsent(party.getId(), party) != null) return Optional.empty();
        DBEarthPartyMembership membership = new DBEarthPartyMembership(founderId, party.getId(), System.currentTimeMillis());
        partyMemberships.put(founderId, membership);
        persistAsync(() -> { politicalPartiesDao.createOrUpdate(party); partyMembershipsDao.createOrUpdate(membership); });
        return Optional.of(party);
    }

    public boolean joinPoliticalParty(UUID playerId, String partyId) {
        DBEarthPoliticalParty party = politicalParties.get(partyId);
        if (party == null || !party.isActive() || !isCitizenOf(playerId, party.getCountryId())) return false;
        DBEarthPartyMembership membership = new DBEarthPartyMembership(playerId, partyId, System.currentTimeMillis());
        partyMemberships.put(playerId, membership);
        persistAsync(() -> partyMembershipsDao.createOrUpdate(membership));
        return true;
    }

    public List<DBEarthBill> getOpenBills(String countryId) {
        String country = countryId.toLowerCase(Locale.ROOT);
        return bills.values().stream().filter(bill -> bill.getCountryId().equals(country) && bill.isOpen()).toList();
    }

    public Optional<DBEarthBill> proposeBill(UUID authorId, String countryId, EarthLawType lawType) {
        String country = countryId.toLowerCase(Locale.ROOT);
        if (!isCountryActive(country) || getActiveLaws(country).contains(lawType)
                || getOpenBills(country).stream().anyMatch(bill -> bill.getLawType() == lawType)) return Optional.empty();
        DBEarthBill bill = new DBEarthBill(country, lawType, authorId, System.currentTimeMillis());
        bills.put(bill.getBillId(), bill);
        persistAsync(() -> billsDao.createOrUpdate(bill));
        return Optional.of(bill);
    }

    public boolean passBill(UUID billId) {
        DBEarthBill bill = bills.get(billId);
        if (bill == null || !bill.isOpen() || !enactLaw(bill.getCountryId(), bill.getLawType(), bill.getAuthorId())) return false;
        bill.resolve(EarthBillStatus.PASSED, System.currentTimeMillis());
        persistAsync(() -> billsDao.createOrUpdate(bill));
        return true;
    }

    public Optional<DBEarthElectionCycle> getElectionCycle(String countryId) {
        return Optional.ofNullable(electionCycles.get(countryId.toLowerCase(Locale.ROOT)));
    }

    public Set<UUID> getElectionCandidates(String countryId) { return Set.copyOf(candidatesByCountry.getOrDefault(countryId.toLowerCase(Locale.ROOT), Map.of()).keySet()); }

    public Optional<DBEarthElectionCandidate> getElectionCandidate(String countryId, UUID candidateId) {
        return Optional.ofNullable(candidatesByCountry.getOrDefault(countryId.toLowerCase(Locale.ROOT), Map.of()).get(candidateId));
    }

    public boolean nominate(UUID playerId, String countryId) {
        String country = countryId.toLowerCase(Locale.ROOT);
        if (!isCitizenOf(playerId, country) || ensureCitizenStatus(playerId).getReputation() < 25
                || getElectionCycle(country).map(DBEarthElectionCycle::getPhaseType).orElse(EarthElectionPhase.CANDIDACY) != EarthElectionPhase.CANDIDACY) return false;
        Map<UUID, DBEarthElectionCandidate> candidates = candidatesByCountry.computeIfAbsent(country, ignored -> new ConcurrentHashMap<>());
        if (candidates.containsKey(playerId)) return false;
        String partyId = getPartyMembership(playerId).map(DBEarthPartyMembership::getPartyId)
                .filter(id -> getPoliticalParty(id).map(party -> party.getCountryId().equals(country) && party.isActive()).orElse(false)).orElse(null);
        DBEarthElectionCandidate candidate = new DBEarthElectionCandidate(country, playerId, partyId, System.currentTimeMillis());
        candidates.put(playerId, candidate);
        persistAsync(() -> candidatesDao.createOrUpdate(candidate));
        return true;
    }

    public boolean vote(UUID voterId, String countryId, UUID candidateId) {
        String country = countryId.toLowerCase(Locale.ROOT);
        if (!isCitizenOf(voterId, country) || !candidatesByCountry.getOrDefault(country, Map.of()).containsKey(candidateId)
                || getElectionCycle(country).map(DBEarthElectionCycle::getPhaseType).orElse(EarthElectionPhase.CANDIDACY) != EarthElectionPhase.VOTING) return false;
        DBEarthElectionVote vote = new DBEarthElectionVote(country, voterId, candidateId, System.currentTimeMillis());
        votesByCountry.computeIfAbsent(country, ignored -> new ConcurrentHashMap<>()).put(voterId, vote);
        DBEarthCitizenStatus status = ensureCitizenStatus(voterId); status.recordVote(vote.getCastAt());
        persistAsync(() -> { votesDao.createOrUpdate(vote); citizenStatusesDao.createOrUpdate(status); });
        return true;
    }

    /** Closes one country ballot. Ties resolve by UUID order, making the outcome deterministic. */
    public Optional<DBEarthCountryOffice> closeElection(String countryId) {
        String country = countryId.toLowerCase(Locale.ROOT);
        if (!isCountryActive(country)) return Optional.empty();
        Map<UUID, DBEarthElectionVote> votes = votesByCountry.getOrDefault(country, Map.of());
        Map<UUID, Long> totals = new HashMap<>();
        votes.values().forEach(vote -> totals.merge(vote.getCandidateId(), 1L, Long::sum));
        UUID winner = totals.entrySet().stream().max(Comparator.<Map.Entry<UUID, Long>>comparingLong(Map.Entry::getValue)
                .thenComparing(entry -> entry.getKey().toString(), Comparator.reverseOrder())).map(Map.Entry::getKey).orElse(null);
        if (winner == null) return Optional.empty();
        long now = System.currentTimeMillis();
        DBEarthCountryOffice office = new DBEarthCountryOffice(country, winner, now, totals.get(winner));
        DBEarthElectionCandidate winningCandidate = candidatesByCountry.getOrDefault(country, Map.of()).get(winner);
        DBEarthGovernment government = governments.get(country);
        clearGovernmentAppointments(country);
        if (government != null) government.installLeader(winner, winningCandidate == null ? null : winningCandidate.getPartyId(), now + electionMandateMillis, now);
        offices.put(country, office);
        Map<UUID, DBEarthElectionCandidate> removedCandidates = candidatesByCountry.remove(country);
        Map<UUID, DBEarthElectionVote> removedVotes = votesByCountry.remove(country);
        Collection<DBEarthElectionCandidate> candidates = removedCandidates == null ? List.of() : new ArrayList<>(removedCandidates.values());
        Collection<DBEarthElectionVote> previousVotes = removedVotes == null ? List.of() : new ArrayList<>(removedVotes.values());
        persistAsync(() -> {
            officesDao.createOrUpdate(office);
            if (government != null) governmentsDao.createOrUpdate(government);
            for (DBEarthElectionCandidate candidate : candidates) candidatesDao.delete(candidate);
            for (DBEarthElectionVote vote : previousVotes) votesDao.delete(vote);
        });
        return Optional.of(office);
    }

    /** Advances persisted wall-clock election phases; missed server time never freezes a mandate. */
    private void advanceElectionCycles(long now) {
        for (DBEarthElectionCycle cycle : electionCycles.values()) {
            if (!isCountryActive(cycle.getCountryId())) continue;
            int transitions = 0;
            while (cycle.phaseHasEnded(now) && transitions++ < 4) {
                switch (cycle.getPhaseType()) {
                    case CANDIDACY -> cycle.moveTo(EarthElectionPhase.CAMPAIGN, now, now + electionCampaignMillis, 0L);
                    case CAMPAIGN -> cycle.moveTo(EarthElectionPhase.VOTING, now, now + electionVotingMillis, 0L);
                    case VOTING -> {
                        if (closeElection(cycle.getCountryId()).isEmpty()) vacateGovernment(cycle.getCountryId(), now);
                        cycle.moveTo(EarthElectionPhase.MANDATE, now, now + electionMandateMillis, now + electionMandateMillis);
                    }
                    case MANDATE -> cycle.moveTo(EarthElectionPhase.CANDIDACY, now, now + electionCandidacyMillis, 0L);
                }
            }
            if (transitions > 0) persistAsync(() -> electionCyclesDao.createOrUpdate(cycle));
        }
    }

    /** A vote-less election produces a vacant government instead of extending a former leader's authority. */
    private void vacateGovernment(String countryId, long now) {
        String country = countryId.toLowerCase(Locale.ROOT);
        DBEarthGovernment government = governments.get(country);
        if (government != null) government.vacateLeadership(now);
        clearGovernmentAppointments(country);
        DBEarthCountryOffice office = offices.remove(country);
        persistAsync(() -> {
            if (government != null) governmentsDao.createOrUpdate(government);
            if (office != null) officesDao.delete(office);
        });
    }

    public List<DBEarthRegionalEvent> getActiveEvents(String regionId) {
        long now = System.currentTimeMillis();
        return regionalEvents.values().stream().filter(event -> event.getRegionId().equalsIgnoreCase(regionId) && event.isActive(now)).toList();
    }

    public Optional<DBEarthRegionalEvent> startRegionalEvent(String regionId, EarthRegionalEventType type, long durationMinutes, UUID createdBy) {
        String region = regionId.toLowerCase(Locale.ROOT);
        if (!regions.containsKey(region) || durationMinutes < 1L || durationMinutes > 1_440L) return Optional.empty();
        long now = System.currentTimeMillis();
        DBEarthRegionalEvent event = new DBEarthRegionalEvent(region, type, now, now + durationMinutes * 60_000L, createdBy);
        regionalEvents.put(event.getEventId(), event);
        persistAsync(() -> regionalEventsDao.createOrUpdate(event));
        return Optional.of(event);
    }

    public List<DBEarthTransportRoute> getRoutesFrom(String regionId) {
        return transportRoutes.values().stream().filter(route -> route.getFromRegion().equalsIgnoreCase(regionId)).toList();
    }

    public Optional<DBEarthTransportRoute> createRoute(String fromRegion, String toRegion, EarthTransportMode mode, double fare, long passMinutes) {
        String from = fromRegion.toLowerCase(Locale.ROOT), to = toRegion.toLowerCase(Locale.ROOT);
        if (!regions.containsKey(from) || !regions.containsKey(to) || from.equals(to) || fare < 0D || passMinutes < 1L || passMinutes > 1_440L) return Optional.empty();
        DBEarthTransportRoute route = new DBEarthTransportRoute(from, to, mode, fare, passMinutes);
        transportRoutes.put(route.getRouteId(), route);
        persistAsync(() -> transportRoutesDao.createOrUpdate(route));
        return Optional.of(route);
    }

    public Optional<DBEarthTravelPass> travel(UUID playerId, String physicalRegionId, UUID routeId) {
        DBEarthTransportRoute route = transportRoutes.get(routeId);
        if (route == null || !route.getFromRegion().equalsIgnoreCase(physicalRegionId)) return Optional.empty();
        DBEarthRegionalIndicators originConditions = regionalIndicators.get(route.getFromRegion());
        double fare = route.getFare() * (originConditions == null ? 1D : originConditions.transportCostMultiplier());
        if (!EconomyManager.withdrawBalance(playerId, fare, "Earth travel: " + route.getMode())) return Optional.empty();
        DBEarthTravelPass pass = new DBEarthTravelPass(playerId, routeId, route.getToRegion(), System.currentTimeMillis() + route.getPassMinutes() * 60_000L);
        travelPasses.put(playerId, pass);
        persistAsync(() -> travelPassesDao.createOrUpdate(pass));
        return Optional.of(pass);
    }

    public String resolveTravelRegion(UUID playerId, String physicalRegionId) {
        DBEarthTravelPass pass = travelPasses.get(playerId);
        if (pass != null && pass.isActive(System.currentTimeMillis())) return pass.getDestinationRegion();
        return physicalRegionId;
    }

    public Optional<DBEarthDemographics> getDemographics(String regionId) { return Optional.ofNullable(demographics.get(regionId.toLowerCase(Locale.ROOT))); }

    public boolean configureDemographics(String regionId, int residents, int workforce) {
        String region = regionId.toLowerCase(Locale.ROOT);
        if (!regions.containsKey(region) || residents < 0 || workforce < 0 || workforce > residents) return false;
        DBEarthDemographics demographic = new DBEarthDemographics(region, residents, workforce,
                demographics.getOrDefault(region, new DBEarthDemographics(region, 0, 0, 0D, 0L)).getHousingPressure(), System.currentTimeMillis());
        demographics.put(region, demographic);
        persistAsync(() -> demographicsDao.createOrUpdate(demographic));
        return true;
    }

    public Optional<Location> getWaypoint(String regionId) {
        DBEarthRegionWaypoint waypoint = waypoints.get(regionId.toLowerCase(Locale.ROOT));
        if (waypoint == null || waypoint.toLocation().getWorld() == null) return Optional.empty();
        return Optional.of(waypoint.toLocation());
    }

    public boolean setWaypoint(String regionId, Location location) {
        String region = regionId.toLowerCase(Locale.ROOT);
        if (!regions.containsKey(region) || location == null || location.getWorld() == null) return false;
        DBEarthRegionWaypoint waypoint = new DBEarthRegionWaypoint(region, location);
        waypoints.put(region, waypoint);
        persistAsync(() -> waypointsDao.createOrUpdate(waypoint));
        return true;
    }

    public List<DBEarthDiplomaticRelation> getRelations(String countryId) {
        return diplomaticRelations.values().stream().filter(relation -> relation.getCountryA().equalsIgnoreCase(countryId) || relation.getCountryB().equalsIgnoreCase(countryId)).toList();
    }

    public boolean setDiplomaticRelation(String countryA, String countryB, EarthDiplomaticStatus status) {
        String a=countryA.toLowerCase(Locale.ROOT), b=countryB.toLowerCase(Locale.ROOT);
        if (a.equals(b) || !isCountryActive(a) || !isCountryActive(b)) return false;
        String id=DBEarthDiplomaticRelation.idFor(a,b);
        DBEarthDiplomaticRelation relation=diplomaticRelations.computeIfAbsent(id, ignored -> new DBEarthDiplomaticRelation(a,b,status,System.currentTimeMillis()));
        relation.setStatus(status,System.currentTimeMillis());
        persistAsync(() -> diplomaticRelationsDao.createOrUpdate(relation));
        return true;
    }

    public Optional<DBEarthJob> getJob(UUID playerId) {
        return Optional.ofNullable(jobs.get(playerId));
    }

    /** Takes or changes a job in the player's current Earth region. Retail jobs must target an Earth business in that region. */
    public boolean takeJob(UUID playerId, EarthOccupation occupation, String regionId, UUID employerShopId) {
        String normalizedRegion = regionId.toLowerCase(Locale.ROOT);
        if (!regions.containsKey(normalizedRegion)) return false;
        if (occupation == EarthOccupation.RETAIL) {
            DBEarthBusiness business = businesses.get(employerShopId);
            if (business == null || !business.getRegionId().equals(normalizedRegion)) return false;
        } else if (employerShopId != null) return false;
        DBEarthJob job = new DBEarthJob(playerId, occupation, normalizedRegion, employerShopId, System.currentTimeMillis());
        jobs.put(playerId, job);
        persistAsync(() -> jobsDao.createOrUpdate(job));
        return true;
    }

    /** Returns a paid and taxed work action when the job is valid, local, and off cooldown. */
    public Optional<WorkResult> recordWork(UUID playerId, String currentRegionId) {
        DBEarthJob job = jobs.get(playerId);
        if (job == null || !job.getRegionId().equalsIgnoreCase(currentRegionId)) return Optional.empty();
        long now = System.currentTimeMillis();
        if (!job.canWork(now, workCooldownMillis)) return Optional.empty();
        EarthOccupation occupation = job.getOccupationType();
        DBEarthRegion region = regions.get(job.getRegionId());
        DBEarthCountry country = region == null ? null : countries.get(region.getCountryId());
        if (region == null || country == null) return Optional.empty();
        DBEarthBusiness employer = occupation == EarthOccupation.RETAIL && job.getEmployerShopId() != null
                ? businesses.get(job.getEmployerShopId()) : null;
        if (occupation == EarthOccupation.RETAIL && employer == null) return Optional.empty();
        if (employer != null && !EconomyManager.withdrawBalance(employer.getOwnerId(), occupation.wage(), "Earth business payroll")) return Optional.empty();
        double tax = occupation.wage() * country.getTaxRate() * taxMultiplier(country.getId());
        double regionalConditionMultiplier = regionalIndicators.getOrDefault(region.getId(), new DBEarthRegionalIndicators(region.getId(), now))
                .productivityMultiplier();
        double production = occupation.productivity() * workProductivityMultiplier(country.getId())
                * ensureCitizenStatus(playerId).productivityMultiplier() * regionalConditionMultiplier;
        job.recordWork(now);
        regionalLedgers.computeIfAbsent(region.getId(), DBEarthRegionalLedger::new)
                .recordSale(production, tax);
        EconomyManager.addBalance(playerId, occupation.wage(), "Earth work: " + occupation.name().toLowerCase(Locale.ROOT));
        persistAsync(() -> {
            jobsDao.createOrUpdate(job);
            regionalLedgersDao.createOrUpdate(regionalLedgers.get(region.getId()));
        });
        return Optional.of(new WorkResult(occupation, occupation.wage(), production, tax));
    }

    public void simulateOnce() {
        long now = System.currentTimeMillis();
        simulationClock.advance(now, simulationRealDaySeconds, simulatedMonthsPerRealDay);
        purgeExpiredTravelPasses(now);
        purgeExpiredPropertySaleOffers(now);
        advanceElectionCycles(now);
        Map<String, Integer> citizensByCountry = new HashMap<>();
        citizenships.values().forEach(citizenship -> citizensByCountry.merge(citizenship.getCountryId(), 1, Integer::sum));
        Map<String, Integer> jobsByRegion = new HashMap<>();
        jobs.values().forEach(job -> jobsByRegion.merge(job.getRegionId(), 1, Integer::sum));
        Map<String, EnumMap<EarthTransportMode, Integer>> transportByRegion = new HashMap<>();
        transportRoutes.values().forEach(route -> {
            addTransportRoute(transportByRegion, route.getFromRegion(), route.getModeType());
            addTransportRoute(transportByRegion, route.getToRegion(), route.getModeType());
        });
        for (DBEarthRegion region : regions.values()) {
            DBEarthCountry country = countries.get(region.getCountryId());
            DBEarthRegionState state = regionStates.get(region.getId());
            if (country == null || !country.isActive() || state == null) continue;
            DBEarthRegionalLedger.CommerceCycle commerce = regionalLedgers
                    .computeIfAbsent(region.getId(), DBEarthRegionalLedger::new).consume();
            state.creditTreasury(commerce.tax());
            state.applyPublicBudget(countryPolicies.get(country.getId()));
            state.applyLaws(getActiveLaws(country.getId()));
            if (offices.containsKey(country.getId())) state.applyDemocraticMandate();
            Set<EarthRegionalEventType> activeEvents = getActiveEvents(region.getId()).stream()
                    .map(DBEarthRegionalEvent::getType).collect(java.util.stream.Collectors.toSet());
            state.applyEvents(activeEvents);
            state.applyTradeClimate(tradeMultiplier(country.getId()));
            int residentialProperties = (int) properties.values().stream().filter(property -> property.getRegionId().equals(region.getId()) && property.getType() == EarthPropertyType.RESIDENTIAL).count();
            DBEarthDemographics demographic = demographics.computeIfAbsent(region.getId(), id -> new DBEarthDemographics(id, 50, 30, 0.5D, now));
            demographic.simulate(state.getProsperity(), state.getEmployment(), residentialProperties, now);
            int activeEconomicActors = citizensByCountry.getOrDefault(country.getId(), 0)
                    + jobsByRegion.getOrDefault(region.getId(), 0) + (int) Math.min(Integer.MAX_VALUE, commerce.sales());
            state.simulate(country.getTaxRate(), activeEconomicActors, now);
            DBEarthRegionalIndicators indicators = regionalIndicators.computeIfAbsent(region.getId(), id -> new DBEarthRegionalIndicators(id, now));
            indicators.simulate(state, countryPolicies.get(country.getId()), getActiveLaws(country.getId()), activeEvents, activeEconomicActors, now);
            indicators.applyGovernmentPortfolios(activePortfolios(country.getId()));
            EnumMap<EarthTransportMode, Integer> routeModes = transportByRegion.getOrDefault(region.getId(), new EnumMap<>(EarthTransportMode.class));
            indicators.applyTransportNetwork(routeModes.getOrDefault(EarthTransportMode.ROAD, 0), routeModes.getOrDefault(EarthTransportMode.RAIL, 0),
                    routeModes.getOrDefault(EarthTransportMode.AIR, 0), routeModes.getOrDefault(EarthTransportMode.WATER, 0));
            state.applySocietalConditions(indicators);
        }
        refreshCountryStatistics(citizensByCountry, now);
        applyWorldConsequences();
        publishMapOverlays();
        persistAsync(() -> {
            persistRegionStates();
            persistRegionalLedgers();
            for (DBEarthRegionalIndicators indicators : regionalIndicators.values()) regionalIndicatorsDao.createOrUpdate(indicators);
            for (DBEarthCountryStatistics statistics : countryStatistics.values()) countryStatisticsDao.createOrUpdate(statistics);
            simulationClockDao.createOrUpdate(simulationClock);
        });
    }

    private static void addTransportRoute(Map<String, EnumMap<EarthTransportMode, Integer>> transportByRegion,
                                          String regionId, EarthTransportMode mode) {
        transportByRegion.computeIfAbsent(regionId, ignored -> new EnumMap<>(EarthTransportMode.class))
                .merge(mode, 1, Integer::sum);
    }

    private void persistAll() {
        try {
            for (DBEarthCitizenship citizenship : citizenships.values()) citizenshipsDao.createOrUpdate(citizenship);
            for (DBEarthProperty property : properties.values()) propertiesDao.createOrUpdate(property);
            for (DBEarthPropertyAccess access : propertyAccess.values()) propertyAccessDao.createOrUpdate(access);
            for (DBEarthPropertySaleOffer offer : propertySaleOffers.values()) propertySaleOffersDao.createOrUpdate(offer);
            for (DBEarthBusiness business : businesses.values()) businessesDao.createOrUpdate(business);
            for (DBEarthCountryPolicy policy : countryPolicies.values()) countryPoliciesDao.createOrUpdate(policy);
            for (DBEarthJob job : jobs.values()) jobsDao.createOrUpdate(job);
            for (DBEarthCitizenStatus status : citizenStatuses.values()) citizenStatusesDao.createOrUpdate(status);
            for (DBEarthCountryOffice office : offices.values()) officesDao.createOrUpdate(office);
            for (DBEarthGovernment government : governments.values()) governmentsDao.createOrUpdate(government);
            for (Map<EarthGovernmentPortfolio, DBEarthGovernmentAppointment> appointments : appointmentsByCountry.values()) {
                for (DBEarthGovernmentAppointment appointment : appointments.values()) governmentAppointmentsDao.createOrUpdate(appointment);
            }
            for (DBEarthPoliticalParty party : politicalParties.values()) politicalPartiesDao.createOrUpdate(party);
            for (DBEarthPartyMembership membership : partyMemberships.values()) partyMembershipsDao.createOrUpdate(membership);
            for (DBEarthBill bill : bills.values()) billsDao.createOrUpdate(bill);
            for (DBEarthElectionCycle cycle : electionCycles.values()) electionCyclesDao.createOrUpdate(cycle);
            for (DBEarthRegionalEvent event : regionalEvents.values()) regionalEventsDao.createOrUpdate(event);
            for (DBEarthTransportRoute route : transportRoutes.values()) transportRoutesDao.createOrUpdate(route);
            for (DBEarthTravelPass pass : travelPasses.values()) travelPassesDao.createOrUpdate(pass);
            for (DBEarthDemographics demographic : demographics.values()) demographicsDao.createOrUpdate(demographic);
            for (DBEarthRegionWaypoint waypoint : waypoints.values()) waypointsDao.createOrUpdate(waypoint);
            for (DBEarthDiplomaticRelation relation : diplomaticRelations.values()) diplomaticRelationsDao.createOrUpdate(relation);
            for (DBEarthLaw law : lawRecords.values()) lawsDao.createOrUpdate(law);
            for (DBEarthRegionalIndicators indicators : regionalIndicators.values()) regionalIndicatorsDao.createOrUpdate(indicators);
            for (DBEarthCountryStatistics statistics : countryStatistics.values()) countryStatisticsDao.createOrUpdate(statistics);
            simulationClockDao.createOrUpdate(simulationClock);
            persistRegionStates();
            persistRegionalLedgers();
        } catch (SQLException exception) {
            throw new IllegalStateException("Unable to save Earth simulation state", exception);
        }
    }

    private void persistRegionStates() throws SQLException {
        for (DBEarthRegionState state : regionStates.values()) regionStatesDao.createOrUpdate(state);
    }

    private void persistRegionalLedgers() throws SQLException {
        for (DBEarthRegionalLedger ledger : regionalLedgers.values()) regionalLedgersDao.createOrUpdate(ledger);
    }

    private void refreshCountryStatistics(Map<String, Integer> citizensByCountry, long now) {
        for (DBEarthCountry country : countries.values()) {
            List<DBEarthRegion> countryRegions = regions.values().stream().filter(region -> region.getCountryId().equals(country.getId())).toList();
            double treasury = countryRegions.stream().map(regionStates::get).filter(Objects::nonNull).mapToDouble(DBEarthRegionState::getTreasury).sum();
            int population = countryRegions.stream().map(demographics::get).filter(Objects::nonNull).mapToInt(DBEarthDemographics::getResidents).sum();
            double economicState = countryRegions.stream().map(regionStates::get).filter(Objects::nonNull)
                    .mapToDouble(DBEarthRegionState::getProsperity).average().orElse(0D);
            DBEarthCountryStatistics statistics = countryStatistics.computeIfAbsent(country.getId(), id -> new DBEarthCountryStatistics(id, now));
            statistics.refresh(treasury, population, citizensByCountry.getOrDefault(country.getId(), 0), economicState, now);
            statistics.setActive(country.isActive(), now);
        }
    }

    private void persistAsync(SqlOperation operation) {
        persistenceQueue.add(operation);
        schedulePersistenceDrain();
    }

    private void schedulePersistenceDrain() {
        if (!persistenceDraining.compareAndSet(false, true)) return;
        Bukkit.getScheduler().runTaskAsynchronously(OMCPlugin.getInstance(), this::drainPersistenceQueue);
    }

    private void drainPersistenceQueue() {
        try {
            SqlOperation operation;
            while ((operation = persistenceQueue.poll()) != null) {
                try {
                    operation.run();
                } catch (SQLException exception) {
                    OMCPlugin.getInstance().getSLF4JLogger().error("Unable to persist Earth simulation state", exception);
                }
            }
        } finally {
            persistenceDraining.set(false);
            // An operation may have arrived after the final poll but before
            // the flag was cleared; re-checking closes that scheduling race.
            if (!persistenceQueue.isEmpty()) schedulePersistenceDrain();
        }
    }

    private void purgeExpiredTravelPasses(long now) {
        List<DBEarthTravelPass> expired = travelPasses.values().stream().filter(pass -> !pass.isActive(now)).toList();
        if (expired.isEmpty()) return;
        expired.forEach(pass -> travelPasses.remove(pass.getPlayerId(), pass));
        persistAsync(() -> { for (DBEarthTravelPass pass : expired) travelPassesDao.delete(pass); });
    }

    private void purgeExpiredPropertySaleOffers(long now) {
        List<DBEarthPropertySaleOffer> expired = propertySaleOffers.values().stream().filter(offer -> !offer.isActive(now)).toList();
        if (expired.isEmpty()) return;
        expired.forEach(offer -> propertySaleOffers.remove(offer.getPropertyId(), offer));
        persistAsync(() -> { for (DBEarthPropertySaleOffer offer : expired) propertySaleOffersDao.delete(offer); });
    }

    /** Main-thread, low-frequency effects for players in severe regional conditions. */
    private void applyWorldConsequences() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            findRegion(player.getLocation().getChunk().getX(), player.getLocation().getChunk().getZ()).ifPresent(region -> {
                DBEarthRegionalIndicators indicators = regionalIndicators.get(region.getId());
                if (indicators == null) return;
                if (indicators.getPollution() >= .70D) {
                    player.addPotionEffect(new PotionEffect(PotionEffectType.NAUSEA, 20 * 75, 0, true, false, false));
                    player.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 20 * 75, 0, true, false, false));
                }
                if (indicators.getFoodSecurity() <= .25D) {
                    player.addPotionEffect(new PotionEffect(PotionEffectType.WEAKNESS, 20 * 75, 0, true, false, false));
                }
                DBEarthDemographics population = demographics.get(region.getId());
                if (population != null) runNpcAdapter(player, region, population, indicators);
            });
        }
    }

    private void publishMapOverlays() {
        List<EarthMapAdapter.RegionOverlay> overlays = regions.values().stream().map(region -> {
            DBEarthRegionState state = regionStates.get(region.getId());
            DBEarthRegionalIndicators indicators = regionalIndicators.get(region.getId());
            return new EarthMapAdapter.RegionOverlay(region.getId(), region.getCountryId(), region.getName(), region.getMinChunkX(),
                    region.getMaxChunkX(), region.getMinChunkZ(), region.getMaxChunkZ(), state == null ? 0D : state.getProsperity(),
                    indicators == null ? 0D : indicators.getPollution(), indicators == null ? 0D : indicators.getApproval(),
                    indicators == null ? 0D : indicators.getInfrastructure());
        }).toList();
        try { mapAdapter.publish(overlays); }
        catch (RuntimeException exception) { OMCPlugin.getInstance().getSLF4JLogger().warn("Earth map adapter failed", exception); }
    }

    private void runNpcAdapter(Player player, DBEarthRegion region, DBEarthDemographics population, DBEarthRegionalIndicators indicators) {
        try { npcAdapter.reconcile(player, region, population, indicators); }
        catch (RuntimeException exception) { OMCPlugin.getInstance().getSLF4JLogger().warn("Earth NPC adapter failed", exception); }
    }

    public OperationalSnapshot getOperationalSnapshot() {
        return new OperationalSnapshot(countries.size(), regions.size(), properties.size(), businesses.size(), jobs.size(),
                citizenStatuses.size(), regionalEvents.size(), transportRoutes.size(), waypoints.size(), diplomaticRelations.size(), travelPasses.size());
    }

    public DBEarthSimulationClock getSimulationClock() { return simulationClock; }

    /** Registration point for optional BlueMap-like overlays; null restores no-op behavior. */
    public void setMapAdapter(EarthMapAdapter adapter) { mapAdapter = adapter == null ? overlays -> { } : adapter; }

    /** Registration point for optional Citizens-like dynamic NPC providers; null restores no-op behavior. */
    public void setNpcAdapter(EarthNpcAdapter adapter) { npcAdapter = adapter == null ? (player, region, population, conditions) -> { } : adapter; }

    @FunctionalInterface
    private interface SqlOperation {
        void run() throws SQLException;
    }

    private double taxMultiplier(String countryId) {
        Set<EarthLawType> laws = getActiveLaws(countryId);
        return laws.contains(EarthLawType.BUSINESS_INCENTIVE) ? 0.88D : 1D;
    }

    private double workProductivityMultiplier(String countryId) {
        Set<EarthLawType> laws = getActiveLaws(countryId);
        double multiplier = 1D;
        if (laws.contains(EarthLawType.WORKER_PROTECTION)) multiplier += 0.10D;
        if (laws.contains(EarthLawType.BUSINESS_INCENTIVE)) multiplier += 0.05D;
        return multiplier;
    }

    private DBEarthCitizenStatus ensureCitizenStatus(UUID playerId) {
        return citizenStatuses.computeIfAbsent(playerId, id -> {
            DBEarthCitizenStatus status = new DBEarthCitizenStatus(id);
            persistAsync(() -> citizenStatusesDao.createOrUpdate(status));
            return status;
        });
    }

    private boolean isCitizenOf(UUID playerId, String countryId) {
        DBEarthCitizenship citizenship = citizenships.get(playerId);
        return citizenship != null && citizenship.getCountryId().equalsIgnoreCase(countryId);
    }

    private double tradeMultiplier(String countryId) {
        List<DBEarthDiplomaticRelation> relations=getRelations(countryId);
        return relations.isEmpty()?1D:relations.stream().mapToDouble(relation -> relation.getStatusType().tradeMultiplier()).average().orElse(1D);
    }

    public record WorkResult(EarthOccupation occupation, double wage, double productivity, double tax) { }
    public record PolicyImpactEstimate(double healthChange, double infrastructureChange, double employmentChange,
                                       double pollutionChange, double approvalChange, double treasuryPressure) { }
    public record OperationalSnapshot(int countries, int regions, int properties, int businesses, int jobs, int citizens,
                                      int events, int routes, int waypoints, int relations, int activeTravelPasses) { }
}
