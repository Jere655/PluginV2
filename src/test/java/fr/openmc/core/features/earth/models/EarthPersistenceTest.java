package fr.openmc.core.features.earth.models;

import com.j256.ormlite.dao.Dao;
import com.j256.ormlite.dao.DaoManager;
import com.j256.ormlite.jdbc.JdbcConnectionSource;
import com.j256.ormlite.table.TableUtils;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EarthPersistenceTest {
    private static final String DATABASE_URL = "jdbc:h2:mem:earth-persistence;DB_CLOSE_DELAY=-1";

    @Test
    void durableSimulationRecordsCanBeReadFromANewConnectionSource() throws Exception {
        UUID propertyId = UUID.randomUUID();
        UUID tenantId = UUID.randomUUID();
        UUID founderId = UUID.randomUUID();
        UUID leaderId = UUID.randomUUID();

        try (JdbcConnectionSource source = new JdbcConnectionSource(DATABASE_URL)) {
            createTables(source);

            DBEarthSimulationClock clock = new DBEarthSimulationClock(1_000L);
            clock.advance(43_201_000L, 86_400L, 6D);
            DaoManager.createDao(source, DBEarthSimulationClock.class).create(clock);

            DBEarthElectionCycle election = new DBEarthElectionCycle("canada", EarthElectionPhase.CANDIDACY, 10L, 20L, 0L);
            election.moveTo(EarthElectionPhase.VOTING, 20L, 30L, 0L);
            DaoManager.createDao(source, DBEarthElectionCycle.class).create(election);

            DBEarthCountryStatistics statistics = new DBEarthCountryStatistics("canada", 10L);
            statistics.refresh(250D, 120, 80, .75D, 20L);
            DaoManager.createDao(source, DBEarthCountryStatistics.class).create(statistics);

            DBEarthPropertyAccess access = new DBEarthPropertyAccess(propertyId, EarthPropertyAccessMode.PRIVATE, 10L);
            access.setLeaseOffer(40D, 11L);
            access.startLease(tenantId, 100L, 12L);
            DaoManager.createDao(source, DBEarthPropertyAccess.class).create(access);
            DBEarthPropertySaleOffer saleOffer = new DBEarthPropertySaleOffer(propertyId, founderId, 125D, 13L, 100L);
            DaoManager.createDao(source, DBEarthPropertySaleOffer.class).create(saleOffer);

            DBEarthPoliticalParty party = new DBEarthPoliticalParty("canada", "Civic Future", "health and infrastructure", founderId, 20L);
            DaoManager.createDao(source, DBEarthPoliticalParty.class).create(party);
            DaoManager.createDao(source, DBEarthPartyMembership.class).create(new DBEarthPartyMembership(leaderId, party.getId(), 21L));
            DBEarthGovernment government = new DBEarthGovernment("canada", "canada-government", "federal", 22L);
            government.installLeader(leaderId, party.getId(), 100L, 23L);
            DaoManager.createDao(source, DBEarthGovernment.class).create(government);
            DBEarthGovernmentAppointment appointment = new DBEarthGovernmentAppointment("canada",
                    EarthGovernmentPortfolio.HEALTH, founderId, leaderId, 24L);
            DaoManager.createDao(source, DBEarthGovernmentAppointment.class).create(appointment);
            DBEarthBill bill = new DBEarthBill("canada", EarthLawType.PUBLIC_SAFETY, leaderId, 24L);
            bill.resolve(EarthBillStatus.PASSED, 25L);
            DaoManager.createDao(source, DBEarthBill.class).create(bill);
        }

        try (JdbcConnectionSource reloadedSource = new JdbcConnectionSource(DATABASE_URL)) {
            Dao<DBEarthSimulationClock, String> clockDao = DaoManager.createDao(reloadedSource, DBEarthSimulationClock.class);
            Dao<DBEarthElectionCycle, String> electionDao = DaoManager.createDao(reloadedSource, DBEarthElectionCycle.class);
            Dao<DBEarthCountryStatistics, String> statisticsDao = DaoManager.createDao(reloadedSource, DBEarthCountryStatistics.class);
            Dao<DBEarthPropertyAccess, UUID> accessDao = DaoManager.createDao(reloadedSource, DBEarthPropertyAccess.class);
            Dao<DBEarthPropertySaleOffer, UUID> saleOfferDao = DaoManager.createDao(reloadedSource, DBEarthPropertySaleOffer.class);
            Dao<DBEarthGovernment, String> governmentDao = DaoManager.createDao(reloadedSource, DBEarthGovernment.class);
            Dao<DBEarthGovernmentAppointment, String> appointmentDao = DaoManager.createDao(reloadedSource, DBEarthGovernmentAppointment.class);
            Dao<DBEarthPartyMembership, UUID> membershipDao = DaoManager.createDao(reloadedSource, DBEarthPartyMembership.class);
            Dao<DBEarthBill, UUID> billDao = DaoManager.createDao(reloadedSource, DBEarthBill.class);
            DBEarthSimulationClock clock = clockDao.queryForId(DBEarthSimulationClock.GLOBAL_ID);
            DBEarthElectionCycle election = electionDao.queryForId("canada");
            DBEarthCountryStatistics statistics = statisticsDao.queryForId("canada");
            DBEarthPropertyAccess access = accessDao.queryForId(propertyId);
            DBEarthPropertySaleOffer saleOffer = saleOfferDao.queryForId(propertyId);
            DBEarthGovernment government = governmentDao.queryForId("canada");
            DBEarthGovernmentAppointment appointment = appointmentDao.queryForId("canada:health");
            DBEarthPartyMembership membership = membershipDao.queryForId(leaderId);
            DBEarthBill bill = billDao.queryForAll().getFirst();

            assertEquals(3D, clock.getSimulatedMonths(), .000001D);
            assertEquals(EarthElectionPhase.VOTING, election.getPhaseType());
            assertEquals(120, statistics.getPopulation());
            assertEquals(.75D, statistics.getEconomicState(), .000001D);
            assertTrue(access.hasActiveLease(99L));
            assertEquals(tenantId, access.getTenantId());
            assertEquals(founderId, saleOffer.getSellerId());
            assertEquals(125D, saleOffer.getPrice(), .000001D);
            assertTrue(saleOffer.isActive(99L));
            assertTrue(!saleOffer.isActive(100L));
            assertEquals(leaderId, government.getLeaderId());
            assertEquals(membership.getPartyId(), government.getRulingPartyId());
            assertEquals(EarthGovernmentPortfolio.HEALTH, appointment.getPortfolioType());
            assertEquals(founderId, appointment.getMinisterId());
            assertEquals(EarthBillStatus.PASSED, bill.getStatusType());
        }
    }

    private static void createTables(JdbcConnectionSource source) throws Exception {
        TableUtils.createTableIfNotExists(source, DBEarthSimulationClock.class);
        TableUtils.createTableIfNotExists(source, DBEarthElectionCycle.class);
        TableUtils.createTableIfNotExists(source, DBEarthCountryStatistics.class);
        TableUtils.createTableIfNotExists(source, DBEarthPropertyAccess.class);
        TableUtils.createTableIfNotExists(source, DBEarthPropertySaleOffer.class);
        TableUtils.createTableIfNotExists(source, DBEarthPoliticalParty.class);
        TableUtils.createTableIfNotExists(source, DBEarthPartyMembership.class);
        TableUtils.createTableIfNotExists(source, DBEarthGovernment.class);
        TableUtils.createTableIfNotExists(source, DBEarthGovernmentAppointment.class);
        TableUtils.createTableIfNotExists(source, DBEarthBill.class);
    }
}
