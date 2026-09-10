package pe.dbperu.gre.web;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import pe.dbperu.gre.repository.CatalogoRepository;

import java.util.*;

/**
 * Catalogos para armar una guia.
 *
 * Cada endpoint se expone dos veces:
 *   /api/v1/...   el contrato nuevo, en minusculas y con guiones
 *   /GREDMK/...   alias del ApiDMK, para poder cambiar de API sin tocar Laravel
 *
 * Los alias se retiran cuando Laravel termine de migrar.
 */
@RestController
public class CatalogoController {

    private final CatalogoRepository repo;

    public CatalogoController(CatalogoRepository repo) {
        this.repo = repo;
    }

    // ---------------- contrato nuevo ----------------

    @GetMapping("/api/v1/formas-pago")
    public List<Map<String, Object>> formasPago() { return repo.formasPago(); }

    @GetMapping("/api/v1/operaciones")
    public List<Map<String, Object>> operaciones() { return repo.operaciones(); }

    @GetMapping("/api/v1/almacenes")
    public List<Map<String, Object>> almacenes() { return repo.almacenes(); }

    @GetMapping("/api/v1/series-guia")
    public List<Map<String, Object>> seriesGuia() { return repo.seriesGuia(); }

    @GetMapping("/api/v1/listas-precio")
    public List<Map<String, Object>> listasPrecio() { return repo.listasPrecio(); }

    @GetMapping("/api/v1/trabajadores")
    public List<Map<String, Object>> trabajadores(
            @RequestParam(name = "codTrabajador", defaultValue = "-1") int cod) {
        return repo.trabajadores(cod);
    }

    @PostMapping("/api/v1/articulos/buscar")
    public List<Map<String, Object>> buscarArticulos(@RequestBody Map<String, Object> req) {
        return repo.articulos(
                str(req.get("valor")),
                entero(req.get("tipoconsulta"), 1),
                entero(req.get("codestacion"), 1),
                entero(req.get("codalmacen"), 1),
                entero(req.get("codlistaprecio"), 1));
    }

    // ---------------- alias compatibles con ApiDMK ----------------

    @PostMapping("/GREDMK/ObtenerArticulo")
    public Map<String, Object> obtenerArticulo(@RequestBody Map<String, Object> req) {
        return envoltorio("articulos", buscarArticulos(req));
    }

    @GetMapping("/GREDMK/ObtenerSucursalPrecio")
    public Map<String, Object> obtenerSucursalPrecio() {
        return envoltorio("listasPrecio", repo.listasPrecio());
    }

    @GetMapping("/GREDMK/ObtenerTrabajador")
    public Map<String, Object> obtenerTrabajador(
            @RequestParam(name = "CodigoTrabajador", defaultValue = "-1") int cod) {
        return envoltorio("trabajador", repo.trabajadores(cod));
    }

    @PostMapping("/GREDMK/ObtieneUbigeos")
    public Map<String, Object> obtieneUbigeos(@RequestBody Map<String, Object> req) {
        return envoltorio("ubigeos", repo.ubigeos(
                str(req.get("codigoUbigeo")), entero(req.get("tipoConsulta"), 1)));
    }

    @PostMapping("/GREDMK/ObtenerTransportista")
    public Map<String, Object> obtenerTransportista(@RequestBody Map<String, Object> req) {
        return envoltorio("transportistas",
                repo.transportistas(str(req.get("valor")), entero(req.get("tipo"), 3)));
    }

    @PostMapping("/GREDMK/ObtenerVehiculo")
    public Map<String, Object> obtenerVehiculo(@RequestBody Map<String, Object> req) {
        return envoltorio("vehiculos",
                repo.vehiculos(str(req.get("valor")), entero(req.get("tipo"), 4)));
    }

    /**
     * Pedia el tipo 5 del procedimiento, que consulta la tabla VEHICULO. Los
     * choferes son el tipo 6. Laravel pinta el desplegable leyendo dniChofer,
     * breveteChofer y nombreChofer, campos que una fila de vehiculo no tiene:
     * con datos en la tabla, la pantalla se rompia al renderizar.
     */
    @PostMapping("/GREDMK/ObtenerChoferes")
    public Map<String, Object> obtenerChoferes(@RequestBody Map<String, Object> req) {
        return envoltorio("choferes", repo.choferes(str(req.get("nombrechofer"))));
    }

    @PostMapping("/GREDMK/obtenerCliente")
    public Map<String, Object> obtenerCliente(@RequestBody Map<String, Object> req) {
        return envoltorio("cliente",
                repo.clientes(str(req.get("valor")), entero(req.get("tipo"), 1)));
    }

    @PostMapping("/GREDMK/ObtenerProveedores")
    public Map<String, Object> obtenerProveedores(@RequestBody Map<String, Object> req) {
        return envoltorio("proveedores",
                repo.proveedores(str(req.get("valor")), entero(req.get("tipo"), 1)));
    }

    private static String str(Object v) { return v == null ? "" : String.valueOf(v); }

    private static int entero(Object v, int porDefecto) {
        if (v == null || String.valueOf(v).trim().isEmpty()) { return porDefecto; }
        try { return Integer.parseInt(String.valueOf(v).trim()); }
        catch (NumberFormatException e) { return porDefecto; }
    }


    @GetMapping("/GREDMK/ObtenerFormasPago")
    public Map<String, Object> obtenerFormasPago() {
        return envoltorio("formasdePago", repo.formasPago());
    }

    @GetMapping("/GREDMK/ObtenerOperacion")
    public Map<String, Object> obtenerOperacion() {
        return envoltorio("operaciones", repo.operaciones());
    }

    @GetMapping("/GREDMK/ObtenerAlmacenes")
    public Map<String, Object> obtenerAlmacenes() {
        return envoltorio("almacenes", repo.almacenes());
    }

    @GetMapping("/GREDMK/obtenerSeriesNumerosGuia")
    public Map<String, Object> obtenerSeriesNumerosGuia() {
        return envoltorio("serienumeros", repo.seriesGuia());
    }

    /** El ApiDMK envolvia todo en {exito, msgerror, <clave>}. */
    private Map<String, Object> envoltorio(String clave, Object datos) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("exito", true);
        m.put("msgerror", null);
        m.put(clave, datos);
        return m;
    }
}
