package pe.dbperu.gre.web;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import pe.dbperu.gre.domain.ResultadoInsercion;
import pe.dbperu.gre.repository.GuiaRepository;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * InsertGuiaDMK con el contrato del ApiDMK.
 *
 * Existe para que Laravel pueda apuntar a la ApiGRE sin migrar todavia a
 * POST /api/v1/guias. Recibe el cuerpo plano de siempre (DatosGuiaRem) y lo
 * pasa tal cual al stored procedure.
 *
 * DIFERENCIA IMPORTANTE CON EL CONTRATO NUEVO
 *   Aqui los importes se toman como vienen: el cliente ya aplico su IGV. El
 *   contrato nuevo recibe precios sin IGV y lo calcula el servidor, que es lo
 *   correcto, pero cambiar eso a mitad de la migracion movería los totales de
 *   las guias sin aviso.
 *
 * Este endpoint se retira cuando Laravel use /api/v1/guias.
 */
@RestController
public class GuiaLegacyController {

    private static final Logger log = LoggerFactory.getLogger(GuiaLegacyController.class);

    private final GuiaRepository repo;

    public GuiaLegacyController(GuiaRepository repo) {
        this.repo = repo;
    }

    @PostMapping("/GREDMK/InsertGuiaDMK")
    public Map<String, Object> insertGuiaDMK(@RequestBody Map<String, Object> body) {
        String xml = construirXml(body);
        ResultadoInsercion r = repo.insertarGuia(xml);

        Map<String, Object> resp = new LinkedHashMap<>();

        if (! r.isExito()) {
            resp.put("exito", false);
            resp.put("msgerror", r.getMensaje());
            return resp;
        }

        // El procedimiento dijo que si, pero eso solo significa que la guia
        // entro a la cola. Se comprueba que haya llegado al destino: el ERP
        // puede rechazarla despues y el codigo de retorno no se entera.
        int    anio   = entero(body.get("anioGuiaRemision"));
        String tipo   = body.get("tipoGuia") == null ? "" : String.valueOf(body.get("tipoGuia"));
        int    serie  = entero(body.get("numSerie"));
        long   numero = largo(body.get("numeroGuia"));

        boolean aterrizo;
        try {
            aterrizo = repo.existeEnDataMart(anio, tipo, serie, numero);
        } catch (Exception e) {
            // Si no se puede comprobar, no se inventa un fracaso: se informa
            // el resultado del procedimiento y se deja rastro.
            log.warn("No se pudo verificar la guia {}-{} en el DataMart: {}", serie, numero, e.getMessage());
            resp.put("exito", true);
            resp.put("msgerror", r.getMensaje());
            resp.put("verificado", false);
            return resp;
        }

        if (aterrizo) {
            resp.put("exito", true);
            resp.put("msgerror", r.getMensaje());
            resp.put("verificado", true);
            return resp;
        }

        String motivo = repo.motivoRechazo(anio, serie, numero);
        log.warn("Guia {}-{} quedo en la cola sin llegar a GuiaRemision. Motivo: {}",
                 serie, numero, motivo == null ? "(sin registrar)" : motivo);

        resp.put("exito", false);
        resp.put("msgerror", motivo != null && ! motivo.isEmpty()
                ? "El DataMart rechazo la guia: " + motivo
                : "La guia quedo en la cola y no llego al DataMart. Revise el proceso del ERP.");
        resp.put("verificado", true);
        resp.put("enCola", true);
        return resp;
    }

    private static int entero(Object v) {
        if (v == null) { return 0; }
        try { return Integer.parseInt(String.valueOf(v).trim()); }
        catch (NumberFormatException e) { return 0; }
    }

    private static long largo(Object v) {
        if (v == null) { return 0L; }
        try { return Long.parseLong(String.valueOf(v).trim()); }
        catch (NumberFormatException e) { return 0L; }
    }

    @SuppressWarnings("unchecked")
    private String construirXml(Map<String, Object> b) {
        StringBuilder x = new StringBuilder(2048);
        x.append("<DatosGuiaRem>");

        tag(x, "AnioGuiaRemision",   b.get("anioGuiaRemision"));
        tag(x, "NumSerie",           b.get("numSerie"));
        tag(x, "NumeroGuia",         b.get("numeroGuia"));
        tag(x, "TipoGuia",           b.get("tipoGuia"));
        tag(x, "CodProveedor",       nz(b.get("codProveedor")));
        tag(x, "CodCliente",         nz(b.get("codCliente")));
        tag(x, "CodEstacion",        nz(b.get("codEstacion")));
        tag(x, "FechaEmision",       fecha(b.get("fechaEmision")));
        tag(x, "TipoOperacion",      b.get("tipoOperacion"));
        tag(x, "ValorVenta",         nzDec(b.get("valorVenta")));
        tag(x, "IGV",                nzDec(b.get("igv")));
        tag(x, "TotalVenta",         nzDec(b.get("totalVenta")));
        tag(x, "Comentario",         b.get("comentario"));
        tag(x, "CodAlmacen",         nz(b.get("codAlmacen")));
        tag(x, "CodAlmacenOrigen",   nz(b.get("codAlmacenOrigen")));
        tag(x, "CodAlmacenDestino",  nz(b.get("codAlmacenDestino")));
        tag(x, "CodListaPrecio",     b.get("codListaPrecio") == null ? 1 : b.get("codListaPrecio"));
        tag(x, "EstadoProceso",      "0");
        tag(x, "Seriefactura",       nz(b.get("seriefactura")));
        tag(x, "NumeroFactura",      nz(b.get("numeroFactura")));
        tag(x, "codtrabajador",      nz(b.get("codtrabajador")));
        tag(x, "tipomonda",          b.get("tipomonda") == null ? 1 : b.get("tipomonda"));
        tag(x, "formapago",          nz(b.get("formapago")));
        tag(x, "descuento",          nzDec(b.get("descuento")));

        tag(x, "modalidadTransporte", b.get("modalidadTransporte"));
        tag(x, "direccionpartida",    b.get("direccionpartida"));
        tag(x, "direccionllegada",    b.get("direccionllegada"));
        tag(x, "ubigeopartida",       b.get("ubigeopartida"));
        tag(x, "ubigeollegada",       b.get("ubigeollegada"));
        tag(x, "placavehiculo",       b.get("placavehiculo"));
        tag(x, "Nombrechofer",        b.get("nombrechofer"));
        tag(x, "DNIChofer",           b.get("dnichofer"));
        tag(x, "BreveteChofer",       b.get("breveteChofer"));
        tag(x, "RucTransportista",    b.get("rucTransportista"));
        tag(x, "NombreTransportista", b.get("nombreTransportista"));

        x.append("<Detalle>");
        Object det = b.get("detalle");
        if (det instanceof List) {
            int item = 1;
            for (Object o : (List<Object>) det) {
                if (!(o instanceof Map)) { continue; }
                Map<String, Object> l = (Map<String, Object>) o;
                x.append("<DatosDetaGuiaRem>");
                tag(x, "AnioGuiaRemision", b.get("anioGuiaRemision"));
                tag(x, "NumSerie",         b.get("numSerie"));
                tag(x, "NumeroGuia",       b.get("numeroGuia"));
                tag(x, "TipoGuia",         b.get("tipoGuia"));
                tag(x, "CodArticulo",      l.get("codArticulo"));
                tag(x, "Cantidad",         nzDec(l.get("cantidad")));
                tag(x, "Precio",           nzDec(l.get("precio")));
                tag(x, "UnidadMedida",     l.get("unidadMedida") == null ? 1 : l.get("unidadMedida"));
                tag(x, "ImporteDetalle",   nzDec(l.get("importeDetalle")));
                // El cliente mandaba item=1 en todas las lineas. Se numera aqui
                // para que el detalle quede correlativo en el DataMart.
                tag(x, "Item",             item++);
                tag(x, "EstadoProceso",    "0");
                tag(x, "Descuento",        nzDec(l.get("descuento")));
                tag(x, "tipoigv",          l.get("tipoIgv") == null ? 1 : l.get("tipoIgv"));
                tag(x, "EsConsignado",     esVerdadero(l.get("esConsignado")) ? "1" : "0");
                x.append("</DatosDetaGuiaRem>");
            }
        }
        x.append("</Detalle>");
        x.append("</DatosGuiaRem>");
        return x.toString();
    }

    private static boolean esVerdadero(Object v) {
        if (v == null) { return false; }
        String s = String.valueOf(v).trim();
        return "1".equals(s) || "true".equalsIgnoreCase(s);
    }

    private static Object nz(Object v)    { return v == null ? 0 : v; }
    private static Object nzDec(Object v) { return v == null ? BigDecimal.ZERO : v; }

    /** El cliente manda "2026-09-10 00:00:00"; el SP espera solo la fecha. */
    private static Object fecha(Object v) {
        if (v == null) { return null; }
        String s = String.valueOf(v);
        return s.length() >= 10 ? s.substring(0, 10) : s;
    }

    private static void tag(StringBuilder x, String nombre, Object valor) {
        if (valor == null) { return; }
        x.append('<').append(nombre).append('>')
         .append(pe.dbperu.gre.repository.GuiaXmlBuilder.escapar(String.valueOf(valor)))
         .append("</").append(nombre).append('>');
    }
}
