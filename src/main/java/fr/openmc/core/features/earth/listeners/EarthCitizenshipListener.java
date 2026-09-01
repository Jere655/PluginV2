package fr.openmc.core.features.earth.listeners;

import fr.openmc.core.features.earth.EarthManager;
import fr.openmc.core.features.homes.events.HomeCreateEvent;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

/** Assigns a configurable default nationality without touching city membership. */
public class EarthCitizenshipListener implements Listener {
    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        EarthManager.getInstance().grantDefaultCitizenship(event.getPlayer().getUniqueId());
    }

    @EventHandler
    public void onHomeCreate(HomeCreateEvent event) {
        EarthManager.getInstance().registerResidentialHome(event.getHome());
    }
}
