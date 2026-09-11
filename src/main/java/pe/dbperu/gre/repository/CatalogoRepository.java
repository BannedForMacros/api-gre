package pe.dbperu.gre.repository;

import org.springframework.jdbc.core.ColumnMapRowMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.PreparedStatementCreator;
import org.springframework.stereotype.Repository;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.CallableStatement;
import java.util.ArrayList;
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
            log.warn("Catalogo sin resultados: {} · args={} · {}", sql, java.util.Arrays.toString(args), e.getMessage());
            return Collections.emptyList();
        }
    }

    /**
     * Como consultar(), pero deja de leer tras {@code limite} filas (0 = todas).
     *
     * Los buscadores de la pantalla solo muestran la primera pagina. Sin esto,
     * abrir el buscador sin texto traia 6.681 articulos o 12.199 clientes que
     * viajaban enteros hasta el navegador para mostrar 20. setMaxRows corta en
     * el driver: el procedimiento del ERP no se toca.
     */
    private List<Map<String, Object>> consultarHasta(int limite, String sql, Object... args) {
        if (limite <= 0) {
            return consultar(sql, args);
        }
        try {
            return jdbc.query((PreparedStatementCreator) con -> {
                CallableStatement cs = con.prepareCall(sql);
                cs.setMaxRows(limite);
                for (int i = 0; i < args.length; i++) {
                    cs.setObject(i + 1, args[i]);
                }
                return cs;
            }, new ColumnMapRowMapper());
        } catch (Exception e) {
            log.warn("Catalogo sin resultados: {} · args={} · limite={} · {}", sql, java.util.Arrays.toString(args), limite, e.getMessage());
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
        return articulos(valor, tipoConsulta, codEstacion, codAlmacen, codListaPrecio, 0);
    }

    public List<Map<String, Object>> articulos(String valor, int tipoConsulta,
                                               int codEstacion, int codAlmacen, int codListaPrecio, int limite) {
        List<Map<String, Object>> filas = consultarHasta(limite,
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

    /**
     * SP: pr_ObtieneUbigeo · 1 departamento · 2 provincia · 3 distrito
     *
     * La firma del SP es (@tipo int, @ubigeo varchar(10)), en ese orden. Se
     * pasaban al reves, asi que el codigo del padre entraba como @tipo: '1501'
     * se convertia al int 1501, ninguna de las tres ramas del SP coincidia, el
     * procedimiento terminaba sin devolver un conjunto de resultados y
     * consultar() lo tragaba como catalogo vacio. La cascada de ubigeos salia
     * vacia en los tres niveles sin ningun error visible.
     *
     * El SP nombra sus columnas ubigeo/Tipo/descripcion, no CodUbigeo: pedir
     * "CodUbigeo" dejaba codUbigeo en null aunque hubiera filas.
     */
    public List<Map<String, Object>> ubigeos(String codigoPadre, int tipoConsulta) {
        List<Map<String, Object>> filas = Mapeo.proyectar(
                consultar("{ call pr_ObtieneUbigeo(?,?) }", tipoConsulta, codigoPadre),
                "ubigeo",      "codUbigeo",
                "descripcion", "descripcion");

        return sinFilasResumen(filas);
    }

    /**
     * Quita del catalogo de ubigeos las filas que no son una opcion elegible.
     *
     * maestrodistrito guarda, junto a los distritos, una fila de resumen por
     * provincia: el codigo termina en "00" (130100 para Trujillo) y viene sin
     * descripcion. El SP la devuelve con el resto, y en el desplegable de
     * distritos aparecia como una opcion EN BLANCO al principio de la lista.
     *
     * Se filtra aqui, en la ApiGRE, y no en el procedimiento ni en la tabla,
     * a proposito: los procedimientos y los datos ya estan instalados en los
     * clientes y funcionan. Arreglarlo en este punto lo corrige para todos sin
     * que nadie tenga que tocar su base de datos.
     */
    private static List<Map<String, Object>> sinFilasResumen(List<Map<String, Object>> filas) {
        List<Map<String, Object>> limpias = new ArrayList<>(filas.size());

        for (Map<String, Object> fila : filas) {
            Object cod  = fila.get("codUbigeo");
            Object desc = fila.get("descripcion");

            String codigo      = cod  == null ? "" : String.valueOf(cod).trim();
            String descripcion = desc == null ? "" : String.valueOf(desc).trim();

            // Sin nombre no se puede elegir, y un distrito real nunca termina
            // en "00": ese sufijo es siempre la cabecera de la provincia.
            if (descripcion.isEmpty() || (codigo.length() == 6 && codigo.endsWith("00"))) {
                continue;
            }

            limpias.add(fila);
        }

        return limpias;
    }

    /**
     * SP: GetGuiaRemisionLikeForTipo. El mismo procedimiento sirve para
     * transportistas, vehiculos y choferes segun el tipo. Se devuelve crudo
     * porque las columnas cambian entre ellos y el mapeo se hace arriba.
     */
    private List<Map<String, Object>> guiaRemisionLike(String valor, int tipo) {
        return guiaRemisionLike(valor, tipo, 0);
    }

    private List<Map<String, Object>> guiaRemisionLike(String valor, int tipo, int limite) {
        return consultarHasta(limite, "{ call GetGuiaRemisionLikeForTipo(?,?) }", valor, tipo);
    }

    /**
     * GetGuiaRemisionLikeForTipo devuelve "select *" de tres tablas distintas
     * segun el tipo, asi que la proyeccion no puede ser una sola: se hace en el
     * metodo de cada catalogo. Sin proyectar salian en PascalCase y Laravel las
     * lee en camelCase, el mismo fallo que tenia el catalogo de proveedores y
     * que reventaba con 500 en cuanto la busqueda encontraba algo. Aqui no se
     * habia notado porque la tabla Transportista esta vacia.
     */
    public List<Map<String, Object>> transportistas(String valor, int tipo) {
        return transportistas(valor, tipo, 0);
    }

    public List<Map<String, Object>> transportistas(String valor, int tipo, int limite) {
        return Mapeo.proyectar(guiaRemisionLike(valor, tipo, limite),
                "CodTransportista",       "codTransportista",
                "NombreTransportista",    "nombreTransportista",
                "DireccionTransportista", "direccionTransportista",
                "RucTransportista",       "rucTransportista",
                "TelefonoTransportista",  "telefonoTransportista",
                "Estado",                 "estado");
    }

    public List<Map<String, Object>> vehiculos(String valor, int tipo) {
        return Mapeo.proyectar(guiaRemisionLike(valor, tipo),
                "PlacaVehiculo",       "placaVehiculo",
                "MarcaVehiculo",       "marcaVehiculo",
                "CodTransportista",    "codTransportista",
                "NombreTransportista", "nombreTransportista",
                "Estado",              "estado");
    }

    public List<Map<String, Object>> choferes(String valor) {
        return Mapeo.proyectar(guiaRemisionLike(valor, 6),
                "NombreChofer",  "nombreChofer",
                "DniChofer",     "dniChofer",
                "BreveteChofer", "breveteChofer",
                "Telefono",      "telefono",
                "Estado",        "estado");
    }

    /**
     * SP: GetDatosClientexTipo
     *
     * La firma es (@Tipoconsulta int, @Valor varchar), en ese orden, y se
     * pasaba al reves: el texto buscado entraba como @Tipoconsulta y SQL Server
     * respondia "Error converting data type nvarchar to int". consultar() lo
     * tragaba como catalogo vacio, asi que el buscador de clientes decia
     * "sin resultados" para CUALQUIER busqueda, sin un solo error visible.
     * Mismo fallo que tenia la cascada de ubigeos.
     */
    public List<Map<String, Object>> clientes(String valor, int tipo) {
        return clientes(valor, tipo, 0);
    }

    public List<Map<String, Object>> clientes(String valor, int tipo, int limite) {
        return Mapeo.proyectar(consultarHasta(limite, "{ call GetDatosClientexTipo(?,?) }", tipo, valor),
                "CodCliente",             "codCliente",
                "RazonSocial",            "razonSocial",
                "Direccion",              "direccion",
                "RucCliente",             "rucCliente",
                "Dni",                    "dni",
                "TipoDocumentoIdentidad", "tipoDocumentoIdentidad");
    }

    /**
     * SPs: GetMaestroproveedoresByRuc · pr_consultaProveedorlikeRazonsocial
     *
     * Era el UNICO catalogo que salia sin proyectar: devolvia las columnas tal
     * como las nombra el procedimiento (CodProveedor, NombreProveedor, Ruc) y
     * Laravel las lee en camelCase, asi que en cuanto la busqueda encontraba
     * algo el buscador de proveedor respondia 500 con "Undefined property:
     * stdClass::$codProveedor". Pasaba desapercibido porque la busqueda por
     * razon social usa un procedimiento que no esta en todas las
     * instalaciones: sin el, la lista sale vacia y nunca se llega a fallar.
     */
    public List<Map<String, Object>> proveedores(String valor, int tipo) {
        return proveedores(valor, tipo, 0);
    }

    public List<Map<String, Object>> proveedores(String valor, int tipo, int limite) {
        String sp = (tipo == 3) ? "pr_consultaProveedorlikeRazonsocial" : "GetMaestroproveedoresByRuc";

        return Mapeo.proyectar(consultarHasta(limite, "{ call " + sp + "(?) }", valor),
                "CodProveedor",     "codProveedor",
                "NombreProveedor",  "nombreproveedor",
                "Ruc",              "ruc",
                "Direccion",        "direccion",
                "Telefono",         "telefono",
                "CodEstacion",      "codEstacion");
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
