package com.example.cinecamara;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.server.ServerLifecycleHooks;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/** Dibuja el camino con partículas, solo para los admins que ejecutaron /cine ver. */
@Mod.EventBusSubscriber(modid = CineMod.MODID)
public class VistaPrevia {

    private static final Set<UUID> ACTIVOS = new HashSet<>();
    private static int contador = 0;

    public static boolean alternar(UUID id) {
        if (ACTIVOS.remove(id)) return false;
        ACTIVOS.add(id);
        return true;
    }

    @SubscribeEvent
    public static void alTick(TickEvent.ServerTickEvent e) {
        if (e.phase != TickEvent.Phase.END || ACTIVOS.isEmpty()) return;
        if (++contador % 10 != 0) return;

        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        if (server == null) return;
        Cinematica c = CineDatos.get(server).actual();
        List<Punto> puntos = c.puntos;
        if (puntos.isEmpty()) return;
        Trayectoria camino = puntos.size() >= 2 ? new Trayectoria(puntos) : null;

        for (UUID id : ACTIVOS) {
            ServerPlayer p = server.getPlayerList().getPlayer(id);
            if (p == null) continue;
            if (!p.level().dimension().location().toString().equals(c.dimension)) continue;
            ServerLevel nivel = p.serverLevel();

            if (camino != null) {
                int muestras = Math.min(150, (puntos.size() - 1) * 20);
                for (int i = 0; i <= muestras; i++) {
                    Punto q = camino.evaluar(i / (double) muestras);
                    nivel.sendParticles(p, ParticleTypes.END_ROD, true, q.x(), q.y(), q.z(), 1, 0, 0, 0, 0);
                }
            }
            for (Punto q : puntos) {
                nivel.sendParticles(p, ParticleTypes.FLAME, true, q.x(), q.y(), q.z(), 3, 0.05, 0.05, 0.05, 0.0);
                // pequeña línea que indica hacia dónde mira la cámara en ese punto
                float f1 = -q.yaw() * ((float) Math.PI / 180F);
                float f = q.pitch() * ((float) Math.PI / 180F);
                double dx = Mth.sin(f1) * Mth.cos(f);
                double dy = -Mth.sin(f);
                double dz = Mth.cos(f1) * Mth.cos(f);
                for (int k = 1; k <= 3; k++) {
                    double d = k * 0.5;
                    nivel.sendParticles(p, ParticleTypes.CRIT, true,
                            q.x() + dx * d, q.y() + dy * d, q.z() + dz * d, 1, 0, 0, 0, 0);
                }
            }
        }
    }

    @SubscribeEvent
    public static void alSalir(PlayerEvent.PlayerLoggedOutEvent e) {
        ACTIVOS.remove(e.getEntity().getUUID());
    }

    @SubscribeEvent
    public static void alCerrarServidor(ServerStoppedEvent e) {
        ACTIVOS.clear();
    }
}
