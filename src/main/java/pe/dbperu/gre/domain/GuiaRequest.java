package pe.dbperu.gre.domain;

import javax.validation.Valid;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * Guia a registrar en el DataMart.
 *
 * NO recibe valorVenta, igv ni totalVenta: los calcula el servidor desde el
 * detalle. Antes Laravel los calculaba, el JavaScript los recalculaba y el
 * stored procedure los volvia a tocar — tres fuentes de verdad para el mismo
 * numero.
 */
public class GuiaRequest {

    @NotNull private Integer anio;
    @NotNull @Pattern(regexp = "N|A", message = "tipoGuia debe ser N (ingreso) o A (salida)")
    private String tipoGuia;
    @NotNull private Integer numSerie;
    @NotNull private Long numeroGuia;
    @NotNull private String fechaEmision;
    private String fechaInicioTraslado;

    @NotNull private Integer codEstacion;
    @NotNull private Integer tipoOperacion;
    private Integer codAlmacen = 0;
    private Integer codAlmacenOrigen = 0;
    private Integer codAlmacenDestino = 0;
    private Integer codListaPrecio = 1;
    private Integer codProveedor = 0;
    private Integer codCliente = 0;
    private Integer codTrabajador = 0;
    private Integer formaPago = 1;
    private Integer tipoMoneda = 1;
    private String  comentario;
    private BigDecimal descuento = BigDecimal.ZERO;

    private DatosTraslado traslado = new DatosTraslado();

    @NotEmpty(message = "la guia debe tener al menos una linea")
    @Valid
    private List<LineaGuia> detalle = new ArrayList<>();

    public Integer getAnio() { return anio; }
    public void setAnio(Integer anio) { this.anio = anio; }
    public String getTipoGuia() { return tipoGuia; }
    public void setTipoGuia(String tipoGuia) { this.tipoGuia = tipoGuia; }
    public Integer getNumSerie() { return numSerie; }
    public void setNumSerie(Integer numSerie) { this.numSerie = numSerie; }
    public Long getNumeroGuia() { return numeroGuia; }
    public void setNumeroGuia(Long numeroGuia) { this.numeroGuia = numeroGuia; }
    public String getFechaEmision() { return fechaEmision; }
    public void setFechaEmision(String fechaEmision) { this.fechaEmision = fechaEmision; }
    public String getFechaInicioTraslado() { return fechaInicioTraslado; }
    public void setFechaInicioTraslado(String f) { this.fechaInicioTraslado = f; }
    public Integer getCodEstacion() { return codEstacion; }
    public void setCodEstacion(Integer v) { this.codEstacion = v; }
    public Integer getTipoOperacion() { return tipoOperacion; }
    public void setTipoOperacion(Integer v) { this.tipoOperacion = v; }
    public Integer getCodAlmacen() { return codAlmacen; }
    public void setCodAlmacen(Integer v) { this.codAlmacen = v; }
    public Integer getCodAlmacenOrigen() { return codAlmacenOrigen; }
    public void setCodAlmacenOrigen(Integer v) { this.codAlmacenOrigen = v; }
    public Integer getCodAlmacenDestino() { return codAlmacenDestino; }
    public void setCodAlmacenDestino(Integer v) { this.codAlmacenDestino = v; }
    public Integer getCodListaPrecio() { return codListaPrecio; }
    public void setCodListaPrecio(Integer v) { this.codListaPrecio = v; }
    public Integer getCodProveedor() { return codProveedor; }
    public void setCodProveedor(Integer v) { this.codProveedor = v; }
    public Integer getCodCliente() { return codCliente; }
    public void setCodCliente(Integer v) { this.codCliente = v; }
    public Integer getCodTrabajador() { return codTrabajador; }
    public void setCodTrabajador(Integer v) { this.codTrabajador = v; }
    public Integer getFormaPago() { return formaPago; }
    public void setFormaPago(Integer v) { this.formaPago = v; }
    public Integer getTipoMoneda() { return tipoMoneda; }
    public void setTipoMoneda(Integer v) { this.tipoMoneda = v; }
    public String getComentario() { return comentario; }
    public void setComentario(String v) { this.comentario = v; }
    public BigDecimal getDescuento() { return descuento; }
    public void setDescuento(BigDecimal v) { this.descuento = v; }
    public DatosTraslado getTraslado() { return traslado; }
    public void setTraslado(DatosTraslado v) { this.traslado = v; }
    public List<LineaGuia> getDetalle() { return detalle; }
    public void setDetalle(List<LineaGuia> v) { this.detalle = v; }
}
