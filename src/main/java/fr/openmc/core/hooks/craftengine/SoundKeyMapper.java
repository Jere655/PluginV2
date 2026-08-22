package fr.openmc.core.hooks.craftengine;

import io.papermc.paper.registry.RegistryAccess;
import io.papermc.paper.registry.RegistryKey;
import net.kyori.adventure.key.Key;
import org.bukkit.Sound;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * Traduit les noms de sons ItemsAdder (BLOCK_STONE_BREAK, block.wood.place) en clés Minecraft.
 */
@SuppressWarnings({"UnstableApiUsage", "removal"})
public final class SoundKeyMapper {
    private static Map<String, String> legacyNames;

    private SoundKeyMapper() {
    }

    /**
     * @param itemsAdderName le nom du son tel qu'écrit dans les contents ItemsAdder
     * @return la clé du son au format minecraft:block.stone.break
     */
    public static String toKey(String itemsAdderName) {
        String name = itemsAdderName.trim();

        if (name.contains(":")) return name.toLowerCase(Locale.ROOT);
        if (name.contains(".")) return "minecraft:" + name.toLowerCase(Locale.ROOT);

        String fromRegistry = legacyNames().get(name.toUpperCase(Locale.ROOT));
        if (fromRegistry != null) return fromRegistry;

        return "minecraft:" + name.toLowerCase(Locale.ROOT).replace('_', '.');
    }

    private static Map<String, String> legacyNames() {
        if (legacyNames != null) return legacyNames;

        Map<String, String> names = new HashMap<>();
        try {
            for (Sound sound : RegistryAccess.registryAccess().getRegistry(RegistryKey.SOUND_EVENT)) {
                Key key = sound.key();
                names.put(key.value().replace('.', '_').toUpperCase(Locale.ROOT), key.asString());
            }
        } catch (Exception ignored) {
            // * Le registre n'est pas encore disponible : on retombe sur la conversion naive
        }

        legacyNames = names;
        return legacyNames;
    }
}
