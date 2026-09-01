package fr.openmc.core.features.earth.integrations;

import java.util.Collection;

/** Optional map bridge. Core Earth gameplay never depends on a map plugin. */
@FunctionalInterface
public interface EarthMapAdapter {
    void publish(Collection<RegionOverlay> regions);

    record RegionOverlay(String regionId, String countryId, String name, int minChunkX, int maxChunkX, int minChunkZ, int maxChunkZ,
                         double prosperity, double pollution, double approval, double infrastructure) { }
}
