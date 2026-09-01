package fr.openmc.core.features.earth.geography;

import fr.openmc.core.features.city.City;
import fr.openmc.core.features.city.CityManager;
import fr.openmc.core.features.earth.EarthManager;
import fr.openmc.core.features.earth.models.DBEarthCountry;
import fr.openmc.core.features.earth.models.DBEarthRegion;
import org.bukkit.Location;

import java.util.Optional;

/**
 * OpenMC-owned location boundary for all Earth gameplay. It deliberately
 * exposes configured region/country/city identity without any external map API.
 */
public final class EarthLocationService {
    private static final EarthLocationService INSTANCE = new EarthLocationService();

    private EarthLocationService() { }

    public static EarthLocationService getInstance() { return INSTANCE; }

    public Optional<DBEarthCountry> getCountry(Location location) {
        return getRegion(location).flatMap(region -> EarthManager.getInstance().getCountry(region.getCountryId()));
    }

    public Optional<DBEarthRegion> getRegion(Location location) {
        if (location == null) return Optional.empty();
        return EarthManager.getInstance().findRegion(location.getChunk().getX(), location.getChunk().getZ());
    }

    public Optional<DBEarthCountry> getCountry(String countryId) {
        return countryId == null ? Optional.empty() : EarthManager.getInstance().getCountry(countryId);
    }

    public Optional<DBEarthRegion> getRegion(String regionId) {
        return regionId == null ? Optional.empty() : EarthManager.getInstance().getRegion(regionId);
    }

    /**
     * Converts Minecraft block coordinates into the configured Earth-projected
     * metres used by map/import adapters. Gameplay lookup continues to use
     * persisted region chunk bounds, so scale changes do not silently move
     * existing claims or citizens.
     */
    public EarthCoordinates project(Location location) {
        if (location == null) return new EarthCoordinates(0D, 0D);
        return project(location.getX(), location.getZ(), EarthManager.getInstance().getEarthScaleMetersPerBlock());
    }

    public static EarthCoordinates project(double blockX, double blockZ, double metresPerBlock) {
        double scale = Math.max(0.001D, metresPerBlock);
        return new EarthCoordinates(blockX * scale, blockZ * scale);
    }

    /** Returns an existing OpenMC city only when it is linked to the containing Earth region. */
    public Optional<EarthCityLocation> getCity(Location location) {
        if (location == null) return Optional.empty();
        City city = CityManager.getCityFromChunk(location.getChunk().getX(), location.getChunk().getZ());
        if (city == null) return Optional.empty();
        return EarthManager.getInstance().getLinkedCityRegion(city.getUniqueId())
                .filter(region -> region.containsChunk(location.getChunk().getX(), location.getChunk().getZ()))
                .flatMap(region -> EarthManager.getInstance().getCountry(region.getCountryId())
                        .map(country -> new EarthCityLocation(city, region, country)));
    }

    public record EarthCityLocation(City city, DBEarthRegion region, DBEarthCountry country) { }
    public record EarthCoordinates(double eastMetres, double northMetres) { }
}
