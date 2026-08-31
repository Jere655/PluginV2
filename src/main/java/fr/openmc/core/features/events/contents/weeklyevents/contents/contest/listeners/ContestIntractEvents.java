package fr.openmc.core.features.events.contents.weeklyevents.contents.contest.listeners;

import net.momirealms.craftengine.bukkit.api.event.FurnitureInteractEvent;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

import java.util.Objects;

public class ContestIntractEvents implements Listener {
    @EventHandler
    private void onFurnitureInteractEvent(FurnitureInteractEvent furniture) {
        if (Objects.equals(furniture.furniture().id().asString(), "omc_contest:borne")) {
            furniture.player().playSound(furniture.player().getLocation(), Sound.BLOCK_BARREL_OPEN, 1.0F, 0.7F);
            Bukkit.dispatchCommand(furniture.player(), "openmc:contest");
        }
    }
}
