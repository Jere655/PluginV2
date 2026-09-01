package fr.openmc.core.features.earth.integrations;

import fr.openmc.core.features.earth.models.DBEarthDemographics;
import fr.openmc.core.features.earth.models.DBEarthRegion;
import fr.openmc.core.features.earth.models.DBEarthRegionalIndicators;
import org.bukkit.entity.Player;

/** Optional dynamic-NPC bridge, called only for online players in active regions. */
@FunctionalInterface
public interface EarthNpcAdapter {
    void reconcile(Player nearbyPlayer, DBEarthRegion region, DBEarthDemographics demographics, DBEarthRegionalIndicators indicators);
}
