package pe.dbperu.gre.web;

import org.junit.jupiter.api.Test;
import pe.dbperu.gre.repository.GuiaRepository;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * La salud dice que procedimientos del ERP faltan: un DataMart incompleto no
 * da errores, solo listas vacias, y asi se descubre al instalar.
 */
class SaludControllerTest {

    private final GuiaRepository repo = mock(GuiaRepository.class);
    private final SaludController salud = new SaludController(repo, "1.0.0", "0.18");

    @Test
    void informa_el_procedimiento_que_falta() {
        when(repo.disponible()).thenReturn(true);
        when(repo.procedimientosFaltantes(SaludController.PROCEDIMIENTOS))
                .thenReturn(Collections.singletonList("pr_consultaProveedorlikeRazonsocial"));
        // Falta solo el fork de articulos: la version del ERP alcanza.
        when(repo.procedimientosFaltantes(SaludController.ARTICULOS))
                .thenReturn(Collections.singletonList(SaludController.ARTICULOS.get(0)));

        Map<String, Object> m = salud.health();

        assertEquals("UP", m.get("status"));
        assertEquals("INCOMPLETOS", m.get("procedimientos"));
        assertEquals(Collections.singletonList("pr_consultaProveedorlikeRazonsocial"), m.get("procedimientosFaltantes"));
    }

    @Test
    void sin_ninguna_version_de_articulos_lo_dice() {
        when(repo.disponible()).thenReturn(true);
        when(repo.procedimientosFaltantes(SaludController.PROCEDIMIENTOS)).thenReturn(Collections.emptyList());
        when(repo.procedimientosFaltantes(SaludController.ARTICULOS)).thenReturn(SaludController.ARTICULOS);

        @SuppressWarnings("unchecked")
        List<String> faltan = (List<String>) salud.health().get("procedimientosFaltantes");

        assertEquals(Collections.singletonList(SaludController.ARTICULOS.get(1)), faltan);
    }

    @Test
    void completo_cuando_estan_todos() {
        when(repo.disponible()).thenReturn(true);
        when(repo.procedimientosFaltantes(anyCollection())).thenReturn(Collections.emptyList());

        Map<String, Object> m = salud.health();

        assertEquals("COMPLETOS", m.get("procedimientos"));
        assertEquals(Collections.emptyList(), m.get("procedimientosFaltantes"));
    }

    @Test
    void sin_sql_server_no_intenta_revisar_procedimientos() {
        when(repo.disponible()).thenReturn(false);

        Map<String, Object> m = salud.health();

        assertEquals("DOWN", m.get("status"));
        assertFalse(m.containsKey("procedimientos"));
        verify(repo, never()).procedimientosFaltantes(anyCollection());
    }
}
