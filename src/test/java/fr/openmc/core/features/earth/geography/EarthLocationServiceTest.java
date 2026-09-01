package fr.openmc.core.features.earth.geography;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class EarthLocationServiceTest {
    @Test
    void projectionUsesConfiguredMetresPerBlockWithoutHardcodedScale() {
        EarthLocationService.EarthCoordinates defaultProjection = EarthLocationService.project(12.5D, -4D, 1_000D);
        EarthLocationService.EarthCoordinates customProjection = EarthLocationService.project(12.5D, -4D, 250D);
        EarthLocationService.EarthCoordinates safeProjection = EarthLocationService.project(2D, 3D, 0D);

        assertEquals(12_500D, defaultProjection.eastMetres());
        assertEquals(-4_000D, defaultProjection.northMetres());
        assertEquals(3_125D, customProjection.eastMetres());
        assertEquals(-1_000D, customProjection.northMetres());
        assertEquals(.002D, safeProjection.eastMetres(), .0000001D);
        assertEquals(.003D, safeProjection.northMetres(), .0000001D);
    }
}
