package pe.dbperu.gre.domain;

import javax.validation.constraints.DecimalMin;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;
import java.math.BigDecimal;

/** Una linea del detalle de la guia. */
public class LineaGuia {

    @Min(value = 1, message = "el item debe ser correlativo desde 1")
    private int item;

    @NotNull(message = "codArticulo es obligatorio")
    private Integer codArticulo;

    /** Mayor que cero: el SP del ERP divide entre (Cantidad * Precio). */
    @NotNull
    @DecimalMin(value = "0.0001", message = "la cantidad debe ser mayor que cero")
    private BigDecimal cantidad;

    /** SIEMPRE sin IGV. La tasa la aplica el servidor, no el cliente. */
    @NotNull
    @DecimalMin(value = "0.0", message = "el precio no puede ser negativo")
    private BigDecimal precioSinIgv;

    private BigDecimal descuento = BigDecimal.ZERO;
    private Integer unidadMedida = 1;
    private Integer tipoIgv = 1;

    /**
     * EL CAMPO QUE FALTABA.
     *
     * Antes el flag de consignado no tenia por donde viajar: el DTO no lo
     * tenia y DetalleGuiaRemision_Odoo no tenia la columna. Por eso el SP
     * terminaba copiandolo de MaestroArticulo.consignacion, y habia que
     * marcar el maestro ANTES del SP o el dato se perdia. De ahi salia el
     * "a veces no se pintan los consignados".
     */
    private boolean esConsignado = false;

    public int getItem() { return item; }
    public void setItem(int item) { this.item = item; }
    public Integer getCodArticulo() { return codArticulo; }
    public void setCodArticulo(Integer codArticulo) { this.codArticulo = codArticulo; }
    public BigDecimal getCantidad() { return cantidad; }
    public void setCantidad(BigDecimal cantidad) { this.cantidad = cantidad; }
    public BigDecimal getPrecioSinIgv() { return precioSinIgv; }
    public void setPrecioSinIgv(BigDecimal precioSinIgv) { this.precioSinIgv = precioSinIgv; }
    public BigDecimal getDescuento() { return descuento; }
    public void setDescuento(BigDecimal descuento) { this.descuento = descuento; }
    public Integer getUnidadMedida() { return unidadMedida; }
    public void setUnidadMedida(Integer unidadMedida) { this.unidadMedida = unidadMedida; }
    public Integer getTipoIgv() { return tipoIgv; }
    public void setTipoIgv(Integer tipoIgv) { this.tipoIgv = tipoIgv; }
    public boolean isEsConsignado() { return esConsignado; }
    public void setEsConsignado(boolean esConsignado) { this.esConsignado = esConsignado; }

    /** Base imponible de la linea, con el descuento ya aplicado. */
    public BigDecimal valorVenta() {
        BigDecimal bruto = cantidad.multiply(precioSinIgv);
        BigDecimal desc  = descuento == null ? BigDecimal.ZERO : descuento;
        return bruto.subtract(desc).setScale(4, BigDecimal.ROUND_HALF_UP);
    }

    public boolean afectoIgv() {
        return tipoIgv != null && tipoIgv == 1;
    }
}
