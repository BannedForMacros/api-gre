package pe.dbperu.gre.repository;

import org.junit.jupiter.api.Test;
import pe.dbperu.gre.domain.GuiaRequest;
import pe.dbperu.gre.domain.LineaGuia;

import java.math.BigDecimal;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.*;

class GuiaXmlBuilderTest {

    private final GuiaXmlBuilder builder = new GuiaXmlBuilder();

    private GuiaRequest guiaConDosLineas() {
        GuiaRequest g = new GuiaRequest();
        g.setAnio(2026);
        g.setTipoGuia("N");
        g.setNumSerie(7);
        g.setNumeroGuia(777002L);
        g.setFechaEmision("2026-09-09");
        g.setCodEstacion(1);
        g.setTipoOperacion(37);
        g.setCodProveedor(312);

        LineaGuia consignada = new LineaGuia();
        consignada.setItem(1);
        consignada.setCodArticulo(40978);
        consignada.setCantidad(new BigDecimal("2"));
        consignada.setPrecioSinIgv(new BigDecimal("50.00"));
        consignada.setEsConsignado(true);

        LineaGuia normal = new LineaGuia();
        normal.setItem(2);
        normal.setCodArticulo(40011);
        normal.setCantidad(new BigDecimal("1"));
        normal.setPrecioSinIgv(new BigDecimal("10.00"));
        normal.setEsConsignado(false);

        g.setDetalle(Arrays.asList(consignada, normal));
        return g;
    }

    @Test
    void el_flag_de_consignado_viaja_en_el_xml_por_linea() {
        String xml = builder.construir(guiaConDosLineas(),
                new BigDecimal("110.00"), new BigDecimal("19.80"), new BigDecimal("129.80"));

        assertTrue(xml.contains("<EsConsignado>1</EsConsignado>"), "la linea consignada");
        assertTrue(xml.contains("<EsConsignado>0</EsConsignado>"), "la linea normal");
        assertEquals(2, xml.split("<EsConsignado>", -1).length - 1, "una por linea");
    }

    @Test
    void la_raiz_y_el_detalle_coinciden_con_lo_que_espera_el_stored_procedure() {
        String xml = builder.construir(guiaConDosLineas(), BigDecimal.ONE, BigDecimal.ONE, BigDecimal.ONE);
        // prc_InsertGuiaDMKWeb lee /DatosGuiaRem y /DatosGuiaRem/Detalle/DatosDetaGuiaRem
        assertTrue(xml.startsWith("<DatosGuiaRem>"));
        assertTrue(xml.contains("<Detalle><DatosDetaGuiaRem>"));
        assertTrue(xml.endsWith("</DatosGuiaRem>"));
    }

    @Test
    void el_item_es_correlativo_y_no_uno_en_todas_las_lineas() {
        String xml = builder.construir(guiaConDosLineas(), BigDecimal.ONE, BigDecimal.ONE, BigDecimal.ONE);
        assertTrue(xml.contains("<Item>1</Item>"));
        assertTrue(xml.contains("<Item>2</Item>"), "el codigo viejo mandaba Item=1 en TODAS");
    }

    @Test
    void escapa_los_caracteres_que_romperian_el_xml() {
        GuiaRequest g = guiaConDosLineas();
        g.setComentario("TORNILLOS 1/2\" & TUERCAS <especiales>");
        String xml = builder.construir(g, BigDecimal.ONE, BigDecimal.ONE, BigDecimal.ONE);

        assertTrue(xml.contains("&amp;"));
        assertTrue(xml.contains("&quot;"));
        assertTrue(xml.contains("&lt;especiales&gt;"));
        assertFalse(xml.contains("<especiales>"), "no debe quedar como etiqueta");
    }

    @Test
    void los_nulos_no_generan_etiquetas_vacias() {
        GuiaRequest g = guiaConDosLineas();
        g.setComentario(null);
        String xml = builder.construir(g, BigDecimal.ONE, BigDecimal.ONE, BigDecimal.ONE);
        assertFalse(xml.contains("<Comentario>"));
    }
}
