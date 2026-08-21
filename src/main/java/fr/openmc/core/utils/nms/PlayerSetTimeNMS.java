package fr.openmc.core.utils.nms;

import net.minecraft.network.protocol.game.ClientboundSetTimePacket;
import net.minecraft.server.level.ServerPlayer;
import org.bukkit.craftbukkit.entity.CraftPlayer;
import org.bukkit.entity.Player;

public class PlayerSetTimeNMS {

    /**
     * Envoie un packet au joueur pour définir l'heure du monde.
     *
     * @param player le joueur à qui envoyer le packet
     * @param time   le temps envoyé
     */
    public static void sendPacketSetTime(Player player, int time) {
        ServerPlayer serverPlayer = ((CraftPlayer) player).getHandle();

        long gameTime = serverPlayer.level().getGameTime();

        ClientboundSetTimePacket packet =
                new ClientboundSetTimePacket(gameTime, time, false);

        serverPlayer.connection.send(packet);
    }
}