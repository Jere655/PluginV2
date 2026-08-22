package fr.openmc.core.listeners;

import org.bukkit.NamespacedKey;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.enchantment.EnchantItemEvent;
import org.bukkit.event.inventory.PrepareAnvilEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

/**
 * Applique les blacklists d'enchantements héritées d'ItemsAdder.
 *
 * <p>CraftEngine peut rendre un item totalement non-enchantable, mais ne fournit
 * pas une blacklist sélective par item. Le convertisseur conserve donc les noms
 * interdits dans le PDC {@code openmc:blocked_enchants} et ce listener reproduit
 * le comportement à la table d'enchantement et à l'enclume.</p>
 */
public final class BlockedEnchantListener implements Listener {
    private static final NamespacedKey BLOCKED_ENCHANTS = new NamespacedKey("openmc", "blocked_enchants");

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onEnchant(EnchantItemEvent event) {
        Set<String> blocked = blockedEnchants(event.getItem());
        if (blocked.isEmpty()) return;

        event.getEnchantsToAdd().keySet().removeIf(enchantment -> blocked.contains(enchantmentName(enchantment)));
        if (event.getEnchantsToAdd().isEmpty()) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPrepareAnvil(PrepareAnvilEvent event) {
        ItemStack base = event.getInventory().getItem(0);
        Set<String> blocked = blockedEnchants(base);
        if (blocked.isEmpty()) return;

        ItemStack result = event.getResult();
        if (result == null || result.getType().isAir()) return;

        ItemStack cleaned = result.clone();
        ItemMeta meta = cleaned.getItemMeta();
        boolean changed = false;

        for (Enchantment enchantment : new HashSet<>(meta.getEnchants().keySet())) {
            if (blocked.contains(enchantmentName(enchantment))) {
                meta.removeEnchant(enchantment);
                changed = true;
            }
        }

        if (changed) {
            cleaned.setItemMeta(meta);
            event.setResult(cleaned);
        }
    }

    private Set<String> blockedEnchants(ItemStack item) {
        if (item == null || item.getType().isAir() || !item.hasItemMeta()) return Set.of();

        PersistentDataContainer pdc = item.getItemMeta().getPersistentDataContainer();
        String raw = pdc.get(BLOCKED_ENCHANTS, PersistentDataType.STRING);
        if (raw == null || raw.isBlank()) return Set.of();

        Set<String> blocked = new HashSet<>();
        for (String value : raw.split(",")) {
            String normalized = value.trim().toUpperCase(Locale.ROOT);
            if (!normalized.isEmpty()) blocked.add(normalized);
        }
        return blocked;
    }

    @SuppressWarnings("deprecation")
    private String enchantmentName(Enchantment enchantment) {
        return enchantment.getKey().getKey().toUpperCase(Locale.ROOT);
    }
}
