package faygolover.rptime.network;

import faygolover.rptime.core.DayCycle;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/** Сервер → клиент: как сейчас идёт время (длины, множитель, пауза, сон). Шлётся при входе и при каждой смене, не каждый тик. */
public record CyclePacket(DayCycle cycle) {
    public void encode(FriendlyByteBuf buf) {
        buf.writeVarInt(cycle.dayStart());
        buf.writeVarInt(cycle.nightStart());
        buf.writeVarLong(cycle.dayTicks());
        buf.writeVarLong(cycle.nightTicks());
        buf.writeDouble(cycle.speed());
        buf.writeDouble(cycle.sleepRate());
    }

    public static CyclePacket decode(FriendlyByteBuf buf) {
        return new CyclePacket(new DayCycle(buf.readVarInt(), buf.readVarInt(), buf.readVarLong(), buf.readVarLong(), buf.readDouble(), buf.readDouble()));
    }

    public void handle(Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                () -> () -> faygolover.rptime.client.ClientTime.set(cycle)));
        ctx.get().setPacketHandled(true);
    }
}
