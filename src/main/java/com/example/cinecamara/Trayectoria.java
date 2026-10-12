package com.example.cinecamara;

import java.util.List;

/** Camino suave (Catmull-Rom) que pasa exactamente por todos los puntos clave. */
public final class Trayectoria {
    private final List<Punto> pts;
    private final float[] yaws;

    public Trayectoria(List<Punto> puntos) {
        this.pts = List.copyOf(puntos);
        this.yaws = new float[pts.size()];
        for (int i = 0; i < yaws.length; i++) {
            float y = pts.get(i).yaw();
            if (i > 0) {
                // tomar siempre el giro más corto entre dos puntos
                float previo = yaws[i - 1];
                while (y - previo > 180f) y -= 360f;
                while (y - previo < -180f) y += 360f;
            }
            yaws[i] = y;
        }
    }

    public int tamano() {
        return pts.size();
    }

    /** progreso: 0 = primer punto, 1 = último punto. */
    public Punto evaluar(double progreso) {
        int n = pts.size();
        if (n == 1) return pts.get(0);
        double u = Math.max(0.0, Math.min(1.0, progreso)) * (n - 1);
        int i = Math.min((int) Math.floor(u), n - 2);
        double f = u - i;
        int a = Math.max(i - 1, 0), b = i, c = i + 1, d = Math.min(i + 2, n - 1);

        double x = cr(pts.get(a).x(), pts.get(b).x(), pts.get(c).x(), pts.get(d).x(), f);
        double y = cr(pts.get(a).y(), pts.get(b).y(), pts.get(c).y(), pts.get(d).y(), f);
        double z = cr(pts.get(a).z(), pts.get(b).z(), pts.get(c).z(), pts.get(d).z(), f);
        double yaw = cr(yaws[a], yaws[b], yaws[c], yaws[d], f);
        double pitch = cr(pts.get(a).pitch(), pts.get(b).pitch(), pts.get(c).pitch(), pts.get(d).pitch(), f);
        pitch = Math.max(-90.0, Math.min(90.0, pitch));
        return new Punto(x, y, z, (float) yaw, (float) pitch);
    }

    private static double cr(double p0, double p1, double p2, double p3, double t) {
        double t2 = t * t, t3 = t2 * t;
        return 0.5 * ((2 * p1)
                + (-p0 + p2) * t
                + (2 * p0 - 5 * p1 + 4 * p2 - p3) * t2
                + (-p0 + 3 * p1 - 3 * p2 + p3) * t3);
    }
}
