package pe.dbperu.gre.web;

import org.junit.jupiter.api.Test;
import pe.dbperu.gre.repository.CatalogoRepository;

import java.util.HashMap;
import java.util.Map;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

/**
 * Los buscadores reciben el limite que manda Laravel y lo pasan al repositorio.
 * Sin limite se trae todo, como antes: un Laravel anterior no cambia de
 * comportamiento al actualizar solo la ApiGRE.
 */
class CatalogoControllerTest {

    private final CatalogoRepository repo = mock(CatalogoRepository.class);
    private final CatalogoController controller = new CatalogoController(repo);

    private static Map<String, Object> req(Object... claveValor) {
        Map<String, Object> m = new HashMap<>();
        for (int i = 0; i < claveValor.length; i += 2) {
            m.put((String) claveValor[i], claveValor[i + 1]);
        }
        return m;
    }

    @Test
    void los_cuatro_buscadores_pasan_el_limite() {
        controller.obtenerArticulo(req("valor", "", "tipoconsulta", 4, "codestacion", 1, "codalmacen", 1, "codlistaprecio", 1, "limite", 21));
        verify(repo).articulos("", 4, 1, 1, 1, 21);

        controller.obtenerCliente(req("valor", "SAC", "tipo", 4, "limite", 21));
        verify(repo).clientes("SAC", 4, 21);

        controller.obtenerProveedores(req("valor", "20", "tipo", 2, "limite", "21"));
        verify(repo).proveedores("20", 2, 21);

        controller.obtenerTransportista(req("valor", "TRANS", "tipo", 1, "limite", 21));
        verify(repo).transportistas("TRANS", 1, 21);
    }

    @Test
    void sin_limite_o_con_basura_trae_todo_como_antes() {
        controller.obtenerArticulo(req("valor", "x", "tipoconsulta", 1));
        verify(repo).articulos("x", 1, 1, 1, 1, 0);

        controller.obtenerCliente(req("valor", "x", "tipo", 4, "limite", "abc"));
        verify(repo).clientes("x", 4, 0);

        controller.obtenerProveedores(req("valor", "x", "tipo", 3, "limite", -5));
        verify(repo).proveedores("x", 3, 0);
    }
}
