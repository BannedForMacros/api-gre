package pe.dbperu.gre.domain;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

public class GuiaResponse {
    private Integer anio;
    private String  tipoGuia;
    private Integer numSerie;
    private Long    numeroGuia;
    private boolean exito;
    private String  mensaje;
    private BigDecimal valorVenta;
    private BigDecimal igv;
    private BigDecimal totalVenta;
    private BigDecimal tasaIgvAplicada;
    private List<LineaRechazada> lineasRechazadas = new ArrayList<>();

    public Integer getAnio() { return anio; }
    public void setAnio(Integer v) { this.anio = v; }
    public String getTipoGuia() { return tipoGuia; }
    public void setTipoGuia(String v) { this.tipoGuia = v; }
    public Integer getNumSerie() { return numSerie; }
    public void setNumSerie(Integer v) { this.numSerie = v; }
    public Long getNumeroGuia() { return numeroGuia; }
    public void setNumeroGuia(Long v) { this.numeroGuia = v; }
    public boolean isExito() { return exito; }
    public void setExito(boolean v) { this.exito = v; }
    public String getMensaje() { return mensaje; }
    public void setMensaje(String v) { this.mensaje = v; }
    public BigDecimal getValorVenta() { return valorVenta; }
    public void setValorVenta(BigDecimal v) { this.valorVenta = v; }
    public BigDecimal getIgv() { return igv; }
    public void setIgv(BigDecimal v) { this.igv = v; }
    public BigDecimal getTotalVenta() { return totalVenta; }
    public void setTotalVenta(BigDecimal v) { this.totalVenta = v; }
    public BigDecimal getTasaIgvAplicada() { return tasaIgvAplicada; }
    public void setTasaIgvAplicada(BigDecimal v) { this.tasaIgvAplicada = v; }
    public List<LineaRechazada> getLineasRechazadas() { return lineasRechazadas; }
    public void setLineasRechazadas(List<LineaRechazada> v) { this.lineasRechazadas = v; }
}
