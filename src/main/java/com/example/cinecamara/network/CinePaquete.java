package com.example.cinecamara.network;

import com.example.cinecamara.Punto;
import com.example.cinecamara.client.ClienteCine;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

/** Servidor -> cliente. Lista de puntos vacía = detener la cinemática. */
public class CinePaquete {
    private static final int MAX = 512;

    private final int duracionMs;
    private final List<Punto> puntos;

    public CinePaquete(int duracionMs, List<Punto> puntos) {
        this.duracionMs = duracionMs;
        this.puntos = puntos;
    }

    public static void codificar(CinePaquete msg, FriendlyByteBuf buf) {
        buf.writeInt(msg.duracionMs);
        buf.writeVarInt(msg.puntos.size());
        for (Punto p : msg.puntos) {
            buf.writeDouble(p.x());
            buf.writeDouble(p.y());
            buf.writeDouble(p.z());
            buf.writeFloat(p.yaw());
            buf.writeFloat(p.pitch());
        }
    }

    public static CinePaquete decodificar(FriendlyByteBuf buf) {
        int duracion = buf.readInt();
        int n = Math.min(buf.readVarInt(), MAX);
        List<Punto> lista = new ArrayList<>(n);
        for (int i = 0; i < n; i++) {
            lista.add(new Punto(buf.readDouble(), buf.readDouble(), buf.readDouble(),
                    buf.readFloat(), buf.readFloat()));
        }
        return new CinePaquete(duracion, lista);
    }

    public static void manejar(CinePaquete msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                () -> () -> ClienteCine.recibir(msg.duracionMs, msg.puntos)));
        ctx.get().setPacketHandled(true);
    }
}
