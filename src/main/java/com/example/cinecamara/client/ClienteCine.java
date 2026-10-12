package com.example.cinecamara.client;

import com.example.cinecamara.Punto;
import com.example.cinecamara.Trayectoria;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.npc.Villager;

import java.util.List;

/**
 * Cámara de cinemática SOLO en el cliente.
 * Usa una entidad "fantasma" (nunca se añade al mundo) como cámara; el cuerpo del jugador
 * no se toca, y al terminar se devuelve la cámara al jugador.
 */
public class ClienteCine {
    private static boolean activo = false;
    private static Trayectoria trayectoria;
    private static long duracionNs;
    private static long transcurridoNs;
    private static long ultimoNs;
    private static Villager camara;
    private static ClientLevel nivel;
    private static Object jugador;
    private static CameraType tipoAnterior;

    public static boolean activo() {
        return activo;
    }

    public static void recibir(int duracionMs, List<Punto> puntos) {
        if (puntos.size() < 2) {
            detener();
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) return;
        if (activo) detener();

        Villager c = EntityType.VILLAGER.create(mc.level);
        if (c == null) return;

        camara = c;
        trayectoria = new Trayectoria(puntos);
        duracionNs = Math.max(1, duracionMs) * 1_000_000L;
        transcurridoNs = 0;
        ultimoNs = System.nanoTime();
        nivel = mc.level;
        jugador = mc.player;
        tipoAnterior = mc.options.getCameraType();
        activo = true;

        aplicar(trayectoria.evaluar(0.0));
        mc.setCameraEntity(camara);
        mc.options.setCameraType(CameraType.FIRST_PERSON);
    }

    /** Se llama una vez por frame, justo antes de dibujar. */
    public static void alRenderizar() {
        if (!activo) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null || mc.level != nivel || mc.player != jugador) {
            detener();
            return;
        }

        long ahora = System.nanoTime();
        if (!mc.isPaused()) transcurridoNs += ahora - ultimoNs;
        ultimoNs = ahora;

        double progreso = (double) transcurridoNs / (double) duracionNs;
        if (progreso >= 1.0) {
            detener();
            return;
        }

        aplicar(trayectoria.evaluar(progreso));
        if (mc.getCameraEntity() != camara) mc.setCameraEntity(camara);
        if (mc.options.getCameraType() != CameraType.FIRST_PERSON) {
            mc.options.setCameraType(CameraType.FIRST_PERSON);
        }
    }

    private static void aplicar(Punto p) {
        // la cámara se coloca a la altura de los ojos de la entidad, así que se compensa
        camara.moveTo(p.x(), p.y() - camara.getEyeHeight(), p.z(), p.yaw(), p.pitch());
        camara.yHeadRot = p.yaw();
        camara.yHeadRotO = p.yaw();
        camara.yBodyRot = p.yaw();
        camara.yBodyRotO = p.yaw();
    }

    public static void detener() {
        if (!activo) return;
        activo = false;
        Minecraft mc = Minecraft.getInstance();
        mc.setCameraEntity(mc.player);
        if (tipoAnterior != null) mc.options.setCameraType(tipoAnterior);
        camara = null;
        trayectoria = null;
        nivel = null;
        jugador = null;
    }
}
