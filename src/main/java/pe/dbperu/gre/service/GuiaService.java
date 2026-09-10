package pe.dbperu.gre.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.dbperu.gre.domain.*;
import pe.dbperu.gre.repository.GuiaRepository;
import pe.dbperu.gre.repository.GuiaXmlBuilder;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Registro de guias en el DataMart.
 *
 * Los importes se calculan AQUI, una sola vez, con la tasa configurada.
 * El cliente manda precios sin IGV y nada mas.
 */
@Service
public class GuiaService {

    private static final Logger log = LoggerFactory.getLogger(GuiaService.class);

    private final GuiaRepository repo;
    private final GuiaXmlBuilder xml;
    private final BigDecimal tasaIgv;

    public GuiaService(GuiaRepository repo,
                       GuiaXmlBuilder xml,
                       @Value("${gre.igv.tasa:0.18}") BigDecimal tasaIgv) {
        this.repo = repo;
        this.xml = xml;
        this.tasaIgv = tasaIgv;
    }

    @Transactional
    public GuiaResponse registrar(GuiaRequest g) {
        // 1. Detectar lineas que el ERP descartaria en silencio por el INNER JOIN.
        List<Integer> codigos = g.getDetalle().stream()
                .map(LineaGuia::getCodArticulo)
                .distinct()
                .collect(Collectors.toList());

        List<Integer> faltantes = repo.articulosInexistentes(codigos);

        List<LineaRechazada> rechazadas = new ArrayList<>();
        for (LineaGuia l : g.getDetalle()) {
            if (faltantes.contains(l.getCodArticulo())) {
                rechazadas.add(new LineaRechazada(
                        l.getItem(), l.getCodArticulo(),
                        "No existe en MaestroArticulo: el DataMart descartaria esta linea"));
            }
        }

        // 2. Totales, una sola vez.
        BigDecimal valorVenta = BigDecimal.ZERO;
        BigDecimal igv        = BigDecimal.ZERO;
        for (LineaGuia l : g.getDetalle()) {
            BigDecimal base = l.valorVenta();
            valorVenta = valorVenta.add(base);
            if (l.afectoIgv()) {
                igv = igv.add(base.multiply(tasaIgv));
            }
        }
        BigDecimal desc = g.getDescuento() == null ? BigDecimal.ZERO : g.getDescuento();
        valorVenta = valorVenta.subtract(desc).setScale(2, RoundingMode.HALF_UP);
        igv        = igv.setScale(2, RoundingMode.HALF_UP);
        BigDecimal total = valorVenta.add(igv).setScale(2, RoundingMode.HALF_UP);

        // 3. Al DataMart.
        ResultadoInsercion r = repo.insertarGuia(xml.construir(g, valorVenta, igv, total));

        GuiaResponse resp = new GuiaResponse();
        resp.setAnio(g.getAnio());
        resp.setTipoGuia(g.getTipoGuia());
        resp.setNumSerie(g.getNumSerie());
        resp.setNumeroGuia(g.getNumeroGuia());
        // El codigo de retorno del procedimiento solo dice que la guia entro a
        // la cola; el ERP puede rechazarla despues y el resultado seguiria
        // diciendo que si. Se comprueba que exista en destino antes de dar el
        // envio por bueno. (Mismo criterio que /GREDMK/InsertGuiaDMK.)
        boolean exito  = r.isExito();
        String  mensaje = r.getMensaje();

        if (exito) {
            try {
                if (! repo.existeEnDataMart(g.getAnio(), g.getTipoGuia(), g.getNumSerie(), g.getNumeroGuia())) {
                    String motivo = repo.motivoRechazo(g.getAnio(), g.getNumSerie(), g.getNumeroGuia());
                    exito   = false;
                    mensaje = motivo != null && ! motivo.isEmpty()
                            ? "El DataMart rechazo la guia: " + motivo
                            : "La guia quedo en la cola y no llego al DataMart. Revise el proceso del ERP.";
                    log.warn("Guia {}-{} no llego a GuiaRemision. Motivo: {}",
                             g.getNumSerie(), g.getNumeroGuia(), motivo == null ? "(sin registrar)" : motivo);
                }
            } catch (Exception e) {
                // No poder comprobar no es lo mismo que fallar: se respeta el
                // resultado del procedimiento y queda el rastro en el log.
                log.warn("No se pudo verificar la guia {}-{}: {}",
                         g.getNumSerie(), g.getNumeroGuia(), e.getMessage());
            }
        }

        resp.setExito(exito);
        resp.setMensaje(mensaje);
        resp.setValorVenta(valorVenta);
        resp.setIgv(igv);
        resp.setTotalVenta(total);
        resp.setTasaIgvAplicada(tasaIgv);
        resp.setLineasRechazadas(rechazadas);
        return resp;
    }
}
