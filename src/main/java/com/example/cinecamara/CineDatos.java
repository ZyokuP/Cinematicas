package com.example.cinecamara;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.LinkedHashMap;
import java.util.Map;

/** Guarda las cinemáticas dentro del mundo (carpeta data/), así sobreviven a reinicios. */
public class CineDatos extends SavedData {
    private static final String NOMBRE = "cinecamara_datos";

    public final Map<String, Cinematica> cinematicas = new LinkedHashMap<>();
    public String activa = "principal";

    public static CineDatos get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(CineDatos::cargar, CineDatos::new, NOMBRE);
    }

    /** La cinemática que se está editando/reproduciendo ahora. */
    public Cinematica actual() {
        return cinematicas.computeIfAbsent(activa, k -> new Cinematica());
    }

    public static CineDatos cargar(CompoundTag tag) {
        CineDatos datos = new CineDatos();
        if (tag.contains("activa")) datos.activa = tag.getString("activa");
        ListTag lista = tag.getList("cinematicas", Tag.TAG_COMPOUND);
        for (int i = 0; i < lista.size(); i++) {
            CompoundTag t = lista.getCompound(i);
            Cinematica c = new Cinematica();
            c.duracionSeg = Math.max(1, t.getInt("duracion"));
            c.dimension = t.getString("dimension");
            ListTag ps = t.getList("puntos", Tag.TAG_COMPOUND);
            for (int j = 0; j < ps.size(); j++) {
                CompoundTag p = ps.getCompound(j);
                c.puntos.add(new Punto(p.getDouble("x"), p.getDouble("y"), p.getDouble("z"),
                        p.getFloat("yaw"), p.getFloat("pitch")));
            }
            datos.cinematicas.put(t.getString("nombre"), c);
        }
        return datos;
    }

    @Override
    public CompoundTag save(CompoundTag tag) {
        tag.putString("activa", activa);
        ListTag lista = new ListTag();
        for (Map.Entry<String, Cinematica> e : cinematicas.entrySet()) {
            Cinematica c = e.getValue();
            CompoundTag t = new CompoundTag();
            t.putString("nombre", e.getKey());
            t.putInt("duracion", c.duracionSeg);
            t.putString("dimension", c.dimension);
            ListTag ps = new ListTag();
            for (Punto p : c.puntos) {
                CompoundTag pt = new CompoundTag();
                pt.putDouble("x", p.x());
                pt.putDouble("y", p.y());
                pt.putDouble("z", p.z());
                pt.putFloat("yaw", p.yaw());
                pt.putFloat("pitch", p.pitch());
                ps.add(pt);
            }
            t.put("puntos", ps);
            lista.add(t);
        }
        tag.put("cinematicas", lista);
        return tag;
    }
}
