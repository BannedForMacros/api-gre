package pe.dbperu.gre.web;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import pe.dbperu.gre.repository.GuiaRepository;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.LinkedHashMap;
import java.util.Map;

/** Para saber, desde fuera, en que estado esta la instalacion de cada cliente. */
@RestController
@RequestMapping("/api/v1")
public class SaludController {

    private final GuiaRepository repo;
    private final String version;
    private final String tasaIgv;

    public SaludController(GuiaRepository repo,
                           @Value("${gre.version:1.0.0}") String version,
                           @Value("${gre.igv.tasa:0.18}") String tasaIgv) {
        this.repo = repo;
        this.version = version;
        this.tasaIgv = tasaIgv;
    }

    /**
     * Procedimientos del ERP que usa la ApiGRE. Si falta uno, esa parte de la
     * pantalla falla en silencio (lista vacia), asi que se informa aqui y el
     * instalador lo deja escrito.
     */
    static final List<String> PROCEDIMIENTOS = Arrays.asList(
            "prc_InsertGuiaDMKWeb", "InsertarGuiasOdooDmk",
            "GetMaestrodocumentoserieByTipodocumento", "GetMaestroformadepagoAll",
            "GetMaestroOperacionByEstado", "GetMaestroalmacenByEstado", "GetSucursalPrecio",
            "GetMaestrotrabajadorByCodtrabajador", "GetDatosClientexTipo",
            "GetMaestroproveedoresByRuc", "pr_consultaProveedorlikeRazonsocial",
            "GetGuiaRemisionLikeForTipo", "pr_ObtieneUbigeo");

    /** Articulos: basta una de las dos versiones, el fork _ApiDMK o la del ERP. */
    static final List<String> ARTICULOS = Arrays.asList(
            "GetMultiAlmacenGuiaIngMaestroarticuloByCodarticulo_ApiDMK",
            "GetMultiAlmacenGuiaIngMaestroarticuloByCodarticulo");

    @GetMapping("/health")
    public Map<String, Object> health() {
        boolean db = repo.disponible();
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("status", db ? "UP" : "DOWN");
        m.put("version", version);
        m.put("sqlServer", db ? "UP" : "DOWN");
        m.put("igvTasa", tasaIgv);

        if (db) {
            try {
                List<String> faltan = new ArrayList<>(repo.procedimientosFaltantes(PROCEDIMIENTOS));
                if (repo.procedimientosFaltantes(ARTICULOS).size() == ARTICULOS.size()) {
                    faltan.add(ARTICULOS.get(1));
                }
                m.put("procedimientos", faltan.isEmpty() ? "COMPLETOS" : "INCOMPLETOS");
                m.put("procedimientosFaltantes", faltan);
            } catch (Exception e) {
                m.put("procedimientos", "NO_VERIFICADO");
            }
        }
        return m;
    }
}
