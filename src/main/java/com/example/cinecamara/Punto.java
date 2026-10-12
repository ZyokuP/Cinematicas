package com.example.cinecamara;

/** Un punto clave de la cámara: posición y hacia dónde mira. */
public record Punto(double x, double y, double z, float yaw, float pitch) {}
