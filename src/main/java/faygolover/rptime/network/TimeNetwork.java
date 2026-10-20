package faygolover.rptime.network;

import faygolover.rptime.RpTime;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;

import java.util.Optional;

/** Канал {@code rptime:main}. Клиент и сервер без мода пускаются: тогда время у клиента идёт по-ванильному с поправкой раз в секунду. */
public final class TimeNetwork {
    private TimeNetwork() {}

    private static final String VERSION = "1";
    public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            new ResourceLocation(RpTime.MODID, "main"), () -> VERSION,
            NetworkRegistry.acceptMissingOr(VERSION), NetworkRegistry.acceptMissingOr(VERSION));

    public static void register() {
        int id = 0;
        CHANNEL.registerMessage(id++, CyclePacket.class, CyclePacket::encode, CyclePacket::decode, CyclePacket::handle,
                Optional.of(NetworkDirection.PLAY_TO_CLIENT));
    }

    public static void send(ServerPlayer player, Object msg) {
        if (CHANNEL.isRemotePresent(player.connection.connection)) CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), msg);
    }

    public static void sendToAll(net.minecraft.server.MinecraftServer server, Object msg) {
        for (ServerPlayer p : server.getPlayerList().getPlayers()) send(p, msg);
    }
}
