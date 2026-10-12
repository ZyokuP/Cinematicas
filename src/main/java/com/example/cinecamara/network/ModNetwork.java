package com.example.cinecamara.network;

import com.example.cinecamara.CineMod;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;

public class ModNetwork {
    private static final String VERSION = "1";

    public static final SimpleChannel CANAL = NetworkRegistry.newSimpleChannel(
            new ResourceLocation(CineMod.MODID, "main"),
            () -> VERSION, VERSION::equals, VERSION::equals);

    public static void registrar() {
        CANAL.registerMessage(0, CinePaquete.class,
                CinePaquete::codificar, CinePaquete::decodificar, CinePaquete::manejar);
    }
}
