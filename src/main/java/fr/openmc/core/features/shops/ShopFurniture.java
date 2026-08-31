package fr.openmc.core.features.shops;

import fr.openmc.core.OMCRegistry;
import fr.openmc.core.hooks.craftengine.OpenMCContent;
import fr.openmc.core.utils.world.Yaw;
import org.bukkit.Material;
import org.bukkit.block.Block;

public class ShopFurniture {
	
	/**
     * Places a specific type of shop furniture at the given block location using CraftEngine, oriented
	 * according to the player's yaw direction.
	 *
	 * @param block The block where the shop furniture should be placed. Must be of type AIR.
	 * @param playerYaw The yaw direction of the player, used to determine the orientation
	 *                  of the furniture.
	 * @return true if the furniture was successfully placed, false otherwise.
	 */
	public static boolean placeShopFurniture(Block block, Yaw playerYaw) {
        if (block.getType() != Material.AIR) return false;
        var furniture = OpenMCContent.placeFurniture(block.getLocation(), OMCRegistry.CUSTOM_ITEMS.CAISSE.getId());
        if (furniture == null || furniture.bukkitEntity() == null) return false;

        furniture.bukkitEntity().setRotation(playerYaw.getPlayerYaw(), 0);
		return true;
	}
	
	/**
     * Removes a specific type of shop furniture at the given block location using CraftEngine.
	 *
	 * @param block The block where the shop furniture is placed.
	 * @return true if the furniture was successfully removed, false otherwise.
	 */
	public static boolean removeShopFurniture(Block block) {
        var placed = OpenMCContent.furnitureAt(block.getLocation());
        if (placed == null || !placed.id().asString().equals(OMCRegistry.CUSTOM_ITEMS.CAISSE.getId())) return false;
        return OpenMCContent.removeFurniture(placed);
	}
	
	/**
	 * Checks if the specified block contains a specific type of shop furniture.
	 *
	 * @param block The block to check for shop furniture. Must not be null.
	 * @return true if the block contains the shop furniture, false otherwise.
	 */
	public static boolean hasFurniture(Block block) {
        var placed = OpenMCContent.furnitureAt(block.getLocation());
        return placed != null && placed.id().asString().equals(OMCRegistry.CUSTOM_ITEMS.CAISSE.getId());
	}
	
}
