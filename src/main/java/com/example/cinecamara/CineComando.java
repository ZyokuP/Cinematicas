package com.example.cinecamara;

import com.example.cinecamara.network.CinePaquete;
import com.example.cinecamara.network.ModNetwork;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.network.PacketDistributor;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;

/**
 * /cine                      -> ayuda rápida
 * /cine punto                -> marca un punto con tu cámara (posición + hacia dónde miras)
 * /cine quitar | borrar      -> quita el último punto | borra todos
 * /cine lista                -> muestra los puntos
 * /cine ver                  -> muestra/oculta el camino con partículas
 * /cine tiempo <5m>          -> duración total
 * /cine play [jugadores]     -> reproduce (tú, o los jugadores indicados, ej: @a)
 * /cine stop [jugadores]     -> detiene (sin indicar, detiene a todos)
 * /cine usar <nombre>        -> elige o crea otra cinemática
 * Solo operadores (nivel 2).
 */
@Mod.EventBusSubscriber(modid = CineMod.MODID)
public class CineComando {

    private static final int MAX_PUNTOS = 256;
    private static final int MAX_SEGUNDOS = 6 * 3600;

    @SubscribeEvent
    public static void registrar(RegisterCommandsEvent e) {
        LiteralArgumentBuilder<CommandSourceStack> raiz = Commands.literal("cine")
                .requires(src -> src.hasPermission(2))
                .executes(c -> ayuda(c.getSource()));

        raiz.then(Commands.literal("punto").executes(c -> punto(c.getSource())));
        raiz.then(Commands.literal("quitar").executes(c -> quitar(c.getSource())));
        raiz.then(Commands.literal("borrar").executes(c -> borrar(c.getSource())));
        raiz.then(Commands.literal("lista").executes(c -> lista(c.getSource())));
        raiz.then(Commands.literal("ver").executes(c -> ver(c.getSource())));

        raiz.then(Commands.literal("tiempo")
                .then(Commands.argument("tiempo", StringArgumentType.word())
                        .suggests((ctx, b) -> SharedSuggestionProvider.suggest(
                                new String[]{"10s", "30s", "1m", "2m"}, b))
                        .executes(c -> tiempo(c.getSource(), StringArgumentType.getString(c, "tiempo")))));

        raiz.then(Commands.literal("usar")
                .then(Commands.argument("nombre", StringArgumentType.word())
                        .suggests((ctx, b) -> SharedSuggestionProvider.suggest(
                                CineDatos.get(ctx.getSource().getServer()).cinematicas.keySet(), b))
                        .executes(c -> usar(c.getSource(), StringArgumentType.getString(c, "nombre")))));

        raiz.then(Commands.literal("play")
                .executes(c -> reproducir(c.getSource(), List.of(c.getSource().getPlayerOrException())))
                .then(Commands.argument("jugadores", EntityArgument.players())
                        .executes(c -> reproducir(c.getSource(), EntityArgument.getPlayers(c, "jugadores")))));

        raiz.then(Commands.literal("stop")
                .executes(c -> detener(c.getSource(),
                        c.getSource().getServer().getPlayerList().getPlayers()))
                .then(Commands.argument("jugadores", EntityArgument.players())
                        .executes(c -> detener(c.getSource(), EntityArgument.getPlayers(c, "jugadores")))));

        e.getDispatcher().register(raiz);
    }

    // ------------------------------------------------------------------ comandos

    private static int ayuda(CommandSourceStack src) {
        String t = """
                === Cinemática ===
                /cine punto  -> marca un punto con tu cámara
                /cine quitar  -> quita el último punto
                /cine borrar  -> borra todos los puntos
                /cine lista  -> muestra los puntos
                /cine ver  -> muestra/oculta el camino (partículas)
                /cine tiempo <5m>  -> duración total
                /cine play [jugadores]  -> reproduce (ej: /cine play @a)
                /cine stop [jugadores]  -> detiene
                /cine usar <nombre>  -> otra cinemática""";
        src.sendSuccess(() -> Component.literal(t), false);
        return 1;
    }

    private static int punto(CommandSourceStack src) throws CommandSyntaxException {
        ServerPlayer p = src.getPlayerOrException();
        CineDatos datos = CineDatos.get(src.getServer());
        Cinematica c = datos.actual();
        String dim = p.level().dimension().location().toString();

        if (!c.puntos.isEmpty() && !c.dimension.equals(dim)) {
            src.sendFailure(Component.literal("Los puntos de '" + datos.activa + "' están en " + c.dimension
                    + ". Usa /cine borrar o /cine usar <otro nombre> para cambiar de dimensión."));
            return 0;
        }
        if (c.puntos.size() >= MAX_PUNTOS) {
            src.sendFailure(Component.literal("Máximo " + MAX_PUNTOS + " puntos por cinemática."));
            return 0;
        }

        Vec3 ojo = p.getEyePosition();
        c.dimension = dim;
        c.puntos.add(new Punto(ojo.x, ojo.y, ojo.z, Mth.wrapDegrees(p.getYRot()), p.getXRot()));
        datos.setDirty();

        int n = c.puntos.size();
        src.sendSuccess(() -> Component.literal("Punto " + n + " guardado (" + datos.activa + ")."), true);
        return 1;
    }

    private static int quitar(CommandSourceStack src) {
        CineDatos datos = CineDatos.get(src.getServer());
        Cinematica c = datos.actual();
        if (c.puntos.isEmpty()) {
            src.sendFailure(Component.literal("No hay puntos que quitar."));
            return 0;
        }
        c.puntos.remove(c.puntos.size() - 1);
        if (c.puntos.isEmpty()) c.dimension = "";
        datos.setDirty();
        int n = c.puntos.size();
        src.sendSuccess(() -> Component.literal("Último punto quitado. Quedan " + n + "."), true);
        return 1;
    }

    private static int borrar(CommandSourceStack src) {
        CineDatos datos = CineDatos.get(src.getServer());
        Cinematica c = datos.actual();
        c.puntos.clear();
        c.dimension = "";
        datos.setDirty();
        src.sendSuccess(() -> Component.literal("Puntos de '" + datos.activa + "' borrados."), true);
        return 1;
    }

    private static int lista(CommandSourceStack src) {
        CineDatos datos = CineDatos.get(src.getServer());
        Cinematica c = datos.actual();
        StringBuilder sb = new StringBuilder();
        sb.append("Cinemática '").append(datos.activa).append("' - ")
                .append(c.puntos.size()).append(" puntos - duración ")
                .append(TiempoUtil.formatear(c.duracionSeg));
        if (!c.dimension.isEmpty()) sb.append(" - ").append(c.dimension);
        for (int i = 0; i < c.puntos.size(); i++) {
            Punto p = c.puntos.get(i);
            sb.append(String.format(Locale.ROOT, "%n%d) %.1f  %.1f  %.1f", i + 1, p.x(), p.y(), p.z()));
        }
        String texto = sb.toString();
        src.sendSuccess(() -> Component.literal(texto), false);
        return 1;
    }

    private static int ver(CommandSourceStack src) throws CommandSyntaxException {
        ServerPlayer p = src.getPlayerOrException();
        boolean activo = VistaPrevia.alternar(p.getUUID());
        src.sendSuccess(() -> Component.literal(activo
                ? "Camino visible (fuego = puntos, línea = camino). Repite /cine ver para ocultarlo."
                : "Camino oculto."), false);
        return 1;
    }

    private static int tiempo(CommandSourceStack src, String texto) {
        long seg = TiempoUtil.parsear(texto);
        if (seg < 1 || seg > MAX_SEGUNDOS) {
            src.sendFailure(Component.literal("Tiempo no válido. Ejemplos: 30s, 1m, 2m30s (máximo 6h)."));
            return 0;
        }
        CineDatos datos = CineDatos.get(src.getServer());
        datos.actual().duracionSeg = (int) seg;
        datos.setDirty();
        src.sendSuccess(() -> Component.literal("Duración: " + TiempoUtil.formatear(seg)), true);
        return 1;
    }

    private static int usar(CommandSourceStack src, String nombre) {
        if (nombre.length() > 32) {
            src.sendFailure(Component.literal("El nombre es demasiado largo (máx. 32)."));
            return 0;
        }
        CineDatos datos = CineDatos.get(src.getServer());
        datos.activa = nombre;
        Cinematica c = datos.actual();
        datos.setDirty();
        int n = c.puntos.size();
        src.sendSuccess(() -> Component.literal("Cinemática activa: '" + nombre + "' (" + n + " puntos)."), true);
        return 1;
    }

    private static int reproducir(CommandSourceStack src, Collection<ServerPlayer> jugadores) {
        CineDatos datos = CineDatos.get(src.getServer());
        Cinematica c = datos.actual();
        if (c.puntos.size() < 2) {
            src.sendFailure(Component.literal("Necesitas al menos 2 puntos (/cine punto)."));
            return 0;
        }
        CinePaquete paquete = new CinePaquete(c.duracionSeg * 1000, new ArrayList<>(c.puntos));
        int enviados = 0, otraDimension = 0;
        for (ServerPlayer j : jugadores) {
            if (!j.level().dimension().location().toString().equals(c.dimension)) {
                otraDimension++;
                continue;
            }
            ModNetwork.CANAL.send(PacketDistributor.PLAYER.with(() -> j), paquete);
            enviados++;
        }
        if (enviados == 0) {
            src.sendFailure(Component.literal("Nadie está en la dimensión de la cinemática (" + c.dimension + ")."));
            return 0;
        }
        int ok = enviados, otra = otraDimension;
        src.sendSuccess(() -> Component.literal("Cinemática '" + datos.activa + "' iniciada para " + ok
                + " jugador(es)" + (otra > 0 ? " (" + otra + " omitido(s) por estar en otra dimensión)." : ".")), true);
        return enviados;
    }

    private static int detener(CommandSourceStack src, Collection<ServerPlayer> jugadores) {
        CinePaquete vacio = new CinePaquete(0, new ArrayList<>());
        for (ServerPlayer j : jugadores) {
            ModNetwork.CANAL.send(PacketDistributor.PLAYER.with(() -> j), vacio);
        }
        int n = jugadores.size();
        src.sendSuccess(() -> Component.literal("Cinemática detenida para " + n + " jugador(es)."), true);
        return n;
    }
}
