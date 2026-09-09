package pe.dbperu.gre.repository;

import org.springframework.stereotype.Component;
import pe.dbperu.gre.domain.GuiaRequest;
import pe.dbperu.gre.domain.LineaGuia;

import java.math.BigDecimal;

/**
 * Arma el XML que espera prc_InsertGuiaDMKWeb.
 *
 * El ApiDMK original usaba XStream para serializar el objeto Java, lo que
 * ataba el formato del XML a los nombres de clase (por eso el SP hace
 * replace de 'DbPeru.Posm.Modelo.DatosGuiaRem'). Aqui el XML se construye
 * explicitamente: el contrato queda a la vista, sin dependencia externa y
 * sin las CVEs de deserializacion de XStream.
 */
@Component
public class GuiaXmlBuilder {

    public String construir(GuiaRequest g, BigDecimal valorVenta, BigDecimal igv, BigDecimal total) {
        StringBuilder x = new StringBuilder(2048);
        x.append("<DatosGuiaRem>");
        tag(x, "AnioGuiaRemision", g.getAnio());
        tag(x, "NumSerie",         g.getNumSerie());
        tag(x, "NumeroGuia",       g.getNumeroGuia());
        tag(x, "TipoGuia",         g.getTipoGuia());
        tag(x, "CodProveedor",     nz(g.getCodProveedor()));
        tag(x, "CodCliente",       nz(g.getCodCliente()));
        tag(x, "CodEstacion",      g.getCodEstacion());
        tag(x, "FechaEmision",     g.getFechaEmision());
        tag(x, "TipoOperacion",    g.getTipoOperacion());
        tag(x, "ValorVenta",       valorVenta);
        tag(x, "IGV",              igv);
        tag(x, "TotalVenta",       total);
        tag(x, "Comentario",       g.getComentario());
        tag(x, "CodAlmacen",       nz(g.getCodAlmacen()));
        tag(x, "CodAlmacenOrigen", nz(g.getCodAlmacenOrigen()));
        tag(x, "CodAlmacenDestino",nz(g.getCodAlmacenDestino()));
        tag(x, "CodListaPrecio",   g.getCodListaPrecio() == null ? 1 : g.getCodListaPrecio());
        tag(x, "EstadoProceso",    "0");
        tag(x, "Seriefactura",     0);
        tag(x, "NumeroFactura",    0);
        tag(x, "codtrabajador",    nz(g.getCodTrabajador()));
        tag(x, "tipomonda",        g.getTipoMoneda() == null ? 1 : g.getTipoMoneda());
        tag(x, "formapago",        nz(g.getFormaPago()));
        tag(x, "descuento",        g.getDescuento() == null ? BigDecimal.ZERO : g.getDescuento());

        if (g.getTraslado() != null) {
            tag(x, "modalidadTransporte", g.getTraslado().getModalidadTransporte());
            tag(x, "direccionpartida",    g.getTraslado().getDireccionPartida());
            tag(x, "direccionllegada",    g.getTraslado().getDireccionLlegada());
            tag(x, "ubigeopartida",       g.getTraslado().getUbigeoPartida());
            tag(x, "ubigeollegada",       g.getTraslado().getUbigeoLlegada());
            tag(x, "placavehiculo",       g.getTraslado().getPlacaVehiculo());
            tag(x, "Nombrechofer",        g.getTraslado().getNombreChofer());
            tag(x, "DNIChofer",           g.getTraslado().getDniChofer());
            tag(x, "BreveteChofer",       g.getTraslado().getBreveteChofer());
            tag(x, "RucTransportista",    g.getTraslado().getRucTransportista());
            tag(x, "NombreTransportista", g.getTraslado().getNombreTransportista());
        }

        x.append("<Detalle>");
        for (LineaGuia l : g.getDetalle()) {
            x.append("<DatosDetaGuiaRem>");
            tag(x, "AnioGuiaRemision", g.getAnio());
            tag(x, "NumSerie",         g.getNumSerie());
            tag(x, "NumeroGuia",       g.getNumeroGuia());
            tag(x, "TipoGuia",         g.getTipoGuia());
            tag(x, "CodArticulo",      l.getCodArticulo());
            tag(x, "Cantidad",         l.getCantidad());
            tag(x, "Precio",           l.getPrecioSinIgv());
            tag(x, "UnidadMedida",     l.getUnidadMedida());
            tag(x, "ImporteDetalle",   l.valorVenta());
            tag(x, "Item",             l.getItem());
            tag(x, "EstadoProceso",    "0");
            tag(x, "Descuento",        l.getDescuento() == null ? BigDecimal.ZERO : l.getDescuento());
            tag(x, "tipoigv",          l.getTipoIgv());
            // El campo que cierra la cadena de consignados hasta el DataMart.
            tag(x, "EsConsignado",     l.isEsConsignado() ? "1" : "0");
            x.append("</DatosDetaGuiaRem>");
        }
        x.append("</Detalle>");
        x.append("</DatosGuiaRem>");
        return x.toString();
    }

    private static int nz(Integer v) { return v == null ? 0 : v; }

    private static void tag(StringBuilder x, String nombre, Object valor) {
        if (valor == null) {
            return;
        }
        x.append('<').append(nombre).append('>')
         .append(escapar(String.valueOf(valor)))
         .append("</").append(nombre).append('>');
    }

    /** Escapa los cinco caracteres que romperian el XML. */
    static String escapar(String s) {
        StringBuilder out = new StringBuilder(s.length() + 16);
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            switch (c) {
                case '&':  out.append("&amp;");  break;
                case '<':  out.append("&lt;");   break;
                case '>':  out.append("&gt;");   break;
                case '"':  out.append("&quot;"); break;
                case '\'': out.append("&apos;"); break;
                default:
                    // descarta caracteres de control ilegales en XML 1.0
                    if (c >= 0x20 || c == '\t' || c == '\n' || c == '\r') {
                        out.append(c);
                    }
            }
        }
        return out.toString();
    }
}
