package com.example.cinecamara;

import java.util.ArrayList;
import java.util.List;

public class Cinematica {
    /** Duración total en segundos. */
    public int duracionSeg = 10;
    /** Dimensión donde se grabaron los puntos (ej: minecraft:overworld). */
    public String dimension = "";
    public final List<Punto> puntos = new ArrayList<>();
}
