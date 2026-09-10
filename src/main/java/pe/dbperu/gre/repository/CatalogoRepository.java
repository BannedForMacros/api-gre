package pe.dbperu.gre.repository;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Catalogos del DataMart. Solo lectura, solo lo que necesita una guia.
 *
 * Los stored procedures del ERP no se tocan: se invocan.
 */
@Repository
public class CatalogoRepository {

    private static final Logger log = LoggerFactory.getLogger(CatalogoRepository.class);

    private final JdbcTemplate jdbc;

    public CatalogoRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /**
     * Ejecuta un stored procedure de catalogo tolerando que no devuelva nada.
     *
     * Varios SPs del ERP tienen ramas que terminan sin SELECT (por ejemplo
     * pr_ObtieneUbigeo con ciertos parametros). El driver JDBC lo reporta como
     * "La instruccion no devolvio un conjunto de resultados" y eso tumbaba la
     * pantalla entera con un 500. Un catalogo vacio es un resultado valido: la
     * pantalla debe abrir igual.
     */
    private List<Map<String, Object>> consultar(String sql, Object... args) {
        try {
            return jdbc.queryForList(sql, args);
        } catch (Exception e) {
            log.warn("Catalogo sin resultados: {} · {}", sql, e.getMessage());
            return Collections.emptyList();
        }
    }

    /** SP: GetMaestroformadepagoAll */
    public List<Map<String, Object>> formasPago() {
        return Mapeo.proyectar(consultar("{ call GetMaestroformadepagoAll }"),
                "CodFormaPago", "codFormaPago",
                "Descripcion",  "descripcion");
    }

    /** SP: GetMaestroOperacionByEstado */
    public List<Map<String, Object>> operaciones() {
        return Mapeo.proyectar(consultar("{ call GetMaestroOperacionByEstado(?) }", 1),
                "TipoOperacion",       "tipoOperacion",
                "Descripcion",         "descripcion",
                "IngresoSalida",       "ingresoSalida",
                "MotivoTraslado",      "motivotraslado",
                "DescriMotivoTraslado","descriMotivotraslado");
    }

    /** SP: GetMaestroalmacenByEstado */
    public List<Map<String, Object>> almacenes() {
        return Mapeo.proyectar(consultar("{ call GetMaestroalmacenByEstado(?) }", 1),
                "CodAlmacen",  "codAlmacen",
                "Descripcion", "descripcion",
                "CodInterno",  "codInterno",
                "Direccion",   "direccion",
                "CodZip",      "ubigeo",
                "CodEstacion", "codEstacion");
    }

    /**
     * Busqueda de articulos con stock y precio.
     *
     * SP: GetMultiAlmacenGuiaIngMaestroarticuloByCodarticulo
     *     (@Codigo, @Descripcion, @CodEstacion, @CodAlmacen, @Estado,
     *      @TipoConsulta, @CodListaPrecio)
     *
     * tipoConsulta: 1 = codigo de barras, 2 = codigo/nombre de articulo.
     */
    public List<Map<String, Object>> articulos(String valor, int tipoConsulta,
                                               int codEstacion, int codAlmacen, int codListaPrecio) {
        List<Map<String, Object>> filas = consultar(
                "{ call " + spArticulos() + "(?,?,?,?,?,?,?) }",
                0, valor, codEstacion, codAlmacen, "1", String.valueOf(tipoConsulta), codListaPrecio);

        return Mapeo.proyectar(filas,
                "Codigo",         "codArticulo",
                "CodBarra",       "codBarra",
                "CodPlu",         "codPlu",
                "NombreArticulo", "nombreArticulo",
                "PrecioPublico",  "precioPublico",
                "PrecioSinIGV",   "precioSinIGV",
                "CostoArticulo",  "costoArticulo",
                "Stock",          "stock",
                "Peso",           "peso",
                "CodUnidad",      "codUnidad",
                "SiglaUnidad",    "descUnidadMedida",
                "CodUNECE",       "siglaUMFE",
                "TipoIgv",        "tipoIgv")
                .stream()
                // Si el DataMart no informa TipoIgv, se asume gravado: es el
                // caso normal y tratarlo como inafecto por omision falsearia
                // el IGV de la guia hacia abajo.
                .peek(a -> a.putIfAbsent("tipoIgv", 1))
                .peek(a -> { if (a.get("tipoIgv") == null) { a.put("tipoIgv", 1); } })
                .collect(java.util.stream.Collectors.toList());
    }

    /** SP: GetSucursalPrecio */
    public List<Map<String, Object>> listasPrecio() {
        return Mapeo.proyectar(consultar("{ call GetSucursalPrecio }"),
                "CodListaPrecio", "codListaPrecio",
                "Precio",         "precio",
                "RazonSocial",    "razonSocial",
                "CodEstacion",    "codEstacion");
    }

    /** SP: GetMaestrotrabajadorByCodtrabajador (-1 = todos) */
    public List<Map<String, Object>> trabajadores(int codTrabajador) {
        return Mapeo.proyectar(consultar("{ call GetMaestrotrabajadorByCodtrabajador(?) }", codTrabajador),
                "CodTrabajador", "codTrabajador",
                "Nombres",       "nombres",
                "Apellidos",     "apellidos",
                "DNI",           "dni",
                "Cargo",         "cargo");
    }

    /** SP: pr_ObtieneUbigeo · 1 departamento · 2 provincia · 3 distrito */
    public List<Map<String, Object>> ubigeos(String codigoPadre, int tipoConsulta) {
        return Mapeo.proyectar(
                consultar("{ call pr_ObtieneUbigeo(?,?) }", codigoPadre, tipoConsulta),
                "CodUbigeo",   "codUbigeo",
                "Descripcion", "descripcion");
    }

    /**
     * SP: GetGuiaRemisionLikeForTipo. El mismo procedimiento sirve para
     * transportistas, vehiculos y choferes segun el tipo. Se devuelve crudo
     * porque las columnas cambian entre ellos y el mapeo se hace arriba.
     */
    public List<Map<String, Object>> guiaRemisionLike(String valor, int tipo) {
        return consultar("{ call GetGuiaRemisionLikeForTipo(?,?) }", valor, tipo);
    }

    /** SP: GetDatosClientexTipo */
    public List<Map<String, Object>> clientes(String valor, int tipo) {
        return consultar("{ call GetDatosClientexTipo(?,?) }", valor, tipo);
    }

    /** SPs: GetMaestroproveedoresByRuc · pr_consultaProveedorlikeRazonsocial */
    public List<Map<String, Object>> proveedores(String valor, int tipo) {
        String sp = (tipo == 3) ? "pr_consultaProveedorlikeRazonsocial" : "GetMaestroproveedoresByRuc";
        return consultar("{ call " + sp + "(?) }", valor);
    }

    /**
     * Cual de los dos SPs de articulos usar.
     *
     * En el DataMart conviven dos versiones del mismo procedimiento:
     *   GetMultiAlmacenGuiaIngMaestroarticuloByCodarticulo          (el del ERP)
     *   GetMultiAlmacenGuiaIngMaestroarticuloByCodarticulo_ApiDMK   (fork para la API)
     *
     * El fork agrega Peso y TipoIgv, y cambia PrecioSinIGV para los articulos
     * inafectos. Sin el, TipoIgv llega nulo y todo articulo pareceria inafecto.
     *
     * Se prefiere el fork cuando existe. Se resuelve una vez y se recuerda: es
     * una propiedad de la instalacion, no cambia entre peticiones.
     */
    private volatile String spArticulos;

    private String spArticulos() {
        if (spArticulos == null) {
            synchronized (this) {
                if (spArticulos == null) {
                    String fork = "GetMultiAlmacenGuiaIngMaestroarticuloByCodarticulo_ApiDMK";
                    Integer existe = jdbc.queryForObject(
                            "SELECT CASE WHEN OBJECT_ID(?, 'P') IS NULL THEN 0 ELSE 1 END",
                            Integer.class, "dbo." + fork);
                    spArticulos = (existe != null && existe == 1)
                            ? fork
                            : "GetMultiAlmacenGuiaIngMaestroarticuloByCodarticulo";
                }
            }
        }
        return spArticulos;
    }

    /** SP: GetMaestrodocumentoserieByTipodocumento (12 = guia de remision) */
    public List<Map<String, Object>> seriesGuia() {
        return Mapeo.proyectar(consultar("{ call GetMaestrodocumentoserieByTipodocumento(?) }", 12),
                "NumSerie",          "numserie",
                "UltimoValorMarket", "ultimoValormarket",
                "TipoDocumento",     "tipodocumento");
    }
}
