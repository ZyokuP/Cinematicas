package com.example.cinecamara.client;

import com.example.cinecamara.CineMod;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.client.event.RenderGuiOverlayEvent;
import net.minecraftforge.client.event.RenderHandEvent;
import net.minecraftforge.client.event.RenderHighlightEvent;
import net.minecraftforge.client.gui.overlay.VanillaGuiOverlay;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.Set;

/**
 * Todos los eventos hacen algo SOLO mientras hay una cinemática reproduciéndose.
 * Fuera de eso el mod no toca nada. Los HUD de otros mods no se tocan.
 */
@Mod.EventBusSubscriber(modid = CineMod.MODID, value = Dist.CLIENT)
public class EventosCine {

    /** Partes del HUD de Minecraft que se mantienen durante la cinemática. */
    private static final Set<ResourceLocation> PERMITIDOS = Set.of(
            VanillaGuiOverlay.CHAT_PANEL.id(),
            VanillaGuiOverlay.TITLE_TEXT.id(),
            VanillaGuiOverlay.SUBTITLES.id(),
            VanillaGuiOverlay.RECORD_OVERLAY.id(),
            VanillaGuiOverlay.DEBUG_TEXT.id());

    @SubscribeEvent
    public static void alRenderizar(TickEvent.RenderTickEvent e) {
        if (e.phase == TickEvent.Phase.START) ClienteCine.alRenderizar();
    }

    /** Oculta la mano y el objeto en primera persona. */
    @SubscribeEvent
    public static void alDibujarMano(RenderHandEvent e) {
        if (ClienteCine.activo()) e.setCanceled(true);
    }

    /** Evita que se marque el bloque al que apunta la cámara. */
    @SubscribeEvent
    public static void alResaltarBloque(RenderHighlightEvent.Block e) {
        if (ClienteCine.activo()) e.setCanceled(true);
    }

    /** Evita romper/colocar/usar cosas "con la cámara" mientras dura la cinemática. */
    @SubscribeEvent
    public static void alInteractuar(InputEvent.InteractionKeyMappingTriggered e) {
        if (ClienteCine.activo()) {
            e.setCanceled(true);
            e.setSwingHand(false);
        }
    }

    /** Oculta el HUD de Minecraft (barra, vida, mira...) pero deja chat y títulos. No toca HUD de otros mods. */
    @SubscribeEvent
    public static void alDibujarHud(RenderGuiOverlayEvent.Pre e) {
        if (!ClienteCine.activo()) return;
        ResourceLocation id = e.getOverlay().id();
        if (!"minecraft".equals(id.getNamespace())) return;
        if (PERMITIDOS.contains(id)) return;
        e.setCanceled(true);
    }

    @SubscribeEvent
    public static void alSalir(ClientPlayerNetworkEvent.LoggingOut e) {
        ClienteCine.detener();
    }
}
