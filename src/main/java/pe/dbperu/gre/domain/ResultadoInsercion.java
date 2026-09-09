package pe.dbperu.gre.domain;

public class ResultadoInsercion {
    private final boolean exito;
    private final String mensaje;

    public ResultadoInsercion(boolean exito, String mensaje) {
        this.exito = exito;
        this.mensaje = mensaje;
    }

    public boolean isExito()   { return exito; }
    public String getMensaje() { return mensaje; }
}
