package fr.openmc.core.features.earth.commands;

import fr.openmc.core.features.earth.EarthManager;
import fr.openmc.core.features.earth.geography.EarthLocationService;
import fr.openmc.core.features.earth.models.DBEarthCitizenship;
import fr.openmc.core.features.earth.models.DBEarthRegion;
import fr.openmc.core.features.earth.models.DBEarthRegionState;
import fr.openmc.core.features.earth.models.DBEarthRegionalIndicators;
import fr.openmc.core.features.earth.models.DBEarthRegionalLedger;
import fr.openmc.core.features.earth.models.DBEarthCountryPolicy;
import fr.openmc.core.features.earth.models.DBEarthJob;
import fr.openmc.core.features.earth.models.EarthOccupation;
import fr.openmc.core.features.earth.models.EarthLawType;
import fr.openmc.core.features.earth.models.DBEarthCitizenStatus;
import fr.openmc.core.features.earth.models.EarthRegionalEventType;
import fr.openmc.core.features.earth.models.EarthTransportMode;
import fr.openmc.core.features.earth.models.DBEarthDemographics;
import fr.openmc.core.features.earth.models.EarthDiplomaticStatus;
import fr.openmc.core.features.earth.models.DBEarthElectionCycle;
import fr.openmc.core.features.earth.models.DBEarthProperty;
import fr.openmc.core.features.earth.models.DBEarthPropertyAccess;
import fr.openmc.core.features.earth.models.EarthPropertyAccessMode;
import fr.openmc.core.features.earth.models.DBEarthSimulationClock;
import fr.openmc.core.features.earth.models.DBEarthCountryStatistics;
import fr.openmc.core.features.earth.models.DBEarthBill;
import fr.openmc.core.features.earth.models.EarthGovernmentPortfolio;
import fr.openmc.core.features.city.City;
import fr.openmc.core.features.city.CityManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.entity.Player;
import revxrsal.commands.annotation.Command;
import revxrsal.commands.annotation.Description;
import revxrsal.commands.annotation.Named;
import revxrsal.commands.annotation.Subcommand;
import revxrsal.commands.bukkit.annotation.CommandPermission;

import java.util.Optional;
import java.util.Locale;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Command("earth")
@CommandPermission(EarthPermissions.USE)
public class EarthCommands {
    @Subcommand("status")
    @Description("View the Earth simulation status for your current region")
    public void status(Player player) {
        EarthManager earth = EarthManager.getInstance();
        Optional<DBEarthRegion> region = EarthLocationService.getInstance().getRegion(player.getLocation());
        if (region.isEmpty()) {
            player.sendMessage(Component.text("This location is not assigned to an Earth region.", NamedTextColor.RED));
            return;
        }
        DBEarthRegion value = region.get();
        DBEarthRegionState state = earth.getRegionState(value.getId()).orElseThrow();
        player.sendMessage(Component.text(value.getName() + " • prosperity " + percent(state.getProsperity())
                + " • employment " + percent(state.getEmployment()) + " • satisfaction " + percent(state.getSatisfaction()), NamedTextColor.AQUA));
    }

    @Subcommand("conditions")
    @Description("View the non-economic conditions currently affecting your Earth region")
    public void conditions(Player player) {
        EarthManager earth = EarthManager.getInstance();
        earth.findRegion(player.getLocation().getChunk().getX(), player.getLocation().getChunk().getZ()).ifPresentOrElse(region -> {
            DBEarthRegionalIndicators indicators = earth.getRegionalIndicators(region.getId()).orElseThrow();
            player.sendMessage(Component.text(region.getName() + " conditions • health " + percent(indicators.getHealth())
                    + " • education " + percent(indicators.getEducation()) + " • safety " + percent(1D - indicators.getCrime())
                    + " • pollution " + percent(indicators.getPollution()) + " • infrastructure " + percent(indicators.getInfrastructure()), NamedTextColor.AQUA));
            player.sendMessage(Component.text("Food " + percent(indicators.getFoodSecurity()) + " • energy " + percent(indicators.getEnergy())
                    + " • inflation " + percent(indicators.getInflation()) + " • public approval " + percent(indicators.getApproval()), NamedTextColor.GRAY));
        }, () -> player.sendMessage(Component.text("This location is outside configured Earth regions.", NamedTextColor.RED)));
    }

    @Subcommand("whereami")
    @Description("Show the Earth region that contains your current chunk")
    public void whereAmI(Player player) {
        EarthManager earth = EarthManager.getInstance();
        EarthLocationService.getInstance().getRegion(player.getLocation())
                .ifPresentOrElse(region -> earth.getCountry(region.getCountryId()).ifPresentOrElse(country ->
                                player.sendMessage(Component.text(region.getName() + ", " + country.getName(), NamedTextColor.GREEN)),
                        () -> player.sendMessage(Component.text(region.getName(), NamedTextColor.GREEN))),
                        () -> player.sendMessage(Component.text("This location is outside configured Earth regions.", NamedTextColor.RED)));
    }

    @Subcommand("country")
    @Description("View persistent aggregate state for an Earth country")
    public void country(Player player, @Named("country id") String countryId) {
        EarthManager earth = EarthManager.getInstance();
        earth.getCountry(countryId).ifPresentOrElse(country -> {
            DBEarthCountryStatistics statistics = earth.getCountryStatistics(country.getId()).orElseThrow();
            player.sendMessage(Component.text(country.getName() + " • capital " + country.getCapital() + " • treasury " + round(statistics.getTreasury())
                    + " • population " + statistics.getPopulation() + " • citizens " + statistics.getCitizenCount()
                    + " • economic state " + percent(statistics.getEconomicState()), NamedTextColor.AQUA));
        }, () -> player.sendMessage(Component.text("Unknown Earth country: " + countryId, NamedTextColor.RED)));
    }

    @Subcommand("citizenship")
    @CommandPermission(EarthPermissions.CITIZENSHIP)
    @Description("Choose a configured country citizenship")
    public void citizenship(Player player, @Named("country id") String countryId) {
        if (!EarthManager.getInstance().chooseCitizenship(player.getUniqueId(), countryId)) {
            player.sendMessage(Component.text("Unknown Earth country: " + countryId, NamedTextColor.RED));
            return;
        }
        DBEarthCitizenship citizenship = EarthManager.getInstance().getCitizenship(player.getUniqueId()).orElseThrow();
        player.sendMessage(Component.text("Citizenship set to " + citizenship.getCountryId() + ".", NamedTextColor.GREEN));
    }

    @Subcommand("citizen")
    @Description("Show your Earth citizenship and civic standing")
    public void citizen(Player player) {
        EarthManager earth = EarthManager.getInstance();
        earth.grantDefaultCitizenship(player.getUniqueId());
        DBEarthCitizenship citizenship = earth.getCitizenship(player.getUniqueId()).orElseThrow();
        DBEarthCitizenStatus status = earth.getCitizenStatus(player.getUniqueId()).orElseThrow();
        player.sendMessage(Component.text("Citizen • " + citizenship.getCountryId() + " • reputation " + status.getReputation()
                + " • civic actions " + status.getCivicActions(), NamedTextColor.AQUA));
    }

    @Subcommand("civic volunteer")
    @CommandPermission(EarthPermissions.CIVIC)
    @Description("Volunteer in your current region to improve civic standing and local satisfaction")
    public void volunteer(Player player) {
        EarthManager earth = EarthManager.getInstance();
        earth.findRegion(player.getLocation().getChunk().getX(), player.getLocation().getChunk().getZ()).ifPresentOrElse(region -> {
            if (!earth.volunteer(player.getUniqueId(), region.getId())) {
                player.sendMessage(Component.text("You cannot volunteer again yet; check the civic cooldown.", NamedTextColor.YELLOW));
                return;
            }
            DBEarthCitizenStatus status = earth.getCitizenStatus(player.getUniqueId()).orElseThrow();
            player.sendMessage(Component.text("Civic contribution recorded • reputation " + status.getReputation(), NamedTextColor.GREEN));
        }, () -> player.sendMessage(Component.text("This location is outside configured Earth regions.", NamedTextColor.RED)));
    }

    @Subcommand("election")
    @Description("Show candidates and the elected officeholder for your country")
    public void election(Player player) {
        EarthManager earth = EarthManager.getInstance();
        DBEarthCitizenship citizenship = earth.getCitizenship(player.getUniqueId()).orElse(null);
        if (citizenship == null) { player.sendMessage(Component.text("Choose citizenship first.", NamedTextColor.RED)); return; }
        String country = citizenship.getCountryId();
        String candidates = earth.getElectionCandidates(country).stream().map(candidate -> candidate + earth.getElectionCandidate(country, candidate)
                .map(value -> value.getPartyId() == null ? "" : " [" + value.getPartyId() + "]").orElse("")).collect(Collectors.joining(", "));
        String office = earth.getOffice(country).map(value -> value.getOfficeholderId() + " (" + value.getVoteCount() + " votes)").orElse("vacant");
        DBEarthElectionCycle cycle = earth.getElectionCycle(country).orElseThrow();
        player.sendMessage(Component.text("Election • " + cycle.getPhaseType().name().toLowerCase(Locale.ROOT)
                + " until " + cycle.getPhaseEndsAt() + " • office " + office + " • candidates " + (candidates.isEmpty() ? "none" : candidates), NamedTextColor.BLUE));
    }

    @Subcommand("election nominate")
    @CommandPermission(EarthPermissions.ELECTION_NOMINATE)
    @Description("Nominate yourself for your country's election (requires 25 reputation)")
    public void nominate(Player player, @Named("country id") String countryId) {
        if (!EarthManager.getInstance().nominate(player.getUniqueId(), countryId)) {
            player.sendMessage(Component.text("Nomination failed: be a citizen of that country with 25 reputation, and nominate only once.", NamedTextColor.RED)); return;
        }
        player.sendMessage(Component.text("You are now a candidate for " + countryId + ".", NamedTextColor.GREEN));
    }

    @Subcommand("election vote")
    @CommandPermission(EarthPermissions.ELECTION_VOTE)
    @Description("Vote for a nominated candidate in your country")
    public void vote(Player player, @Named("country id") String countryId, @Named("candidate UUID") String candidateId) {
        try {
            if (!EarthManager.getInstance().vote(player.getUniqueId(), countryId, UUID.fromString(candidateId))) {
                player.sendMessage(Component.text("Vote failed: confirm your citizenship and candidate UUID.", NamedTextColor.RED)); return;
            }
            player.sendMessage(Component.text("Vote recorded. Your civic reputation increased.", NamedTextColor.GREEN));
        } catch (IllegalArgumentException exception) { player.sendMessage(Component.text("Candidate identifier must be a UUID.", NamedTextColor.RED)); }
    }

    @Subcommand("party")
    @Description("Show your current political-party affiliation")
    public void party(Player player) {
        EarthManager earth = EarthManager.getInstance();
        earth.getPartyMembership(player.getUniqueId()).flatMap(membership -> earth.getPoliticalParty(membership.getPartyId()))
                .ifPresentOrElse(value -> player.sendMessage(Component.text("Party • " + value.getName() + " • " + value.getCountryId()
                                + " • " + value.getPlatform(), NamedTextColor.LIGHT_PURPLE)),
                        () -> player.sendMessage(Component.text("You are not currently affiliated with an Earth political party.", NamedTextColor.YELLOW)));
    }

    @Subcommand("party create")
    @CommandPermission(EarthPermissions.PARTY_MANAGE)
    @Description("Found a country-scoped political party as a citizen")
    public void createParty(Player player, @Named("country id") String countryId, @Named("name") String name, @Named("platform") String platform) {
        EarthManager.getInstance().createPoliticalParty(player.getUniqueId(), countryId, name, platform).ifPresentOrElse(party ->
                        player.sendMessage(Component.text("Party founded • " + party.getName() + " (" + party.getId() + ")", NamedTextColor.GREEN)),
                () -> player.sendMessage(Component.text("Party creation failed: be a citizen, use a unique 1-40 character name, and provide a 1-160 character platform.", NamedTextColor.RED)));
    }

    @Subcommand("party join")
    @Description("Join an active political party in your citizenship country")
    public void joinParty(Player player, @Named("party id") String partyId) {
        if (!EarthManager.getInstance().joinPoliticalParty(player.getUniqueId(), partyId)) {
            player.sendMessage(Component.text("Party join failed: it may be inactive, unknown, or outside your citizenship country.", NamedTextColor.RED));
            return;
        }
        player.sendMessage(Component.text("Political-party affiliation updated.", NamedTextColor.GREEN));
    }

    @Subcommand("election close")
    @CommandPermission(EarthPermissions.ADMIN_ELECTION)
    @Description("Close a country election and install its deterministic winner")
    public void closeElection(Player player, @Named("country id") String countryId) {
        EarthManager.getInstance().closeElection(countryId).ifPresentOrElse(office ->
                        player.sendMessage(Component.text("Election closed • winner " + office.getOfficeholderId() + " with " + office.getVoteCount() + " votes.", NamedTextColor.GREEN)),
                () -> player.sendMessage(Component.text("No valid votes are available for that country.", NamedTextColor.RED)));
    }

    @Subcommand("events")
    @Description("Show active regional Earth events")
    public void events(Player player) {
        EarthManager earth = EarthManager.getInstance();
        earth.findRegion(player.getLocation().getChunk().getX(), player.getLocation().getChunk().getZ()).ifPresentOrElse(region -> {
            String active = earth.getActiveEvents(region.getId()).stream().map(event -> event.getType().name().toLowerCase(Locale.ROOT))
                    .collect(Collectors.joining(", "));
            player.sendMessage(Component.text(active.isEmpty() ? "No Earth regional events are active." : "Active regional events • " + active, NamedTextColor.LIGHT_PURPLE));
        }, () -> player.sendMessage(Component.text("This location is outside configured Earth regions.", NamedTextColor.RED)));
    }

    @Subcommand("event start")
    @CommandPermission(EarthPermissions.ADMIN_EVENT)
    @Description("Start a persisted Earth regional event for 1 to 1440 minutes")
    public void startEvent(Player player, @Named("region id") String regionId, @Named("event type") String typeName, @Named("minutes") long minutes) {
        EarthRegionalEventType type;
        try { type = EarthRegionalEventType.valueOf(typeName.toUpperCase(Locale.ROOT)); }
        catch (IllegalArgumentException exception) {
            player.sendMessage(Component.text("Event types: harvest_festival, civic_celebration, infrastructure_disruption, recession, pollution_crisis, strike, epidemic, food_shortage, energy_shortage, migration_wave.", NamedTextColor.RED)); return;
        }
        EarthManager.getInstance().startRegionalEvent(regionId, type, minutes, player.getUniqueId()).ifPresentOrElse(event ->
                        player.sendMessage(Component.text("Started " + event.getType().name().toLowerCase(Locale.ROOT) + " in " + regionId + ".", NamedTextColor.GREEN)),
                () -> player.sendMessage(Component.text("Invalid region or duration (1-1440 minutes).", NamedTextColor.RED)));
    }

    @Subcommand("city")
    @Description("Show the Earth region linked to your OpenMC city")
    public void city(Player player) {
        City city = CityManager.getPlayerCity(player.getUniqueId());
        if (city == null) {
            player.sendMessage(Component.text("You are not a member of an OpenMC city.", NamedTextColor.RED));
            return;
        }
        EarthManager.getInstance().getLinkedCityRegion(city.getUniqueId()).ifPresentOrElse(
                region -> player.sendMessage(Component.text(city.getName() + " belongs to " + region.getName() + ".", NamedTextColor.AQUA)),
                () -> player.sendMessage(Component.text(city.getName() + " has not been linked to an Earth region yet.", NamedTextColor.YELLOW)));
    }

    @Subcommand("property")
    @Description("List your Earth-registered properties")
    public void properties(Player player) {
        List<DBEarthProperty> properties = EarthManager.getInstance().getOwnedProperties(player.getUniqueId());
        if (properties.isEmpty()) {
            player.sendMessage(Component.text("You do not own an Earth-registered property yet. Homes and shops register when created or used.", NamedTextColor.YELLOW));
            return;
        }
        String entries = properties.stream().limit(8).map(property -> property.getPropertyId() + " (" + property.getType().name().toLowerCase(Locale.ROOT) + ")")
                .collect(Collectors.joining(", "));
        player.sendMessage(Component.text("Properties • " + entries + (properties.size() > 8 ? " …" : ""), NamedTextColor.AQUA));
    }

    @Subcommand("property info")
    @Description("View the access and lease state of an Earth property")
    public void propertyInfo(Player player, @Named("property UUID") String propertyId) {
        try {
            UUID id = UUID.fromString(propertyId);
            EarthManager earth = EarthManager.getInstance();
            DBEarthProperty property = earth.getProperty(id).orElse(null);
            DBEarthPropertyAccess access = earth.getPropertyAccess(id).orElse(null);
            if (property == null || access == null) { player.sendMessage(Component.text("Unknown Earth property.", NamedTextColor.RED)); return; }
            player.sendMessage(Component.text("Property • " + property.getType().name().toLowerCase(Locale.ROOT)
                    + " • access " + access.getAccessModeType().name().toLowerCase(Locale.ROOT)
                    + " • lease " + round(access.getLeasePrice()) + " • active tenant " + (access.hasActiveLease(System.currentTimeMillis()) ? access.getTenantId() : "none"), NamedTextColor.AQUA));
        } catch (IllegalArgumentException exception) { player.sendMessage(Component.text("Property identifier must be a UUID.", NamedTextColor.RED)); }
    }

    @Subcommand("property access")
    @CommandPermission(EarthPermissions.PROPERTY_MANAGE)
    @Description("Set the access intent for one of your Earth properties")
    public void propertyAccess(Player player, @Named("property UUID") String propertyId, @Named("mode") String modeName) {
        try {
            EarthPropertyAccessMode mode = EarthPropertyAccessMode.valueOf(modeName.toUpperCase(Locale.ROOT));
            if (!EarthManager.getInstance().configurePropertyAccess(player.getUniqueId(), UUID.fromString(propertyId), mode)) {
                player.sendMessage(Component.text("You can only configure a property you own.", NamedTextColor.RED)); return;
            }
            player.sendMessage(Component.text("Property access set to " + mode.name().toLowerCase(Locale.ROOT) + ".", NamedTextColor.GREEN));
        } catch (IllegalArgumentException exception) { player.sendMessage(Component.text("Modes: public, visitable, private, business, government.", NamedTextColor.RED)); }
    }

    @Subcommand("property lease-offer")
    @CommandPermission(EarthPermissions.PROPERTY_MANAGE)
    @Description("Set a one-time lease price for one of your Earth properties")
    public void propertyLeaseOffer(Player player, @Named("property UUID") String propertyId, @Named("price") double price) {
        try {
            if (!EarthManager.getInstance().offerPropertyLease(player.getUniqueId(), UUID.fromString(propertyId), price)) {
                player.sendMessage(Component.text("Lease offer failed: use a property you own and a non-negative price.", NamedTextColor.RED)); return;
            }
            player.sendMessage(Component.text("Lease offer set.", NamedTextColor.GREEN));
        } catch (IllegalArgumentException exception) { player.sendMessage(Component.text("Property identifier must be a UUID.", NamedTextColor.RED)); }
    }

    @Subcommand("property rent")
    @CommandPermission(EarthPermissions.PROPERTY_RENT)
    @Description("Rent an available Earth property for 1 to 43200 minutes")
    public void propertyRent(Player player, @Named("property UUID") String propertyId, @Named("minutes") long minutes) {
        try {
            if (!EarthManager.getInstance().rentProperty(player.getUniqueId(), UUID.fromString(propertyId), minutes)) {
                player.sendMessage(Component.text("Rental failed: the property may be occupied, unavailable, or you may lack funds.", NamedTextColor.RED)); return;
            }
            player.sendMessage(Component.text("Lease started. Existing claim and home protections remain in force.", NamedTextColor.GREEN));
        } catch (IllegalArgumentException exception) { player.sendMessage(Component.text("Property identifier must be a UUID.", NamedTextColor.RED)); }
    }

    @Subcommand("property sale-offer")
    @CommandPermission(EarthPermissions.PROPERTY_MANAGE)
    @Description("Offer one of your properties for sale for 1 to 43200 minutes")
    public void propertySaleOffer(Player player, @Named("property UUID") String propertyId, @Named("price") double price,
                                  @Named("minutes") long minutes) {
        try {
            EarthManager.getInstance().offerPropertySale(player.getUniqueId(), UUID.fromString(propertyId), price, minutes).ifPresentOrElse(
                    offer -> player.sendMessage(Component.text("Sale offer created for " + round(offer.getPrice()) + ". Buyer must use /earth property buy " + offer.getPropertyId() + ".", NamedTextColor.GREEN)),
                    () -> player.sendMessage(Component.text("Sale offer failed: use a property you own, non-negative price, and 1 to 43200 minutes.", NamedTextColor.RED)));
        } catch (IllegalArgumentException exception) { player.sendMessage(Component.text("Property identifier must be a UUID.", NamedTextColor.RED)); }
    }

    @Subcommand("property sale-withdraw")
    @CommandPermission(EarthPermissions.PROPERTY_MANAGE)
    @Description("Withdraw your unaccepted Earth property sale offer")
    public void withdrawPropertySale(Player player, @Named("property UUID") String propertyId) {
        try {
            if (!EarthManager.getInstance().withdrawPropertySale(player.getUniqueId(), UUID.fromString(propertyId))) {
                player.sendMessage(Component.text("No active sale offer for that property belongs to you.", NamedTextColor.RED));
                return;
            }
            player.sendMessage(Component.text("Property sale offer withdrawn.", NamedTextColor.YELLOW));
        } catch (IllegalArgumentException exception) { player.sendMessage(Component.text("Property identifier must be a UUID.", NamedTextColor.RED)); }
    }

    @Subcommand("property buy")
    @CommandPermission(EarthPermissions.PROPERTY_PURCHASE)
    @Description("Accept an active seller-created Earth property offer")
    public void buyProperty(Player player, @Named("property UUID") String propertyId) {
        try {
            if (!EarthManager.getInstance().acceptPropertySale(player.getUniqueId(), UUID.fromString(propertyId))) {
                player.sendMessage(Component.text("Purchase failed: the offer may be expired, withdrawn, unaffordable, or the property authority rejected transfer.", NamedTextColor.RED));
                return;
            }
            player.sendMessage(Component.text("Property purchase completed. Home/shop ownership and Earth access state were updated.", NamedTextColor.GREEN));
        } catch (IllegalArgumentException exception) { player.sendMessage(Component.text("Property identifier must be a UUID.", NamedTextColor.RED)); }
    }

    @Subcommand("economy")
    @Description("Show commercial activity awaiting the next regional budget cycle")
    public void economy(Player player) {
        EarthManager earth = EarthManager.getInstance();
        earth.findRegion(player.getLocation().getChunk().getX(), player.getLocation().getChunk().getZ()).ifPresentOrElse(region -> {
            DBEarthRegionalLedger ledger = earth.getRegionalLedger(region.getId()).orElseThrow();
            player.sendMessage(Component.text(region.getName() + " • pending sales " + ledger.getPendingSales()
                    + " • turnover " + round(ledger.getPendingTurnover()) + " • public tax " + round(ledger.getPendingTax()), NamedTextColor.GOLD));
        }, () -> player.sendMessage(Component.text("This location is outside configured Earth regions.", NamedTextColor.RED)));
    }

    @Subcommand("government")
    @Description("Show the public budget policy of your current Earth country")
    public void government(Player player) {
        EarthManager earth = EarthManager.getInstance();
        earth.findRegion(player.getLocation().getChunk().getX(), player.getLocation().getChunk().getZ()).ifPresentOrElse(region ->
                earth.getCountryPolicy(region.getCountryId()).ifPresentOrElse(policy -> {
                            String government = earth.getGovernment(region.getCountryId())
                                    .map(value -> value.getGovernmentType() + " • leader " + (value.getLeaderId() == null ? "vacant" : value.getLeaderId()))
                                    .orElse("government not initialized");
                            player.sendMessage(Component.text("Government • " + government, NamedTextColor.BLUE));
                            player.sendMessage(Component.text("Public budget • services " + percent(policy.getSocialServices())
                                    + " • infrastructure " + percent(policy.getInfrastructure())
                                    + " • investment " + percent(policy.getInvestment()), NamedTextColor.BLUE));
                        },
                        () -> player.sendMessage(Component.text("No country policy is configured for this region.", NamedTextColor.RED))),
                () -> player.sendMessage(Component.text("This location is outside configured Earth regions.", NamedTextColor.RED)));
    }

    @Subcommand("job")
    @Description("Show your Earth occupation")
    public void job(Player player) {
        EarthManager.getInstance().getJob(player.getUniqueId()).ifPresentOrElse(job ->
                        player.sendMessage(Component.text("Job • " + job.getOccupationType().name().toLowerCase(Locale.ROOT)
                                + " • region " + job.getRegionId() + " • work units " + job.getWorkUnits(), NamedTextColor.GREEN)),
                () -> player.sendMessage(Component.text("You do not have an Earth job. Use /earth job take <occupation>.", NamedTextColor.YELLOW)));
    }

    @Subcommand("job take")
    @CommandPermission(EarthPermissions.JOB_TAKE)
    @Description("Take a public, construction, or agriculture occupation in this region")
    public void takeJob(Player player, @Named("occupation") String occupationName) {
        EarthOccupation occupation;
        try {
            occupation = EarthOccupation.valueOf(occupationName.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException exception) {
            player.sendMessage(Component.text("Available jobs: civil_service, construction, agriculture, retail.", NamedTextColor.RED));
            return;
        }
        if (occupation == EarthOccupation.RETAIL) {
            player.sendMessage(Component.text("Retail jobs require /earth job take-retail <shop UUID>.", NamedTextColor.YELLOW));
            return;
        }
        takeJob(player, occupation, null);
    }

    @Subcommand("job take-retail")
    @CommandPermission(EarthPermissions.JOB_TAKE)
    @Description("Take a retail occupation with an existing Earth business")
    public void takeRetailJob(Player player, @Named("shop UUID") String shopId) {
        try {
            takeJob(player, EarthOccupation.RETAIL, UUID.fromString(shopId));
        } catch (IllegalArgumentException exception) {
            player.sendMessage(Component.text("The shop identifier must be a UUID.", NamedTextColor.RED));
        }
    }

    @Subcommand("job work")
    @CommandPermission(EarthPermissions.JOB_WORK)
    @Description("Complete one paid work unit at your current Earth workplace")
    public void work(Player player) {
        EarthManager earth = EarthManager.getInstance();
        earth.findRegion(player.getLocation().getChunk().getX(), player.getLocation().getChunk().getZ()).ifPresentOrElse(region ->
                        earth.recordWork(player.getUniqueId(), earth.resolveTravelRegion(player.getUniqueId(), region.getId())).ifPresentOrElse(result ->
                                        player.sendMessage(Component.text("Work completed • " + result.occupation().name().toLowerCase(Locale.ROOT)
                                                + " wage " + round(result.wage()) + " • regional production " + round(result.productivity()), NamedTextColor.GREEN)),
                                () -> player.sendMessage(Component.text("No local eligible job action is available yet; check your job and cooldown.", NamedTextColor.YELLOW))),
                () -> player.sendMessage(Component.text("This location is outside configured Earth regions.", NamedTextColor.RED)));
    }

    @Subcommand("routes")
    @Description("Show available transport routes from your current region")
    public void routes(Player player) {
        EarthManager earth = EarthManager.getInstance();
        earth.findRegion(player.getLocation().getChunk().getX(), player.getLocation().getChunk().getZ()).ifPresentOrElse(region -> {
            String routes = earth.getRoutesFrom(region.getId()).stream().map(route -> route.getRouteId() + " → " + route.getToRegion() + " (" + round(route.getFare()) + ")").collect(Collectors.joining(" | "));
            player.sendMessage(Component.text(routes.isEmpty() ? "No transport routes depart this region." : routes, NamedTextColor.GOLD));
        }, () -> player.sendMessage(Component.text("This location is outside configured Earth regions.", NamedTextColor.RED)));
    }

    @Subcommand("population")
    @Description("Show aggregate NPC demographics for your current region")
    public void population(Player player) {
        EarthManager earth = EarthManager.getInstance();
        earth.findRegion(player.getLocation().getChunk().getX(), player.getLocation().getChunk().getZ()).ifPresentOrElse(region -> {
            DBEarthDemographics population = earth.getDemographics(region.getId()).orElseThrow();
            player.sendMessage(Component.text("Population • residents " + population.getResidents() + " • workforce " + population.getWorkforce()
                    + " • housing pressure " + percent(population.getHousingPressure() / 2D), NamedTextColor.DARK_AQUA));
        }, () -> player.sendMessage(Component.text("This location is outside configured Earth regions.", NamedTextColor.RED)));
    }

    @Subcommand("population set")
    @CommandPermission(EarthPermissions.ADMIN_POPULATION)
    @Description("Set initial aggregate NPC residents and workforce for a region")
    public void setPopulation(Player player, @Named("region") String regionId, @Named("residents") int residents, @Named("workforce") int workforce) {
        if (!EarthManager.getInstance().configureDemographics(regionId, residents, workforce)) {
            player.sendMessage(Component.text("Invalid region or population values.", NamedTextColor.RED)); return;
        }
        player.sendMessage(Component.text("Population configured for " + regionId + ".", NamedTextColor.GREEN));
    }

    @Subcommand("diplomacy")
    @Description("Show the diplomatic relations of your current country")
    public void diplomacy(Player player) {
        EarthManager earth=EarthManager.getInstance();
        DBEarthCitizenship citizenship=earth.getCitizenship(player.getUniqueId()).orElse(null);
        if(citizenship==null){player.sendMessage(Component.text("Choose citizenship first.",NamedTextColor.RED));return;}
        String relations=earth.getRelations(citizenship.getCountryId()).stream().map(relation -> relation.getCountryA()+"/"+relation.getCountryB()+" "+relation.getStatusType().name().toLowerCase(Locale.ROOT)).collect(Collectors.joining(", "));
        player.sendMessage(Component.text(relations.isEmpty()?"No bilateral relations are configured.":relations,NamedTextColor.BLUE));
    }

    @Subcommand("diplomacy set")
    @CommandPermission(EarthPermissions.ADMIN_DIPLOMACY)
    @Description("Set a persistent bilateral diplomatic status")
    public void setDiplomacy(Player player,@Named("country A") String countryA,@Named("country B") String countryB,@Named("status") String statusName){
        try { EarthDiplomaticStatus status=EarthDiplomaticStatus.valueOf(statusName.toUpperCase(Locale.ROOT));
            if(!EarthManager.getInstance().setDiplomaticRelation(countryA,countryB,status)){player.sendMessage(Component.text("Invalid countries or identical country pair.",NamedTextColor.RED));return;}
            player.sendMessage(Component.text("Diplomatic relation saved.",NamedTextColor.GREEN));
        } catch(IllegalArgumentException exception){player.sendMessage(Component.text("Statuses: allied, neutral, tense, sanctioned.",NamedTextColor.RED));}
    }

    @Subcommand("diagnose")
    @CommandPermission(EarthPermissions.ADMIN_DIAGNOSE)
    @Description("Show operational counts for the persistent Earth simulation")
    public void diagnose(Player player) {
        EarthManager.OperationalSnapshot snapshot = EarthManager.getInstance().getOperationalSnapshot();
        player.sendMessage(Component.text("Earth diagnostics • countries " + snapshot.countries() + " • regions " + snapshot.regions()
                + " • properties " + snapshot.properties() + " • businesses " + snapshot.businesses() + " • jobs " + snapshot.jobs()
                + " • citizens " + snapshot.citizens() + " • events " + snapshot.events() + " • routes " + snapshot.routes()
                + " • waypoints " + snapshot.waypoints() + " • relations " + snapshot.relations() + " • travel passes " + snapshot.activeTravelPasses(), NamedTextColor.GRAY));
    }

    @Subcommand("sim status")
    @Description("Show the accelerated Earth simulation calendar")
    public void simulationStatus(Player player) {
        DBEarthSimulationClock clock = EarthManager.getInstance().getSimulationClock();
        long completedMonths = (long) Math.floor(clock.getSimulatedMonths());
        player.sendMessage(Component.text("Earth calendar • year " + (completedMonths / 12L + 1L)
                + " • month " + (completedMonths % 12L + 1L) + " • total simulated months " + round(clock.getSimulatedMonths()), NamedTextColor.AQUA));
    }

    @Subcommand("sim tick")
    @CommandPermission(EarthPermissions.ADMIN_SIMULATION)
    @Description("Run one bounded Earth simulation cycle for operational recovery")
    public void simulationTick(Player player) {
        EarthManager.getInstance().simulateOnce();
        player.sendMessage(Component.text("Earth simulation cycle completed.", NamedTextColor.GREEN));
    }

    @Subcommand("travel")
    @CommandPermission(EarthPermissions.TRAVEL)
    @Description("Buy a route pass that permits destination-region work until expiry")
    public void travel(Player player, @Named("route UUID") String routeId) {
        try {
            UUID id = UUID.fromString(routeId);
            EarthManager earth = EarthManager.getInstance();
            earth.findRegion(player.getLocation().getChunk().getX(), player.getLocation().getChunk().getZ()).ifPresentOrElse(region ->
                    earth.travel(player.getUniqueId(), region.getId(), id).ifPresentOrElse(pass -> {
                                boolean arrived = earth.getWaypoint(pass.getDestinationRegion()).map(player::teleport).orElse(false);
                                player.sendMessage(Component.text(arrived ? "Arrived in " + pass.getDestinationRegion() + "." : "Travel pass issued for " + pass.getDestinationRegion() + "; no arrival waypoint is configured yet.", NamedTextColor.GREEN));
                            },
                            () -> player.sendMessage(Component.text("Route unavailable here or insufficient funds.", NamedTextColor.RED))),
                    () -> player.sendMessage(Component.text("This location is outside configured Earth regions.", NamedTextColor.RED)));
        } catch (IllegalArgumentException exception) { player.sendMessage(Component.text("Route identifier must be a UUID.", NamedTextColor.RED)); }
    }

    @Subcommand("route create")
    @CommandPermission(EarthPermissions.ADMIN_TRANSPORT)
    @Description("Create an Earth transport route")
    public void createRoute(Player player, @Named("from") String from, @Named("to") String to, @Named("mode") String modeName, @Named("fare") double fare, @Named("minutes") long minutes) {
        try {
            EarthTransportMode mode = EarthTransportMode.valueOf(modeName.toUpperCase(Locale.ROOT));
            EarthManager.getInstance().createRoute(from, to, mode, fare, minutes).ifPresentOrElse(route -> player.sendMessage(Component.text("Route created: " + route.getRouteId(), NamedTextColor.GREEN)),
                    () -> player.sendMessage(Component.text("Invalid route configuration.", NamedTextColor.RED)));
        } catch (IllegalArgumentException exception) { player.sendMessage(Component.text("Modes: road, rail, air, water.", NamedTextColor.RED)); }
    }

    @Subcommand("waypoint set")
    @CommandPermission(EarthPermissions.ADMIN_TRANSPORT)
    @Description("Set the safe arrival waypoint for an Earth region at your current location")
    public void setWaypoint(Player player, @Named("region") String regionId) {
        if (!EarthManager.getInstance().setWaypoint(regionId, player.getLocation())) {
            player.sendMessage(Component.text("Unknown Earth region or invalid location.", NamedTextColor.RED)); return;
        }
        player.sendMessage(Component.text("Arrival waypoint saved for " + regionId + ".", NamedTextColor.GREEN));
    }

    @Subcommand("policy")
    @CommandPermission(EarthPermissions.GOVERNMENT_MANAGE)
    @Description("Set a country's public budget allocation weights")
    public void policy(Player player, @Named("country id") String countryId, @Named("social services") double socialServices,
                       @Named("infrastructure") double infrastructure, @Named("investment") double investment) {
        if (!canManageGovernment(player, countryId)) {
            player.sendMessage(Component.text("Only the elected country leader or an Earth administrator can change this policy.", NamedTextColor.RED));
            return;
        }
        if (!EarthManager.getInstance().configureCountryPolicy(countryId, socialServices, infrastructure, investment)) {
            player.sendMessage(Component.text("Invalid country or budget weights.", NamedTextColor.RED));
            return;
        }
        DBEarthCountryPolicy policy = EarthManager.getInstance().getCountryPolicy(countryId).orElseThrow();
        player.sendMessage(Component.text("Policy saved • services " + percent(policy.getSocialServices())
                + " • infrastructure " + percent(policy.getInfrastructure()) + " • investment " + percent(policy.getInvestment()), NamedTextColor.GREEN));
    }

    @Subcommand("government ministries")
    @Description("Show the appointed national portfolios for your current Earth country")
    public void ministries(Player player) {
        EarthManager earth = EarthManager.getInstance();
        earth.findRegion(player.getLocation().getChunk().getX(), player.getLocation().getChunk().getZ()).ifPresentOrElse(region -> {
            String entries = earth.getGovernmentAppointments(region.getCountryId()).values().stream()
                    .map(appointment -> appointment.getPortfolioType().name().toLowerCase(Locale.ROOT) + " → " + appointment.getMinisterId())
                    .sorted().collect(Collectors.joining(" • "));
            player.sendMessage(Component.text(entries.isEmpty() ? "No national portfolios have been appointed." : "Cabinet • " + entries, NamedTextColor.BLUE));
        }, () -> player.sendMessage(Component.text("This location is outside configured Earth regions.", NamedTextColor.RED)));
    }

    @Subcommand("government appoint")
    @CommandPermission(EarthPermissions.GOVERNMENT_MANAGE)
    @Description("Appoint a same-country citizen to a national portfolio")
    public void appointMinister(Player player, @Named("country id") String countryId, @Named("portfolio") String portfolioName,
                                @Named("citizen UUID") String ministerId) {
        EarthGovernmentPortfolio portfolio;
        UUID minister;
        try {
            portfolio = EarthGovernmentPortfolio.valueOf(portfolioName.toUpperCase(Locale.ROOT));
            minister = UUID.fromString(ministerId);
        } catch (IllegalArgumentException exception) {
            player.sendMessage(Component.text("Portfolios: finance, health, education, infrastructure, environment, public_safety, labour, foreign_affairs. Citizen must be a UUID.", NamedTextColor.RED));
            return;
        }
        EarthManager.getInstance().appointMinister(player.getUniqueId(), countryId, portfolio, minister).ifPresentOrElse(
                appointment -> player.sendMessage(Component.text("Appointed " + appointment.getMinisterId() + " as " + appointment.getPortfolioType().name().toLowerCase(Locale.ROOT) + ".", NamedTextColor.GREEN)),
                () -> player.sendMessage(Component.text("Only the current elected leader can appoint a citizen of that active country.", NamedTextColor.RED)));
    }

    @Subcommand("policy preview")
    @CommandPermission(EarthPermissions.USE)
    @Description("Estimate the next-cycle impact of a proposed country budget before saving it")
    public void previewPolicy(Player player, @Named("country id") String countryId, @Named("social services") double socialServices,
                              @Named("infrastructure") double infrastructure, @Named("investment") double investment) {
        EarthManager.getInstance().previewCountryPolicy(countryId, socialServices, infrastructure, investment).ifPresentOrElse(estimate ->
                        player.sendMessage(Component.text("Estimated next-cycle impact • health " + signedPercent(estimate.healthChange())
                                + " • infrastructure " + signedPercent(estimate.infrastructureChange()) + " • employment " + signedPercent(estimate.employmentChange())
                                + " • pollution " + signedPercent(estimate.pollutionChange()) + " • approval " + signedPercent(estimate.approvalChange())
                                + " • treasury pressure " + signedPercent(estimate.treasuryPressure()), NamedTextColor.AQUA)),
                () -> player.sendMessage(Component.text("Invalid country or budget weights.", NamedTextColor.RED)));
    }

    @Subcommand("law")
    @Description("Show country laws affecting the current Earth region")
    public void law(Player player) {
        EarthManager earth = EarthManager.getInstance();
        earth.findRegion(player.getLocation().getChunk().getX(), player.getLocation().getChunk().getZ()).ifPresentOrElse(region -> {
            String laws = earth.getActiveLaws(region.getCountryId()).stream().map(law -> law.name().toLowerCase(Locale.ROOT))
                    .sorted().collect(Collectors.joining(", "));
            player.sendMessage(Component.text(laws.isEmpty() ? "No national Earth laws are active." : "Active laws • " + laws, NamedTextColor.BLUE));
        }, () -> player.sendMessage(Component.text("This location is outside configured Earth regions.", NamedTextColor.RED)));
    }

    @Subcommand("law enact")
    @CommandPermission(EarthPermissions.ADMIN_GOVERNMENT)
    @Description("Emergency administrator activation of a country law; elected leaders use bills")
    public void enactLaw(Player player, @Named("country id") String countryId, @Named("law") String lawName) {
        if (!player.hasPermission(EarthPermissions.ADMIN_GOVERNMENT)) {
            player.sendMessage(Component.text("Elected leaders enact laws by proposing and passing an Earth bill.", NamedTextColor.RED));
            return;
        }
        EarthLawType law = parseLaw(player, lawName);
        if (law == null) return;
        if (!EarthManager.getInstance().enactLaw(countryId, law, player.getUniqueId())) {
            player.sendMessage(Component.text("That law is already active or the country is unknown.", NamedTextColor.RED));
            return;
        }
        player.sendMessage(Component.text("Administrator emergency activation: " + law.name().toLowerCase(Locale.ROOT) + " for " + countryId + ".", NamedTextColor.GREEN));
    }

    @Subcommand("law repeal")
    @CommandPermission(EarthPermissions.GOVERNMENT_MANAGE)
    @Description("Repeal an active country law")
    public void repealLaw(Player player, @Named("country id") String countryId, @Named("law") String lawName) {
        if (!canManageGovernment(player, countryId)) {
            player.sendMessage(Component.text("Only the elected country leader or an Earth administrator can repeal laws.", NamedTextColor.RED));
            return;
        }
        EarthLawType law = parseLaw(player, lawName);
        if (law == null) return;
        if (!EarthManager.getInstance().repealLaw(countryId, law)) {
            player.sendMessage(Component.text("That law is not active for this country.", NamedTextColor.RED));
            return;
        }
        player.sendMessage(Component.text("Repealed " + law.name().toLowerCase(Locale.ROOT) + " for " + countryId + ".", NamedTextColor.YELLOW));
    }

    @Subcommand("bill")
    @Description("Show open country bills for your current Earth region")
    public void bills(Player player) {
        EarthManager earth = EarthManager.getInstance();
        earth.findRegion(player.getLocation().getChunk().getX(), player.getLocation().getChunk().getZ()).ifPresentOrElse(region -> {
            String entries = earth.getOpenBills(region.getCountryId()).stream()
                    .map(bill -> bill.getBillId() + " (" + bill.getLawType().name().toLowerCase(Locale.ROOT) + ")")
                    .collect(Collectors.joining(", "));
            player.sendMessage(Component.text(entries.isEmpty() ? "No country bills are currently open." : "Open bills • " + entries, NamedTextColor.BLUE));
        }, () -> player.sendMessage(Component.text("This location is outside configured Earth regions.", NamedTextColor.RED)));
    }

    @Subcommand("bill propose")
    @CommandPermission(EarthPermissions.GOVERNMENT_MANAGE)
    @Description("Propose a law bill for your elected government to pass")
    public void proposeBill(Player player, @Named("country id") String countryId, @Named("law") String lawName) {
        if (!canManageGovernment(player, countryId)) {
            player.sendMessage(Component.text("Only the elected country leader or an Earth administrator can propose bills.", NamedTextColor.RED));
            return;
        }
        EarthLawType law = parseLaw(player, lawName);
        if (law == null) return;
        EarthManager.getInstance().proposeBill(player.getUniqueId(), countryId, law).ifPresentOrElse(bill ->
                        player.sendMessage(Component.text("Bill proposed • " + bill.getBillId(), NamedTextColor.GREEN)),
                () -> player.sendMessage(Component.text("A bill or active law already covers that policy, or the country is inactive.", NamedTextColor.RED)));
    }

    @Subcommand("bill pass")
    @CommandPermission(EarthPermissions.GOVERNMENT_MANAGE)
    @Description("Pass an open bill, activating its law's simulation effects")
    public void passBill(Player player, @Named("country id") String countryId, @Named("bill UUID") String billId) {
        if (!canManageGovernment(player, countryId)) {
            player.sendMessage(Component.text("Only the elected country leader or an Earth administrator can pass bills.", NamedTextColor.RED));
            return;
        }
        try {
            UUID id = UUID.fromString(billId);
            DBEarthBill bill = EarthManager.getInstance().getOpenBills(countryId).stream().filter(value -> value.getBillId().equals(id)).findFirst().orElse(null);
            if (bill == null || !EarthManager.getInstance().passBill(id)) {
                player.sendMessage(Component.text("That open bill could not be passed.", NamedTextColor.RED));
                return;
            }
            player.sendMessage(Component.text("Bill passed • " + bill.getLawType().name().toLowerCase(Locale.ROOT) + " is now active.", NamedTextColor.GREEN));
        } catch (IllegalArgumentException exception) {
            player.sendMessage(Component.text("Bill identifier must be a UUID.", NamedTextColor.RED));
        }
    }

    @Subcommand("link-city")
    @CommandPermission(EarthPermissions.ADMIN_CITY)
    @Description("Link an existing OpenMC city to an Earth region")
    public void linkCity(Player player, @Named("city") String cityName, @Named("region id") String regionId) {
        City city = CityManager.getCityByName(cityName);
        if (city == null) {
            player.sendMessage(Component.text("Unknown OpenMC city: " + cityName, NamedTextColor.RED));
            return;
        }
        if (!EarthManager.getInstance().linkCityToRegion(city.getUniqueId(), regionId)) {
            player.sendMessage(Component.text("Unknown Earth region: " + regionId, NamedTextColor.RED));
            return;
        }
        player.sendMessage(Component.text(city.getName() + " is now linked to " + regionId + ".", NamedTextColor.GREEN));
    }

    private static String percent(double value) {
        return Math.round(value * 100D) + "%";
    }

    private static String round(double value) {
        return String.format(java.util.Locale.ROOT, "%.2f", value);
    }

    private static String signedPercent(double value) {
        return String.format(java.util.Locale.ROOT, "%+.1f%%", value * 100D);
    }

    private static void takeJob(Player player, EarthOccupation occupation, UUID employerShopId) {
        EarthManager earth = EarthManager.getInstance();
        earth.findRegion(player.getLocation().getChunk().getX(), player.getLocation().getChunk().getZ()).ifPresentOrElse(region -> {
            if (!earth.takeJob(player.getUniqueId(), occupation, region.getId(), employerShopId)) {
                player.sendMessage(Component.text("That occupation is not available in this region.", NamedTextColor.RED));
                return;
            }
            player.sendMessage(Component.text("You now work in " + occupation.name().toLowerCase(Locale.ROOT) + ".", NamedTextColor.GREEN));
        }, () -> player.sendMessage(Component.text("This location is outside configured Earth regions.", NamedTextColor.RED)));
    }

    private static EarthLawType parseLaw(Player player, String lawName) {
        try {
            return EarthLawType.valueOf(lawName.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException exception) {
            player.sendMessage(Component.text("Available laws: business_incentive, worker_protection, public_safety, green_infrastructure.", NamedTextColor.RED));
            return null;
        }
    }

    private static boolean canManageGovernment(Player player, String countryId) {
        return player.hasPermission(EarthPermissions.ADMIN_GOVERNMENT)
                || EarthManager.getInstance().isGovernmentLeader(player.getUniqueId(), countryId);
    }
}
