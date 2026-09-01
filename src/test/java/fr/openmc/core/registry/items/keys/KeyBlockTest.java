package fr.openmc.core.registry.items.keys;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class KeyBlockTest {
    private static final String MISSING_ID = "omc_blocks:definitely_missing_block";

    @Test
    @DisplayName("name() ne casse pas quand le bloc custom est absent du registre CraftEngine")
    void testNameWithMissingCustomBlock() {
        KeyBlock keyBlock = KeyBlock.custom(MISSING_ID);

        Component name = Assertions.assertDoesNotThrow(keyBlock::name);

        Assertions.assertEquals(MISSING_ID, PlainTextComponentSerializer.plainText().serialize(name));
        Assertions.assertTrue(keyBlock.isCustom());
        Assertions.assertEquals(MISSING_ID, keyBlock.getNamespacedID());
        Assertions.assertNull(keyBlock.getCustomItem());
        Assertions.assertTrue(KeyBlock.getKnownCustomIDs().contains(MISSING_ID));
    }

    @Test
    @DisplayName("matches() renvoie false au lieu de lever quand le bloc est inconnu")
    void testMatchesWithoutItemsAdder() {
        KeyBlock keyBlock = KeyBlock.custom(MISSING_ID);

        Assertions.assertFalse(keyBlock.matches(null));
    }
}
