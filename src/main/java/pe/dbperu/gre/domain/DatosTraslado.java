package pe.dbperu.gre.domain;

/** Datos de traslado. Solo aplican a guias de salida (tipoGuia = A). */
public class DatosTraslado {
    /** Catalogo 18 SUNAT. Antes estaba cableado como "18" en tres sitios. */
    private String modalidadTransporte = "18";
    private String direccionPartida;
    private String ubigeoPartida;
    private String direccionLlegada;
    private String ubigeoLlegada;
    private String placaVehiculo;
    private String nombreChofer;
    private String dniChofer;
    private String breveteChofer;
    private String rucTransportista;
    private String nombreTransportista;

    public String getModalidadTransporte() { return modalidadTransporte; }
    public void setModalidadTransporte(String v) { this.modalidadTransporte = v; }
    public String getDireccionPartida() { return direccionPartida; }
    public void setDireccionPartida(String v) { this.direccionPartida = v; }
    public String getUbigeoPartida() { return ubigeoPartida; }
    public void setUbigeoPartida(String v) { this.ubigeoPartida = v; }
    public String getDireccionLlegada() { return direccionLlegada; }
    public void setDireccionLlegada(String v) { this.direccionLlegada = v; }
    public String getUbigeoLlegada() { return ubigeoLlegada; }
    public void setUbigeoLlegada(String v) { this.ubigeoLlegada = v; }
    public String getPlacaVehiculo() { return placaVehiculo; }
    public void setPlacaVehiculo(String v) { this.placaVehiculo = v; }
    public String getNombreChofer() { return nombreChofer; }
    public void setNombreChofer(String v) { this.nombreChofer = v; }
    public String getDniChofer() { return dniChofer; }
    public void setDniChofer(String v) { this.dniChofer = v; }
    public String getBreveteChofer() { return breveteChofer; }
    public void setBreveteChofer(String v) { this.breveteChofer = v; }
    public String getRucTransportista() { return rucTransportista; }
    public void setRucTransportista(String v) { this.rucTransportista = v; }
    public String getNombreTransportista() { return nombreTransportista; }
    public void setNombreTransportista(String v) { this.nombreTransportista = v; }
}
