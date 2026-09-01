package fr.openmc.core.features.earth.models;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

import java.util.UUID;
import java.util.Set;

class EarthRegionStateTest {
    @Test
    void regionBoundsAreInclusive() {
        DBEarthRegion region = new DBEarthRegion("canada-ontario", "canada", "Ontario", -2, 3, -4, 5);

        assertTrue(region.containsChunk(-2, -4));
        assertTrue(region.containsChunk(3, 5));
        assertFalse(region.containsChunk(4, 5));
        assertFalse(region.containsChunk(3, -5));
    }

    @Test
    void regionsValidateBoundsAndDetectInclusiveChunkOverlap() {
        DBEarthRegion ontario = new DBEarthRegion("canada-ontario", "canada", "Ontario", -2, 3, -4, 5);
        DBEarthRegion quebec = new DBEarthRegion("canada-quebec", "canada", "Quebec", 3, 8, 5, 10);
        DBEarthRegion france = new DBEarthRegion("france-idf", "france", "Île-de-France", 4, 8, 6, 10);
        DBEarthRegion invalid = new DBEarthRegion("invalid", "canada", "Invalid", 3, -2, 1, 2);

        assertTrue(ontario.overlaps(quebec));
        assertFalse(ontario.overlaps(france));
        assertTrue(ontario.hasValidBounds());
        assertFalse(invalid.hasValidBounds());
    }

    @Test
    void simulationKeepsPublicMetricsBoundedAndAdvancesTimestamp() {
        DBEarthRegionState state = new DBEarthRegionState("canada-ontario", 1_000D, 0.50D, 0.55D, 0.55D, 10L);

        state.simulate(0.12D, 25, 20L);

        assertTrue(state.getTreasury() >= 0D);
        assertTrue(state.getProsperity() >= 0D && state.getProsperity() <= 1D);
        assertTrue(state.getEmployment() >= 0.15D && state.getEmployment() <= 0.98D);
        assertTrue(state.getSatisfaction() >= 0D && state.getSatisfaction() <= 1D);
        assertEquals(20L, state.getLastSimulationAt());
    }

    @Test
    void commercialLedgerDrainsExactlyOncePerSimulationCycle() {
        DBEarthRegionalLedger ledger = new DBEarthRegionalLedger("canada-ontario");
        ledger.recordSale(80D, 9.6D);
        ledger.recordSale(20D, 2.4D);

        DBEarthRegionalLedger.CommerceCycle cycle = ledger.consume();

        assertEquals(100D, cycle.turnover());
        assertEquals(12D, cycle.tax());
        assertEquals(2L, cycle.sales());
        assertEquals(0D, ledger.getPendingTurnover());
        assertEquals(0D, ledger.getPendingTax());
        assertEquals(0L, ledger.getPendingSales());
    }

    @Test
    void publicBudgetConsumesTreasuryAndImprovesAtLeastOnePublicMetric() {
        DBEarthRegionState state = new DBEarthRegionState("canada-ontario", 1_000D, 0.50D, 0.55D, 0.55D, 10L);
        DBEarthCountryPolicy policy = new DBEarthCountryPolicy("canada", 0.4D, 0.35D, 0.25D, 10L);

        state.applyPublicBudget(policy);

        assertTrue(state.getTreasury() < 1_000D);
        assertTrue(state.getProsperity() > 0.50D);
        assertTrue(state.getEmployment() > 0.55D);
        assertTrue(state.getSatisfaction() > 0.55D);
    }

    @Test
    void jobTracksWorkUnitsAndCooldownWithoutBukkitState() {
        DBEarthJob job = new DBEarthJob(UUID.randomUUID(), EarthOccupation.CONSTRUCTION, "canada-ontario", null, 100L);

        assertTrue(job.canWork(1_000L, 500L));
        job.recordWork(1_000L);

        assertEquals(1L, job.getWorkUnits());
        assertFalse(job.canWork(1_400L, 500L));
        assertTrue(job.canWork(1_500L, 500L));
    }

    @Test
    void propertyRetainsItsLandUseClassification() {
        DBEarthProperty property = new DBEarthProperty(UUID.randomUUID(), UUID.randomUUID(), null, "canada-ontario", "world",
                10, -4, EarthPropertyType.RESIDENTIAL, 250D, 1L);

        assertEquals(EarthPropertyType.RESIDENTIAL, property.getType());
        assertEquals("canada-ontario", property.getRegionId());
    }

    @Test
    void publicLawsHaveBoundedRegionalEffects() {
        DBEarthRegionState state = new DBEarthRegionState("canada-ontario", 1_000D, 0.50D, 0.55D, 0.55D, 10L);

        state.applyLaws(Set.of(EarthLawType.PUBLIC_SAFETY, EarthLawType.GREEN_INFRASTRUCTURE, EarthLawType.WORKER_PROTECTION));

        assertTrue(state.getProsperity() > 0.50D);
        assertTrue(state.getSatisfaction() > 0.55D);
        assertTrue(state.getProsperity() <= 1D && state.getSatisfaction() <= 1D);
    }

    @Test
    void civicStandingTracksVolunteerCooldownAndBoundedProductivity() {
        DBEarthCitizenStatus status = new DBEarthCitizenStatus(UUID.randomUUID());

        assertTrue(status.canVolunteer(1_000L, 500L));
        status.recordVolunteer(1_000L);

        assertEquals(5, status.getReputation());
        assertEquals(1L, status.getCivicActions());
        assertFalse(status.canVolunteer(1_400L, 500L));
        assertTrue(status.productivityMultiplier() > 1D && status.productivityMultiplier() <= 1.15D);
    }

    @Test
    void electionRecordsAreCountryScopedAndDemocraticMandateIsBounded() {
        UUID candidate = UUID.randomUUID();
        DBEarthElectionCandidate nomination = new DBEarthElectionCandidate("Canada", candidate, 10L);
        DBEarthElectionVote vote = new DBEarthElectionVote("Canada", UUID.randomUUID(), candidate, 20L);
        DBEarthRegionState state = new DBEarthRegionState("canada-ontario", 1_000D, 0.50D, 0.55D, 0.55D, 10L);

        state.applyDemocraticMandate();

        assertEquals("canada:" + candidate, nomination.getId());
        assertTrue(vote.getId().startsWith("canada:"));
        assertTrue(state.getSatisfaction() > 0.55D && state.getSatisfaction() <= 1D);
    }

    @Test
    void regionalEventsApplyBoundedPositiveAndNegativeEffects() {
        DBEarthRegionState state = new DBEarthRegionState("canada-ontario", 1_000D, 0.50D, 0.55D, 0.55D, 10L);
        state.applyEvents(Set.of(EarthRegionalEventType.HARVEST_FESTIVAL, EarthRegionalEventType.CIVIC_CELEBRATION));
        assertTrue(state.getProsperity() > 0.50D);
        assertTrue(state.getSatisfaction() > 0.55D);
        state.applyEvents(Set.of(EarthRegionalEventType.INFRASTRUCTURE_DISRUPTION));
        assertTrue(state.getEmployment() < 0.55D);
        assertTrue(state.getEmployment() >= 0.15D);
    }

    @Test
    void demographicsStayBoundedByHousingAndEmployment() {
        DBEarthDemographics population = new DBEarthDemographics("canada-ontario", 50, 30, 0.5D, 10L);
        population.simulate(0.8D, 0.8D, 5, 20L);
        assertTrue(population.getResidents() >= 0);
        assertTrue(population.getWorkforce() >= 0 && population.getWorkforce() <= population.getResidents());
        assertTrue(population.getHousingPressure() >= 0D && population.getHousingPressure() <= 2D);
    }

    @Test
    void travelPassExpiresAtItsConfiguredBoundary() {
        DBEarthTravelPass pass = new DBEarthTravelPass(UUID.randomUUID(), UUID.randomUUID(), "canada-ontario", 100L);
        assertTrue(pass.isActive(99L));
        assertFalse(pass.isActive(100L));
    }

    @Test
    void diplomacyHasStablePairIdsAndBoundedTradeEffect() {
        DBEarthDiplomaticRelation relation=new DBEarthDiplomaticRelation("Canada","France",EarthDiplomaticStatus.ALLIED,10L);
        DBEarthRegionState state=new DBEarthRegionState("canada-ontario",1000D,.5D,.55D,.55D,10L);
        state.applyTradeClimate(relation.getStatusType().tradeMultiplier());
        assertEquals("canada:france",relation.getId());
        assertTrue(state.getProsperity()>.5D && state.getProsperity()<=1D);
    }

    @Test
    void causalIndicatorsStayBoundedAndFeedBackIntoRegionalState() {
        DBEarthRegionState state = new DBEarthRegionState("canada-ontario", 1_000D, .60D, .65D, .60D, 10L);
        DBEarthRegionalIndicators indicators = new DBEarthRegionalIndicators("canada-ontario", 10L);
        DBEarthCountryPolicy policy = new DBEarthCountryPolicy("canada", .40D, .35D, .25D, 10L);

        indicators.simulate(state, policy, Set.of(EarthLawType.GREEN_INFRASTRUCTURE, EarthLawType.PUBLIC_SAFETY),
                Set.of(EarthRegionalEventType.POLLUTION_CRISIS, EarthRegionalEventType.ENERGY_SHORTAGE), 40, 20L);
        state.applySocietalConditions(indicators);

        assertTrue(indicators.getPollution() >= 0D && indicators.getPollution() <= 1D);
        assertTrue(indicators.getCrime() >= 0D && indicators.getCrime() <= 1D);
        assertTrue(indicators.getHealth() >= 0D && indicators.getHealth() <= 1D);
        assertTrue(indicators.getInflation() >= 0D && indicators.getInflation() <= .35D);
        assertTrue(indicators.getApproval() >= 0D && indicators.getApproval() <= 1D);
        assertTrue(indicators.productivityMultiplier() >= .45D && indicators.productivityMultiplier() <= 1.25D);
        assertEquals(20L, indicators.getLastUpdatedAt());
        assertTrue(state.getProsperity() >= 0D && state.getProsperity() <= 1D);
        assertTrue(state.getSatisfaction() >= 0D && state.getSatisfaction() <= 1D);
    }

    @Test
    void electionCycleUsesPersistedWallClockPhaseBoundaries() {
        DBEarthElectionCycle cycle = new DBEarthElectionCycle("canada", EarthElectionPhase.CANDIDACY, 10L, 20L, 0L);

        assertEquals(EarthElectionPhase.CANDIDACY, cycle.getPhaseType());
        assertFalse(cycle.phaseHasEnded(19L));
        assertTrue(cycle.phaseHasEnded(20L));
        cycle.moveTo(EarthElectionPhase.MANDATE, 20L, 100L, 100L);
        assertEquals(EarthElectionPhase.MANDATE, cycle.getPhaseType());
        assertEquals(100L, cycle.getMandateEndsAt());
    }

    @Test
    void propertyLeaseAndAccessArePersistentPolicyState() {
        UUID owner = UUID.randomUUID();
        UUID tenant = UUID.randomUUID();
        DBEarthPropertyAccess access = new DBEarthPropertyAccess(UUID.randomUUID(), EarthPropertyAccessMode.PRIVATE, 10L);

        assertFalse(access.canAccess(tenant, owner, 10L));
        access.setLeaseOffer(25D, 20L);
        access.startLease(tenant, 100L, 20L);
        assertTrue(access.hasActiveLease(99L));
        assertTrue(access.canAccess(tenant, owner, 99L));
        assertFalse(access.hasActiveLease(100L));
        access.setAccessMode(EarthPropertyAccessMode.PUBLIC, 101L);
        assertTrue(access.canAccess(UUID.randomUUID(), owner, 101L));
    }

    @Test
    void propertySaleOfferRequiresAStillActiveSellerAuthorization() {
        UUID property = UUID.randomUUID();
        UUID seller = UUID.randomUUID();
        DBEarthPropertySaleOffer offer = new DBEarthPropertySaleOffer(property, seller, 75D, 10L, 20L);

        assertEquals(property, offer.getPropertyId());
        assertEquals(seller, offer.getSellerId());
        assertTrue(offer.isActive(19L));
        assertFalse(offer.isActive(20L));
    }

    @Test
    void budgetPolicyNormalizesNonNegativeAllocations() {
        DBEarthCountryPolicy policy = new DBEarthCountryPolicy("canada", 4D, 3D, 3D, 10L);

        assertEquals(1D, policy.getSocialServices() + policy.getInfrastructure() + policy.getInvestment(), 0.000001D);
        assertEquals(.4D, policy.getSocialServices(), 0.000001D);
        assertEquals(.3D, policy.getInfrastructure(), 0.000001D);
    }

    @Test
    void regionalTransportCostIsBoundedAndNeverDiscountsTravel() {
        DBEarthRegionalIndicators indicators = new DBEarthRegionalIndicators("canada-ontario", 10L);

        assertTrue(indicators.transportCostMultiplier() >= 1D);
        assertTrue(indicators.transportCostMultiplier() <= 1.30D);
    }

    @Test
    void transportNetworkFeedsBoundedInfrastructurePollutionAndEnergyConditions() {
        DBEarthRegionalIndicators indicators = new DBEarthRegionalIndicators("canada-ontario", 10L);
        double initialInfrastructure = indicators.getInfrastructure();
        double initialPollution = indicators.getPollution();

        indicators.applyTransportNetwork(2, 3, 1, 1);

        assertTrue(indicators.getInfrastructure() > initialInfrastructure);
        assertTrue(indicators.getPollution() >= initialPollution);
        assertTrue(indicators.getEnergy() >= 0D && indicators.getEnergy() <= 1D);
    }

    @Test
    void appointedGovernmentPortfoliosProduceBoundedConditionEffects() {
        DBEarthRegionalIndicators indicators = new DBEarthRegionalIndicators("canada-ontario", 10L);
        double initialHealth = indicators.getHealth();
        double initialCrime = indicators.getCrime();
        double initialPollution = indicators.getPollution();

        indicators.applyGovernmentPortfolios(Set.of(EarthGovernmentPortfolio.HEALTH,
                EarthGovernmentPortfolio.PUBLIC_SAFETY, EarthGovernmentPortfolio.ENVIRONMENT));

        assertTrue(indicators.getHealth() > initialHealth);
        assertTrue(indicators.getCrime() < initialCrime);
        assertTrue(indicators.getPollution() < initialPollution);
        assertTrue(indicators.getHealth() <= 1D && indicators.getCrime() >= 0D && indicators.getPollution() >= 0D);
    }

    @Test
    void repeatedCausalCyclesRemainBoundedUnderRepresentativeModelLoad() {
        DBEarthRegionState state = new DBEarthRegionState("canada-ontario", 5_000D, .60D, .65D, .60D, 10L);
        DBEarthRegionalIndicators indicators = new DBEarthRegionalIndicators("canada-ontario", 10L);
        DBEarthCountryPolicy policy = new DBEarthCountryPolicy("canada", .40D, .35D, .25D, 10L);
        Set<EarthLawType> laws = Set.of(EarthLawType.GREEN_INFRASTRUCTURE, EarthLawType.PUBLIC_SAFETY);
        Set<EarthGovernmentPortfolio> cabinet = Set.of(EarthGovernmentPortfolio.HEALTH,
                EarthGovernmentPortfolio.INFRASTRUCTURE, EarthGovernmentPortfolio.PUBLIC_SAFETY);

        for (int cycle = 1; cycle <= 2_000; cycle++) {
            state.applyPublicBudget(policy);
            state.applyLaws(laws);
            state.simulate(.12D, 40, cycle);
            indicators.simulate(state, policy, laws, Set.of(), 40, cycle);
            indicators.applyGovernmentPortfolios(cabinet);
            indicators.applyTransportNetwork(2, 1, 0, 1);
            state.applySocietalConditions(indicators);
        }

        assertTrue(state.getTreasury() >= 0D);
        assertTrue(state.getProsperity() >= 0D && state.getProsperity() <= 1D);
        assertTrue(state.getEmployment() >= .15D && state.getEmployment() <= .98D);
        assertTrue(state.getSatisfaction() >= 0D && state.getSatisfaction() <= 1D);
        assertTrue(indicators.getInflation() >= 0D && indicators.getInflation() <= .35D);
        assertTrue(indicators.getPublicDebt() >= 0D);
    }

    @Test
    void virtualCalendarAdvancesByConfiguredRealTimeRatioAndCanResumeSafely() {
        DBEarthSimulationClock clock = new DBEarthSimulationClock(1_000L);
        clock.advance(43_201_000L, 86_400L, 6D);
        assertEquals(3D, clock.getSimulatedMonths(), .000001D);
        clock.resume(50_000_000L);
        clock.advance(50_000_000L, 86_400L, 6D);
        assertEquals(3D, clock.getSimulatedMonths(), .000001D);
    }

    @Test
    void countryStatisticsBoundAggregatesAndTrackRefreshTime() {
        DBEarthCountryStatistics statistics = new DBEarthCountryStatistics("canada", 10L);
        statistics.refresh(-5D, -2, -3, 1.5D, 20L);

        assertEquals(0D, statistics.getTreasury());
        assertEquals(0, statistics.getPopulation());
        assertEquals(0, statistics.getCitizenCount());
        assertEquals(1D, statistics.getEconomicState());
        assertTrue(statistics.isActive());
        assertEquals(20L, statistics.getUpdatedAt());
    }

    @Test
    void countryStatisticsCanExposeAnInactiveCountryWithoutDiscardingItsData() {
        DBEarthCountryStatistics statistics = new DBEarthCountryStatistics("canada", 10L);
        statistics.refresh(100D, 12, 8, .75D, 20L);
        statistics.setActive(false, 30L);

        assertFalse(statistics.isActive());
        assertEquals(100D, statistics.getTreasury());
        assertEquals(30L, statistics.getUpdatedAt());
    }

    @Test
    void countryConfigurationRefreshPreservesIdentityAndCreationTime() {
        DBEarthCountry country = new DBEarthCountry("canada", "CA", "Canada", "Ottawa", "CAD", "federal", "canada-government", .12D, true, 10L);
        DBEarthCountry configured = new DBEarthCountry("canada", "CAN", "Canada Updated", "Ottawa", "CAD", "federal", "canada-government", .15D, false, 20L);

        country.applyConfiguredMetadata(configured, 30L);

        assertEquals("canada", country.getId());
        assertEquals("CAN", country.getCode());
        assertEquals("Canada Updated", country.getName());
        assertFalse(country.isActive());
        assertEquals(10L, country.getCreatedAt());
        assertEquals(30L, country.getUpdatedAt());
    }

    @Test
    void governmentInstallsAnElectedLeaderForTheMandate() {
        UUID leader = UUID.randomUUID();
        DBEarthGovernment government = new DBEarthGovernment("canada", "canada-federal-government", "federal_democracy", 10L);

        government.installLeader(leader, 100L, 20L);

        assertEquals("canada", government.getCountryId());
        assertEquals(leader, government.getLeaderId());
        assertEquals(100L, government.getMandateEndsAt());
        assertEquals(20L, government.getTransitionedAt());
    }

    @Test
    void vacantElectionClearsLeadershipAndRulingParty() {
        DBEarthGovernment government = new DBEarthGovernment("canada", "canada-federal-government", "federal_democracy", 10L);
        government.installLeader(UUID.randomUUID(), "canada:civic-future", 100L, 20L);

        government.vacateLeadership(30L);

        assertNull(government.getLeaderId());
        assertNull(government.getRulingPartyId());
        assertEquals(0L, government.getMandateEndsAt());
        assertEquals(30L, government.getTransitionedAt());
    }

    @Test
    void partyAffiliationFlowsFromCandidateToGovernment() {
        UUID founder = UUID.randomUUID();
        UUID candidateId = UUID.randomUUID();
        DBEarthPoliticalParty party = new DBEarthPoliticalParty("canada", "Civic Future", "health and infrastructure", founder, 10L);
        DBEarthPartyMembership membership = new DBEarthPartyMembership(candidateId, party.getId(), 11L);
        DBEarthElectionCandidate candidate = new DBEarthElectionCandidate("canada", candidateId, membership.getPartyId(), 12L);
        DBEarthGovernment government = new DBEarthGovernment("canada", "canada-government", "federal", 13L);

        government.installLeader(candidate.getCandidateId(), candidate.getPartyId(), 100L, 20L);

        assertEquals("canada:civic-future", party.getId());
        assertEquals(party.getId(), membership.getPartyId());
        assertEquals(party.getId(), candidate.getPartyId());
        assertEquals(party.getId(), government.getRulingPartyId());
    }

    @Test
    void billHasAnAuditableProposedAndPassedLifecycle() {
        DBEarthBill bill = new DBEarthBill("canada", EarthLawType.PUBLIC_SAFETY, UUID.randomUUID(), 10L);

        assertTrue(bill.isOpen());
        assertEquals(EarthBillStatus.PROPOSED, bill.getStatusType());
        bill.resolve(EarthBillStatus.PASSED, 20L);
        assertFalse(bill.isOpen());
        assertEquals(EarthBillStatus.PASSED, bill.getStatusType());
        assertEquals(20L, bill.getResolvedAt());
    }
}
