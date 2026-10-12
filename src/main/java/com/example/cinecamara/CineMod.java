package com.example.cinecamara;

import com.example.cinecamara.network.ModNetwork;
import net.minecraftforge.fml.common.Mod;

@Mod(CineMod.MODID)
public class CineMod {
    public static final String MODID = "cinecamara";

    public CineMod() {
        ModNetwork.registrar();
    }
}
