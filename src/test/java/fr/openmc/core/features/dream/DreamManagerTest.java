package fr.openmc.core.features.dream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;

import java.util.UUID;

import org.junit.jupiter.api.Test;

import fr.openmc.core.features.dream.models.db.DBDreamPlayer;

public class DreamManagerTest {

    @Test
    public void testCreatesDreamPlayerDataWhenCacheIsEmpty() {
        UUID playerUUID = UUID.randomUUID();

        DBDreamPlayer created = DreamManager.updateOrCreateDreamPlayerData(
                playerUUID, null, DreamManager.BASE_DREAM_TIME, "inventory", 10.0, 70.0, -20.0);

        assertNotNull(created);
        assertEquals(playerUUID, created.getPlayerUUID());
        assertEquals(DreamManager.BASE_DREAM_TIME, created.getMaxDreamTime());
        assertEquals("inventory", created.getDreamInventory());
        assertEquals(10.0, created.getDreamX());
        assertEquals(70.0, created.getDreamY());
        assertEquals(-20.0, created.getDreamZ());
    }

    @Test
    public void testUpdatesExistingDreamPlayerData() {
        UUID playerUUID = UUID.randomUUID();
        DBDreamPlayer cached = new DBDreamPlayer(playerUUID, 600L, "old", 1.0, 2.0, 3.0, 5);

        DBDreamPlayer updated = DreamManager.updateOrCreateDreamPlayerData(
                playerUUID, cached, DreamManager.BASE_DREAM_TIME, "new", 10.0, 70.0, -20.0);

        assertSame(cached, updated);
        assertEquals("new", updated.getDreamInventory());
        assertEquals(10.0, updated.getDreamX());
        assertEquals(70.0, updated.getDreamY());
        assertEquals(-20.0, updated.getDreamZ());
        assertEquals(600L, updated.getMaxDreamTime());
        assertEquals(5, updated.getProgressionOrb());
    }
}
