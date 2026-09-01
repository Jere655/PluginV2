package fr.openmc.core.features.earth.commands;

import org.bukkit.entity.Player;
import org.junit.jupiter.api.Test;
import revxrsal.commands.bukkit.annotation.CommandPermission;

import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class EarthPermissionsTest {
    @Test
    void earthCommandsUseStableLuckPermsCompatibleNodesInsteadOfGenericOp() throws Exception {
        assertEquals(EarthPermissions.USE, EarthCommands.class.getAnnotation(CommandPermission.class).value());
        assertPermission("citizenship", EarthPermissions.CITIZENSHIP, String.class);
        assertPermission("volunteer", EarthPermissions.CIVIC);
        assertPermission("nominate", EarthPermissions.ELECTION_NOMINATE, String.class);
        assertPermission("vote", EarthPermissions.ELECTION_VOTE, String.class, String.class);
        assertPermission("propertyAccess", EarthPermissions.PROPERTY_MANAGE, String.class, String.class);
        assertPermission("propertyRent", EarthPermissions.PROPERTY_RENT, String.class, long.class);
        assertPermission("propertySaleOffer", EarthPermissions.PROPERTY_MANAGE, String.class, double.class, long.class);
        assertPermission("withdrawPropertySale", EarthPermissions.PROPERTY_MANAGE, String.class);
        assertPermission("buyProperty", EarthPermissions.PROPERTY_PURCHASE, String.class);
        assertPermission("takeJob", EarthPermissions.JOB_TAKE, String.class);
        assertPermission("work", EarthPermissions.JOB_WORK);
        assertPermission("travel", EarthPermissions.TRAVEL, String.class);
        assertPermission("createParty", EarthPermissions.PARTY_MANAGE, String.class, String.class, String.class);
        assertPermission("simulationTick", EarthPermissions.ADMIN_SIMULATION);
        assertPermission("policy", EarthPermissions.GOVERNMENT_MANAGE, String.class, double.class, double.class, double.class);
        assertPermission("enactLaw", EarthPermissions.ADMIN_GOVERNMENT, String.class, String.class);
        assertPermission("repealLaw", EarthPermissions.GOVERNMENT_MANAGE, String.class, String.class);
        assertPermission("proposeBill", EarthPermissions.GOVERNMENT_MANAGE, String.class, String.class);
        assertPermission("passBill", EarthPermissions.GOVERNMENT_MANAGE, String.class, String.class);
        assertPermission("linkCity", EarthPermissions.ADMIN_CITY, String.class, String.class);

        for (String permission : new String[] {
                EarthPermissions.ADMIN_ELECTION, EarthPermissions.ADMIN_EVENT, EarthPermissions.ADMIN_POPULATION,
                EarthPermissions.ADMIN_DIPLOMACY, EarthPermissions.ADMIN_DIAGNOSE, EarthPermissions.ADMIN_SIMULATION,
                EarthPermissions.ADMIN_TRANSPORT, EarthPermissions.ADMIN_GOVERNMENT, EarthPermissions.ADMIN_CITY
        }) {
            assertFalse("op".equals(permission));
        }
    }

    private static void assertPermission(String methodName, String expected, Class<?>... arguments) throws Exception {
        Class<?>[] signature = new Class<?>[arguments.length + 1];
        signature[0] = Player.class;
        System.arraycopy(arguments, 0, signature, 1, arguments.length);
        Method method = EarthCommands.class.getMethod(methodName, signature);
        assertEquals(expected, method.getAnnotation(CommandPermission.class).value(), methodName);
    }
}
